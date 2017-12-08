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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.goobox.sync.storj.db.DB;
import io.goobox.sync.storj.db.SyncState;
import io.goobox.sync.storj.helpers.AssertState;
import io.goobox.sync.storj.helpers.AssertSyncFile;
import io.goobox.sync.storj.helpers.StorjUtil;
import io.goobox.sync.storj.mocks.DBMock;
import io.goobox.sync.storj.mocks.FileMock;
import io.goobox.sync.storj.mocks.FilesMock;
import io.goobox.sync.storj.mocks.StorjMock;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class DeleteLocalFileTaskTest {

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
    public void oneLocalDelete() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        StorjUtil.deleteFile(StorjMock.FILE_1);
        DB.setForLocalDelete(FileMock.FILE_1.getPath());

        new DeleteLocalFileTask(FileMock.FILE_1.getPath()).run();

        assertFalse(Files.exists(FileMock.FILE_1.getPath()));
        AssertState.assertEmptyDB();
    }

    @Test
    public void oneOfTwoLocalDelete() throws Exception {
        new StorjMock(StorjMock.FILE_1, StorjMock.FILE_2);
        new FilesMock(FileMock.FILE_1, FileMock.FILE_2);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.setSynced(StorjMock.FILE_2, FileMock.FILE_2.getPath());
        StorjUtil.deleteFile(StorjMock.FILE_1);
        DB.setForLocalDelete(FileMock.FILE_1.getPath());

        new DeleteLocalFileTask(FileMock.FILE_1.getPath()).run();

        assertFalse(Files.exists(FileMock.FILE_1.getPath()));
        assertTrue(Files.exists(FileMock.FILE_2.getPath()));
        AssertState.assertDB(StorjMock.FILE_2, FileMock.FILE_2, SyncState.SYNCED);
    }

    @Test
    public void nonExistingLocalDelete() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());

        new DeleteLocalFileTask(Paths.get("/some/folder/non-existing-file.txt")).run();

        assertTrue(Files.exists(FileMock.FILE_1.getPath()));
        AssertState.assertDB(StorjMock.FILE_1, FileMock.FILE_1, SyncState.SYNCED);
    }

    @Test
    public void emptyDirDelete() throws Exception {
        new StorjMock(StorjMock.DIR);
        new FilesMock(FileMock.DIR);

        DB.setSynced(StorjMock.DIR, FileMock.DIR.getPath());
        StorjUtil.deleteFile(StorjMock.DIR);
        DB.setForLocalDelete(FileMock.DIR.getPath());

        new DeleteLocalFileTask(FileMock.DIR.getPath()).run();

        assertFalse(Files.exists(FileMock.DIR.getPath()));
        AssertState.assertEmptyDB();
    }

    @Test
    public void nonEmptyDirDelete() throws Exception {
        new StorjMock(StorjMock.DIR, StorjMock.SUB_FILE);
        new FilesMock(FileMock.DIR, FileMock.SUB_FILE);

        DB.setSynced(StorjMock.DIR, FileMock.DIR.getPath());
        DB.setSynced(StorjMock.SUB_FILE, FileMock.SUB_FILE.getPath());
        StorjUtil.deleteFile(StorjMock.DIR);
        DB.setForLocalDelete(FileMock.DIR.getPath());

        new DeleteLocalFileTask(FileMock.DIR.getPath()).run();

        assertTrue(Files.exists(FileMock.DIR.getPath()));
        assertTrue(Files.exists(FileMock.SUB_FILE.getPath()));
        AssertState.assertDB(StorjMock.SUB_FILE, FileMock.SUB_FILE, SyncState.SYNCED);
    }

    @Test
    public void dirAndFileDelete() throws Exception {
        new StorjMock(StorjMock.DIR, StorjMock.SUB_FILE);
        new FilesMock(FileMock.DIR, FileMock.SUB_FILE);

        DB.setSynced(StorjMock.DIR, FileMock.DIR.getPath());
        DB.setSynced(StorjMock.SUB_FILE, FileMock.SUB_FILE.getPath());
        StorjUtil.deleteFile(StorjMock.DIR);
        StorjUtil.deleteFile(StorjMock.SUB_FILE);
        DB.setForLocalDelete(FileMock.DIR.getPath());
        DB.setForLocalDelete(FileMock.SUB_FILE.getPath());

        new DeleteLocalFileTask(FileMock.DIR.getPath()).run();
        new DeleteLocalFileTask(FileMock.SUB_FILE.getPath()).run();

        assertFalse(Files.exists(FileMock.DIR.getPath()));
        assertFalse(Files.exists(FileMock.SUB_FILE.getPath()));
        AssertState.assertEmptyDB();
    }

    @Test
    public void onlyFileInDirDelete() throws Exception {
        new StorjMock(StorjMock.DIR, StorjMock.SUB_FILE);
        new FilesMock(FileMock.DIR, FileMock.SUB_FILE);

        DB.setSynced(StorjMock.DIR, FileMock.DIR.getPath());
        DB.setSynced(StorjMock.SUB_FILE, FileMock.SUB_FILE.getPath());
        StorjUtil.deleteFile(StorjMock.SUB_FILE);
        DB.setForLocalDelete(FileMock.SUB_FILE.getPath());

        new DeleteLocalFileTask(FileMock.SUB_FILE.getPath()).run();
        DB.addForLocalCreateDir(StorjMock.DIR);
        new CreateLocalDirTask(StorjMock.DIR).run();

        assertTrue(Files.exists(FileMock.DIR.getPath()));
        assertFalse(Files.exists(FileMock.SUB_FILE.getPath()));
        AssertState.assertDB(StorjMock.DIR, FileMock.DIR, SyncState.SYNCED);
    }

    @Test
    public void onlyFileInDirDeleteNoDirEntryInCloud() throws Exception {
        new StorjMock(StorjMock.SUB_FILE);
        new FilesMock(FileMock.DIR, FileMock.SUB_FILE);

        DB.setSynced(StorjMock.SUB_FILE, FileMock.SUB_FILE.getPath());
        StorjUtil.deleteFile(StorjMock.SUB_FILE);
        DB.setForLocalDelete(FileMock.SUB_FILE.getPath());

        new DeleteLocalFileTask(FileMock.SUB_FILE.getPath()).run();

        assertFalse(Files.exists(FileMock.DIR.getPath()));
        assertFalse(Files.exists(FileMock.SUB_FILE.getPath()));
        AssertState.assertEmptyDB();
    }

    @Test
    public void fileInDirDelete() throws Exception {
        new StorjMock(StorjMock.DIR, StorjMock.SUB_FILE, StorjMock.SUB_DIR);
        new FilesMock(FileMock.DIR, FileMock.SUB_FILE, FileMock.SUB_DIR);

        DB.setSynced(StorjMock.DIR, FileMock.DIR.getPath());
        DB.setSynced(StorjMock.SUB_FILE, FileMock.SUB_FILE.getPath());
        DB.setSynced(StorjMock.SUB_DIR, FileMock.SUB_DIR.getPath());
        StorjUtil.deleteFile(StorjMock.SUB_FILE);
        DB.setForLocalDelete(FileMock.SUB_FILE.getPath());

        new DeleteLocalFileTask(FileMock.SUB_FILE.getPath()).run();

        assertTrue(Files.exists(FileMock.DIR.getPath()));
        assertFalse(Files.exists(FileMock.SUB_FILE.getPath()));
        assertTrue(Files.exists(FileMock.SUB_DIR.getPath()));
        assertEquals(2, DB.size());
        assertTrue(DB.contains(StorjMock.DIR));
        AssertSyncFile.assertWith(StorjMock.DIR, FileMock.DIR, SyncState.SYNCED);
        assertTrue(DB.contains(StorjMock.DIR));
        AssertSyncFile.assertWith(StorjMock.SUB_DIR, FileMock.SUB_DIR, SyncState.SYNCED);
    }

}
