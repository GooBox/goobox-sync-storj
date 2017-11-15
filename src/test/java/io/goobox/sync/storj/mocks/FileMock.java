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
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;

public class FileMock {

    public static final FileMock FILE_1 = new FileMock("file-1-name", 1510243787000L, 12345);
    public static final FileMock FILE_2 = new FileMock("file-2-name", 1510667191000L, 983249);
    public static final FileMock ENCRYPTED_FILE = new FileMock("encrypted-file-name", 1510566682000L, 23423313);
    public static final FileMock MODIFIED_FILE_1 = new FileMock("file-1-name", 1510739536L, 12653);

    private static final String PARENT_FOLDER = "/some/local/folder";

    private String name;
    private long lastModified;
    private long size;

    public FileMock(String name, long lastModified, long size) {
        this.name = name;
        this.lastModified = lastModified;
        this.size = size;
    }

    public Path getPath() {
        return Paths.get(PARENT_FOLDER).resolve(name);
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

}
