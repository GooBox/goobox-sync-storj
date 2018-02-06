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

import java.nio.file.Files;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.goobox.sync.storj.db.DB;
import io.goobox.sync.storj.db.SyncState;
import io.goobox.sync.storj.helpers.AssertState;
import io.goobox.sync.storj.mocks.AppMock;
import io.goobox.sync.storj.mocks.DBMock;
import io.goobox.sync.storj.mocks.FileMock;
import io.goobox.sync.storj.mocks.FilesMock;
import io.goobox.sync.storj.mocks.StorjMock;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class DownloadFileTaskTest {

    @BeforeClass
    public static void applySharedFakes() {
        new AppMock();
        new DBMock();
    }

    @Before
    public void setup() {
    }

    @After
    public void cleanUp() {
        DB.close();
    }

    @Test
    public void successfulDownload() throws Exception {
        new StorjMock(new FilesMock(), StorjMock.FILE_1);

        DB.addForDownload(StorjMock.FILE_1);

        new DownloadFileTask(StorjMock.BUCKET, StorjMock.FILE_1).run();

        Assert.assertTrue(Files.exists(FileMock.FILE_1.getPath()));
        AssertState.assertDB(StorjMock.FILE_1, FileMock.FILE_1, SyncState.SYNCED);
    }

    @Test
    public void erroneousDownload() throws Exception {
        new StorjMock(StorjMock.FILE_2);

        DB.addForDownload(StorjMock.FILE_2);

        new DownloadFileTask(StorjMock.BUCKET, StorjMock.FILE_2).run();

        AssertState.assertDB(StorjMock.FILE_2, SyncState.DOWNLOAD_FAILED);
    }

    @Test
    public void subFileDownload() throws Exception {
        new StorjMock(new FilesMock(), StorjMock.SUB_FILE);
        new FilesMock();

        DB.addForDownload(StorjMock.SUB_FILE);

        new DownloadFileTask(StorjMock.BUCKET, StorjMock.SUB_FILE).run();

        Assert.assertTrue(Files.exists(FileMock.SUB_FILE.getPath()));
        AssertState.assertDB(StorjMock.SUB_FILE, FileMock.SUB_FILE, SyncState.SYNCED);
    }

    @Test
    public void subSubFileDownload() throws Exception {
        new StorjMock(new FilesMock(), StorjMock.SUB_SUB_FILE);
        new FilesMock();

        DB.addForDownload(StorjMock.SUB_SUB_FILE);

        new DownloadFileTask(StorjMock.BUCKET, StorjMock.SUB_SUB_FILE).run();

        Assert.assertTrue(Files.exists(FileMock.SUB_SUB_FILE.getPath()));
        AssertState.assertDB(StorjMock.SUB_SUB_FILE, FileMock.SUB_SUB_FILE, SyncState.SYNCED);
    }

}
