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
package io.goobox.sync.storj.helpers;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;

import io.goobox.sync.storj.Utils;
import io.goobox.sync.storj.db.DB;
import io.goobox.sync.storj.db.SyncFile;
import io.goobox.sync.storj.db.SyncState;
import io.goobox.sync.storj.mocks.FileMock;
import io.storj.libstorj.File;

public class AssertSyncFile {

    public static void assertWith(File storjFile, SyncState state) throws ParseException {
        SyncFile syncFile = DB.get(storjFile);
        assertEquals(storjFile.getName(), syncFile.getName());
        assertEquals(storjFile.getId(), syncFile.getStorjId());
        assertEquals(Utils.getTime(storjFile.getCreated()), syncFile.getStorjCreatedTime());
        assertEquals(storjFile.getSize(), syncFile.getStorjSize());
        assertEquals(0, syncFile.getLocalModifiedTime());
        assertEquals(0, syncFile.getLocalSize());
        assertEquals(state, syncFile.getState());
    }

    public static void assertWith(FileMock localFile, SyncState state) {
        SyncFile syncFile = DB.get(localFile.getPath());
        assertEquals(localFile.getName(), syncFile.getName());
        assertEquals(null, syncFile.getStorjId());
        assertEquals(0, syncFile.getStorjCreatedTime());
        assertEquals(0, syncFile.getStorjSize());
        assertEquals(localFile.lastModified(), syncFile.getLocalModifiedTime());
        assertEquals(localFile.size(), syncFile.getLocalSize());
        assertEquals(state, syncFile.getState());
    }

    public static void assertWith(File storjFile, FileMock localFile, SyncState state) throws ParseException {
        SyncFile syncFile = DB.get(storjFile);
        assertEquals(storjFile.getName(), syncFile.getName());
        assertEquals(storjFile.getId(), syncFile.getStorjId());
        assertEquals(Utils.getTime(storjFile.getCreated()), syncFile.getStorjCreatedTime());
        assertEquals(storjFile.getSize(), syncFile.getStorjSize());
        assertEquals(localFile.lastModified(), syncFile.getLocalModifiedTime());
        assertEquals(localFile.size(), syncFile.getLocalSize());
        assertEquals(state, syncFile.getState());
    }

}
