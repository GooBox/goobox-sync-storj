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
package io.goobox.sync.storj.mocks;

import java.util.concurrent.LinkedBlockingQueue;

import io.goobox.sync.common.overlay.OverlayHelper;
import io.goobox.sync.storj.App;
import io.goobox.sync.storj.FileWatcher;
import io.goobox.sync.storj.StorjExecutorService;
import io.goobox.sync.storj.TaskQueue;
import io.goobox.sync.storj.ipc.IpcExecutor;
import io.goobox.sync.storj.overlay.StorjOverlayIconProvider;
import io.storj.libstorj.Bucket;
import io.storj.libstorj.Storj;
import mockit.Mock;
import mockit.MockUp;

public class AppMock extends MockUp<App> {

    private App instance = new App();
    private Storj storj;
    private IpcExecutor ipcExecutor = new IpcExecutor();
    private TaskQueue tasks = new TaskQueue();
    private FileWatcher fileWatcher = new FileWatcher();
    private OverlayHelper overlayHelper = new OverlayHelper(
            instance.getSyncDir(), new StorjOverlayIconProvider());
    private StorjExecutorService storjExecutorService = new StorjExecutorService(1, new LinkedBlockingQueue<Runnable>());

    @Mock
    public App getInstance() {
        return instance;
    }

    @Mock
    public Storj getStorj() {
        if (storj == null) {
            storj = new Storj();
        }
        return storj;
    }

    @Mock
    public Bucket getGooboxBucket() {
        return StorjMock.BUCKET;
    }

    @Mock
    public IpcExecutor getIpcExecutor() {
        return ipcExecutor;
    }

    @Mock
    public TaskQueue getTaskQueue() {
        return tasks;
    }

    @Mock
    public FileWatcher getFileWatcher() {
        return fileWatcher;
    }

    @Mock
    public OverlayHelper getOverlayHelper() {
        return overlayHelper;
    }

    @Mock
    public StorjExecutorService getStorjExecutorService() {
        return storjExecutorService;
    }

}
