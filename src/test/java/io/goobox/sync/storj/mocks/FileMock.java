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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;

import io.goobox.sync.storj.App;

public class FileMock {

    public static final FileMock FILE_1 = new FileMock("file-1-name", 1510243787000L, 12345, false);
    public static final FileMock FILE_2 = new FileMock("file-2-name", 1510667191000L, 983249, false);
    public static final FileMock ENCRYPTED_FILE = new FileMock("encrypted-file-name", 1510566682000L, 23423313, false);
    public static final FileMock EXCLUDED_FILE = new FileMock("~$excluded.txt", 1512921930000L, 6532, false);
    public static final FileMock MODIFIED_FILE_1 = new FileMock("file-1-name", 1510739536000L, 12653, false);
    public static final FileMock MODIFIED_FILE_1_SAMESIZE = new FileMock("file-1-name", 1510739536000L, 12421, false);
    public static final FileMock MODIFIED_FILE_1_NEWER = new FileMock("file-1-name", 1511778030312L, 12653, false);
    public static final FileMock DIR = new FileMock("dir-name", 1512372256000L, 4096, true);
    public static final FileMock SUB_DIR = new FileMock("dir-name/sub-dir-name", 1512395371000L, 4096, true);
    public static final FileMock SUB_FILE = new FileMock("dir-name/sub-file-name", 1512398082000L, 2455, false);
    public static final FileMock SUB_SUB_FILE = new FileMock("dir-name/sub-dir-name/sub-sub-file-name", 1512398147000L,
            23467, false);

    private String relPath;
    private String name;
    private long lastModified;
    private long size;
    private boolean directory;

    public FileMock(String relPath, long lastModified, long size, boolean directory) {
        this.relPath = relPath;
        this.name = Paths.get(relPath).getFileName().toString();
        this.lastModified = lastModified;
        this.size = size;
        this.directory = directory;
    }

    public Path getPath() {
        return App.getInstance().getSyncDir().resolve(relPath);
    }

    public String getName() {
        return name;
    }

    public long lastModified() {
        return lastModified;
    }

    public FileTime getLastModifiedTime() {
        return FileTime.fromMillis(lastModified);
    }

    public long size() {
        return size;
    }

    public boolean isDirectory() {
        return directory;
    }

}
