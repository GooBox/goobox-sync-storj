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
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobox.sync.common.Utils;
import io.goobox.sync.storj.db.DB;
import io.storj.libstorj.Bucket;
import io.storj.libstorj.DeleteFileCallback;
import io.storj.libstorj.File;
import io.storj.libstorj.GetFileIdCallback;
import io.storj.libstorj.Storj;
import io.storj.libstorj.UploadFileCallback;

public class UploadFileTask implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(UploadFileTask.class);

    private Bucket bucket;
    private Path path;
    private String fileName;
    private long uploadState;

    public UploadFileTask(Bucket bucket, Path path) {
        this.bucket = bucket;
        this.path = path;
        this.fileName = StorjUtil.getStorjName(path);
    }

    @Override
    public void run() {
        try {
            deleteIfExisting();
        } catch (InterruptedException e) {
            // interrupted - stop execution
            return;
        }

        logger.info("Uploading file {}", fileName);

        final boolean repeat[] = { true };
        Path tmpDir = null;
        Path tmpPath = null;

        try {
            tmpDir = Files.createTempDirectory(Utils.getSyncDir(), ".~");
            tmpPath = Files.createTempFile(tmpDir, "file", ".tmp");
            Files.copy(path, tmpPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e1) {
            logger.info("file {} removed during temporary file creation?", path);
        }

        while (repeat[0]) {
            final CountDownLatch latch = new CountDownLatch(1);

            uploadState = App.getInstance().getStorj().uploadFile(bucket, fileName, tmpPath.toString(), new UploadFileCallback() {
                @Override
                public void onProgress(String filePath, double progress, long uploadedBytes, long totalBytes) {
                    String progressMessage = String.format("  %3d%% %15d/%d bytes",
                            (int) (progress * 100), uploadedBytes, totalBytes);
                    logger.info(progressMessage);

                    // user might have delete large file during uploading. so we check this situation to ensure canceling is possible
                    if (!Files.exists(path)) {
                        logger.info("File {} does not exist anymore (renamed, deleted or moved). Canceling upload.", path);
                        App.getInstance().getStorj().cancelUpload(uploadState);
                    }
                }

                @Override
                public void onComplete(String filePath, File file) {
                    try {
                        DB.setSynced(file, path);
                        DB.commit();
                        logger.info("Upload completed");
                    } catch (IOException e) {
                        logger.error("I/O error", e);
                    }

                    repeat[0] = false;
                    latch.countDown();
                }

                @Override
                public void onError(String filePath, int code, String message) {
                    if (StorjUtil.isTemporaryError(code)) {
                        logger.error("Upload failed due to temporary error: {} ({}). Trying again.", message, code);
                    } else {
                        try {
                            DB.setUploadFailed(path);
                            DB.commit();
                            logger.error("Upload failed: {} ({})", message, code);
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
            } finally {
                try {
                    Files.deleteIfExists(tmpPath);
                    Files.deleteIfExists(tmpDir);
                } catch (IOException e) { }
            }
        }
    }

    private void deleteIfExisting() throws InterruptedException {
        final boolean repeat[] = { true };

        while (repeat[0]) {
            final CountDownLatch latch = new CountDownLatch(1);

            App.getInstance().getStorj().getFileId(bucket, fileName, new GetFileIdCallback() {
                @Override
                public void onFileIdReceived(String fileName, String fileId) {
                    logger.info("Deleting old version of {} on the cloud", fileName);

                    App.getInstance().getStorj().deleteFile(bucket.getId(), fileId, new DeleteFileCallback() {
                        @Override
                        public void onFileDeleted(String fileId) {
                            logger.info("Old version of {} deleted", fileName);
                            repeat[0] = false;
                            latch.countDown();
                        }

                        @Override
                        public void onError(String fileId, int code, String message) {
                            if (StorjUtil.isTemporaryError(code)) {
                                logger.error(
                                        "Failed deleting old version due to temporary error: {} ({}). Trying again.",
                                        message, code);
                            } else {
                                logger.error("Failed deleting old version: {} ({})", message, code);
                                repeat[0] = false;
                            }
                            latch.countDown();
                        }
                    });
                }

                @Override
                public void onError(String fileName, int code, String message) {
                    if (code == Storj.HTTP_NOT_FOUND) {
                        // no file to delete
                        repeat[0] = false;
                    } else if (StorjUtil.isTemporaryError(code)) {
                        logger.error(
                                "Error checking if file with name {} exists due to temporary error: {} ({}). Trying again.",
                                fileName, message, code);
                    } else {
                        logger.error("Error checking if file with name {} exists: {} ({})", fileName, message, code);
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

}
