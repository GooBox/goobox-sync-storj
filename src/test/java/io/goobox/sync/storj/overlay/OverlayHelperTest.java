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
package io.goobox.sync.storj.overlay;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.goobox.sync.storj.db.DB;
import io.goobox.sync.storj.helpers.StorjUtil;
import io.goobox.sync.storj.mocks.AppMock;
import io.goobox.sync.storj.mocks.DBMock;
import io.goobox.sync.storj.mocks.FileMock;
import io.goobox.sync.storj.mocks.FilesMock;
import io.goobox.sync.storj.mocks.StorjMock;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class OverlayHelperTest {

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
    public void fileNotInDB() throws Exception {
        new FilesMock(FileMock.FILE_1);

        Assert.assertEquals(OverlayIcon.SYNCING.id(), getIconId(FileMock.FILE_1));
    }

    @Test
    public void syncedFile() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());

        Assert.assertEquals(OverlayIcon.OK.id(), getIconId(FileMock.FILE_1));
    }

    @Test
    public void fileForUpload() throws Exception {
        new FilesMock(FileMock.FILE_1);

        DB.addForUpload(FileMock.FILE_1.getPath());

        Assert.assertEquals(OverlayIcon.SYNCING.id(), getIconId(FileMock.FILE_1));
    }

    @Test
    public void fileForReUpload() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.addForUpload(StorjMock.FILE_1, FileMock.FILE_1.getPath());

        Assert.assertEquals(OverlayIcon.SYNCING.id(), getIconId(FileMock.FILE_1));
    }

    @Test
    public void fileForDownload() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.addForDownload(StorjMock.FILE_1);

        Assert.assertEquals(OverlayIcon.SYNCING.id(), getIconId(FileMock.FILE_1));
    }

    @Test
    public void fileForReDownload() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.addForDownload(StorjMock.FILE_1, FileMock.FILE_1.getPath());

        Assert.assertEquals(OverlayIcon.SYNCING.id(), getIconId(FileMock.FILE_1));
    }

    @Test
    public void fileForLocalDelete() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.setSynced(StorjMock.FILE_1, FileMock.FILE_1.getPath());
        StorjUtil.deleteFile(StorjMock.FILE_1);
        DB.setForLocalDelete(FileMock.FILE_1.getPath());

        Assert.assertEquals(OverlayIcon.SYNCING.id(), getIconId(FileMock.FILE_1));
    }

    @Test
    public void fileFailedDownload() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.addForDownload(StorjMock.FILE_1);
        DB.setDownloadFailed(StorjMock.FILE_1, FileMock.FILE_1.getPath());

        Assert.assertEquals(OverlayIcon.ERROR.id(), getIconId(FileMock.FILE_1));
    }

    @Test
    public void fileFailedUpload() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.addForUpload(FileMock.FILE_1.getPath());
        DB.setDownloadFailed(StorjMock.FILE_1, FileMock.FILE_1.getPath());

        Assert.assertEquals(OverlayIcon.ERROR.id(), getIconId(FileMock.FILE_1));
    }

    @Test
    public void fileInConflict() throws Exception {
        new StorjMock(StorjMock.FILE_1);
        new FilesMock(FileMock.FILE_1);

        DB.setConflict(StorjMock.FILE_1, FileMock.FILE_1.getPath());

        Assert.assertEquals(OverlayIcon.WARNING.id(), getIconId(FileMock.FILE_1));
    }

    @Test
    public void dirForCloudCreate() throws Exception {
        new FilesMock(FileMock.DIR);

        DB.addForCloudCreateDir(FileMock.DIR.getPath());

        Assert.assertEquals(OverlayIcon.SYNCING.id(), getIconId(FileMock.DIR));
    }

    @Test
    public void dirForLocalCreate() throws Exception {
        new StorjMock(StorjMock.DIR);
        new FilesMock(FileMock.DIR);

        DB.addForLocalCreateDir(StorjMock.DIR);

        Assert.assertEquals(OverlayIcon.SYNCING.id(), getIconId(FileMock.DIR));
    }

    @Test
    public void dirAllSynced() throws Exception {
        new StorjMock(StorjMock.DIR, StorjMock.SUB_FILE, StorjMock.SUB_DIR, StorjMock.SUB_SUB_FILE);
        new FilesMock(FileMock.DIR, FileMock.SUB_FILE, FileMock.SUB_DIR, FileMock.SUB_SUB_FILE);

        DB.setSynced(StorjMock.DIR, FileMock.DIR.getPath());
        DB.setSynced(StorjMock.SUB_FILE, FileMock.SUB_FILE.getPath());
        DB.setSynced(StorjMock.SUB_DIR, FileMock.SUB_DIR.getPath());
        DB.setSynced(StorjMock.SUB_SUB_FILE, FileMock.SUB_SUB_FILE.getPath());

        Assert.assertEquals(OverlayIcon.OK.id(), getIconId(FileMock.DIR));
        Assert.assertEquals(OverlayIcon.OK.id(), getIconId(FileMock.SUB_FILE));
        Assert.assertEquals(OverlayIcon.OK.id(), getIconId(FileMock.SUB_DIR));
        Assert.assertEquals(OverlayIcon.OK.id(), getIconId(FileMock.SUB_SUB_FILE));
    }

    @Test
    public void subFileResyncing() throws Exception {
        new StorjMock(StorjMock.DIR, StorjMock.SUB_FILE, StorjMock.SUB_DIR, StorjMock.SUB_SUB_FILE);
        new FilesMock(FileMock.DIR, FileMock.SUB_FILE, FileMock.SUB_DIR, FileMock.SUB_SUB_FILE);

        DB.setSynced(StorjMock.DIR, FileMock.DIR.getPath());
        DB.setSynced(StorjMock.SUB_FILE, FileMock.SUB_FILE.getPath());
        DB.setSynced(StorjMock.SUB_DIR, FileMock.SUB_DIR.getPath());
        DB.setSynced(StorjMock.SUB_SUB_FILE, FileMock.SUB_SUB_FILE.getPath());
        DB.addForDownload(StorjMock.SUB_FILE, FileMock.SUB_FILE.getPath());

        Assert.assertEquals(OverlayIcon.SYNCING.id(), getIconId(FileMock.DIR));
        Assert.assertEquals(OverlayIcon.SYNCING.id(), getIconId(FileMock.SUB_FILE));
        Assert.assertEquals(OverlayIcon.OK.id(), getIconId(FileMock.SUB_DIR));
        Assert.assertEquals(OverlayIcon.OK.id(), getIconId(FileMock.SUB_SUB_FILE));
    }

    @Test
    public void subSubFileResyncing() throws Exception {
        new StorjMock(StorjMock.DIR, StorjMock.SUB_FILE, StorjMock.SUB_DIR, StorjMock.SUB_SUB_FILE);
        new FilesMock(FileMock.DIR, FileMock.SUB_FILE, FileMock.SUB_DIR, FileMock.SUB_SUB_FILE);

        DB.setSynced(StorjMock.DIR, FileMock.DIR.getPath());
        DB.setSynced(StorjMock.SUB_FILE, FileMock.SUB_FILE.getPath());
        DB.setSynced(StorjMock.SUB_DIR, FileMock.SUB_DIR.getPath());
        DB.setSynced(StorjMock.SUB_SUB_FILE, FileMock.SUB_SUB_FILE.getPath());
        DB.addForDownload(StorjMock.SUB_SUB_FILE, FileMock.SUB_SUB_FILE.getPath());

        Assert.assertEquals(OverlayIcon.SYNCING.id(), getIconId(FileMock.DIR));
        Assert.assertEquals(OverlayIcon.OK.id(), getIconId(FileMock.SUB_FILE));
        Assert.assertEquals(OverlayIcon.SYNCING.id(), getIconId(FileMock.SUB_DIR));
        Assert.assertEquals(OverlayIcon.SYNCING.id(), getIconId(FileMock.SUB_SUB_FILE));
    }

    @Test
    public void dirAllResyncing() throws Exception {
        new StorjMock(StorjMock.DIR, StorjMock.SUB_FILE, StorjMock.SUB_DIR, StorjMock.SUB_SUB_FILE);
        new FilesMock(FileMock.DIR, FileMock.SUB_FILE, FileMock.SUB_DIR, FileMock.SUB_SUB_FILE);

        DB.setSynced(StorjMock.DIR, FileMock.DIR.getPath());
        DB.setSynced(StorjMock.SUB_FILE, FileMock.SUB_FILE.getPath());
        DB.setSynced(StorjMock.SUB_DIR, FileMock.SUB_DIR.getPath());
        DB.setSynced(StorjMock.SUB_SUB_FILE, FileMock.SUB_SUB_FILE.getPath());
        DB.addForDownload(StorjMock.SUB_FILE, FileMock.SUB_FILE.getPath());
        DB.addForDownload(StorjMock.SUB_SUB_FILE, FileMock.SUB_SUB_FILE.getPath());

        Assert.assertEquals(OverlayIcon.SYNCING.id(), getIconId(FileMock.DIR));
        Assert.assertEquals(OverlayIcon.SYNCING.id(), getIconId(FileMock.SUB_FILE));
        Assert.assertEquals(OverlayIcon.SYNCING.id(), getIconId(FileMock.SUB_DIR));
        Assert.assertEquals(OverlayIcon.SYNCING.id(), getIconId(FileMock.SUB_SUB_FILE));
    }

    @Test
    public void subFileFailedsubSubFileResyncing() throws Exception {
        new StorjMock(StorjMock.DIR, StorjMock.SUB_FILE, StorjMock.SUB_DIR, StorjMock.SUB_SUB_FILE);
        new FilesMock(FileMock.DIR, FileMock.SUB_FILE, FileMock.SUB_DIR, FileMock.SUB_SUB_FILE);

        DB.setSynced(StorjMock.DIR, FileMock.DIR.getPath());
        DB.setSynced(StorjMock.SUB_FILE, FileMock.SUB_FILE.getPath());
        DB.setSynced(StorjMock.SUB_DIR, FileMock.SUB_DIR.getPath());
        DB.setSynced(StorjMock.SUB_SUB_FILE, FileMock.SUB_SUB_FILE.getPath());
        DB.setDownloadFailed(StorjMock.SUB_FILE, FileMock.SUB_FILE.getPath());
        DB.addForDownload(StorjMock.SUB_SUB_FILE, FileMock.SUB_SUB_FILE.getPath());

        Assert.assertEquals(OverlayIcon.ERROR.id(), getIconId(FileMock.DIR));
        Assert.assertEquals(OverlayIcon.ERROR.id(), getIconId(FileMock.SUB_FILE));
        Assert.assertEquals(OverlayIcon.SYNCING.id(), getIconId(FileMock.SUB_DIR));
        Assert.assertEquals(OverlayIcon.SYNCING.id(), getIconId(FileMock.SUB_SUB_FILE));
    }

    private int getIconId(FileMock file) {
        return OverlayHelper.getInstance().getIconForFile(file.getPath().toString());
    }

}
