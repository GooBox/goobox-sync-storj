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
        System.out.print("Deleting local file " + path.getFileName() + "... ");

        try {
            boolean success = Files.deleteIfExists(path);
            if (success) {
                System.out.println("done");
                DB.remove(path);
                DB.commit();
            } else {
                System.out.println("failed");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

}
