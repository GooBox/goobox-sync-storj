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

import io.goobox.sync.storj.db.DB;
import io.storj.libstorj.Bucket;
import io.storj.libstorj.DeleteFileCallback;
import io.storj.libstorj.File;
import io.storj.libstorj.Storj;

public class DeleteCloudFileTask implements Runnable {

    private Bucket bucket;
    private File file;

    public DeleteCloudFileTask(Bucket bucket, File file) {
        this.bucket = bucket;
        this.file = file;
    }

    @Override
    public void run() {
        System.out.println("Deleting cloud file " + file.getName() + "... ");

        Storj.getInstance().deleteFile(bucket, file, new DeleteFileCallback() {
            @Override
            public void onFileDeleted() {
                System.out.println("done");
                DB.remove(file);
                DB.commit();
            }

            @Override
            public void onError(String message) {
                System.out.println("failed. " + message);
            }
        });
    }

}
