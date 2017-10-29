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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import io.storj.libstorj.Bucket;
import io.storj.libstorj.CreateBucketCallback;
import io.storj.libstorj.GetBucketsCallback;
import io.storj.libstorj.KeysNotFoundException;
import io.storj.libstorj.Storj;

public class App {

    private static BlockingQueue<Runnable> queue;

    public static void main(String[] args) {
        init();

        if (!checkAndCreateLocalSyncDir()) {
            System.exit(1);
        }

        Bucket gooboxBucket = checkAndCreateCloudBucket();
        if (gooboxBucket == null) {
            System.exit(1);
        }

        new FileWatcher().start();

        queue = new LinkedBlockingQueue<>();
        queue.add(new CheckCloudTask(gooboxBucket, queue));
        new TaskExecutor(queue).start();
    }

    private static void init() {
        Storj.setConfigDirectory(Utils.getConfigDir());
        Storj.setDownloadDirectory(Utils.getSyncDir());
    }

    private static boolean checkAndCreateLocalSyncDir() {
        System.out.print("Checking if local Goobox folder exists... ");
        File gooboxDir = Utils.getSyncDir();
        if (gooboxDir.exists()) {
            System.out.println("yes");
            return true;
        } else {
            System.out.print("no. ");
            boolean created = gooboxDir.mkdir();
            if (created) {
                System.out.println("Local Goobox folder created.");
                return true;
            } else {
                System.out.println("Failed creating local Goobox folder.");
                return false;
            }
        }
    }

    private static Bucket checkAndCreateCloudBucket() {
        System.out.print("Checking if cloud Goobox bucket exists... ");
        final CountDownLatch latch = new CountDownLatch(1);
        final Bucket[] result = { null };
        final Storj storj = Storj.getInstance();
        try {
            storj.getBuckets(new GetBucketsCallback() {
                @Override
                public void onError(String message) {
                    System.out.println(message);
                    latch.countDown();
                }

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
            });
        } catch (KeysNotFoundException e) {
            System.out.println(
                    "No keys found. Have your imported your keys using libstorj? Make sure you don't specify a passcode.");
        }
        return result[0];
    }
}
