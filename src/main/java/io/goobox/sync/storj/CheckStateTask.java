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
import java.util.concurrent.BlockingQueue;

import io.goobox.sync.storj.db.DB;
import io.goobox.sync.storj.db.SyncFile;
import io.goobox.sync.storj.db.SyncState;
import io.storj.libstorj.Bucket;
import io.storj.libstorj.File;
import io.storj.libstorj.ListFilesCallback;
import io.storj.libstorj.Storj;

public class CheckStateTask implements Runnable {

    private Bucket gooboxBucket;
    private BlockingQueue<Runnable> tasks;

    public CheckStateTask(Bucket gooboxBucket, BlockingQueue<Runnable> tasks) {
        this.gooboxBucket = gooboxBucket;
        this.tasks = tasks;
    }

    @Override
    public void run() {
        System.out.println("Checking for changes...");
        Storj.getInstance().listFiles(gooboxBucket, new ListFilesCallback() {
            @Override
            public void onFilesReceived(File[] files) {
                List<Path> localPaths = getLocalPaths();
                for (File file : files) {
                    try {
                        Path localPath = getLocalPath(file.getName(), localPaths);
                        if (DB.contains(file)) {
                            if (localPath == null) {
                                DB.setForCloudDelete(file);
                                tasks.add(new DeleteCloudFileTask(gooboxBucket, file));
                            } else {
                                try {
                                    SyncFile syncFile = DB.get(file.getName());
                                    boolean cloudChanged = syncFile.getState() != SyncState.UPLOAD_FAILED
                                            && syncFile.getStorjCreatedTime() != Utils.getTime(file.getCreated());
                                    boolean localChanged = syncFile.getState() != SyncState.DOWNLOAD_FAILED
                                            && syncFile.getLocalModifiedTime() != Files.getLastModifiedTime(localPath).toMillis();
                                    if (cloudChanged && localChanged) {
                                        // conflict
                                        // DB.addConflict(file, localFile);
                                        System.out.println("TODO conflict detected for " + file.getName());
                                    } else if (cloudChanged) {
                                        // download
                                        DB.addForDownload(file);
                                        tasks.add(new DownloadFileTask(gooboxBucket, file));
                                    } else if (localChanged) {
                                        // upload
                                        DB.addForUpload(localPath);
                                        tasks.add(new UploadFileTask(gooboxBucket, localPath));
                                    } else {
                                        // no change - do nothing
                                    }
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                            }
                        } else {
                            if (localPath == null) {
                                DB.addForDownload(file);
                                tasks.add(new DownloadFileTask(gooboxBucket, file));
                            } else {
                                // DB.addConflict(file, localFile);
                                System.out.println("TODO conflict detected for " + file.getName());
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
                                DB.setForLocalDelete(path);
                                tasks.add(new DeleteLocalFileTask(path));
                            } else if (syncFile.getState() == SyncState.UPLOAD_FAILED
                                    && syncFile.getLocalModifiedTime() != Files.getLastModifiedTime(path).toMillis()) {
                                DB.addForUpload(path);
                                tasks.add(new UploadFileTask(gooboxBucket, path));
                            }
                        } else {
                            DB.addForUpload(path);
                            tasks.add(new UploadFileTask(gooboxBucket, path));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

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
        });
    }

}
