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
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobox.sync.storj.db.DB;

public class DeleteLocalFileTask implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(DeleteLocalFileTask.class);

    private Path path;

    public DeleteLocalFileTask(Path path) {
        this.path = path;
    }

    @Override
    public void run() {
        logger.info("Deleting local {}", StorjUtil.getStorjName(path));

        try {
            Files.deleteIfExists(path);
            DB.remove(path);
            deleteParentIfEmpty();
            logger.info("Local deletetion successful");
        } catch (DirectoryNotEmptyException e) {
            DB.remove(path);
        } catch (Exception e) {
            logger.error("Failed deleting locally", e);
        } finally {
            DB.commit();
        }
    }

    private void deleteParentIfEmpty() {
        Path parent = path.getParent();
        if (!parent.equals(App.getInstance().getSyncDir())) {
            try {
                Files.deleteIfExists(parent);
                DB.remove(parent);
            } catch (IOException e) {
                // do nothing - most probably the dir is not empty
            }
        }

    }

}
