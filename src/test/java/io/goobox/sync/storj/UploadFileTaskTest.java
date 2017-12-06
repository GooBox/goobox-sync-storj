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

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.goobox.sync.storj.db.DB;
import io.goobox.sync.storj.db.SyncState;
import io.goobox.sync.storj.helpers.AssertState;
import io.goobox.sync.storj.mocks.DBMock;
import io.goobox.sync.storj.mocks.FileMock;
import io.goobox.sync.storj.mocks.FilesMock;
import io.goobox.sync.storj.mocks.StorjMock;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class UploadFileTaskTest {

    @BeforeClass
    public static void applySharedFakes() {
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
    public void successfulUpload() throws Exception {
        new StorjMock();
        new FilesMock(FileMock.FILE_1);

        DB.addForUpload(FileMock.FILE_1.getPath());

        new UploadFileTask(null, FileMock.FILE_1.getPath()).run();

        AssertState.assertDB(StorjMock.FILE_1, FileMock.FILE_1, SyncState.SYNCED);
    }

    @Test
    public void successfulUploadOverwrite() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.addForUpload(FileMock.FILE_1.getPath());

        new UploadFileTask(null, FileMock.FILE_1.getPath()).run();

        AssertState.assertDB(StorjMock.FILE_1, FileMock.FILE_1, SyncState.SYNCED);
    }

    @Test
    public void erroneousUpload() throws Exception {
        new StorjMock();
        new FilesMock(FileMock.FILE_2);

        DB.addForUpload(FileMock.FILE_2.getPath());

        new UploadFileTask(null, FileMock.FILE_2.getPath()).run();

        AssertState.assertDB(FileMock.FILE_2, SyncState.UPLOAD_FAILED);
    }

    @Test
    public void subFileUpload() throws Exception {
        new StorjMock();
        new FilesMock(FileMock.SUB_FILE);

        DB.addForUpload(FileMock.SUB_FILE.getPath());

        new UploadFileTask(null, FileMock.SUB_FILE.getPath()).run();

        AssertState.assertDB(StorjMock.SUB_FILE, FileMock.SUB_FILE, SyncState.SYNCED);
    }

    @Test
    public void subSubFileUpload() throws Exception {
        new StorjMock();
        new FilesMock(FileMock.SUB_SUB_FILE);

        DB.addForUpload(FileMock.SUB_SUB_FILE.getPath());

        new UploadFileTask(null, FileMock.SUB_SUB_FILE.getPath()).run();

        AssertState.assertDB(StorjMock.SUB_SUB_FILE, FileMock.SUB_SUB_FILE, SyncState.SYNCED);
    }

}
