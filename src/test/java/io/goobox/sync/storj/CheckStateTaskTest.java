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

import java.nio.file.Files;

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
import io.goobox.sync.storj.mocks.AppMock;
import io.goobox.sync.storj.mocks.DBMock;
import io.goobox.sync.storj.mocks.FileMock;
import io.goobox.sync.storj.mocks.FileWatcherMock;
import io.goobox.sync.storj.mocks.FilesMock;
import io.goobox.sync.storj.mocks.StorjMock;
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
    public void noCloudAndLocal() throws Exception {
        new StorjMock();
        new FilesMock();

        new CheckStateTask().run();

        AssertState.assertSleepEmptyDB();
    }

    @Test
    public void cloudAndLocalInSync() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());

        new CheckStateTask().run();

        AssertState.assertSynced(StorjMock.FILE_1, FileMock.FILE_1);
    }

    @Test
    public void cloudFileNoLocal() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock();

        new CheckStateTask().run();

        AssertState.assertForDownload(StorjMock.FILE_1);
    }

    @Test
    public void localFileNoCloud() throws Exception {
        new StorjMock();
        new FilesMock(FileMock.FILE_1);

        new CheckStateTask().run();

        AssertState.assertForUpload(FileMock.FILE_1);
    }

    @Test
    public void encryptedCloudNoLocal() throws Exception {
        new StorjMock(StorjMock.ENCRYPTED_FILE);
        new FilesMock();

        new CheckStateTask().run();

        AssertState.assertSleepEmptyDB();
    }

    @Test
    public void encryptedCloudAndLocal() throws Exception {
        new StorjMock(StorjMock.ENCRYPTED_FILE);
        new FilesMock(FileMock.ENCRYPTED_FILE);

        new CheckStateTask().run();

        AssertState.assertSleepEmptyDB();
    }

    @Test
    public void localDeleted() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        Files.deleteIfExists(FileMock.FILE_1.getPath());

        new CheckStateTask().run();

        AssertState.assertForCloudDelete(StorjMock.FILE_1, FileMock.FILE_1);
    }

    @Test
    public void localDeletedCloudModified() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        storjMock.modifyFile(StorjMock.FILE_1, StorjMock.MODIFIED_FILE_1);
        Files.deleteIfExists(FileMock.FILE_1.getPath());

        new CheckStateTask().run();

        AssertState.assertForDownload(StorjMock.MODIFIED_FILE_1);
    }

    @Test
    public void cloudDeleted() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        StorjUtil.deleteFile(StorjMock.FILE_1);

        new CheckStateTask().run();

        AssertState.assertForLocalDelete(StorjMock.FILE_1, FileMock.FILE_1);
    }

    @Test
    public void cloudDeletedLocalModified() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        StorjUtil.deleteFile(StorjMock.FILE_1);
        filesMock.modifyFile(FileMock.FILE_1, FileMock.MODIFIED_FILE_1);

        new CheckStateTask().run();

        AssertState.assertForUpload(FileMock.MODIFIED_FILE_1);
    }

    @Test
    public void bothDeleted() throws Exception {
    }

    @Test
    public void modifiedCloud() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        storjMock.modifyFile(StorjMock.FILE_1, StorjMock.MODIFIED_FILE_1);

        new CheckStateTask().run();

        AssertState.assertForDownload(StorjMock.MODIFIED_FILE_1, FileMock.FILE_1);
    }

    @Test
    public void modifiedLocal() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        filesMock.modifyFile(FileMock.FILE_1, FileMock.MODIFIED_FILE_1);

        new CheckStateTask().run();

        AssertState.assertForUpload(StorjMock.FILE_1, FileMock.MODIFIED_FILE_1);
    }

    @Test
    public void sameSizeCloudAndLocalNoDB() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        new CheckStateTask().run();

        AssertState.assertSynced(StorjMock.FILE_1, FileMock.FILE_1);
    }

    @Test
    public void modifiedBothSameTimeNoDB() throws Exception {
        new StorjMock(StorjMock.MODIFIED_FILE_1_NEWER);
        new FilesMock(FileMock.MODIFIED_FILE_1_NEWER);

        new CheckStateTask().run();

        AssertState.assertForDownload(StorjMock.MODIFIED_FILE_1_NEWER, FileMock.MODIFIED_FILE_1_NEWER);
    }

    @Test
    public void modifiedBothCloudNewerNoDB() throws Exception {
        new StorjMock(StorjMock.MODIFIED_FILE_1_NEWER);
        new FilesMock(FileMock.MODIFIED_FILE_1);

        new CheckStateTask().run();

        AssertState.assertForDownload(StorjMock.MODIFIED_FILE_1_NEWER, FileMock.MODIFIED_FILE_1);
    }

    @Test
    public void modifiedBothLocalNewerNoDB() throws Exception {
        new StorjMock(StorjMock.MODIFIED_FILE_1);
        new FilesMock(FileMock.MODIFIED_FILE_1_NEWER);

        new CheckStateTask().run();

        AssertState.assertForUpload(StorjMock.MODIFIED_FILE_1, FileMock.MODIFIED_FILE_1_NEWER);
    }

    @Test
    public void modifiedBothSameSize() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        storjMock.modifyFile(StorjMock.FILE_1, StorjMock.MODIFIED_FILE_1_SAMESIZE);
        filesMock.modifyFile(FileMock.FILE_1, FileMock.MODIFIED_FILE_1_SAMESIZE);

        new CheckStateTask().run();

        AssertState.assertSynced(StorjMock.MODIFIED_FILE_1_SAMESIZE, FileMock.MODIFIED_FILE_1_SAMESIZE);
    }

    @Test
    public void modifiedBothSameTime() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        storjMock.modifyFile(StorjMock.FILE_1, StorjMock.MODIFIED_FILE_1_NEWER);
        filesMock.modifyFile(FileMock.FILE_1, FileMock.MODIFIED_FILE_1_NEWER);

        new CheckStateTask().run();

        AssertState.assertForDownload(StorjMock.MODIFIED_FILE_1_NEWER, FileMock.MODIFIED_FILE_1_NEWER);
    }

    @Test
    public void modifiedBothCloudNewer() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        storjMock.modifyFile(StorjMock.FILE_1, StorjMock.MODIFIED_FILE_1_NEWER);
        filesMock.modifyFile(FileMock.FILE_1, FileMock.MODIFIED_FILE_1);

        new CheckStateTask().run();

        AssertState.assertForDownload(StorjMock.MODIFIED_FILE_1_NEWER, FileMock.MODIFIED_FILE_1);
    }

    @Test
    public void modifiedBothLocalNewer() throws Exception {
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
    public void resyncForDownloadCloudModified() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.addForDownload(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        storjMock.modifyFile(StorjMock.FILE_1, StorjMock.MODIFIED_FILE_1);

        new CheckStateTask().run();

        AssertState.assertForDownload(StorjMock.MODIFIED_FILE_1, FileMock.FILE_1);
    }

    @Test
    public void resyncForDownloadLocalModified() throws Exception {
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
        StorjUtil.deleteFile(StorjMock.FILE_1);

        new CheckStateTask().run();

        AssertState.assertForLocalDelete(StorjMock.FILE_1, FileMock.FILE_1);
    }

    @Test
    public void resyncForDownloadBothDeleted() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.addForDownload(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        StorjUtil.deleteFile(StorjMock.FILE_1);
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
    public void resyncForUploadCloudModified() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.addForUpload(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        storjMock.modifyFile(StorjMock.FILE_1, StorjMock.MODIFIED_FILE_1);

        new CheckStateTask().run();

        AssertState.assertForDownload(StorjMock.MODIFIED_FILE_1, FileMock.FILE_1);
    }

    @Test
    public void resyncForUploadLocalModified() throws Exception {
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
        StorjUtil.deleteFile(StorjMock.FILE_1);

        new CheckStateTask().run();

        AssertState.assertForLocalDelete(StorjMock.FILE_1, FileMock.FILE_1);
    }

    @Test
    public void resyncForUploadBothDeleted() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.addForUpload(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        StorjUtil.deleteFile(StorjMock.FILE_1);
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
    public void resyncForCloudDeleteCloudModified() throws Exception {
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
    public void resyncForCloudDeleteLocalModified() throws Exception {
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
        StorjUtil.deleteFile(StorjMock.FILE_1);

        new CheckStateTask().run();

        AssertState.assertSleepEmptyDB();
    }

    @Test
    public void resyncForLocalDelete() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        StorjUtil.deleteFile(StorjMock.FILE_1);
        DB.setForLocalDelete(FileMock.FILE_1.getPath());

        new CheckStateTask().run();

        AssertState.assertForLocalDelete(StorjMock.FILE_1, FileMock.FILE_1);
    }

    @Test
    public void resyncForLocalDeleteCloudModified() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        StorjUtil.deleteFile(StorjMock.FILE_1);
        DB.setForLocalDelete(FileMock.FILE_1.getPath());
        storjMock.addFile(StorjMock.MODIFIED_FILE_1);

        new CheckStateTask().run();

        AssertState.assertForDownload(StorjMock.MODIFIED_FILE_1, FileMock.FILE_1);
    }

    @Test
    public void resyncForLocalDeleteLocalModified() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        FilesMock filesMock = new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        StorjUtil.deleteFile(StorjMock.FILE_1);
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
        StorjUtil.deleteFile(StorjMock.FILE_1);
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
        StorjUtil.deleteFile(StorjMock.FILE_1);
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
        StorjUtil.deleteFile(StorjMock.FILE_1);
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
        StorjUtil.deleteFile(StorjMock.FILE_1);
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
        StorjUtil.deleteFile(StorjMock.FILE_1);
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
    public void resyncFailedDownloadCloudModified() throws Exception {
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
    public void resyncFailedDownloadCloudModifiedNoLocal() throws Exception {
        StorjMock storjMock = new StorjMock(StorjMock.FILE_1);
        new FilesMock();

        DB.addForDownload(StorjMock.FILE_1);
        DB.setDownloadFailed(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        storjMock.modifyFile(StorjMock.FILE_1, StorjMock.MODIFIED_FILE_1);

        new CheckStateTask().run();

        AssertState.assertForDownload(StorjMock.MODIFIED_FILE_1);
    }

    @Test
    public void resyncFailedDownloadLocalModified() throws Exception {
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
        StorjUtil.deleteFile(StorjMock.FILE_1);

        new CheckStateTask().run();

        AssertState.assertForLocalDelete(StorjMock.FILE_1, FileMock.FILE_1);
    }

    @Test
    public void resyncFailedDownloadCloudDeletedNoLocal() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock();

        DB.addForDownload(StorjMock.FILE_1);
        DB.setDownloadFailed(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        StorjUtil.deleteFile(StorjMock.FILE_1);

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
        StorjUtil.deleteFile(StorjMock.FILE_1);
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
    public void resyncFailedUploadCloudModified() throws Exception {
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
    public void resyncFailedUploadLocalModified() throws Exception {
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
    public void resyncFailedUploadLocalModifiedNoCloud() throws Exception {
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
        StorjUtil.deleteFile(StorjMock.FILE_1);

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
        StorjUtil.deleteFile(StorjMock.FILE_1);
        Files.deleteIfExists(FileMock.FILE_1.getPath());

        new CheckStateTask().run();

        AssertState.assertSleepEmptyDB();
    }

    @Test
    public void fileOpsInProgress() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_2);
        new FileWatcherMock(true);

        new CheckStateTask().run();

        AssertState.assertAllEmpty();
    }

    @Test
    public void cloudAndLocalDirInSync() throws Exception {
        new StorjMock(StorjMock.DIR);
        new FilesMock(FileMock.DIR);

        DB.setSynced(StorjMock.DIR, FileMock.DIR.getPath());

        new CheckStateTask().run();

        AssertState.assertSynced(StorjMock.DIR, FileMock.DIR);
    }

    @Test
    public void cloudAndLocalDirNoDB() throws Exception {
        new StorjMock(StorjMock.DIR);
        new FilesMock(FileMock.DIR);

        new CheckStateTask().run();

        AssertState.assertSynced(StorjMock.DIR, FileMock.DIR);
    }

    @Test
    public void cloudDirNoLocal() throws Exception {
        new StorjMock(StorjMock.DIR);
        new FilesMock();

        new CheckStateTask().run();

        AssertState.assertForLocalCreateDir(StorjMock.DIR);
    }

    @Test
    public void localDirNoCloud() throws Exception {
        new StorjMock();
        new FilesMock(FileMock.DIR);

        new CheckStateTask().run();

        AssertState.assertForCloudCreateDir(FileMock.DIR);
    }

    @Test
    public void localDirDeleted() throws Exception {
        new StorjMock(StorjMock.DIR);
        new FilesMock(FileMock.DIR);

        DB.setSynced(StorjMock.DIR, FileMock.DIR.getPath());
        Files.deleteIfExists(FileMock.DIR.getPath());

        new CheckStateTask().run();

        AssertState.assertForCloudDelete(StorjMock.DIR, FileMock.DIR);
    }

    @Test
    public void cloudDirDeleted() throws Exception {
        new StorjMock(StorjMock.DIR);
        new FilesMock(FileMock.DIR);

        DB.setSynced(StorjMock.DIR, FileMock.DIR.getPath());
        StorjUtil.deleteFile(StorjMock.DIR);

        new CheckStateTask().run();

        AssertState.assertForLocalDelete(StorjMock.DIR, FileMock.DIR);
    }

    @Test
    public void bothDirsDeleted() throws Exception {
        new StorjMock(StorjMock.DIR);
        new FilesMock(FileMock.DIR);

        DB.setSynced(StorjMock.DIR, FileMock.DIR.getPath());
        StorjUtil.deleteFile(StorjMock.DIR);
        Files.deleteIfExists(FileMock.DIR.getPath());

        new CheckStateTask().run();

        AssertState.assertSleepEmptyDB();
    }

    @Test
    public void cloudAndLocalSubDirInSync() throws Exception {
        new StorjMock(StorjMock.DIR, StorjMock.SUB_DIR);
        new FilesMock(FileMock.DIR, FileMock.SUB_DIR);

        DB.setSynced(StorjMock.DIR, FileMock.DIR.getPath());
        DB.setSynced(StorjMock.SUB_DIR, FileMock.SUB_DIR.getPath());

        new CheckStateTask().run();

        AssertState.assertTaskQueue(SleepTask.class);
        assertEquals(2, DB.size());
        assertTrue(DB.contains(StorjMock.DIR));
        AssertSyncFile.assertWith(StorjMock.DIR, FileMock.DIR, SyncState.SYNCED);
        assertTrue(DB.contains(StorjMock.SUB_DIR));
        AssertSyncFile.assertWith(StorjMock.SUB_DIR, FileMock.SUB_DIR, SyncState.SYNCED);
    }

    @Test
    public void cloudAndLocalSubDirNoDB() throws Exception {
        new StorjMock(StorjMock.DIR, StorjMock.SUB_DIR);
        new FilesMock(FileMock.DIR, FileMock.SUB_DIR);

        new CheckStateTask().run();

        AssertState.assertTaskQueue(SleepTask.class);
        assertEquals(2, DB.size());
        assertTrue(DB.contains(StorjMock.DIR));
        AssertSyncFile.assertWith(StorjMock.DIR, FileMock.DIR, SyncState.SYNCED);
        assertTrue(DB.contains(StorjMock.SUB_DIR));
        AssertSyncFile.assertWith(StorjMock.SUB_DIR, FileMock.SUB_DIR, SyncState.SYNCED);
    }

    @Test
    public void cloudSubDirNoLocal() throws Exception {
        new StorjMock(StorjMock.SUB_DIR);
        new FilesMock();

        new CheckStateTask().run();

        AssertState.assertForLocalCreateDir(StorjMock.SUB_DIR);
    }

    @Test
    public void localSubDirNoCloud() throws Exception {
        new StorjMock();
        new FilesMock(FileMock.DIR, FileMock.SUB_DIR);

        new CheckStateTask().run();

        TaskQueue tasks = App.getInstance().getTaskQueue();
        assertEquals(CreateCloudDirTask.class, tasks.poll().getClass());
        assertEquals(CreateCloudDirTask.class, tasks.poll().getClass());
        assertEquals(CheckStateTask.class, tasks.poll().getClass());
        assertTrue(tasks.isEmpty());
        assertEquals(2, DB.size());
        assertTrue(DB.contains(FileMock.DIR.getPath()));
        AssertSyncFile.assertWith(FileMock.DIR, SyncState.FOR_CLOUD_CREATE_DIR);
        assertTrue(DB.contains(FileMock.SUB_DIR.getPath()));
        AssertSyncFile.assertWith(FileMock.SUB_DIR, SyncState.FOR_CLOUD_CREATE_DIR);
    }

    @Test
    public void localSubDirDeleted() throws Exception {
        new StorjMock(StorjMock.SUB_DIR);
        new FilesMock(FileMock.SUB_DIR);

        DB.setSynced(StorjMock.SUB_DIR, FileMock.SUB_DIR.getPath());
        Files.deleteIfExists(FileMock.SUB_DIR.getPath());

        new CheckStateTask().run();

        AssertState.assertForCloudDelete(StorjMock.SUB_DIR, FileMock.SUB_DIR);
    }

    @Test
    public void cloudSubDirDeleted() throws Exception {
        new StorjMock(StorjMock.DIR, StorjMock.SUB_DIR);
        new FilesMock(FileMock.DIR, FileMock.SUB_DIR);

        DB.setSynced(StorjMock.DIR, FileMock.DIR.getPath());
        DB.setSynced(StorjMock.SUB_DIR, FileMock.SUB_DIR.getPath());
        StorjUtil.deleteFile(StorjMock.SUB_DIR);

        new CheckStateTask().run();

        AssertState.assertTaskQueue(DeleteLocalFileTask.class);
        assertEquals(2, DB.size());
        assertTrue(DB.contains(StorjMock.DIR));
        AssertSyncFile.assertWith(StorjMock.DIR, FileMock.DIR, SyncState.SYNCED);
        assertTrue(DB.contains(StorjMock.SUB_DIR));
        AssertSyncFile.assertWith(StorjMock.SUB_DIR, FileMock.SUB_DIR, SyncState.FOR_LOCAL_DELETE);
    }

    @Test
    public void bothSubDirsDeleted() throws Exception {
        new StorjMock(StorjMock.SUB_DIR);
        new FilesMock(FileMock.SUB_DIR);

        DB.setSynced(StorjMock.SUB_DIR, FileMock.SUB_DIR.getPath());
        StorjUtil.deleteFile(StorjMock.SUB_DIR);
        Files.deleteIfExists(FileMock.SUB_DIR.getPath());

        new CheckStateTask().run();

        AssertState.assertSleepEmptyDB();
    }

    @Test
    public void cloudAndLocalSubFileInSync() throws Exception {
        new StorjMock(StorjMock.DIR, StorjMock.SUB_FILE);
        new FilesMock(FileMock.DIR, FileMock.SUB_FILE);

        DB.setSynced(StorjMock.DIR, FileMock.DIR.getPath());
        DB.setSynced(StorjMock.SUB_FILE, FileMock.SUB_FILE.getPath());

        new CheckStateTask().run();

        AssertState.assertTaskQueue(SleepTask.class);
        assertEquals(2, DB.size());
        assertTrue(DB.contains(StorjMock.DIR));
        AssertSyncFile.assertWith(StorjMock.DIR, FileMock.DIR, SyncState.SYNCED);
        assertTrue(DB.contains(StorjMock.SUB_FILE));
        AssertSyncFile.assertWith(StorjMock.SUB_FILE, FileMock.SUB_FILE, SyncState.SYNCED);
    }

    @Test
    public void cloudSubFileNoLocal() throws Exception {
        new StorjMock(StorjMock.SUB_FILE);
        new FilesMock();

        new CheckStateTask().run();

        AssertState.assertForDownload(StorjMock.SUB_FILE);
    }

    @Test
    public void localSubFileNoCloud() throws Exception {
        new StorjMock();
        new FilesMock(FileMock.DIR, FileMock.SUB_FILE);

        new CheckStateTask().run();

        TaskQueue tasks = App.getInstance().getTaskQueue();
        assertEquals(CreateCloudDirTask.class, tasks.poll().getClass());
        assertEquals(UploadFileTask.class, tasks.poll().getClass());
        assertEquals(CheckStateTask.class, tasks.poll().getClass());
        assertTrue(tasks.isEmpty());
        assertEquals(2, DB.size());
        assertTrue(DB.contains(FileMock.DIR.getPath()));
        AssertSyncFile.assertWith(FileMock.DIR, SyncState.FOR_CLOUD_CREATE_DIR);
        assertTrue(DB.contains(FileMock.SUB_FILE.getPath()));
        AssertSyncFile.assertWith(FileMock.SUB_FILE, SyncState.FOR_UPLOAD);
    }

    @Test
    public void localSubFileDeleted() throws Exception {
        new StorjMock(StorjMock.SUB_FILE);
        new FilesMock(FileMock.SUB_FILE);

        DB.setSynced(StorjMock.SUB_FILE, FileMock.SUB_FILE.getPath());
        Files.deleteIfExists(FileMock.SUB_FILE.getPath());

        new CheckStateTask().run();

        AssertState.assertForCloudDelete(StorjMock.SUB_FILE, FileMock.SUB_FILE);
    }

    @Test
    public void cloudSubFileDeleted() throws Exception {
        new StorjMock(StorjMock.DIR, StorjMock.SUB_FILE);
        new FilesMock(FileMock.DIR, FileMock.SUB_FILE);

        DB.setSynced(StorjMock.DIR, FileMock.DIR.getPath());
        DB.setSynced(StorjMock.SUB_FILE, FileMock.SUB_FILE.getPath());
        StorjUtil.deleteFile(StorjMock.SUB_FILE);

        new CheckStateTask().run();

        AssertState.assertTaskQueue(DeleteLocalFileTask.class);
        assertEquals(2, DB.size());
        assertTrue(DB.contains(StorjMock.DIR));
        AssertSyncFile.assertWith(StorjMock.DIR, FileMock.DIR, SyncState.SYNCED);
        assertTrue(DB.contains(StorjMock.SUB_FILE));
        AssertSyncFile.assertWith(StorjMock.SUB_FILE, FileMock.SUB_FILE, SyncState.FOR_LOCAL_DELETE);
    }

    @Test
    public void bothSubFileDeleted() throws Exception {
        new StorjMock(StorjMock.SUB_FILE);
        new FilesMock(FileMock.SUB_FILE);

        DB.setSynced(StorjMock.SUB_FILE, FileMock.SUB_FILE.getPath());
        StorjUtil.deleteFile(StorjMock.SUB_FILE);
        Files.deleteIfExists(FileMock.SUB_FILE.getPath());

        new CheckStateTask().run();

        AssertState.assertSleepEmptyDB();
    }

    @Test
    public void cloudAndLocalSubSubFileInSync() throws Exception {
        new StorjMock(StorjMock.DIR, StorjMock.SUB_DIR, StorjMock.SUB_SUB_FILE);
        new FilesMock(FileMock.DIR, FileMock.SUB_DIR, FileMock.SUB_SUB_FILE);

        DB.setSynced(StorjMock.DIR, FileMock.DIR.getPath());
        DB.setSynced(StorjMock.SUB_DIR, FileMock.SUB_DIR.getPath());
        DB.setSynced(StorjMock.SUB_SUB_FILE, FileMock.SUB_SUB_FILE.getPath());

        new CheckStateTask().run();

        AssertState.assertTaskQueue(SleepTask.class);
        assertEquals(3, DB.size());
        assertTrue(DB.contains(StorjMock.DIR));
        AssertSyncFile.assertWith(StorjMock.DIR, FileMock.DIR, SyncState.SYNCED);
        assertTrue(DB.contains(StorjMock.SUB_DIR));
        AssertSyncFile.assertWith(StorjMock.SUB_DIR, FileMock.SUB_DIR, SyncState.SYNCED);
        assertTrue(DB.contains(StorjMock.SUB_SUB_FILE));
        AssertSyncFile.assertWith(StorjMock.SUB_SUB_FILE, FileMock.SUB_SUB_FILE, SyncState.SYNCED);
    }

    @Test
    public void cloudSubSubFileNoLocal() throws Exception {
        new StorjMock(StorjMock.SUB_SUB_FILE);
        new FilesMock();

        new CheckStateTask().run();

        AssertState.assertForDownload(StorjMock.SUB_SUB_FILE);
    }

    @Test
    public void localSubSubFileNoCloud() throws Exception {
        new StorjMock();
        new FilesMock(FileMock.DIR, FileMock.SUB_DIR, FileMock.SUB_SUB_FILE);

        new CheckStateTask().run();

        TaskQueue tasks = App.getInstance().getTaskQueue();
        assertEquals(CreateCloudDirTask.class, tasks.poll().getClass());
        assertEquals(CreateCloudDirTask.class, tasks.poll().getClass());
        assertEquals(UploadFileTask.class, tasks.poll().getClass());
        assertEquals(CheckStateTask.class, tasks.poll().getClass());
        assertTrue(tasks.isEmpty());
        assertEquals(3, DB.size());
        assertTrue(DB.contains(FileMock.DIR.getPath()));
        AssertSyncFile.assertWith(FileMock.DIR, SyncState.FOR_CLOUD_CREATE_DIR);
        assertTrue(DB.contains(FileMock.SUB_DIR.getPath()));
        AssertSyncFile.assertWith(FileMock.SUB_DIR, SyncState.FOR_CLOUD_CREATE_DIR);
        assertTrue(DB.contains(FileMock.SUB_SUB_FILE.getPath()));
        AssertSyncFile.assertWith(FileMock.SUB_SUB_FILE, SyncState.FOR_UPLOAD);
    }

    @Test
    public void localSubSubFileDeleted() throws Exception {
        new StorjMock(StorjMock.SUB_SUB_FILE);
        new FilesMock(FileMock.SUB_SUB_FILE);

        DB.setSynced(StorjMock.SUB_SUB_FILE, FileMock.SUB_SUB_FILE.getPath());
        Files.deleteIfExists(FileMock.SUB_SUB_FILE.getPath());

        new CheckStateTask().run();

        AssertState.assertForCloudDelete(StorjMock.SUB_SUB_FILE, FileMock.SUB_SUB_FILE);
    }

    @Test
    public void cloudSubSubFileDeleted() throws Exception {
        new StorjMock(StorjMock.DIR, StorjMock.SUB_DIR, StorjMock.SUB_SUB_FILE);
        new FilesMock(FileMock.DIR, FileMock.SUB_DIR, FileMock.SUB_SUB_FILE);

        DB.setSynced(StorjMock.DIR, FileMock.DIR.getPath());
        DB.setSynced(StorjMock.SUB_DIR, FileMock.SUB_DIR.getPath());
        DB.setSynced(StorjMock.SUB_SUB_FILE, FileMock.SUB_SUB_FILE.getPath());
        StorjUtil.deleteFile(StorjMock.SUB_SUB_FILE);

        new CheckStateTask().run();

        AssertState.assertTaskQueue(DeleteLocalFileTask.class);
        assertEquals(3, DB.size());
        assertTrue(DB.contains(StorjMock.DIR));
        AssertSyncFile.assertWith(StorjMock.DIR, FileMock.DIR, SyncState.SYNCED);
        assertTrue(DB.contains(StorjMock.SUB_DIR));
        AssertSyncFile.assertWith(StorjMock.SUB_DIR, FileMock.SUB_DIR, SyncState.SYNCED);
        assertTrue(DB.contains(StorjMock.SUB_SUB_FILE));
        AssertSyncFile.assertWith(StorjMock.SUB_SUB_FILE, FileMock.SUB_SUB_FILE, SyncState.FOR_LOCAL_DELETE);
    }

    @Test
    public void bothSubSubFileDeleted() throws Exception {
        new StorjMock(StorjMock.SUB_SUB_FILE);
        new FilesMock(FileMock.SUB_SUB_FILE);

        DB.setSynced(StorjMock.SUB_SUB_FILE, FileMock.SUB_SUB_FILE.getPath());
        StorjUtil.deleteFile(StorjMock.SUB_SUB_FILE);
        Files.deleteIfExists(FileMock.SUB_SUB_FILE.getPath());

        new CheckStateTask().run();

        AssertState.assertSleepEmptyDB();
    }

    @Test
    public void excludedFileNoCloud() throws Exception {
        new StorjMock();
        new FilesMock(FileMock.EXCLUDED_FILE);

        new CheckStateTask().run();

        AssertState.assertSleepEmptyDB();
    }

}
