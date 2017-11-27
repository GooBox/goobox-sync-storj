/*
 * Copyright (C) 2017 Kaloyan Raev
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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import io.goobox.sync.storj.db.DB;
import io.goobox.sync.storj.db.SyncFile;
import io.goobox.sync.storj.db.SyncState;
import io.storj.libstorj.Bucket;
import io.storj.libstorj.File;
import io.storj.libstorj.ListFilesCallback;
import io.storj.libstorj.Storj;

public class CheckStateTask implements Runnable {

    private Bucket gooboxBucket;
    private TaskQueue tasks;

    public CheckStateTask() {
        this.gooboxBucket = App.getInstance().getGooboxBucket();
        this.tasks = App.getInstance().getTaskQueue();
    }

    @Override
    public void run() {
        // check if there are local file operations in progress
        if (App.getInstance().getFileWatcher().isInProgress()) {
            System.out.println("Skip checking for changes - local file operations in progress...");
            return;
        }

        System.out.println("Checking for changes...");
        Storj.getInstance().listFiles(gooboxBucket, new ListFilesCallback() {
            @Override
            public void onFilesReceived(File[] files) {
                processFiles(files);

                DB.commit();

                if (tasks.isEmpty()) {
                    // Sleep some time to avoid overloading the bridge
                    tasks.add(new SleepTask());
                }
                // Add itself to the queueAdd itself to the queue
                tasks.add(CheckStateTask.this);
            }

            @Override
            public void onError(String message) {
                System.out.println("  " + message);
                // Try again
                tasks.add(CheckStateTask.this);
            }
        });
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
                            SyncFile syncFile = DB.get(file.getName());
                            if (localPath == null) {
                                if (syncFile.getState() == SyncState.FOR_DOWNLOAD
                                        && syncFile.getLocalModifiedTime() == 0) {
                                    addForDownload(file);
                                } else {
                                    setForCloudDelete(file);
                                }
                            } else {
                                boolean cloudChanged = cloudChanged(syncFile, file);
                                boolean localChanged = localChanged(syncFile, localPath);
                                if (cloudChanged && localChanged || syncFile.getState() == SyncState.FOR_DOWNLOAD) {
                                    resolveConflict(file, localPath);
                                } else if (cloudChanged) {
                                    addForDownload(file);
                                } else if (localChanged) {
                                    addForUpload(localPath);
                                } else {
                                    // no change - do nothing
                                }
                            }
                        } else {
                            if (localPath == null) {
                                addForDownload(file);
                            } else {
                                resolveConflict(file, localPath);
                            }
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                if (localPath != null) {
                    // Remove from the list of local file to avoid double processing
                    localPaths.remove(localPath);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Process local files without cloud counterpart
        for (Path path : localPaths) {
            try {
                if (DB.contains(path)) {
                    SyncFile syncFile = DB.get(path.getFileName());
                    if (syncFile.getState().isSynced()) {
                        setForLocalDelete(path);
                    } else if (syncFile.getState() == SyncState.FOR_UPLOAD) {
                        if (syncFile.getStorjCreatedTime() == 0) {
                            addForUpload(path);
                        } else {
                            setForLocalDelete(path);
                        }
                    } else if (syncFile.getState() == SyncState.FOR_LOCAL_DELETE
                            || syncFile.getState() == SyncState.FOR_DOWNLOAD) {
                        setForLocalDelete(path);
                    } else if (syncFile.getState() == SyncState.UPLOAD_FAILED
                            && syncFile.getLocalModifiedTime() != getLocalTimestamp(path)) {
                        addForUpload(path);
                    }
                } else {
                    addForUpload(path);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private File getStorjFile(String name, File[] files) {
        for (File file : files) {
            if (file.getName().toString().equals(name)) {
                return file;
            }
        }
        return null;
    }

    private List<Path> getLocalPaths() {
        List<Path> paths = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Utils.getSyncDir())) {
            for (Path path : stream) {
                paths.add(path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return paths;
    }

    private Path getLocalPath(String name, List<Path> localPaths) {
        for (Path path : localPaths) {
            if (path.getFileName().toString().equals(name)) {
                return path;
            }
        }
        return null;
    }

    private long getCloudTimestamp(File file) throws ParseException {
        return Utils.getTime(file.getCreated());
    }

    private long getLocalTimestamp(Path path) throws IOException {
        return Files.getLastModifiedTime(path).toMillis();
    }

    private boolean cloudChanged(SyncFile syncFile, File file) throws ParseException {
        return syncFile.getState() != SyncState.UPLOAD_FAILED
                && syncFile.getStorjCreatedTime() != getCloudTimestamp(file);
    }

    private boolean localChanged(SyncFile syncFile, Path path) throws IOException {
        return syncFile.getState() != SyncState.DOWNLOAD_FAILED
                && syncFile.getLocalModifiedTime() != getLocalTimestamp(path);
    }

    private void resolveConflict(File file, Path path) throws IOException, ParseException {
        // check if local and cloud file are same
        // TODO #29 check HMAC instead of size
        if (file.getSize() == Files.size(path)) {
            DB.setSynced(file, path);
        } else if (getCloudTimestamp(file) < getLocalTimestamp(path)) {
            addForUpload(file, path);
        } else {
            addForDownload(file, path);
        }
    }

    private void addForDownload(File file) {
        DB.addForDownload(file);
        tasks.add(new DownloadFileTask(gooboxBucket, file));
    }

    private void addForDownload(File file, Path path) throws IOException {
        DB.addForDownload(file, path);
        tasks.add(new DownloadFileTask(gooboxBucket, file));
    }

    private void addForUpload(Path path) throws IOException {
        DB.addForUpload(path);
        tasks.add(new UploadFileTask(gooboxBucket, path));
    }

    private void addForUpload(File file, Path path) throws IOException {
        DB.addForUpload(file, path);
        tasks.add(new UploadFileTask(gooboxBucket, path));
    }

    private void setForCloudDelete(File file) {
        DB.setForCloudDelete(file);
        tasks.add(new DeleteCloudFileTask(gooboxBucket, file));
    }

    private void setForLocalDelete(Path path) {
        DB.setForLocalDelete(path);
        tasks.add(new DeleteLocalFileTask(path));
    }

    private void cleanDeletedFilesFromDB(File[] files, List<Path> localPaths) {
        cleanDeletedFilesFromDB(SyncState.FOR_DOWNLOAD, files, localPaths);
        cleanDeletedFilesFromDB(SyncState.FOR_UPLOAD, files, localPaths);
        cleanDeletedFilesFromDB(SyncState.FOR_CLOUD_DELETE, files, localPaths);
        cleanDeletedFilesFromDB(SyncState.FOR_LOCAL_DELETE, files, localPaths);
    }

    private void cleanDeletedFilesFromDB(SyncState state, File[] files, List<Path> localPaths) {
        List<SyncFile> syncFiles = DB.getWithState(state);
        for (SyncFile syncFile : syncFiles) {
            String fileName = syncFile.getName();
            File storjFile = getStorjFile(fileName, files);
            Path localPath = getLocalPath(fileName, localPaths);
            if (storjFile == null && localPath == null) {
                DB.remove(fileName);
            }
        }
    }

}
