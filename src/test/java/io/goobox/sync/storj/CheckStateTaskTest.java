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

import java.nio.file.Files;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.goobox.sync.storj.db.DB;
import io.goobox.sync.storj.helpers.AssertState;
import io.goobox.sync.storj.mocks.AppMock;
import io.goobox.sync.storj.mocks.DBMock;
import io.goobox.sync.storj.mocks.FileMock;
import io.goobox.sync.storj.mocks.FileWatcherMock;
import io.goobox.sync.storj.mocks.FilesMock;
import io.goobox.sync.storj.mocks.StorjMock;
import io.storj.libstorj.DeleteFileCallback;
import io.storj.libstorj.Storj;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class CheckStateTaskTest {

    @BeforeClass
    public static void applySharedFakes() {
        new DBMock();
    }

    @Before
    public void setup() {
        new AppMock();
    }

    @After
    public void cleanUp() {
        DB.close();
    }

    @Test
    public void noCloudAndLocalTest() throws Exception {
        new StorjMock();
        new FilesMock();

        new CheckStateTask().run();

        AssertState.assertSleepEmptyDB();
    }

    @Test
    public void cloudAndLocalInSyncTest() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());

        new CheckStateTask().run();

        AssertState.assertSynced(StorjMock.FILE_1, FileMock.FILE_1);
    }

    @Test
    public void cloudFileNoLocalTest() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock();

        new CheckStateTask().run();

        AssertState.assertForDownload(StorjMock.FILE_1);
    }

    @Test
    public void localFileNoCloudTest() throws Exception {
        new StorjMock();
        new FilesMock(FileMock.FILE_1);

        new CheckStateTask().run();

        AssertState.assertForUpload(FileMock.FILE_1);
    }

    @Test
    public void encryptedCloudNoLocalTest() throws Exception {
        new StorjMock(StorjMock.ENCRYPTED_FILE);
        new FilesMock();

        new CheckStateTask().run();

        AssertState.assertSleepEmptyDB();
    }

    @Test
    public void encryptedCloudAndLocalTest() throws Exception {
        new StorjMock(StorjMock.ENCRYPTED_FILE);
        new FilesMock(FileMock.ENCRYPTED_FILE);

        new CheckStateTask().run();

        AssertState.assertSleepEmptyDB();
    }

    @Test
    public void localDeletedTest() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        Files.deleteIfExists(FileMock.FILE_1.getPath());

        new CheckStateTask().run();

        AssertState.assertForCloudDelete(StorjMock.FILE_1, FileMock.FILE_1);
    }

    @Test
    public void localDeletedCloudModifiedTest() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        storjMock.modifyFile(StorjMock.FILE_1, StorjMock.MODIFIED_FILE_1);
        Files.deleteIfExists(FileMock.FILE_1.getPath());

        new CheckStateTask().run();

        AssertState.assertForDownload(StorjMock.MODIFIED_FILE_1);
    }

    @Test
    public void cloudDeletedTest() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        Storj.getInstance().deleteFile(null, StorjMock.FILE_1, new TestDeleteFileCallback());

        new CheckStateTask().run();

        AssertState.assertForLocalDelete(StorjMock.FILE_1, FileMock.FILE_1);
    }

    @Test
    public void cloudDeletedLocalModifiedTest() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        Storj.getInstance().deleteFile(null, StorjMock.FILE_1, new TestDeleteFileCallback());
        filesMock.modifyFile(FileMock.FILE_1, FileMock.MODIFIED_FILE_1);

        new CheckStateTask().run();

        AssertState.assertForUpload(FileMock.MODIFIED_FILE_1);
    }

    @Test
    public void bothDeleted() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        Storj.getInstance().deleteFile(null, StorjMock.FILE_1, new TestDeleteFileCallback());
        Files.deleteIfExists(FileMock.FILE_1.getPath());

        new CheckStateTask().run();

        AssertState.assertSleepEmptyDB();
    }

    @Test
    public void modifiedCloudTest() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        storjMock.modifyFile(StorjMock.FILE_1, StorjMock.MODIFIED_FILE_1);

        new CheckStateTask().run();

        AssertState.assertForDownload(StorjMock.MODIFIED_FILE_1, FileMock.FILE_1);
    }

    @Test
    public void modifiedLocalTest() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        filesMock.modifyFile(FileMock.FILE_1, FileMock.MODIFIED_FILE_1);

        new CheckStateTask().run();

        AssertState.assertForUpload(StorjMock.FILE_1, FileMock.MODIFIED_FILE_1);
    }

    @Test
    public void sameSizeCloudAndLocalNoDBTest() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        new CheckStateTask().run();

        AssertState.assertSynced(StorjMock.FILE_1, FileMock.FILE_1);
    }

    @Test
    public void modifiedBothSameTimeNoDBTest() throws Exception {
        new StorjMock(StorjMock.MODIFIED_FILE_1_NEWER);
        new FilesMock(FileMock.MODIFIED_FILE_1_NEWER);

        new CheckStateTask().run();

        AssertState.assertForDownload(StorjMock.MODIFIED_FILE_1_NEWER, FileMock.MODIFIED_FILE_1_NEWER);
    }

    @Test
    public void modifiedBothCloudNewerNoDBTest() throws Exception {
        new StorjMock(StorjMock.MODIFIED_FILE_1_NEWER);
        new FilesMock(FileMock.MODIFIED_FILE_1);

        new CheckStateTask().run();

        AssertState.assertForDownload(StorjMock.MODIFIED_FILE_1_NEWER, FileMock.MODIFIED_FILE_1);
    }

    @Test
    public void modifiedBothLocalNewerNoDBTest() throws Exception {
        new StorjMock(StorjMock.MODIFIED_FILE_1);
        new FilesMock(FileMock.MODIFIED_FILE_1_NEWER);

        new CheckStateTask().run();

        AssertState.assertForUpload(StorjMock.MODIFIED_FILE_1, FileMock.MODIFIED_FILE_1_NEWER);
    }

    @Test
    public void modifiedBothSameSizeTest() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        storjMock.modifyFile(StorjMock.FILE_1, StorjMock.MODIFIED_FILE_1_SAMESIZE);
        filesMock.modifyFile(FileMock.FILE_1, FileMock.MODIFIED_FILE_1_SAMESIZE);

        new CheckStateTask().run();

        AssertState.assertSynced(StorjMock.MODIFIED_FILE_1_SAMESIZE, FileMock.MODIFIED_FILE_1_SAMESIZE);
    }

    @Test
    public void modifiedBothSameTimeTest() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        storjMock.modifyFile(StorjMock.FILE_1, StorjMock.MODIFIED_FILE_1_NEWER);
        filesMock.modifyFile(FileMock.FILE_1, FileMock.MODIFIED_FILE_1_NEWER);

        new CheckStateTask().run();

        AssertState.assertForDownload(StorjMock.MODIFIED_FILE_1_NEWER, FileMock.MODIFIED_FILE_1_NEWER);
    }

    @Test
    public void modifiedBothCloudNewerTest() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        storjMock.modifyFile(StorjMock.FILE_1, StorjMock.MODIFIED_FILE_1_NEWER);
        filesMock.modifyFile(FileMock.FILE_1, FileMock.MODIFIED_FILE_1);

        new CheckStateTask().run();

        AssertState.assertForDownload(StorjMock.MODIFIED_FILE_1_NEWER, FileMock.MODIFIED_FILE_1);
    }

    @Test
    public void modifiedBothLocalNewerTest() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        storjMock.modifyFile(StorjMock.FILE_1, StorjMock.MODIFIED_FILE_1);
        filesMock.modifyFile(FileMock.FILE_1, FileMock.MODIFIED_FILE_1_NEWER);

        new CheckStateTask().run();

        AssertState.assertForUpload(StorjMock.MODIFIED_FILE_1, FileMock.MODIFIED_FILE_1_NEWER);
    }

    @Test
    public void resyncForDownloadNoLocal() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock();

        DB.addForDownload(StorjMock.FILE_1);

        new CheckStateTask().run();

        AssertState.assertForDownload(StorjMock.FILE_1);
    }

    @Test
    public void resyncForDownloadCloudModifiedTest() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.addForDownload(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        storjMock.modifyFile(StorjMock.FILE_1, StorjMock.MODIFIED_FILE_1);

        new CheckStateTask().run();

        AssertState.assertForDownload(StorjMock.MODIFIED_FILE_1, FileMock.FILE_1);
    }

    @Test
    public void resyncForDownloadLocalModifiedTest() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.addForDownload(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        filesMock.modifyFile(FileMock.FILE_1, FileMock.MODIFIED_FILE_1);

        new CheckStateTask().run();

        AssertState.assertForUpload(StorjMock.FILE_1, FileMock.MODIFIED_FILE_1);
    }

    @Test
    public void resyncForDownloadBothModifiedSameSize() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.addForDownload(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        storjMock.modifyFile(StorjMock.FILE_1, StorjMock.MODIFIED_FILE_1_SAMESIZE);
        filesMock.modifyFile(FileMock.FILE_1, FileMock.MODIFIED_FILE_1_SAMESIZE);

        new CheckStateTask().run();

        AssertState.assertSynced(StorjMock.MODIFIED_FILE_1_SAMESIZE, FileMock.MODIFIED_FILE_1_SAMESIZE);
    }

    @Test
    public void resyncForDownloadBothModifiedSameTime() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.addForDownload(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        storjMock.modifyFile(StorjMock.FILE_1, StorjMock.MODIFIED_FILE_1_NEWER);
        filesMock.modifyFile(FileMock.FILE_1, FileMock.MODIFIED_FILE_1_NEWER);

        new CheckStateTask().run();

        AssertState.assertForDownload(StorjMock.MODIFIED_FILE_1_NEWER, FileMock.MODIFIED_FILE_1_NEWER);
    }

    @Test
    public void resyncForDownloadBothModifiedCloudNewer() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.addForDownload(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        storjMock.modifyFile(StorjMock.FILE_1, StorjMock.MODIFIED_FILE_1_NEWER);
        filesMock.modifyFile(FileMock.FILE_1, FileMock.MODIFIED_FILE_1);

        new CheckStateTask().run();

        AssertState.assertForDownload(StorjMock.MODIFIED_FILE_1_NEWER, FileMock.MODIFIED_FILE_1);
    }

    @Test
    public void resyncForDownloadBothModifiedLocalNewer() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.addForDownload(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        storjMock.modifyFile(StorjMock.FILE_1, StorjMock.MODIFIED_FILE_1);
        filesMock.modifyFile(FileMock.FILE_1, FileMock.MODIFIED_FILE_1_NEWER);

        new CheckStateTask().run();

        AssertState.assertForUpload(StorjMock.MODIFIED_FILE_1, FileMock.MODIFIED_FILE_1_NEWER);
    }

    @Test
    public void resyncForDownloadLocalDeleted() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.addForDownload(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        Files.deleteIfExists(FileMock.FILE_1.getPath());

        new CheckStateTask().run();

        AssertState.assertForCloudDelete(StorjMock.FILE_1, FileMock.FILE_1);
    }

    @Test
    public void resyncForDownloadCloudDeleted() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.addForDownload(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        Storj.getInstance().deleteFile(null, StorjMock.FILE_1, new TestDeleteFileCallback());

        new CheckStateTask().run();

        AssertState.assertForLocalDelete(StorjMock.FILE_1, FileMock.FILE_1);
    }

    @Test
    public void resyncForDownloadBothDeleted() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.addForDownload(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        Storj.getInstance().deleteFile(null, StorjMock.FILE_1, new TestDeleteFileCallback());
        Files.deleteIfExists(FileMock.FILE_1.getPath());

        new CheckStateTask().run();

        AssertState.assertSleepEmptyDB();
    }

    @Test
    public void resyncForUploadNoCloud() throws Exception {
        new StorjMock();
        new FilesMock(FileMock.FILE_1);

        DB.addForUpload(FileMock.FILE_1.getPath());

        new CheckStateTask().run();

        AssertState.assertForUpload(FileMock.FILE_1);
    }

    @Test
    public void resyncForUploadCloudModifiedTest() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.addForUpload(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        storjMock.modifyFile(StorjMock.FILE_1, StorjMock.MODIFIED_FILE_1);

        new CheckStateTask().run();

        AssertState.assertForDownload(StorjMock.MODIFIED_FILE_1, FileMock.FILE_1);
    }

    @Test
    public void resyncForUploadLocalModifiedTest() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.addForUpload(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        filesMock.modifyFile(FileMock.FILE_1, FileMock.MODIFIED_FILE_1);

        new CheckStateTask().run();

        AssertState.assertForUpload(StorjMock.FILE_1, FileMock.MODIFIED_FILE_1);
    }

    @Test
    public void resyncForUploadBothModifiedSameSize() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.addForUpload(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        storjMock.modifyFile(StorjMock.FILE_1, StorjMock.MODIFIED_FILE_1_SAMESIZE);
        filesMock.modifyFile(FileMock.FILE_1, FileMock.MODIFIED_FILE_1_SAMESIZE);

        new CheckStateTask().run();

        AssertState.assertSynced(StorjMock.MODIFIED_FILE_1_SAMESIZE, FileMock.MODIFIED_FILE_1_SAMESIZE);
    }

    @Test
    public void resyncForUploadBothModifiedSameTime() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.addForUpload(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        storjMock.modifyFile(StorjMock.FILE_1, StorjMock.MODIFIED_FILE_1_NEWER);
        filesMock.modifyFile(FileMock.FILE_1, FileMock.MODIFIED_FILE_1_NEWER);

        new CheckStateTask().run();

        AssertState.assertForDownload(StorjMock.MODIFIED_FILE_1_NEWER, FileMock.MODIFIED_FILE_1_NEWER);
    }

    @Test
    public void resyncForUploadBothModifiedCloudNewer() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.addForUpload(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        storjMock.modifyFile(StorjMock.FILE_1, StorjMock.MODIFIED_FILE_1_NEWER);
        filesMock.modifyFile(FileMock.FILE_1, FileMock.MODIFIED_FILE_1);

        new CheckStateTask().run();

        AssertState.assertForDownload(StorjMock.MODIFIED_FILE_1_NEWER, FileMock.MODIFIED_FILE_1);
    }

    @Test
    public void resyncForUploadBothModifiedLocalNewer() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.addForUpload(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        storjMock.modifyFile(StorjMock.FILE_1, StorjMock.MODIFIED_FILE_1);
        filesMock.modifyFile(FileMock.FILE_1, FileMock.MODIFIED_FILE_1_NEWER);

        new CheckStateTask().run();

        AssertState.assertForUpload(StorjMock.MODIFIED_FILE_1, FileMock.MODIFIED_FILE_1_NEWER);
    }

    @Test
    public void resyncForUploadLocalDeleted() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.addForUpload(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        Files.deleteIfExists(FileMock.FILE_1.getPath());

        new CheckStateTask().run();

        AssertState.assertForCloudDelete(StorjMock.FILE_1, FileMock.FILE_1);
    }

    @Test
    public void resyncForUploadCloudDeleted() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.addForUpload(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        Storj.getInstance().deleteFile(null, StorjMock.FILE_1, new TestDeleteFileCallback());

        new CheckStateTask().run();

        AssertState.assertForLocalDelete(StorjMock.FILE_1, FileMock.FILE_1);
    }

    @Test
    public void resyncForUploadBothDeleted() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.addForUpload(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        Storj.getInstance().deleteFile(null, StorjMock.FILE_1, new TestDeleteFileCallback());
        Files.deleteIfExists(FileMock.FILE_1.getPath());

        new CheckStateTask().run();

        AssertState.assertSleepEmptyDB();
    }

    @Test
    public void resyncForCloudDelete() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        Files.deleteIfExists(FileMock.FILE_1.getPath());
        DB.setForCloudDelete(StorjMock.FILE_1);

        new CheckStateTask().run();

        AssertState.assertForCloudDelete(StorjMock.FILE_1, FileMock.FILE_1);
    }

    @Test
    public void resyncForCloudDeleteCloudModifiedTest() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        Files.deleteIfExists(FileMock.FILE_1.getPath());
        DB.setForCloudDelete(StorjMock.FILE_1);
        storjMock.modifyFile(StorjMock.FILE_1, StorjMock.MODIFIED_FILE_1);

        new CheckStateTask().run();

        AssertState.assertForDownload(StorjMock.MODIFIED_FILE_1);
    }

    @Test
    public void resyncForCloudDeleteLocalModifiedTest() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        Files.deleteIfExists(FileMock.FILE_1.getPath());
        DB.setForCloudDelete(StorjMock.FILE_1);
        filesMock.addFile(FileMock.MODIFIED_FILE_1);

        new CheckStateTask().run();

        AssertState.assertForUpload(StorjMock.FILE_1, FileMock.MODIFIED_FILE_1);
    }

    @Test
    public void resyncForCloudDeleteBothModifiedSameSize() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        Files.deleteIfExists(FileMock.FILE_1.getPath());
        DB.setForCloudDelete(StorjMock.FILE_1);
        storjMock.modifyFile(StorjMock.FILE_1, StorjMock.MODIFIED_FILE_1_SAMESIZE);
        filesMock.addFile(FileMock.MODIFIED_FILE_1_SAMESIZE);

        new CheckStateTask().run();

        AssertState.assertSynced(StorjMock.MODIFIED_FILE_1_SAMESIZE, FileMock.MODIFIED_FILE_1_SAMESIZE);
    }

    @Test
    public void resyncForCloudDeleteBothModifiedSameTime() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        Files.deleteIfExists(FileMock.FILE_1.getPath());
        DB.setForCloudDelete(StorjMock.FILE_1);
        storjMock.modifyFile(StorjMock.FILE_1, StorjMock.MODIFIED_FILE_1_NEWER);
        filesMock.addFile(FileMock.MODIFIED_FILE_1_NEWER);

        new CheckStateTask().run();

        AssertState.assertForDownload(StorjMock.MODIFIED_FILE_1_NEWER, FileMock.MODIFIED_FILE_1_NEWER);
    }

    @Test
    public void resyncForCloudDeleteBothModifiedCloudNewer() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        Files.deleteIfExists(FileMock.FILE_1.getPath());
        DB.setForCloudDelete(StorjMock.FILE_1);
        storjMock.modifyFile(StorjMock.FILE_1, StorjMock.MODIFIED_FILE_1_NEWER);
        filesMock.addFile(FileMock.MODIFIED_FILE_1);

        new CheckStateTask().run();

        AssertState.assertForDownload(StorjMock.MODIFIED_FILE_1_NEWER, FileMock.MODIFIED_FILE_1);
    }

    @Test
    public void resyncForCloudDeleteBothModifiedLocalNewer() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        Files.deleteIfExists(FileMock.FILE_1.getPath());
        DB.setForCloudDelete(StorjMock.FILE_1);
        storjMock.modifyFile(StorjMock.FILE_1, StorjMock.MODIFIED_FILE_1);
        filesMock.addFile(FileMock.MODIFIED_FILE_1_NEWER);

        new CheckStateTask().run();

        AssertState.assertForUpload(StorjMock.MODIFIED_FILE_1, FileMock.MODIFIED_FILE_1_NEWER);
    }

    @Test
    public void resyncForCloudDeleteBothDeleted() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        Files.deleteIfExists(FileMock.FILE_1.getPath());
        DB.setForCloudDelete(StorjMock.FILE_1);
        Storj.getInstance().deleteFile(null, StorjMock.FILE_1, new TestDeleteFileCallback());

        new CheckStateTask().run();

        AssertState.assertSleepEmptyDB();
    }

    @Test
    public void resyncForLocalDelete() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        Storj.getInstance().deleteFile(null, StorjMock.FILE_1, new TestDeleteFileCallback());
        DB.setForLocalDelete(FileMock.FILE_1.getPath());

        new CheckStateTask().run();

        AssertState.assertForLocalDelete(StorjMock.FILE_1, FileMock.FILE_1);
    }

    @Test
    public void resyncForLocalDeleteCloudModifiedTest() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        Storj.getInstance().deleteFile(null, StorjMock.FILE_1, new TestDeleteFileCallback());
        DB.setForLocalDelete(FileMock.FILE_1.getPath());
        storjMock.addFile(StorjMock.MODIFIED_FILE_1);

        new CheckStateTask().run();

        AssertState.assertForDownload(StorjMock.MODIFIED_FILE_1, FileMock.FILE_1);
    }

    @Test
    public void resyncForLocalDeleteLocalModifiedTest() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        Storj.getInstance().deleteFile(null, StorjMock.FILE_1, new TestDeleteFileCallback());
        DB.setForLocalDelete(FileMock.FILE_1.getPath());
        filesMock.modifyFile(FileMock.FILE_1, FileMock.MODIFIED_FILE_1);

        new CheckStateTask().run();

        AssertState.assertForUpload(FileMock.MODIFIED_FILE_1);
    }

    @Test
    public void resyncForLocalDeleteBothModifiedSameSize() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        Storj.getInstance().deleteFile(null, StorjMock.FILE_1, new TestDeleteFileCallback());
        DB.setForLocalDelete(FileMock.FILE_1.getPath());
        storjMock.addFile(StorjMock.MODIFIED_FILE_1_SAMESIZE);
        filesMock.modifyFile(FileMock.FILE_1, FileMock.MODIFIED_FILE_1_SAMESIZE);

        new CheckStateTask().run();

        AssertState.assertSynced(StorjMock.MODIFIED_FILE_1_SAMESIZE, FileMock.MODIFIED_FILE_1_SAMESIZE);
    }

    @Test
    public void resyncForLocalDeleteBothModifiedSameTime() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        Storj.getInstance().deleteFile(null, StorjMock.FILE_1, new TestDeleteFileCallback());
        DB.setForLocalDelete(FileMock.FILE_1.getPath());
        storjMock.addFile(StorjMock.MODIFIED_FILE_1_NEWER);
        filesMock.modifyFile(FileMock.FILE_1, FileMock.MODIFIED_FILE_1_NEWER);

        new CheckStateTask().run();

        AssertState.assertForDownload(StorjMock.MODIFIED_FILE_1_NEWER, FileMock.MODIFIED_FILE_1_NEWER);
    }

    @Test
    public void resyncForLocalDeleteBothModifiedCloudNewer() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        Storj.getInstance().deleteFile(null, StorjMock.FILE_1, new TestDeleteFileCallback());
        DB.setForLocalDelete(FileMock.FILE_1.getPath());
        storjMock.addFile(StorjMock.MODIFIED_FILE_1_NEWER);
        filesMock.modifyFile(FileMock.FILE_1, FileMock.MODIFIED_FILE_1);

        new CheckStateTask().run();

        AssertState.assertForDownload(StorjMock.MODIFIED_FILE_1_NEWER, FileMock.MODIFIED_FILE_1);
    }

    @Test
    public void resyncForLocalDeleteBothModifiedLocalNewer() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        Storj.getInstance().deleteFile(null, StorjMock.FILE_1, new TestDeleteFileCallback());
        DB.setForLocalDelete(FileMock.FILE_1.getPath());
        storjMock.addFile(StorjMock.MODIFIED_FILE_1);
        filesMock.modifyFile(FileMock.FILE_1, FileMock.MODIFIED_FILE_1_NEWER);

        new CheckStateTask().run();

        AssertState.assertForUpload(StorjMock.MODIFIED_FILE_1, FileMock.MODIFIED_FILE_1_NEWER);
    }

    @Test
    public void resyncForLocalDeleteBothDeleted() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        Storj.getInstance().deleteFile(null, StorjMock.FILE_1, new TestDeleteFileCallback());
        DB.setForLocalDelete(FileMock.FILE_1.getPath());
        Files.deleteIfExists(FileMock.FILE_1.getPath());

        new CheckStateTask().run();

        AssertState.assertSleepEmptyDB();
    }

    @Test
    public void resyncFailedDownload() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.addForDownload(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.setDownloadFailed(StorjMock.FILE_1, FileMock.FILE_1.getPath());

        new CheckStateTask().run();

        AssertState.assertForDownloadFailed(StorjMock.FILE_1, FileMock.FILE_1);
    }

    @Test
    public void resyncFailedDownloadNoLocal() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock();

        DB.addForDownload(StorjMock.FILE_1);
        DB.setDownloadFailed(StorjMock.FILE_1, FileMock.FILE_1.getPath());

        new CheckStateTask().run();

        AssertState.assertForDownloadFailed(StorjMock.FILE_1);
    }

    @Test
    public void resyncFailedDownloadCloudModifiedTest() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.addForDownload(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.setDownloadFailed(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        storjMock.modifyFile(StorjMock.FILE_1, StorjMock.MODIFIED_FILE_1);

        new CheckStateTask().run();

        AssertState.assertForDownload(StorjMock.MODIFIED_FILE_1, FileMock.FILE_1);
    }

    @Test
    public void resyncFailedDownloadCloudModifiedNoLocalTest() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        new FilesMock();

        DB.addForDownload(StorjMock.FILE_1);
        DB.setDownloadFailed(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        storjMock.modifyFile(StorjMock.FILE_1, StorjMock.MODIFIED_FILE_1);

        new CheckStateTask().run();

        AssertState.assertForDownload(StorjMock.MODIFIED_FILE_1);
    }

    @Test
    public void resyncFailedDownloadLocalModifiedTest() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.addForDownload(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.setDownloadFailed(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        filesMock.modifyFile(FileMock.FILE_1, FileMock.MODIFIED_FILE_1);

        new CheckStateTask().run();

        AssertState.assertForUpload(StorjMock.FILE_1, FileMock.MODIFIED_FILE_1);
    }

    @Test
    public void resyncFailedDownloadBothModifiedSameSize() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.addForDownload(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.setDownloadFailed(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        storjMock.modifyFile(StorjMock.FILE_1, StorjMock.MODIFIED_FILE_1_SAMESIZE);
        filesMock.modifyFile(FileMock.FILE_1, FileMock.MODIFIED_FILE_1_SAMESIZE);

        new CheckStateTask().run();

        AssertState.assertSynced(StorjMock.MODIFIED_FILE_1_SAMESIZE, FileMock.MODIFIED_FILE_1_SAMESIZE);
    }

    @Test
    public void resyncFailedDownloadBothModifiedSameTime() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.addForDownload(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.setDownloadFailed(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        storjMock.modifyFile(StorjMock.FILE_1, StorjMock.MODIFIED_FILE_1_NEWER);
        filesMock.modifyFile(FileMock.FILE_1, FileMock.MODIFIED_FILE_1_NEWER);

        new CheckStateTask().run();

        AssertState.assertForDownload(StorjMock.MODIFIED_FILE_1_NEWER, FileMock.MODIFIED_FILE_1_NEWER);
    }

    @Test
    public void resyncFailedDownloadBothModifiedCloudNewer() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.addForDownload(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.setDownloadFailed(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        storjMock.modifyFile(StorjMock.FILE_1, StorjMock.MODIFIED_FILE_1_NEWER);
        filesMock.modifyFile(FileMock.FILE_1, FileMock.MODIFIED_FILE_1);

        new CheckStateTask().run();

        AssertState.assertForDownload(StorjMock.MODIFIED_FILE_1_NEWER, FileMock.MODIFIED_FILE_1);
    }

    @Test
    public void resyncFailedDownloadBothModifiedLocalNewer() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.addForDownload(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.setDownloadFailed(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        storjMock.modifyFile(StorjMock.FILE_1, StorjMock.MODIFIED_FILE_1);
        filesMock.modifyFile(FileMock.FILE_1, FileMock.MODIFIED_FILE_1_NEWER);

        new CheckStateTask().run();

        AssertState.assertForUpload(StorjMock.MODIFIED_FILE_1, FileMock.MODIFIED_FILE_1_NEWER);
    }

    @Test
    public void resyncFailedDownloadCloudDeleted() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.addForDownload(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.setDownloadFailed(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        Storj.getInstance().deleteFile(null, StorjMock.FILE_1, new TestDeleteFileCallback());

        new CheckStateTask().run();

        AssertState.assertForLocalDelete(StorjMock.FILE_1, FileMock.FILE_1);
    }

    @Test
    public void resyncFailedDownloadCloudDeletedNoLocal() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock();

        DB.addForDownload(StorjMock.FILE_1);
        DB.setDownloadFailed(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        Storj.getInstance().deleteFile(null, StorjMock.FILE_1, new TestDeleteFileCallback());

        new CheckStateTask().run();

        AssertState.assertSleepEmptyDB();
    }

    @Test
    public void resyncFailedDownloadLocalDeleted() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.addForDownload(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.setDownloadFailed(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        Files.deleteIfExists(FileMock.FILE_1.getPath());

        new CheckStateTask().run();

        AssertState.assertForDownload(StorjMock.FILE_1);
    }

    @Test
    public void resyncFailedDownloadBothDeleted() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.addForDownload(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.setDownloadFailed(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        Storj.getInstance().deleteFile(null, StorjMock.FILE_1, new TestDeleteFileCallback());
        Files.deleteIfExists(FileMock.FILE_1.getPath());

        new CheckStateTask().run();

        AssertState.assertSleepEmptyDB();
    }

    @Test
    public void resyncFailedUpload() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.addForUpload(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.setUploadFailed(FileMock.FILE_1.getPath());

        new CheckStateTask().run();

        AssertState.assertForUploadFailed(StorjMock.FILE_1, FileMock.FILE_1);
    }

    @Test
    public void resyncFailedUploadNoCloud() throws Exception {
        new StorjMock();
        new FilesMock(FileMock.FILE_1);

        DB.addForUpload(FileMock.FILE_1.getPath());
        DB.setUploadFailed(FileMock.FILE_1.getPath());

        new CheckStateTask().run();

        AssertState.assertForUploadFailed(FileMock.FILE_1);
    }

    @Test
    public void resyncFailedUploadCloudModifiedTest() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.addForUpload(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.setUploadFailed(FileMock.FILE_1.getPath());
        storjMock.modifyFile(StorjMock.FILE_1, StorjMock.MODIFIED_FILE_1);

        new CheckStateTask().run();

        AssertState.assertForDownload(StorjMock.MODIFIED_FILE_1, FileMock.FILE_1);
    }

    @Test
    public void resyncFailedUploadLocalModifiedTest() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.addForUpload(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.setUploadFailed(FileMock.FILE_1.getPath());
        filesMock.modifyFile(FileMock.FILE_1, FileMock.MODIFIED_FILE_1);

        new CheckStateTask().run();

        AssertState.assertForUpload(StorjMock.FILE_1, FileMock.MODIFIED_FILE_1);
    }

    @Test
    public void resyncFailedUploadLocalModifiedNoCloudTest() throws Exception {
        new StorjMock();
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.addForUpload(FileMock.FILE_1.getPath());
        DB.setUploadFailed(FileMock.FILE_1.getPath());
        filesMock.modifyFile(FileMock.FILE_1, FileMock.MODIFIED_FILE_1);

        new CheckStateTask().run();

        AssertState.assertForUpload(FileMock.MODIFIED_FILE_1);
    }

    @Test
    public void resyncFailedUploadBothModifiedSameSize() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.addForUpload(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.setUploadFailed(FileMock.FILE_1.getPath());
        storjMock.modifyFile(StorjMock.FILE_1, StorjMock.MODIFIED_FILE_1_SAMESIZE);
        filesMock.modifyFile(FileMock.FILE_1, FileMock.MODIFIED_FILE_1_SAMESIZE);

        new CheckStateTask().run();

        AssertState.assertSynced(StorjMock.MODIFIED_FILE_1_SAMESIZE, FileMock.MODIFIED_FILE_1_SAMESIZE);
    }

    @Test
    public void resyncFailedUploadBothModifiedSameTime() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.addForUpload(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.setUploadFailed(FileMock.FILE_1.getPath());
        storjMock.modifyFile(StorjMock.FILE_1, StorjMock.MODIFIED_FILE_1_NEWER);
        filesMock.modifyFile(FileMock.FILE_1, FileMock.MODIFIED_FILE_1_NEWER);

        new CheckStateTask().run();

        AssertState.assertForDownload(StorjMock.MODIFIED_FILE_1_NEWER, FileMock.MODIFIED_FILE_1_NEWER);
    }

    @Test
    public void resyncFailedUploadBothModifiedCloudNewer() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.addForUpload(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.setUploadFailed(FileMock.FILE_1.getPath());
        storjMock.modifyFile(StorjMock.FILE_1, StorjMock.MODIFIED_FILE_1_NEWER);
        filesMock.modifyFile(FileMock.FILE_1, FileMock.MODIFIED_FILE_1);

        new CheckStateTask().run();

        AssertState.assertForDownload(StorjMock.MODIFIED_FILE_1_NEWER, FileMock.MODIFIED_FILE_1);
    }

    @Test
    public void resyncFailedUploadBothModifiedLocalNewer() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.addForUpload(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.setUploadFailed(FileMock.FILE_1.getPath());
        storjMock.modifyFile(StorjMock.FILE_1, StorjMock.MODIFIED_FILE_1);
        filesMock.modifyFile(FileMock.FILE_1, FileMock.MODIFIED_FILE_1_NEWER);

        new CheckStateTask().run();

        AssertState.assertForUpload(StorjMock.MODIFIED_FILE_1, FileMock.MODIFIED_FILE_1_NEWER);
    }

    @Test
    public void resyncFailedUploadCloudDeleted() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.addForUpload(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.setUploadFailed(FileMock.FILE_1.getPath());
        Storj.getInstance().deleteFile(null, StorjMock.FILE_1, new TestDeleteFileCallback());

        new CheckStateTask().run();

        AssertState.assertForLocalDelete(StorjMock.FILE_1, FileMock.FILE_1);
    }

    @Test
    public void resyncFailedUploadLocalDeleted() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.addForUpload(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.setUploadFailed(FileMock.FILE_1.getPath());
        Files.deleteIfExists(FileMock.FILE_1.getPath());

        new CheckStateTask().run();

        AssertState.assertForCloudDelete(StorjMock.FILE_1, FileMock.FILE_1);
    }

    @Test
    public void resyncFailedUploadLocalDeletedNoCloud() throws Exception {
        new StorjMock();
        new FilesMock(FileMock.FILE_1);

        DB.addForUpload(FileMock.FILE_1.getPath());
        DB.setUploadFailed(FileMock.FILE_1.getPath());
        Files.deleteIfExists(FileMock.FILE_1.getPath());

        new CheckStateTask().run();

        AssertState.assertSleepEmptyDB();
    }

    @Test
    public void resyncFailedUploadBothDeleted() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.addForUpload(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        DB.setUploadFailed(FileMock.FILE_1.getPath());
        Storj.getInstance().deleteFile(null, StorjMock.FILE_1, new TestDeleteFileCallback());
        Files.deleteIfExists(FileMock.FILE_1.getPath());

        new CheckStateTask().run();

        AssertState.assertSleepEmptyDB();
    }

    @Test
    public void fileOpsInProgressTest() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_2);
        new FileWatcherMock(true);

        new CheckStateTask().run();

        AssertState.assertAllEmpty();
    }

    private class TestDeleteFileCallback implements DeleteFileCallback {

        @Override
        public void onFileDeleted() {
        }

        @Override
        public void onError(String message) {
            throw new IllegalStateException(message);
        }

    }

}
