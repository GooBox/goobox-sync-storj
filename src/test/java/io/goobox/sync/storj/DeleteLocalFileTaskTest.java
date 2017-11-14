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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.goobox.sync.storj.db.DB;
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
    public void oneLocalDeleteTest() throws IOException {
        new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());

        new DeleteLocalFileTask(FileMock.FILE_1.getPath()).run();

        assertFalse(Files.exists(FileMock.FILE_1.getPath()));
        assertEquals(0, DB.size());
        assertNull(DB.get(FileMock.FILE_1.getPath()));
    }

    @Test
    public void oneOfTwoLocalDeleteTest() throws IOException {
        new FilesMock(FileMock.FILE_1, FileMock.FILE_2);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.setSynced(StorjMock.FILE_2, FileMock.FILE_2.getPath());

        new DeleteLocalFileTask(FileMock.FILE_1.getPath()).run();

        assertFalse(Files.exists(FileMock.FILE_1.getPath()));
        assertTrue(Files.exists(FileMock.FILE_2.getPath()));
        assertEquals(1, DB.size());
        assertNull(DB.get(FileMock.FILE_1.getPath()));
        assertNotNull(DB.get(FileMock.FILE_2.getPath()));
    }

    @Test
    public void nonExistingLocalDeleteTest() throws IOException {
        new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());

        new DeleteLocalFileTask(Paths.get("/some/folder/non-existing-file.txt")).run();

        assertTrue(Files.exists(FileMock.FILE_1.getPath()));
        assertEquals(1, DB.size());
        assertNotNull(DB.get(FileMock.FILE_1.getPath()));
    }

}
