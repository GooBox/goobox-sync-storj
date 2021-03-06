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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SleepTask implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(SleepTask.class);

    @Override
    public synchronized void run() {
        logger.info("Sleeping for 1 minute");
        try {
            wait(60000);
        } catch (InterruptedException e) {
            // nothing to do
        }
    }

    public synchronized void interrupt() {
        logger.info("Sleep interrupted");
        notify();
    }

}
