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
import io.storj.libstorj.ListFilesCallback;
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
            File storjDir = getCloudDir(dirName);
            if (storjDir != null) {
                DB.setSynced(storjDir, path);
                DB.commit();
            } else {
                final Path tmp = createTempDirFile();

                logger.info("Creating cloud directory {}", dirName);

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
                            deleteTempDirFile(tmp);

                            DB.setSynced(file, path);
                            DB.commit();
                        } catch (IOException e) {
                            logger.error("I/O error", e);
                        }
                    }

                    @Override
                    public void onError(String filePath, String message) {
                        try {
                            deleteTempDirFile(tmp);

                            DB.setUploadFailed(path);
                            DB.commit();

                            logger.error(message);
                        } catch (IOException e) {
                            logger.error("I/O error", e);
                        }
                    }
                });
            }
        } catch (IOException e) {
            logger.error("Failed creating temp file", e);
        } catch (InterruptedException e) {
            // interrupted - stop execution
            return;
        }
    }

    private File getCloudDir(final String dirName) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final File result[] = { null };
        final boolean repeat[] = { true };

        while (repeat[0]) {
            App.getInstance().getStorj().listFiles(bucket, new ListFilesCallback() {
                @Override
                public void onFilesReceived(File[] files) {
                    for (File f : files) {
                        if (dirName.equals(f.getName())) {
                            result[0] = f;
                        }
                    }

                    repeat[0] = false;
                    latch.countDown();
                }

                @Override
                public void onError(String message) {
                    logger.error("Error checking if directory with name {} exists: {}. Trying again.", dirName,
                            message);
                    latch.countDown();
                }
            });
        }

        latch.await();

        return result[0];
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
