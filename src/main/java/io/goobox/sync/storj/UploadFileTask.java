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

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

import io.goobox.sync.storj.db.DB;
import io.storj.libstorj.Bucket;
import io.storj.libstorj.DeleteFileCallback;
import io.storj.libstorj.File;
import io.storj.libstorj.ListFilesCallback;
import io.storj.libstorj.Storj;
import io.storj.libstorj.UploadFileCallback;

public class UploadFileTask implements Runnable {

    private Bucket bucket;
    private Path path;

    public UploadFileTask(Bucket bucket, Path path) {
        this.bucket = bucket;
        this.path = path;
    }

    @Override
    public void run() {
        try {
            deleteIfExisting();
        } catch (InterruptedException e) {
            // interrupted - stop execution
            return;
        }

        System.out.println("Uploading file " + path.getFileName() + "... ");

        Storj.getInstance().uploadFile(bucket, path.toAbsolutePath().toString(), new UploadFileCallback() {
            @Override
            public void onProgress(String filePath, double progress, long uploadedBytes, long totalBytes) {
                String progressMessage = String.format("  %3d%% %15d/%d bytes",
                        (int) (progress * 100), uploadedBytes, totalBytes);
                System.out.println(progressMessage);
            }

            @Override
            public void onComplete(final String filePath, final String fileId) {
                Storj.getInstance().listFiles(bucket, new ListFilesCallback() {
                    @Override
                    public void onFilesReceived(File[] files) {
                        File storjFile = null;
                        for (File f : files) {
                            if (fileId.equals(f.getId())) {
                                storjFile = f;
                            }
                        }

                        if (storjFile != null) {
                            try {
                                DB.setSynced(storjFile, path);
                                DB.commit();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            System.out.println("Cannot find uploaded file with id " + fileId);
                        }
                    }

                    @Override
                    public void onError(String message) {
                        System.out.println("Failed getting info for uploaded file with id " + fileId);
                    }
                });
                System.out.println("  done.");
            }

            @Override
            public void onError(String filePath, String message) {
                DB.setUploadFailed(path);
                DB.commit();
                System.out.println("  " + message);
            }
        });
    }

    private void deleteIfExisting() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        Storj.getInstance().listFiles(bucket, new ListFilesCallback() {
            @Override
            public void onFilesReceived(File[] files) {
                String fileName = path.getFileName().toString();
                File storjFile = null;
                for (File f : files) {
                    if (fileName.equals(f.getName())) {
                        storjFile = f;
                    }
                }

                if (storjFile == null) {
                    // no file to delete
                    latch.countDown();
                } else {
                    System.out.print("Deleting old version of " + fileName + " on the cloud... ");

                    Storj.getInstance().deleteFile(bucket, storjFile, new DeleteFileCallback() {
                        @Override
                        public void onFileDeleted() {
                            System.out.println("done");
                            latch.countDown();
                        }

                        @Override
                        public void onError(String message) {
                            System.out.println(message);
                            latch.countDown();
                        }
                    });
                }
            }

            @Override
            public void onError(String message) {
                System.out.println(String.format("Error checkging if file with name %s exists: %s",
                        path.getFileName(), message));
                latch.countDown();
            }
        });

        latch.await();
    }

}
