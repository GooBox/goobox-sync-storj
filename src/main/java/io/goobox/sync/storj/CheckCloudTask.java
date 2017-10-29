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

import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import io.storj.libstorj.Bucket;
import io.storj.libstorj.File;
import io.storj.libstorj.ListFilesCallback;
import io.storj.libstorj.Storj;

public class CheckCloudTask implements Runnable {

    private Bucket gooboxBucket;
    private BlockingQueue<Runnable> tasks;
    private Set<Path> syncingFiles;

    public CheckCloudTask(Bucket gooboxBucket, BlockingQueue<Runnable> queue, Set<Path> syncingFiles) {
        this.gooboxBucket = gooboxBucket;
        this.tasks = queue;
        this.syncingFiles = syncingFiles;
    }

    @Override
    public void run() {
        System.out.println("Checking Storj Bridge for changes...");
        Storj.getInstance().listFiles(gooboxBucket, new ListFilesCallback() {
            @Override
            public void onFilesReceived(File[] files) {
                List<java.io.File> localFiles = new ArrayList<>(Arrays.asList(Utils.getSyncDir().toFile().listFiles()));
                for (File file : files) {
                    java.io.File localFile = getLocalFile(file.getName(), localFiles);
                    if (localFile == null) {
                        tasks.add(new DownloadFileTask(gooboxBucket, file, syncingFiles));
                    } else {
                        // Remove from the list of local file to avoid delete it
                        localFiles.remove(localFile);
                        try {
                            long cloudTime = Utils.getTime(file.getCreated());
                            long localTime = localFile.lastModified();
                            if (cloudTime > localTime) {
                                tasks.add(new DownloadFileTask(gooboxBucket, file, syncingFiles));
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                }

                // Delete all local files without cloud counterpart
                for (java.io.File file : localFiles) {
                    tasks.add(new DeleteLocalFileTask(file, syncingFiles));
                }

                if (tasks.isEmpty()) {
                    // Sleep 1 minute to avoid overloading the bridge
                    System.out.println("Sleeping for 1 minute...");
                    try {
                        Thread.sleep(60000);
                    } catch (InterruptedException e) {
                        // nothing to do
                    }
                }
                // Add itself to the queue
                tasks.add(CheckCloudTask.this);
            }

            @Override
            public void onError(String message) {
                System.out.println(message);
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
