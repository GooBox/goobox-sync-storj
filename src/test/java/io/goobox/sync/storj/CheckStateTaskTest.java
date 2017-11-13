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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.goobox.sync.storj.db.DB;
import io.goobox.sync.storj.db.SyncFile;
import io.goobox.sync.storj.db.SyncState;
import io.goobox.sync.storj.mocks.DBMock;
import io.goobox.sync.storj.mocks.FileMock;
import io.goobox.sync.storj.mocks.FilesMock;
import io.goobox.sync.storj.mocks.StorjMock;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class CheckStateTaskTest {

    private LinkedBlockingQueue<Runnable> tasks;

    @BeforeClass
    public static void applySharedFakes() {
        new DBMock();
    }

    @Before
    public void setup() {
        tasks = new LinkedBlockingQueue<>();
    }

    @After
    public void cleanUp() {
        DB.close();
    }

    @Test
    public void emptyCloudAndLocalTest() throws InterruptedException {
        new StorjMock();
        new FilesMock();

        new CheckStateTask(null, tasks).run();

        assertEquals(SleepTask.class, tasks.poll().getClass());
        assertEquals(CheckStateTask.class, tasks.poll().getClass());
        assertTrue(tasks.isEmpty());

        assertEquals(0, DB.size());
    }

    @Test
    public void fileInCloudEmptyLocalTest() throws InterruptedException, ParseException {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock();

        new CheckStateTask(null, tasks).run();

        assertEquals(DownloadFileTask.class, tasks.poll().getClass());
        assertEquals(CheckStateTask.class, tasks.poll().getClass());
        assertTrue(tasks.isEmpty());

        assertEquals(1, DB.size());
        assertTrue(DB.contains(StorjMock.FILE_1.getName()));

        SyncFile syncFile = DB.get(StorjMock.FILE_1.getName());
        assertEquals(StorjMock.FILE_1.getName(), syncFile.getName());
        assertEquals(StorjMock.FILE_1.getId(), syncFile.getStorjId());
        assertEquals(Utils.getTime(StorjMock.FILE_1.getCreated()), syncFile.getStorjCreatedTime());
        assertEquals(StorjMock.FILE_1.getSize(), syncFile.getStorjSize());
        assertEquals(0, syncFile.getLocalModifiedTime());
        assertEquals(0, syncFile.getLocalSize());
        assertEquals(SyncState.FOR_DOWNLOAD, syncFile.getState());
    }

    @Test
    public void emptyCloudFileInLocalTest() throws InterruptedException, ParseException {
        new StorjMock();
        new FilesMock(FileMock.FILE_1);

        new CheckStateTask(null, tasks).run();

        assertEquals(UploadFileTask.class, tasks.poll().getClass());
        assertEquals(CheckStateTask.class, tasks.poll().getClass());
        assertTrue(tasks.isEmpty());

        assertEquals(1, DB.size());
        assertTrue(DB.contains(FileMock.FILE_1.getName()));

        SyncFile syncFile = DB.get(FileMock.FILE_1.getName());
        assertEquals(FileMock.FILE_1.getName(), syncFile.getName());
        assertEquals(null, syncFile.getStorjId());
        assertEquals(0, syncFile.getStorjCreatedTime());
        assertEquals(0, syncFile.getStorjSize());
        assertEquals(FileMock.FILE_1.lastModified(), syncFile.getLocalModifiedTime());
        assertEquals(FileMock.FILE_1.size(), syncFile.getLocalSize());
        assertEquals(SyncState.FOR_UPLOAD, syncFile.getState());
    }

    @Test
    public void encryptedFileInCloudEmptyLocalTest() throws InterruptedException {
        new StorjMock(StorjMock.ENCRYPTED_FILE);
        new FilesMock();

        new CheckStateTask(null, tasks).run();

        assertEquals(SleepTask.class, tasks.poll().getClass());
        assertEquals(CheckStateTask.class, tasks.poll().getClass());
        assertTrue(tasks.isEmpty());

        assertEquals(0, DB.size());
    }

    @Test
    public void encryptedFileInCloudAndLocalTest() throws InterruptedException {
        new StorjMock(StorjMock.ENCRYPTED_FILE);
        new FilesMock(FileMock.ENCRYPTED_FILE);

        new CheckStateTask(null, tasks).run();

        assertEquals(SleepTask.class, tasks.poll().getClass());
        assertEquals(CheckStateTask.class, tasks.poll().getClass());
        assertTrue(tasks.isEmpty());

        assertEquals(0, DB.size());
    }

}
