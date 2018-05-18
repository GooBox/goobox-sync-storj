/*
 * Copyright (C) 2017-2018 Kaloyan Raev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobox.sync.storj;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobox.sync.storj.db.DB;
import io.goobox.sync.storj.db.SyncFile;
import io.goobox.sync.storj.db.SyncState;
import io.storj.libstorj.Bucket;
import io.storj.libstorj.File;
import io.storj.libstorj.ListFilesCallback;

public class CheckStateTask implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(CheckStateTask.class);

    private Bucket gooboxBucket;
    private TaskQueue tasks;
    private static boolean idle;

    public CheckStateTask() {
        this.gooboxBucket = App.getInstance().getGooboxBucket();
        this.tasks = App.getInstance().getTaskQueue();
    }

    @Override
    public void run() {
        // check if there are local file operations in progress
        if (App.getInstance().getFileWatcher().isInProgress()) {
            logger.info("Skip checking for changes - local file operations in progress");
            return;
        }

        logger.info("Checking for changes");

        final CountDownLatch latch = new CountDownLatch(1);

        App.getInstance().getStorj().listFiles(gooboxBucket, new ListFilesCallback() {
            @Override
            public void onFilesReceived(String bucketId, File[] files) {
                processFiles(files);

                DB.commit();

                if (tasks.isEmpty()) {
                    setIdle();

                    // Sleep some time to avoid overloading the bridge
                    tasks.add(new SleepTask());
                }
                // Add itself to the queueAdd itself to the queue
                tasks.add(CheckStateTask.this);
                latch.countDown();
            }

            @Override
            public void onError(String bucketId, int code, String message) {
                logger.error("{} ({})", message, code);
                // wait 3 seconds before trying again
                try {
                    Thread.sleep(3000);
                    tasks.add(CheckStateTask.this);
                } catch (InterruptedException e) {
                    // interrupted - stop execution
                    return;
                } finally {
                    latch.countDown();
                }
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            // interrupted - stop execution
            return;
        }
    }

    private void processFiles(File[] files) {
        List<Path> localPaths = getLocalPaths();

        cleanDeletedFilesFromDB(files, localPaths);

        for (File file : files) {
            try {
                Path localPath = getLocalPath(file.getName(), localPaths);
                // process only files encrypted with the current key
                if (file.isDecrypted()) {
                    try {
                        if (DB.contains(file)) {
                            SyncFile syncFile = DB.get(file);
                            boolean cloudChanged = cloudChanged(syncFile, file);
                            if (localPath == null) {
                                if (cloudChanged || syncFile.getState() == SyncState.FOR_DOWNLOAD
                                        && syncFile.getLocalModifiedTime() == 0) {
                                    addForDownload(file);
                                } else if (syncFile.getState() == SyncState.DOWNLOAD_FAILED) {
                                    if (syncFile.getLocalModifiedTime() == 0) {
                                        DB.setDownloadFailed(file, localPath);
                                    } else {
                                        addForDownload(file);
                                    }
                                } else {
                                    setForCloudDelete(file);
                                }
                            } else {
                                boolean localChanged = localChanged(syncFile, localPath);
                                if (cloudChanged && localChanged || syncFile.getState() == SyncState.FOR_DOWNLOAD) {
                                    resolveConflict(file, localPath);
                                } else if (cloudChanged) {
                                    addForDownload(file, localPath);
                                } else if (localChanged) {
                                    addForUpload(file, localPath);
                                } else {
                                    // no change - do nothing
                                }
                            }
                        } else {
                            if (localPath == null) {
                                if (file.isDirectory()) {
                                    addForLocalCreateDir(file);
                                } else {
                                    addForDownload(file);
                                }
                            } else {
                                resolveConflict(file, localPath);
                            }
                        }
                    } catch (ParseException e) {
                        logger.error("Cannot parse timestamp", e);
                    }
                }

                if (localPath != null) {
                    // Remove from the list of local file to avoid double processing
                    localPaths.remove(localPath);
                }
            } catch (IOException e) {
                logger.error("I/O error", e);
            }
        }

        // Process local files without cloud counterpart
        for (Path path : localPaths) {
            try {
                if (DB.contains(path)) {
                    SyncFile syncFile = DB.get(path);
                    if (localChanged(syncFile, path)
                            || syncFile.getState() == SyncState.FOR_UPLOAD && syncFile.getStorjCreatedTime() == 0) {
                        addForUpload(path);
                    } else if (syncFile.getState() == SyncState.UPLOAD_FAILED && syncFile.getStorjCreatedTime() == 0) {
                        DB.setUploadFailed(path);
                    } else {
                        setForLocalDelete(path);
                    }
                } else if (!StorjUtil.isExcluded(path)) {
                    if (Files.isDirectory(path)) {
                        addForCloudCreateDir(path);
                    } else {
                        addForUpload(path);
                    }
                }
            } catch (IOException e) {
                logger.error("I/O error", e);
            }
        }
    }

    private File getStorjFile(String name, File[] files) {
        for (File file : files) {
            if (DB.getName(file).toString().equals(name)) {
                return file;
            }
        }
        return null;
    }

    private List<Path> getLocalPaths() {
        Deque<Path> stack = new ArrayDeque<Path>();
        List<Path> paths = new ArrayList<>();

        stack.push(App.getInstance().getSyncDir());

        while (!stack.isEmpty()) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(stack.pop())) {
                for (Path path : stream) {
                    if (Files.isDirectory(path)) {
                        stack.push(path);
                    }
                    paths.add(path);
                }
            } catch (IOException e) {
                logger.error("I/O error", e);
            }
        }

        return paths;
    }

    private Path getLocalPath(String name, List<Path> localPaths) {
        for (Path path : localPaths) {
            if (StorjUtil.getStorjPath(path).equals(Paths.get(name))) {
                return path;
            }
        }
        return null;
    }

    private long getCloudTimestamp(File file) throws ParseException {
        return StorjUtil.getTime(file.getCreated());
    }

    private long getLocalTimestamp(Path path) throws IOException {
        return Files.getLastModifiedTime(path).toMillis();
    }

    private boolean cloudChanged(SyncFile syncFile, File file) throws ParseException {
        return !file.isDirectory() && syncFile.getStorjCreatedTime() != getCloudTimestamp(file);
    }

    private boolean localChanged(SyncFile syncFile, Path path) throws IOException {
        return !Files.isDirectory(path) && syncFile.getLocalModifiedTime() != getLocalTimestamp(path);
    }

    private void resolveConflict(File file, Path path) throws IOException, ParseException {
        // check if local and cloud file are same
        // TODO #29 check HMAC instead of size
        if (file.isDirectory() && Files.isDirectory(path)) {
            DB.setSynced(file, path);
        } else if (file.getSize() == Files.size(path)) {
            DB.setSynced(file, path);
        } else if (getCloudTimestamp(file) < getLocalTimestamp(path)) {
            addForUpload(file, path);
        } else {
            addForDownload(file, path);
        }
    }

    private void addForDownload(File file) {
        DB.addForDownload(file);
        setSynchronizing();
        tasks.add(new DownloadFileTask(gooboxBucket, file));
    }

    private void addForDownload(File file, Path path) throws IOException {
        DB.addForDownload(file, path);
        setSynchronizing();
        tasks.add(new DownloadFileTask(gooboxBucket, file));
    }

    private void addForUpload(Path path) throws IOException {
        DB.addForUpload(path);
        setSynchronizing();
        tasks.add(new UploadFileTask(gooboxBucket, path));
    }

    private void addForUpload(File file, Path path) throws IOException {
        DB.addForUpload(file, path);
        setSynchronizing();
        tasks.add(new UploadFileTask(gooboxBucket, path));
    }

    private void setForCloudDelete(File file) {
        DB.setForCloudDelete(file);
        setSynchronizing();
        tasks.add(new DeleteCloudFileTask(gooboxBucket, file));
    }

    private void setForLocalDelete(Path path) throws IOException {
        DB.setForLocalDelete(path);
        tasks.add(new DeleteLocalFileTask(path));
    }

    private void addForLocalCreateDir(File file) throws IOException {
        DB.addForLocalCreateDir(file);
        tasks.add(new CreateLocalDirTask(file));
    }

    private void addForCloudCreateDir(Path path) throws IOException {
        DB.addForCloudCreateDir(path);
        setSynchronizing();
        tasks.add(new CreateCloudDirTask(gooboxBucket, path));
    }

    private void cleanDeletedFilesFromDB(File[] files, List<Path> localPaths) {
        for (SyncFile syncFile : DB.all()) {
            String fileName = syncFile.getName();
            File storjFile = getStorjFile(fileName, files);
            Path localPath = getLocalPath(fileName, localPaths);
            if (storjFile == null && localPath == null) {
                DB.remove(fileName);
            }
        }
    }

    private void setSynchronizing() {
        if (idle) {
            App.getInstance().getIpcExecutor().sendSyncEvent();
            App.getInstance().getOverlayHelper().setSynchronizing();
            idle = false;
        }
    }

    private void setIdle() {
        if (!idle) {
            App.getInstance().getIpcExecutor().sendIdleEvent();
            App.getInstance().getOverlayHelper().setOK();
            idle = true;
        }
    }

}
