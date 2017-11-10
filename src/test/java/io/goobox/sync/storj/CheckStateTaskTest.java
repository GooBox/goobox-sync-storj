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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.text.ParseException;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

import org.dizitart.no2.Nitrite;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.goobox.sync.storj.db.DB;
import io.goobox.sync.storj.db.SyncFile;
import io.goobox.sync.storj.db.SyncState;
import io.storj.libstorj.Bucket;
import io.storj.libstorj.File;
import io.storj.libstorj.KeysNotFoundException;
import io.storj.libstorj.ListFilesCallback;
import io.storj.libstorj.Storj;
import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class CheckStateTaskTest {

    private LinkedBlockingQueue<Runnable> tasks;

    @BeforeClass
    public static void applySharedFakes() {
        new MockUp<Storj>() {
            @Mock
            private void loadLibrary() {
                // do not load any native library
            }
        };

        new MockUp<DB>() {
            @Mock
            private Nitrite open() {
                return Nitrite.builder().compressed().openOrCreate();
            }
        };
    }

    @Before
    public void setup() {
        tasks = new LinkedBlockingQueue<>();
    }

    @After
    public void cleanUp() {
        DB.close();
    }

    private void noCloudFilesMock() {
        new MockUp<Storj>() {
            @Mock
            public void listFiles(Bucket bucket, ListFilesCallback callback) throws KeysNotFoundException {
                callback.onFilesReceived(new File[0]);
            }
        };
    }
    
    private void oneCloudFileMock() {
        new MockUp<Storj>() {
            @Mock
            private void loadLibrary() {
                // do not load any native library
            }

            @Mock
            public void listFiles(Bucket bucket, ListFilesCallback callback) throws KeysNotFoundException {
                File file = new File("file-id", "file-name", "2017-11-09T17:51:14.123Z", true, 12345, null, null, null, null);
                callback.onFilesReceived(new File[] { file });
            }
        };
    }

    private void noLocalFilesMock() {
        new MockUp<Files>() {
            @Mock
            public DirectoryStream<Path> newDirectoryStream(Path dir) {
                return new DirectoryStream<Path>() {
                    @Override
                    public void close() throws IOException {
                    }

                    @Override
                    public Iterator<Path> iterator() {
                        return Collections.emptyIterator();
                    }
                };
            }
        };
    }

    private void oneLocalFileMock() {
        new MockUp<Files>() {
            @Mock
            public DirectoryStream<Path> newDirectoryStream(Path dir) {
                return new DirectoryStream<Path>() {
                    @Override
                    public void close() throws IOException {
                    }

                    @Override
                    public Iterator<Path> iterator() {
                        Path path = Paths.get("file-name");
                        return Collections.singleton(path).iterator();
                    }
                };
            }

            @Mock
            public FileTime getLastModifiedTime(Path path, LinkOption... options) {
                return FileTime.fromMillis(1510243787000L);
            }

            @Mock
            public long size(Path path) {
                return 12345;
            }
        };
    }

    @Test
    public void emptyCloudAndLocalTest() throws InterruptedException {
        noCloudFilesMock();
        noLocalFilesMock();

        new CheckStateTask(null, tasks).run();

        assertEquals(2, tasks.size());
        assertEquals(SleepTask.class, tasks.poll().getClass());
        assertEquals(CheckStateTask.class, tasks.poll().getClass());

        assertEquals(0, DB.size());
    }

    @Test
    public void fileInCloudEmptyLocalTest() throws InterruptedException, ParseException {
        oneCloudFileMock();
        noLocalFilesMock();

        new CheckStateTask(null, tasks).run();

        assertEquals(2, tasks.size());
        assertEquals(DownloadFileTask.class, tasks.poll().getClass());
        assertEquals(CheckStateTask.class, tasks.poll().getClass());

        assertEquals(1, DB.size());
        assertTrue(DB.contains("file-name"));

        SyncFile syncFile = DB.get("file-name");
        assertEquals("file-name", syncFile.getName());
        assertEquals("file-id", syncFile.getStorjId());
        assertEquals(Utils.getTime("2017-11-09T17:51:14.123Z"), syncFile.getStorjCreatedTime());
        assertEquals(12345, syncFile.getStorjSize());
        assertEquals(0, syncFile.getLocalModifiedTime());
        assertEquals(0, syncFile.getLocalSize());
        assertEquals(SyncState.FOR_DOWNLOAD, syncFile.getState());
    }

    @Test
    public void emptyCloudFileInLocalTest() throws InterruptedException, ParseException {
        noCloudFilesMock();
        oneLocalFileMock();

        new CheckStateTask(null, tasks).run();

        assertEquals(2, tasks.size());
        assertEquals(UploadFileTask.class, tasks.poll().getClass());
        assertEquals(CheckStateTask.class, tasks.poll().getClass());

        assertEquals(1, DB.size());
        assertTrue(DB.contains("file-name"));

        SyncFile syncFile = DB.get("file-name");
        assertEquals("file-name", syncFile.getName());
        assertEquals(null, syncFile.getStorjId());
        assertEquals(0, syncFile.getStorjCreatedTime());
        assertEquals(0, syncFile.getStorjSize());
        assertEquals(1510243787000L, syncFile.getLocalModifiedTime());
        assertEquals(12345, syncFile.getLocalSize());
        assertEquals(SyncState.FOR_UPLOAD, syncFile.getState());
    }

}
