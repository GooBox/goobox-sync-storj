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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import io.methvin.watcher.DirectoryChangeEvent;
import io.methvin.watcher.DirectoryChangeListener;
import io.methvin.watcher.DirectoryWatcher;

public class FileWatcher extends Thread implements DirectoryChangeListener {
    
    private long lastEventTime;
    private Map<Path, Long> copyInProgress = new HashMap<>();

    @Override
    public void run() {
        try {
            DirectoryWatcher watcher = DirectoryWatcher.create(App.getInstance().getSyncDir(), this);
            watcher.watchAsync();

            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    synchronized (FileWatcher.this) {
                        if (lastEventTime == 0) {
                            // no file event occurred recently
                            return;
                        }

                        long currentTime = System.currentTimeMillis();
                        if (currentTime < lastEventTime + 3000) {
                            // last file event was less then 3 seconds ago
                            return;
                        }
                        
                        // check if file copy is still in progress
                        Iterator<Path> i = copyInProgress.keySet().iterator();
                        while (i.hasNext()) {
                            Path path = i.next();
                            try {
                                long size = Files.size(path);
                                if (copyInProgress.get(path) == size) {
                                    // file size is steady - copying finished
                                    i.remove();
                                } else {
                                    // update file size in map
                                    copyInProgress.put(path, size);
                                }
                            } catch (IOException e) {
                                // file does not exist anymore
                                i.remove();
                            }
                        }
                        
                        if (copyInProgress.isEmpty()) {
                            // last file event was more than 3 seconds ago - fire a sync check
                            lastEventTime = 0;
                            System.out.println("3 seconds after the last file event");
                            App.getInstance().getTaskQueue().add(new CheckStateTask());
                            App.getInstance().getTaskExecutor().interruptSleeping();
                        }
                    }
                }
            }, 3000, 3000);
        } catch (IOException e) {
            System.out.println("File watcher error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void onEvent(final DirectoryChangeEvent event) {
        lastEventTime = System.currentTimeMillis();

        switch (event.eventType()) {
        case CREATE:
        case MODIFY:
            // TODO System.out.println(lastEventTime + " CREATE " + event.path() + "
            // count: " + event.count());
            try {
                copyInProgress.put(event.path(), Files.size(event.path()));
            } catch (IOException e) {
                // file does not exist anymore?
                copyInProgress.remove(event.path());
            }
            break;
        case DELETE:
            // TODO System.out.println(lastEventTime + " DELETE " + event.path() + " count:
            // " + event.count());
            copyInProgress.remove(event.path());
            break;
        case OVERFLOW:
            // TODO System.out.println(lastEventTime + " OVERFLOW " + event.path() + "
            // count: " + event.count());
            break;
        }
    }

    public synchronized boolean isInProgress() {
        return lastEventTime != 0;
    }

}
