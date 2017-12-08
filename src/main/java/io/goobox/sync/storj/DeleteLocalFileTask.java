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
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.goobox.sync.storj.db.DB;

public class DeleteLocalFileTask implements Runnable {

    private Path path;

    public DeleteLocalFileTask(Path path) {
        this.path = path;
    }

    @Override
    public void run() {
        System.out.printf("Deleting local %s %s...\n",
                Files.isDirectory(path) ? "directory" : "file",
                Utils.getStorjName(path));

        try {
            Files.deleteIfExists(path);
            DB.remove(path);
            deleteParentIfEmpty();
            System.out.println("done");
        } catch (DirectoryNotEmptyException e) {
            DB.remove(path);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            DB.commit();
        }
    }

    private void deleteParentIfEmpty() {
        Path parent = path.getParent();
        if (!parent.equals(Utils.getSyncDir())) {
            try {
                Files.deleteIfExists(parent);
                DB.remove(parent);
            } catch (IOException e) {
                // do nothing - most probably the dir is not empty
            }
        }

    }

}
