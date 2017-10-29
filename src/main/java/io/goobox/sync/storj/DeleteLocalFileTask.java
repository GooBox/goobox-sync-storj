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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

public class DeleteLocalFileTask implements Runnable {

    private File file;
    private Set<Path> syncingFiles;

    public DeleteLocalFileTask(File file, Set<Path> syncingFiles) {
        this.file = file;
        this.syncingFiles = syncingFiles;
    }

    @Override
    public void run() {
        System.out.print("Deleting local file " + file.getName() + "... ");

        Path path = Paths.get(file.getName());
        syncingFiles.add(path);

        try {
            boolean success = file.delete();
            if (success) {
                System.out.println("done");
            } else {
                syncingFiles.remove(path);
                System.out.println("failed");
            }
        } catch (Exception e) {
            syncingFiles.remove(path);
            System.out.println(e.getMessage());
        }
    }

}
