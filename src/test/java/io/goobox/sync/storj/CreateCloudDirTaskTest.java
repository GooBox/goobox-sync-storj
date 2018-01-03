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
public class CreateCloudDirTaskTest {

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
    public void createCloudDir() throws Exception {
        new StorjMock();
        new FilesMock(FileMock.DIR);

        DB.addForCloudCreateDir(FileMock.DIR.getPath());

        new CreateCloudDirTask(null, FileMock.DIR.getPath()).run();

        AssertState.assertDB(StorjMock.DIR, FileMock.DIR, SyncState.SYNCED);
    }

    @Test
    public void createLocalDirAlreadyExists() throws Exception {
        new StorjMock(StorjMock.DIR);
        new FilesMock(FileMock.DIR);

        DB.addForCloudCreateDir(FileMock.DIR.getPath());

        new CreateCloudDirTask(null, FileMock.DIR.getPath()).run();

        AssertState.assertDB(StorjMock.DIR, FileMock.DIR, SyncState.SYNCED);
    }

    @Test
    public void createCloudSubDir() throws Exception {
        new StorjMock();
        new FilesMock(FileMock.SUB_DIR);

        DB.addForCloudCreateDir(FileMock.SUB_DIR.getPath());

        new CreateCloudDirTask(null, FileMock.SUB_DIR.getPath()).run();

        AssertState.assertDB(StorjMock.SUB_DIR, FileMock.SUB_DIR, SyncState.SYNCED);
    }

    @Test
    public void createCloudSubDirAlreadyExists() throws Exception {
        new StorjMock(StorjMock.DIR);
        new FilesMock(FileMock.SUB_DIR);

        DB.addForCloudCreateDir(FileMock.SUB_DIR.getPath());

        new CreateCloudDirTask(null, FileMock.SUB_DIR.getPath()).run();

        AssertState.assertDB(StorjMock.SUB_DIR, FileMock.SUB_DIR, SyncState.SYNCED);
    }

}
