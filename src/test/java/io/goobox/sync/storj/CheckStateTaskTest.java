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
    public void LocalFileNoCloudTest() throws Exception {
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
    public void cloudDeletedTest() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        Storj.getInstance().deleteFile(null, StorjMock.FILE_1, new TestDeleteFileCallback());

        new CheckStateTask().run();

        AssertState.assertForLocalDelete(StorjMock.FILE_1, FileMock.FILE_1);
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
    public void fileOpsInProgressTest() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_2);
        new FileWatcherMock(true);

        new CheckStateTask().run();

        AssertState.assertAllEmpty();
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
