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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
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

    public CheckStateTask(Bucket gooboxBucket, BlockingQueue<Runnable> queue) {
        this.gooboxBucket = gooboxBucket;
        this.tasks = queue;
    }

    @Override
    public void run() {
        System.out.println("Checking for changes...");
        Storj.getInstance().listFiles(gooboxBucket, new ListFilesCallback() {
            @Override
            public void onFilesReceived(File[] files) {
                List<java.io.File> localFiles = new ArrayList<>(Arrays.asList(Utils.getSyncDir().toFile().listFiles()));
                for (File file : files) {
                    java.io.File localFile = getLocalFile(file.getName(), localFiles);
                    if (DB.contains(file)) {
                        if (localFile == null) {
                            DB.setForCloudDelete(file);
                            tasks.add(new DeleteCloudFileTask(gooboxBucket, file));
                        } else {
                            try {
                                SyncFile syncFile = DB.get(file.getName());
                                boolean cloudChanged = syncFile.getState() != SyncState.UPLOAD_FAILED
                                        && syncFile.getStorjCreatedTime() != Utils.getTime(file.getCreated());
                                boolean localChanged = syncFile.getState() != SyncState.DOWNLOAD_FAILED
                                        && syncFile.getLocalModifiedTime() != localFile.lastModified();
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
                                    DB.addForUpload(localFile);
                                    tasks.add(new UploadFileTask(gooboxBucket, localFile));
                                } else {
                                    // no change - do nothing
                                }
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        if (localFile == null) {
                            DB.addForDownload(file);
                            tasks.add(new DownloadFileTask(gooboxBucket, file));
                        } else {
                            // DB.addConflict(file, localFile);
                            System.out.println("TODO conflict detected for " + file.getName());
                        }
                    }

                    if (localFile != null) {
                        // Remove from the list of local file to avoid double processing
                        localFiles.remove(localFile);
                    }
                }

                // Process local files without cloud counterpart
                for (java.io.File file : localFiles) {
                    if (DB.contains(file)) {
                        SyncFile syncFile = DB.get(file.getName());
                        if (syncFile.getState().isSynced()) {
                            DB.setForLocalDelete(file);
                            tasks.add(new DeleteLocalFileTask(file));
                        } else if (syncFile.getState() == SyncState.UPLOAD_FAILED
                                && syncFile.getLocalModifiedTime() != file.lastModified()) {
                            DB.addForUpload(file);
                            tasks.add(new UploadFileTask(gooboxBucket, file));
                        }
                    } else {
                        DB.addForUpload(file);
                        tasks.add(new UploadFileTask(gooboxBucket, file));
                    }
                }

                DB.commit();

                if (tasks.isEmpty()) {
                    // Sleep 1 minute to avoid overloading the bridge
                    System.out.println("Sleeping for 1 minute...");
                    try {
                        Thread.sleep(60000);
                    } catch (InterruptedException e) {
                        // nothing to do
                    }
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

            private java.io.File getLocalFile(String name, List<java.io.File> localFiles) {
                for (java.io.File file : localFiles) {
                    if (file.getName().equals(name)) {
                        return file;
                    }
                }
                return null;
            }
        });
    }

}
