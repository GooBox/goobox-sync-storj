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
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobox.sync.storj.db.DB;
import io.storj.libstorj.Bucket;
import io.storj.libstorj.File;
import io.storj.libstorj.GetFileCallback;
import io.storj.libstorj.GetFileIdCallback;
import io.storj.libstorj.Storj;
import io.storj.libstorj.UploadFileCallback;

public class CreateCloudDirTask implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(CreateCloudDirTask.class);

    private Bucket bucket;
    private Path path;

    public CreateCloudDirTask(Bucket bucket, Path path) {
        this.bucket = bucket;
        this.path = path;
    }

    @Override
    public void run() {
        try {
            String dirName = StorjUtil.getStorjName(path);
            String dirId = getDirId(dirName);
            if (dirId != null) {
                setSynced(dirId);
            } else {
                final Path tmp = createTempDirFile();

                logger.info("Creating cloud directory {}", dirName);

                final boolean repeat[] = { true };

                while (repeat[0]) {
                    final CountDownLatch latch = new CountDownLatch(1);

                    App.getInstance().getStorj().uploadFile(bucket, dirName, tmp.toString(), new UploadFileCallback() {
                        @Override
                        public void onProgress(String filePath, double progress, long uploadedBytes, long totalBytes) {
                            String progressMessage = String.format("  %3d%% %15d/%d bytes",
                                    (int) (progress * 100), uploadedBytes, totalBytes);
                            logger.info(progressMessage);
                        }

                        @Override
                        public void onComplete(final String filePath, final File file) {
                            try {
                                DB.setSynced(file, path);
                                DB.commit();

                                deleteTempDirFile(tmp);
                            } catch (IOException e) {
                                logger.error("I/O error", e);
                            }

                            repeat[0] = false;
                            latch.countDown();
                        }

                        @Override
                        public void onError(String filePath, int code, String message) {
                            if (StorjUtil.isTemporaryError(code)) {
                                logger.error(
                                        "Creating cloud directory failed due to temporary error: {} ({}). Trying again.",
                                        message, code);
                            } else if (code == Storj.STORJ_BRIDGE_BUCKET_FILE_EXISTS) {
                                // ignore it - this happens sometimes after farmer request error
                            } else {
                                logger.error("Creating cloud directory failed: {} ({})", message, code);

                                try {
                                    DB.setUploadFailed(path);
                                    DB.commit();

                                    deleteTempDirFile(tmp);
                                } catch (IOException e) {
                                    logger.error("I/O error", e);
                                }

                                repeat[0] = false;
                            }

                            latch.countDown();
                        }
                    });

                    latch.await();

                    if (repeat[0]) {
                        // error - wait 3 seconds before trying again
                        Thread.sleep(3000);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Failed creating temp file", e);
        } catch (InterruptedException e) {
            // interrupted - stop execution
            return;
        }
    }

    private String getDirId(final String dirName) throws InterruptedException {
        final String result[] = { null };
        final boolean repeat[] = { true };

        while (repeat[0]) {
            final CountDownLatch latch = new CountDownLatch(1);

            App.getInstance().getStorj().getFileId(bucket, dirName, new GetFileIdCallback() {
                @Override
                public void onFileIdReceived(String fileName, String fileId) {
                    result[0] = fileId;
                    repeat[0] = false;
                    latch.countDown();
                }

                @Override
                public void onError(String fileName, int code, String message) {
                    if (code == Storj.HTTP_NOT_FOUND) {
                        // no such dir
                        repeat[0] = false;
                    } else if (StorjUtil.isTemporaryError(code)) {
                        logger.error(
                                "Error checking if directory with name {} exists due to temporary error: {} ({}). Trying again.",
                                dirName, message, code);
                    } else {
                        logger.error(
                                "Error checking if directory with name {} exists: {} ({})",
                                dirName, message, code);
                        repeat[0] = false;
                    }
                    latch.countDown();
                }
            });

            latch.await();

            if (repeat[0]) {
                // error - wait 3 seconds before trying again
                Thread.sleep(3000);
            }
        }

        return result[0];
    }

    private void setSynced(String dirId) throws InterruptedException {
        final boolean repeat[] = { true };

        while (repeat[0]) {
            final CountDownLatch latch = new CountDownLatch(1);

            App.getInstance().getStorj().getFile(bucket, dirId, new GetFileCallback() {
                @Override
                public void onFileReceived(File dir) {
                    try {
                        DB.setSynced(dir, path);
                        DB.commit();
                    } catch (IOException e) {
                        logger.error("I/O error", e);
                    }
                    repeat[0] = false;
                    latch.countDown();
                }

                @Override
                public void onError(String fileId, int code, String message) {
                    if (StorjUtil.isTemporaryError(code)) {
                        logger.error(
                                "Error getting directory metadata for {} due to temporary error: {} ({}). Trying again.",
                                dirId, message, code);
                    } else {
                        logger.error(
                                "Error getting directory metadata for {}: {} ({})",
                                dirId, message, code);
                        repeat[0] = false;
                    }
                    latch.countDown();
                }
            });

            latch.await();

            if (repeat[0]) {
                // error - wait 3 seconds before trying again
                Thread.sleep(3000);
            }
        }
    }

    private Path createTempDirFile() throws IOException {
        Path tmp = Files.createTempFile("storj", "dir");
        Files.write(tmp, "/".getBytes());
        return tmp;
    }

    private void deleteTempDirFile(Path tmp) {
        try {
            Files.deleteIfExists(tmp);
        } catch (IOException e) {
            logger.error("Failed deleting temp file", e);
        }
    }

}
