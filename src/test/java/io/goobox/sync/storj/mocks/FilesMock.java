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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import mockit.Mock;
import mockit.MockUp;

public class FilesMock extends MockUp<Files> {

    private FileMock[] files;

    public FilesMock(FileMock... files) {
        this.files = files;
    }

    @Mock
    public DirectoryStream<Path> newDirectoryStream(Path dir) {
        return new DirectoryStream<Path>() {
            @Override
            public void close() throws IOException {
            }

            @Override
            public Iterator<Path> iterator() {
                List<Path> paths = new ArrayList<>();
                for (FileMock file : files) {
                    paths.add(Paths.get(file.getName()));
                }
                return paths.iterator();
            }
        };
    }

    @Mock
    public FileTime getLastModifiedTime(Path path, LinkOption... options) {
        String fileName = path.getFileName().toString();
        for (FileMock file : files) {
            if (file.getName().equals(fileName)) {
                return file.getLastModifiedTime();
            }
        }
        return null;
    }

    @Mock
    public long size(Path path) {
        String fileName = path.getFileName().toString();
        for (FileMock file : files) {
            if (file.getName().equals(fileName)) {
                return file.size();
            }
        }
        return -1;
    }

}
