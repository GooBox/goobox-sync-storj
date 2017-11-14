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
package io.goobox.sync.storj.mocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import io.storj.libstorj.Bucket;
import io.storj.libstorj.DeleteFileCallback;
import io.storj.libstorj.DownloadFileCallback;
import io.storj.libstorj.File;
import io.storj.libstorj.KeysNotFoundException;
import io.storj.libstorj.ListFilesCallback;
import io.storj.libstorj.Storj;
import mockit.Mock;
import mockit.MockUp;

public class StorjMock extends MockUp<Storj> {

    public static final File FILE_1 = new File("file-1-id", "file-1-name", "2017-11-09T17:51:14.123Z", true, 12345,
            null, null, null, null);
    public static final File FILE_2 = new File("file-2-id", "file-2-name", "2017-11-14T15:44:20.832Z", true, 983249,
            null, null, null, null);
    public static final File ENCRYPTED_FILE = new File("encrypted-file-id", "encrypted-file-name",
            "2017-11-13T10:10:28.243Z", false, 23423313, null, null, null, null);

    private List<File> files;

    public StorjMock(File... files) {
        this.files = new ArrayList<>(Arrays.asList(files));
    }

    @Mock
    private void loadLibrary() {
        // do not load any native library
    }

    @Mock
    public void listFiles(Bucket bucket, ListFilesCallback callback) throws KeysNotFoundException {
        callback.onFilesReceived(files.toArray(new File[files.size()]));
    }

    @Mock
    public void deleteFile(Bucket bucket, File file, DeleteFileCallback callback) throws KeysNotFoundException {
        Iterator<File> i = files.iterator();
        while (i.hasNext()) {
            File f = i.next();
            if (f.equals(file)) {
                i.remove();
                callback.onFileDeleted();
            } else {
                callback.onError("file not found");
            }
            return;
        }
    }

    @Mock
    public void downloadFile(Bucket bucket, File file, DownloadFileCallback callback) throws KeysNotFoundException {
        if (FILE_1.equals(file)) {
            callback.onComplete(file, FileMock.FILE_1.getPath().toString());
        } else {
            callback.onError(file, "error downloading");
        }
    }

}
