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

import java.util.concurrent.LinkedBlockingQueue;

@SuppressWarnings("serial")
public class TaskQueue extends LinkedBlockingQueue<Runnable> {

    @Override
    public boolean add(Runnable task) {
        if (task instanceof CheckStateTask) {
            // don't add another check state task if the queue already contains one
            for (Runnable t : this) {
                if (t instanceof CheckStateTask) {
                    return false;
                }
            }
        }
        return super.add(task);
    }

}
