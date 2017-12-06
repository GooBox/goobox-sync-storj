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
import io.storj.libstorj.File;

public class CreateLocalDirTask implements Runnable {

    private File storjDir;

    public CreateLocalDirTask(File storjDir) {
        this.storjDir = storjDir;
    }

    @Override
    public void run() {
        Path relPath = Utils.getSyncDir().resolve(storjDir.getName());
        System.out.print("Creating local directory " + relPath + "... ");

        try {
            Path localDir = Files.createDirectories(relPath);
            System.out.println("done");
            DB.setSynced(storjDir, localDir);
            DB.commit();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

}
