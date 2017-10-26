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
import java.util.concurrent.CountDownLatch;

import io.storj.libstorj.Bucket;
import io.storj.libstorj.CreateBucketCallback;
import io.storj.libstorj.GetBucketsCallback;
import io.storj.libstorj.KeysNotFoundException;
import io.storj.libstorj.Storj;

public class App {
    public static void main(String[] args) {
        init();
        checkAndCreateLocalFolder();
        checkAndCreateCloudBucket();
    }

    private static void init() {
        Storj.appDir = new java.io.File(Utils.getHomeFolder(), ".storj").getAbsolutePath();
    }

    private static void checkAndCreateLocalFolder() {
        System.out.print("Checking if local Goobox folder exists... ");
        File gooboxFolder = new File(Utils.getHomeFolder(), "Goobox");
        if (gooboxFolder.exists()) {
            System.out.println("yes");
        } else {
            System.out.print("no. ");
            boolean created = gooboxFolder.mkdir();
            if (created) {
                System.out.println("Local Goobox folder created.");
            } else {
                System.out.println("Failed creating local Goobox folder.");
            }
        }
    }

    private static void checkAndCreateCloudBucket() {
        System.out.print("Checking if cloud Goobox bucket exists... ");
        final CountDownLatch latch = new CountDownLatch(1);
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
                    boolean exists = false;
                    for (Bucket bucket : buckets) {
                        if ("Goobox".equals(bucket.getName())) {
                            exists = true;
                            break;
                        }
                    }

                    if (exists) {
                        System.out.println("yes");
                        latch.countDown();
                    } else {
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
                                latch.countDown();
                            }
                        });
                    }
                }
            });
        } catch (KeysNotFoundException e) {
            System.out.println(
                    "No keys found. Have your imported your keys using libstorj? Make sure you don't specify a passcode.");
        }
    }
}
