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

import io.storj.libstorj.Bucket;
import io.storj.libstorj.GetBucketsCallback;
import io.storj.libstorj.KeysNotFoundException;
import io.storj.libstorj.Storj;

public class App {
    public static void main(String[] args) {
        Storj.appDir = new java.io.File(System.getProperty("user.home"), ".storj").getAbsolutePath();
        Storj storj = Storj.getInstance();
        try {
            storj.getBuckets(new GetBucketsCallback() {
                @Override
                public void onError(String message) {
                    System.out.println(message);
                }

                @Override
                public void onBucketsReceived(Bucket[] buckets) {
                    String user = Storj.getInstance().getKeys("").getUser();
                    System.out.println(buckets.length + " buckets in " + user);
                    System.out.println("---------------------------------------------");
                    for (Bucket bucket : buckets) {
                        System.out.println(bucket.getId() + " " + bucket.getName());
                    }
                }
            });
        } catch (KeysNotFoundException e) {
            System.out.println("No keys found. Have your imported your keys using libstorj? Make sure you don't specify a passcode.");
        }
    }
}
