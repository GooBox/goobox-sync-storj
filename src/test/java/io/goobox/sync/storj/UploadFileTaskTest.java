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

import java.io.IOException;
import java.text.ParseException;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.goobox.sync.storj.db.DB;
import io.goobox.sync.storj.db.SyncState;
import io.goobox.sync.storj.helpers.AssertSyncFile;
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
    public void successfulUploadTest() throws IOException, ParseException {
        new StorjMock();
        new FilesMock(FileMock.FILE_1);

        DB.addForUpload(FileMock.FILE_1.getPath());

        new UploadFileTask(null, FileMock.FILE_1.getPath()).run();

        assertEquals(1, DB.size());
        assertTrue(DB.contains(FileMock.FILE_1.getPath()));
        AssertSyncFile.assertWith(StorjMock.FILE_1, FileMock.FILE_1, SyncState.SYNCED);
    }

    @Test
    public void successfulUploadOverwriteTest() throws IOException, ParseException {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.addForUpload(FileMock.FILE_1.getPath());

        new UploadFileTask(null, FileMock.FILE_1.getPath()).run();

        assertEquals(1, DB.size());
        assertTrue(DB.contains(FileMock.FILE_1.getPath()));
        AssertSyncFile.assertWith(StorjMock.FILE_1, FileMock.FILE_1, SyncState.SYNCED);
    }

    @Test
    public void erroneousUploadTest() throws IOException, ParseException {
        new StorjMock();
        new FilesMock(FileMock.FILE_2);

        DB.addForUpload(FileMock.FILE_2.getPath());

        new UploadFileTask(null, FileMock.FILE_2.getPath()).run();

        assertEquals(1, DB.size());
        assertTrue(DB.contains(FileMock.FILE_2.getPath()));
        AssertSyncFile.assertWith(FileMock.FILE_2, SyncState.UPLOAD_FAILED);
    }

}
