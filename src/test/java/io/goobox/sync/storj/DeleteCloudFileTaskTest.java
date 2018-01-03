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
import io.storj.libstorj.File;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class DeleteCloudFileTaskTest {

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
    public void oneCloudDelete() throws Exception {
        cloudDeleteTest(StorjMock.FILE_1, FileMock.FILE_1);
    }

    @Test
    public void oneOfTwoCloudDelete() throws Exception {
        new StorjMock(StorjMock.FILE_1, StorjMock.FILE_2);
        new FilesMock(FileMock.FILE_1, FileMock.FILE_2);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.setSynced(StorjMock.FILE_2, FileMock.FILE_2.getPath());
        Files.deleteIfExists(FileMock.FILE_1.getPath());
        DB.setForCloudDelete(StorjMock.FILE_1);

        new DeleteCloudFileTask(null, StorjMock.FILE_1).run();

        AssertState.assertDB(StorjMock.FILE_2, FileMock.FILE_2, SyncState.SYNCED);
    }

    @Test
    public void nonExistingLocalDelete() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());

        File nonExisting = new File("non-existing-id", "bucket-id", "non-existing", null, true, 12432, null, null, null, null);
        new DeleteCloudFileTask(null, nonExisting).run();

        AssertState.assertDB(StorjMock.FILE_1, FileMock.FILE_1, SyncState.SYNCED);
    }

    @Test
    public void dirCloudDelete() throws Exception {
        cloudDeleteTest(StorjMock.DIR, FileMock.DIR);
    }

    @Test
    public void subDirCloudDelete() throws Exception {
        cloudDeleteTest(StorjMock.SUB_DIR, FileMock.SUB_DIR);
    }

    @Test
    public void subFileCloudDelete() throws Exception {
        cloudDeleteTest(StorjMock.SUB_FILE, FileMock.SUB_FILE);
    }

    @Test
    public void subSubFileCloudDelete() throws Exception {
        cloudDeleteTest(StorjMock.SUB_SUB_FILE, FileMock.SUB_SUB_FILE);
    }

    private void cloudDeleteTest(File storjFile, FileMock localFile) throws Exception {
        new StorjMock(storjFile);
        new FilesMock(localFile);

        DB.setSynced(storjFile, localFile.getPath());
        Files.deleteIfExists(localFile.getPath());
        DB.setForCloudDelete(storjFile);

        new DeleteCloudFileTask(null, storjFile).run();

        AssertState.assertEmptyDB();
    }

}
