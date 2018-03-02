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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobox.sync.storj.db.DB;
import io.storj.libstorj.Bucket;
import io.storj.libstorj.DownloadFileCallback;
import io.storj.libstorj.File;

public class DownloadFileTask implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(DownloadFileTask.class);

    private Bucket bucket;
    private File file;

    public DownloadFileTask(Bucket bucket, File file) {
        this.bucket = bucket;
        this.file = file;
    }

    @Override
    public void run() {
        logger.info("Downloading file {}", file.getName());

        try {
            Files.createDirectories(App.getInstance().getSyncDir().resolve(file.getName()).getParent());
        } catch (IOException e) {
            logger.error("Failed creating parent directories", e);
            return;
        }

        final boolean repeat[] = { true };

        while (repeat[0]) {
            final CountDownLatch latch = new CountDownLatch(1);

            App.getInstance().getStorj().downloadFile(bucket, file, new DownloadFileCallback() {
                @Override
                public void onProgress(String fileId, double progress, long downloadedBytes, long totalBytes) {
                    String progressMessage = String.format("  %3d%% %15d/%d bytes",
                            (int) (progress * 100), downloadedBytes, totalBytes);
                    logger.info(progressMessage);
                }

                @Override
                public void onComplete(String fileId, String localPath) {
                    try {
                        DB.setSynced(file, Paths.get(localPath));
                        DB.commit();
                        logger.info("Download completed");
                    } catch (IOException e) {
                        logger.error("I/O error", e);
                    }

                    repeat[0] = false;
                    latch.countDown();
                }

                @Override
                public void onError(String fileId, int code, String message) {
                    if (StorjUtil.isTemporaryError(code)) {
                        logger.error("Download failed due to temporary error: {} ({}). Trying again.", message, code);
                    } else {
                        Path localPath = App.getInstance().getSyncDir().resolve(file.getName());
                        try {
                            DB.setDownloadFailed(file, localPath);
                            DB.commit();
                            logger.error("Download failed: {} ({})", message, code);
                        } catch (IOException e) {
                            logger.error("I/O error", e);
                        }

                        repeat[0] = false;
                    }

                    latch.countDown();
                }
            });

            try {
                latch.await();

                if (repeat[0]) {
                    // error - wait 3 seconds before trying again
                    Thread.sleep(3000);
                }
            } catch (InterruptedException e) {
                // interrupted - stop execution
                return;
            }
        }
    }

}
