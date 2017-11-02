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

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

public class FileWatcher extends Thread {

    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    @Override
    public void run() {
        try {
            WatchService watcher = FileSystems.getDefault().newWatchService();
            Utils.getSyncDir().register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

            WatchKey key;
            while ((key = watcher.take()) != null) {
                for (WatchEvent<?> ev : key.pollEvents()) {
                    WatchEvent<Path> event = cast(ev);
                    Path path = event.context();

                    if (!isHidden(path)) {
//                        System.out.println("Event kind:" + event.kind() + ". Count: " + event.count()
//                                + ". File affected: " + event.context() + ".");
                    }
                }
                key.reset();
            }
        } catch (IOException e) {
            System.out.println("File watcher error: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            // do nothing
        }
    }

    private boolean isHidden(Path path) {
        return Utils.getStorjConfigDir().resolve(path.toString()).toFile().isHidden();
    }

}
