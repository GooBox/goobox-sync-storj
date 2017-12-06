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
import static org.junit.Assert.assertTrue;

import java.text.ParseException;

import io.goobox.sync.storj.App;
import io.goobox.sync.storj.CheckStateTask;
import io.goobox.sync.storj.CreateCloudDirTask;
import io.goobox.sync.storj.CreateLocalDirTask;
import io.goobox.sync.storj.DeleteCloudFileTask;
import io.goobox.sync.storj.DeleteLocalFileTask;
import io.goobox.sync.storj.DownloadFileTask;
import io.goobox.sync.storj.SleepTask;
import io.goobox.sync.storj.TaskQueue;
import io.goobox.sync.storj.UploadFileTask;
import io.goobox.sync.storj.db.DB;
import io.goobox.sync.storj.db.SyncState;
import io.goobox.sync.storj.mocks.FileMock;
import io.storj.libstorj.File;

public class AssertState {

    public static void assertAllEmpty() {
        assertEmptyTaskQueue();
        assertEmptyDB();
    }

    public static void assertSleepEmptyDB() {
        assertTaskQueue(SleepTask.class);
        assertEmptyDB();
    }

    public static void assertSynced(File storjFile, FileMock localFile) throws ParseException {
        assertTaskQueue(SleepTask.class);
        assertDB(storjFile, localFile, SyncState.SYNCED);
    }

    public static void assertForDownload(File storjFile) throws ParseException {
        assertTaskQueue(DownloadFileTask.class);
        assertDB(storjFile, SyncState.FOR_DOWNLOAD);
    }

    public static void assertForDownload(File storjFile, FileMock localFile) throws ParseException {
        assertTaskQueue(DownloadFileTask.class);
        assertDB(storjFile, localFile, SyncState.FOR_DOWNLOAD);
    }

    public static void assertForUpload(FileMock localFile) throws ParseException {
        assertTaskQueue(UploadFileTask.class);
        assertDB(localFile, SyncState.FOR_UPLOAD);
    }

    public static void assertForUpload(File storjFile, FileMock localFile) throws ParseException {
        assertTaskQueue(UploadFileTask.class);
        assertDB(storjFile, localFile, SyncState.FOR_UPLOAD);
    }

    public static void assertForCloudDelete(File storjFile, FileMock localFile) throws ParseException {
        assertTaskQueue(DeleteCloudFileTask.class);
        assertDB(storjFile, localFile, SyncState.FOR_CLOUD_DELETE);
    }

    public static void assertForLocalDelete(File storjFile, FileMock localFile) throws ParseException {
        assertTaskQueue(DeleteLocalFileTask.class);
        assertDB(storjFile, localFile, SyncState.FOR_LOCAL_DELETE);
    }

    public static void assertForLocalCreateDir(File storjFile) throws ParseException {
        assertTaskQueue(CreateLocalDirTask.class);
        assertDB(storjFile, SyncState.FOR_LOCAL_CREATE_DIR);
    }

    public static void assertForCloudCreateDir(FileMock localFile) throws ParseException {
        assertTaskQueue(CreateCloudDirTask.class);
        assertDB(localFile, SyncState.FOR_CLOUD_CREATE_DIR);
    }

    public static void assertForDownloadFailed(File storjFile) throws ParseException {
        assertTaskQueue(SleepTask.class);
        assertDB(storjFile, SyncState.DOWNLOAD_FAILED);
    }

    public static void assertForDownloadFailed(File storjFile, FileMock localFile) throws ParseException {
        assertTaskQueue(SleepTask.class);
        assertDB(storjFile, localFile, SyncState.DOWNLOAD_FAILED);
    }

    public static void assertForUploadFailed(FileMock localFile) throws ParseException {
        assertTaskQueue(SleepTask.class);
        assertDB(localFile, SyncState.UPLOAD_FAILED);
    }

    public static void assertForUploadFailed(File storjFile, FileMock localFile) throws ParseException {
        assertTaskQueue(SleepTask.class);
        assertDB(storjFile, localFile, SyncState.UPLOAD_FAILED);
    }

    private static void assertEmptyTaskQueue() {
        TaskQueue tasks = App.getInstance().getTaskQueue();
        assertTrue(tasks.isEmpty());
    }

    public static void assertTaskQueue(Class<? extends Runnable> task) {
        TaskQueue tasks = App.getInstance().getTaskQueue();
        assertEquals(task, tasks.poll().getClass());
        assertEquals(CheckStateTask.class, tasks.poll().getClass());
        assertTrue(tasks.isEmpty());
    }

    public static void assertEmptyDB() {
        assertEquals(0, DB.size());
    }

    public static void assertDB(File storjFile, SyncState state) throws ParseException {
        assertEquals(1, DB.size());
        assertTrue(DB.contains(storjFile));
        AssertSyncFile.assertWith(storjFile, state);
    }

    public static void assertDB(FileMock localFile, SyncState state) throws ParseException {
        assertEquals(1, DB.size());
        assertTrue(DB.contains(localFile.getPath()));
        AssertSyncFile.assertWith(localFile, state);
    }

    public static void assertDB(File storjFile, FileMock localFile, SyncState state) throws ParseException {
        assertEquals(1, DB.size());
        assertTrue(DB.contains(storjFile));
        AssertSyncFile.assertWith(storjFile, localFile, state);
    }

}
