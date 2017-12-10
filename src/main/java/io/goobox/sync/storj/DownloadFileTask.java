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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.goobox.sync.common.Utils;
import io.goobox.sync.storj.db.DB;
import io.storj.libstorj.Bucket;
import io.storj.libstorj.DownloadFileCallback;
import io.storj.libstorj.File;
import io.storj.libstorj.Storj;

public class DownloadFileTask implements Runnable {

    private Bucket bucket;
    private File file;

    public DownloadFileTask(Bucket bucket, File file) {
        this.bucket = bucket;
        this.file = file;
    }

    @Override
    public void run() {
        System.out.println("Downloading file " + file.getName() + "... ");

        try {
            Files.createDirectories(Utils.getSyncDir().resolve(file.getName()).getParent());
        } catch (IOException e) {
            System.out.println("Failed creating parent directories: " + e.getMessage());
        }

        Storj.getInstance().downloadFile(bucket, file, new DownloadFileCallback() {
            @Override
            public void onProgress(File file, double progress, long downloadedBytes, long totalBytes) {
                String progressMessage = String.format("  %3d%% %15d/%d bytes",
                        (int) (progress * 100), downloadedBytes, totalBytes);
                System.out.println(progressMessage);
            }

            @Override
            public void onComplete(File file, String localPath) {
                try {
                    DB.setSynced(file, Paths.get(localPath));
                    DB.commit();
                    System.out.println("  done.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(File file, String message) {
                Path localPath = Utils.getSyncDir().resolve(file.getName());
                try {
                    DB.setDownloadFailed(file, localPath);
                    DB.commit();
                    System.out.println("  " + message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

}
