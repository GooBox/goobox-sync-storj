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

import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobox.sync.storj.db.DB;
import io.storj.libstorj.Bucket;
import io.storj.libstorj.DeleteFileCallback;
import io.storj.libstorj.File;

public class DeleteCloudFileTask implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(DeleteCloudFileTask.class);

    private Bucket bucket;
    private File file;

    public DeleteCloudFileTask(Bucket bucket, File file) {
        this.bucket = bucket;
        this.file = file;
    }

    @Override
    public void run() {
        logger.info("Deleting cloud {}", file.getName());

        final CountDownLatch latch = new CountDownLatch(1);

        App.getInstance().getStorj().deleteFile(bucket, file, new DeleteFileCallback() {
            @Override
            public void onFileDeleted(String fileId) {
                logger.info("Cloud deletion successful");
                DB.remove(file);
                DB.commit();
                latch.countDown();
            }

            @Override
            public void onError(String fileId, int code, String message) {
                logger.error("Failed deleting on cloud: {} ({})", message, code);
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            // interrupted - stop execution
            return;
        }
    }

}
