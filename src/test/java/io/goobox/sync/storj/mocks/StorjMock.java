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

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import io.storj.libstorj.Bucket;
import io.storj.libstorj.DeleteFileCallback;
import io.storj.libstorj.DownloadFileCallback;
import io.storj.libstorj.File;
import io.storj.libstorj.KeysNotFoundException;
import io.storj.libstorj.ListFilesCallback;
import io.storj.libstorj.Storj;
import io.storj.libstorj.UploadFileCallback;
import mockit.Mock;
import mockit.MockUp;

public class StorjMock extends MockUp<Storj> {

    public static final File FILE_1 = new File("file-1-id", "file-1-name", "2017-11-09T17:51:14.123Z", true, 12345,
            null, null, null, null);
    public static final File FILE_2 = new File("file-2-id", "file-2-name", "2017-11-14T15:44:20.832Z", true, 983249,
            null, null, null, null);
    public static final File ENCRYPTED_FILE = new File("encrypted-file-id", "encrypted-file-name",
            "2017-11-13T10:10:28.243Z", false, 23423313, null, null, null, null);
    public static final File MODIFIED_FILE_1 = new File("modified-file-1-id", "file-1-name", "2017-11-15T11:43:20.622Z",
            true, 12421, null, null, null, null);
    public static final File MODIFIED_FILE_1_SAMESIZE = new File("modified-file-1-id", "file-1-name",
            "2017-11-15T11:43:20.622Z", true, 12421, null, null, null, null);
    public static final File MODIFIED_FILE_1_NEWER = new File("modified-file-1-id", "file-1-name",
            "2017-11-27T10:20:30.312Z", true, 12421, null, null, null, null);
    public static final File DIR = new File("dir-id", "dir-name/", "2017-12-04T07:11:56.825Z", true,
            1, null, null, null, null);
    public static final File SUB_DIR = new File("sub-dir-id", "dir-name/sub-dir-name/", "2017-12-04T11:46:34.712Z",
            true, 1, null, null, null, null);
    public static final File SUB_FILE = new File("sub-file-id", "dir-name/sub-file-name", "2017-12-04T14:37:30.934Z",
            true, 2455, null, null, null, null);
    public static final File SUB_SUB_FILE = new File("sub-sub-file-id", "dir-name/sub-dir-name/sub-sub-file-name",
            "2017-12-04T14:38:35.192Z", true, 23467, null, null, null, null);

    private Set<File> files;
    private FilesMock filesMock;

    public StorjMock(File... files) {
        this.files = new HashSet<>(Arrays.asList(files));
    }

    public StorjMock(FilesMock filesMock, File... files) {
        this(files);
        this.filesMock = filesMock;
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
                return;
            }
        }
        callback.onError("file not found");
    }

    @Mock
    public void downloadFile(Bucket bucket, File file, DownloadFileCallback callback) throws KeysNotFoundException {
        if (FILE_1.equals(file)) {
            filesMock.addFile(FileMock.FILE_1);
            callback.onComplete(file, FileMock.FILE_1.getPath().toString());
        } else if (SUB_FILE.equals(file)) {
            filesMock.addFile(FileMock.SUB_FILE);
            callback.onComplete(file, FileMock.SUB_FILE.getPath().toString());
        } else if (SUB_SUB_FILE.equals(file)) {
            filesMock.addFile(FileMock.SUB_SUB_FILE);
            callback.onComplete(file, FileMock.SUB_SUB_FILE.getPath().toString());
        } else {
            callback.onError(file, "error downloading");
        }
    }

    @Mock
    public void uploadFile(Bucket bucket, String fileName, Path localPath, UploadFileCallback callback)
            throws KeysNotFoundException {
        String path = localPath.toAbsolutePath().toString();
        if (FileMock.FILE_1.getPath().equals(localPath)) {
            if (files.contains(FILE_1)) {
                callback.onError(path, "File already exists");
            } else {
                files.add(FILE_1);
                callback.onComplete(path, FILE_1.getId());
            }
        } else if (DIR.getName().equals(fileName)) {
            files.add(DIR);
            callback.onComplete(path, DIR.getId());
        } else if (SUB_DIR.getName().equals(fileName)) {
            files.add(SUB_DIR);
            callback.onComplete(path, SUB_DIR.getId());
        } else if (SUB_FILE.getName().equals(fileName)) {
            files.add(SUB_FILE);
            callback.onComplete(path, SUB_FILE.getId());
        } else if (SUB_SUB_FILE.getName().equals(fileName)) {
            files.add(SUB_SUB_FILE);
            callback.onComplete(path, SUB_SUB_FILE.getId());
        } else {
            callback.onError(path, "error uploading");
        }
    }

    public void modifyFile(File oldFile, File newFile) {
        if (files.contains(oldFile)) {
            files.remove(oldFile);
            files.add(newFile);
        } else {
            throw new IllegalStateException(oldFile.getName() + " not found");
        }
    }

    public void addFile(File file) {
        files.add(file);
    }

}
