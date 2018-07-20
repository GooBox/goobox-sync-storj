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

public class TaskExecutor extends Thread {

    private TaskQueue tasks;
    private volatile Runnable currentTask;
    private StorjExecutorService ses;

    public TaskExecutor(TaskQueue tasks, StorjExecutorService ses) {
        this.tasks = tasks;
        this.ses = ses;
    }

    @Override
    public void run() {
        while (true) {
            try {
                currentTask = tasks.take();
                if ( currentTask instanceof CheckStateTask || currentTask instanceof SleepTask) {

                    while( ses.getActiveCount() > 0) {
                        Thread.sleep(1000);
                    }

                    currentTask.run();
                } else {
                    ses.submit(currentTask);
                }

                currentTask = null;
            } catch (InterruptedException e) {
                // nothing to do
            }
        }
    }

    public void interruptSleeping() {
        if (currentTask instanceof SleepTask) {
            ((SleepTask) currentTask).interrupt();
        }
    }

}
