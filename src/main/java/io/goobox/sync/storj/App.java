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

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import io.storj.libstorj.Bucket;
import io.storj.libstorj.CreateBucketCallback;
import io.storj.libstorj.GetBucketsCallback;
import io.storj.libstorj.KeysNotFoundException;
import io.storj.libstorj.Storj;

public class App {

    public static void main(String[] args) {
        new App().init();
    }

    private void init() {
        Storj.setConfigDirectory(Utils.getStorjConfigDir().toFile());
        Storj.setDownloadDirectory(Utils.getSyncDir().toFile());

        if (!checkAndCreateSyncDir()) {
            System.exit(1);
        }

        if (!checkAndCreateConfigDir()) {
            System.exit(1);
        }

        Bucket gooboxBucket = checkAndCreateCloudBucket();
        if (gooboxBucket == null) {
            System.exit(1);
        }

        BlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();
        tasks.add(new CheckStateTask(gooboxBucket, tasks));

        new FileWatcher().start();
        new TaskExecutor(tasks).start();
    }

    private boolean checkAndCreateSyncDir() {
        System.out.print("Checking if local Goobox folder exists... ");
        return checkAndCreateFolder(Utils.getSyncDir());
    }

    private boolean checkAndCreateConfigDir() {
        System.out.print("Checking if Goobox config folder exists... ");
        return checkAndCreateFolder(Utils.getConfigDir());
    }

    private boolean checkAndCreateFolder(Path path) {
        File dir = path.toFile();
        if (dir.exists()) {
            System.out.println("yes");
            return true;
        } else {
            System.out.print("no. ");
            boolean created = dir.mkdir();
            if (created) {
                System.out.println("Folder created.");
                return true;
            } else {
                System.out.println("Failed creating folder.");
                return false;
            }
        }
    }

    private Bucket checkAndCreateCloudBucket() {
        System.out.print("Checking if cloud Goobox bucket exists... ");
        final Bucket[] result = { null };
        final Storj storj = Storj.getInstance();

        try {
            while (result[0] == null) {
                final CountDownLatch latch = new CountDownLatch(1);

                storj.getBuckets(new GetBucketsCallback() {
                    @Override
                    public void onBucketsReceived(Bucket[] buckets) {
                        for (Bucket bucket : buckets) {
                            if ("Goobox".equals(bucket.getName())) {
                                result[0] = bucket;
                                System.out.println("yes");
                                latch.countDown();
                                return;
                            }
                        }

                        System.out.print("no. ");
                        storj.createBucket("Goobox", new CreateBucketCallback() {
                            @Override
                            public void onError(String message) {
                                System.out.println("Failed creating cloud Goobox bucket.");
                                latch.countDown();
                            }

                            @Override
                            public void onBucketCreated(Bucket bucket) {
                                System.out.println("Cloud Goobox bucket created.");
                                result[0] = bucket;
                                latch.countDown();
                            }
                        });
                    }

                    @Override
                    public void onError(String message) {
                        System.out.println(message);
                        latch.countDown();
                    }
                });

                latch.await();

                if (result[0] == null) {
                    // error - wait 3 seconds before trying again
                    Thread.sleep(3000);
                }
            }
        } catch (KeysNotFoundException e) {
            System.out.println(
                    "No keys found. Have your imported your keys using libstorj? Make sure you don't specify a passcode.");
        } catch (InterruptedException e) {
            // do nothing
        }

        return result[0];
    }
}
