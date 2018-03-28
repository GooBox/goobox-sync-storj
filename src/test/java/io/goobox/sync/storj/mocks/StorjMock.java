/*
 * Copyright (C) 2017-2018 Kaloyan Raev
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import io.storj.libstorj.Bucket;
import io.storj.libstorj.DeleteFileCallback;
import io.storj.libstorj.DownloadFileCallback;
import io.storj.libstorj.File;
import io.storj.libstorj.GetFileCallback;
import io.storj.libstorj.GetFileIdCallback;
import io.storj.libstorj.KeysNotFoundException;
import io.storj.libstorj.ListFilesCallback;
import io.storj.libstorj.Storj;
import io.storj.libstorj.UploadFileCallback;
import mockit.Mock;
import mockit.MockUp;

public class StorjMock extends MockUp<Storj> {

    public static final Bucket BUCKET = new Bucket("bucket-id", "Goobox", "2017-11-09T13:50:55.632Z", true);
    public static final File FILE_1 = new File("file-1-id", BUCKET.getId(), "file-1-name", "2017-11-09T17:51:14.123Z",
            true, 12345, null, null, null, null);
    public static final File FILE_2 = new File("file-2-id", BUCKET.getId(), "file-2-name", "2017-11-14T15:44:20.832Z",
            true, 983249, null, null, null, null);
    public static final File ENCRYPTED_FILE = new File("encrypted-file-id", BUCKET.getId(), "encrypted-file-name",
            "2017-11-13T10:10:28.243Z", false, 23423313, null, null, null, null);
    public static final File MODIFIED_FILE_1 = new File("modified-file-1-id", BUCKET.getId(), "file-1-name",
            "2017-11-15T11:43:20.622Z", true, 12421, null, null, null, null);
    public static final File MODIFIED_FILE_1_SAMESIZE = new File("modified-file-1-id", BUCKET.getId(), "file-1-name",
            "2017-11-15T11:43:20.622Z", true, 12421, null, null, null, null);
    public static final File MODIFIED_FILE_1_NEWER = new File("modified-file-1-id", BUCKET.getId(), "file-1-name",
            "2017-11-27T10:20:30.312Z", true, 12421, null, null, null, null);
    public static final File DIR = new File("dir-id", BUCKET.getId(), "dir-name/", "2017-12-04T07:11:56.825Z", true,
            1, null, null, null, null);
    public static final File SUB_DIR = new File("sub-dir-id", BUCKET.getId(), "dir-name/sub-dir-name/",
            "2017-12-04T11:46:34.712Z", true, 1, null, null, null, null);
    public static final File SUB_FILE = new File("sub-file-id", BUCKET.getId(), "dir-name/sub-file-name",
            "2017-12-04T14:37:30.934Z", true, 2455, null, null, null, null);
    public static final File SUB_SUB_FILE = new File("sub-sub-file-id", BUCKET.getId(),
            "dir-name/sub-dir-name/sub-sub-file-name", "2017-12-04T14:38:35.192Z", true, 23467, null, null, null, null);

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
        callback.onFilesReceived(bucket.getId(), files.toArray(new File[files.size()]));
    }

    @Mock
    public void getFileId(Bucket bucket, String fileName, GetFileIdCallback callback) throws KeysNotFoundException {
        for (File file : files) {
            if (fileName.equals(file.getName())) {
                callback.onFileIdReceived(fileName, file.getId());
                return;
            }
        }
        callback.onError(fileName, Storj.HTTP_NOT_FOUND, "File not found");
    }

    @Mock
    public void getFile(Bucket bucket, String fileId, GetFileCallback callback) throws KeysNotFoundException {
        for (File file : files) {
            if (fileId.equals(file.getId())) {
                callback.onFileReceived(file);
                return;
            }
        }
        callback.onError(fileId, Storj.HTTP_NOT_FOUND, "File not found");
    }

    @Mock
    public void deleteFile(Bucket bucket, File file, DeleteFileCallback callback) throws KeysNotFoundException {
        deleteFile(bucket.getId(), file.getId(), callback);
    }

    @Mock
    public void deleteFile(String bucketId, String fileId, DeleteFileCallback callback) throws KeysNotFoundException {
        Iterator<File> i = files.iterator();
        while (i.hasNext()) {
            File f = i.next();
            if (f.getId().equals(fileId)) {
                i.remove();
                callback.onFileDeleted(fileId);
                return;
            }
        }
        callback.onError(fileId, Storj.HTTP_NOT_FOUND, "file not found");
    }

    @Mock
    public long downloadFile(Bucket bucket, File file, DownloadFileCallback callback) throws KeysNotFoundException {
        if (FILE_1.equals(file)) {
            filesMock.addFile(FileMock.FILE_1);
            callback.onComplete(file.getId(), FileMock.FILE_1.getPath().toString());
        } else if (SUB_FILE.equals(file)) {
            filesMock.addFile(FileMock.SUB_FILE);
            callback.onComplete(file.getId(), FileMock.SUB_FILE.getPath().toString());
        } else if (SUB_SUB_FILE.equals(file)) {
            filesMock.addFile(FileMock.SUB_SUB_FILE);
            callback.onComplete(file.getId(), FileMock.SUB_SUB_FILE.getPath().toString());
        } else {
            callback.onError(file.getId(), Storj.STORJ_BRIDGE_FILE_NOTFOUND_ERROR, "error downloading");
        }
        return 0;
    }

    @Mock
    public long uploadFile(Bucket bucket, String fileName, String localPath, UploadFileCallback callback)
            throws KeysNotFoundException {
        if (FileMock.FILE_1.getPath().toString().equals(localPath)) {
            if (files.contains(FILE_1)) {
                callback.onError(localPath, Storj.STORJ_BRIDGE_BUCKET_FILE_EXISTS, "File already exists");
            } else {
                files.add(FILE_1);
                callback.onComplete(localPath, FILE_1);
            }
        } else if (DIR.getName().equals(fileName)) {
            files.add(DIR);
            callback.onComplete(localPath, DIR);
        } else if (SUB_DIR.getName().equals(fileName)) {
            files.add(SUB_DIR);
            callback.onComplete(localPath, SUB_DIR);
        } else if (SUB_FILE.getName().equals(fileName)) {
            files.add(SUB_FILE);
            callback.onComplete(localPath, SUB_FILE);
        } else if (SUB_SUB_FILE.getName().equals(fileName)) {
            files.add(SUB_SUB_FILE);
            callback.onComplete(localPath, SUB_SUB_FILE);
        } else {
            callback.onError(localPath, Storj.ENOENT, "error uploading");
        }
        return 0;
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
