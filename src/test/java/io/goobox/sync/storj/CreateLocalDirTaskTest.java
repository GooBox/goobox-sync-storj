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

import static org.junit.Assert.assertTrue;

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
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class CreateLocalDirTaskTest {

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
    public void createLocalDir() throws Exception {
        new StorjMock(StorjMock.DIR);
        new FilesMock();

        DB.addForLocalCreateDir(StorjMock.DIR);

        new CreateLocalDirTask(StorjMock.DIR).run();

        assertTrue(Files.exists(FileMock.DIR.getPath()));
        assertTrue(Files.isDirectory(FileMock.DIR.getPath()));
        AssertState.assertDB(StorjMock.DIR, FileMock.DIR, SyncState.SYNCED);
    }

    @Test
    public void createLocalDirAlreadyExists() throws Exception {
        new StorjMock(StorjMock.DIR);
        new FilesMock(FileMock.DIR);

        DB.addForLocalCreateDir(StorjMock.DIR);

        new CreateLocalDirTask(StorjMock.DIR).run();

        assertTrue(Files.exists(FileMock.DIR.getPath()));
        assertTrue(Files.isDirectory(FileMock.DIR.getPath()));
        AssertState.assertDB(StorjMock.DIR, FileMock.DIR, SyncState.SYNCED);
    }

    @Test
    public void createLocalSubDir() throws Exception {
        new StorjMock(StorjMock.SUB_DIR);
        new FilesMock();

        DB.addForLocalCreateDir(StorjMock.SUB_DIR);

        new CreateLocalDirTask(StorjMock.SUB_DIR).run();

        assertTrue(Files.exists(FileMock.SUB_DIR.getPath()));
        assertTrue(Files.isDirectory(FileMock.SUB_DIR.getPath()));
        AssertState.assertDB(StorjMock.SUB_DIR, FileMock.SUB_DIR, SyncState.SYNCED);
    }

}
