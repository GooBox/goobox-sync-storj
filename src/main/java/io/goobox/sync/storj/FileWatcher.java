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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.methvin.watcher.DirectoryChangeEvent;
import io.methvin.watcher.DirectoryChangeListener;
import io.methvin.watcher.DirectoryWatcher;

public class FileWatcher extends Thread implements DirectoryChangeListener {

    private static final Logger logger = LoggerFactory.getLogger(FileWatcher.class);
    
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
                            logger.info("3 seconds after the last file event");
                            App.getInstance().getTaskQueue().add(new CheckStateTask());
                            App.getInstance().getTaskExecutor().interruptSleeping();
                        }
                    }
                }
            }, 3000, 3000);
        } catch (IOException e) {
            logger.error("File watcher error", e);
        }
    }

    @Override
    public synchronized void onEvent(final DirectoryChangeEvent event) {
        lastEventTime = System.currentTimeMillis();

        logger.debug("{} {} {} count: {}", lastEventTime, event.eventType(), event.path(), event.count());

        switch (event.eventType()) {
        case CREATE:
        case MODIFY:
            try {
                copyInProgress.put(event.path(), Files.size(event.path()));
            } catch (IOException e) {
                // file does not exist anymore?
                copyInProgress.remove(event.path());
            }
            break;
        case DELETE:
            copyInProgress.remove(event.path());
            break;
        case OVERFLOW:
            break;
        }
    }

    public synchronized boolean isInProgress() {
        return lastEventTime != 0;
    }

}
