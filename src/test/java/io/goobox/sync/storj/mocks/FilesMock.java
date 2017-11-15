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
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import mockit.Mock;
import mockit.MockUp;

public class FilesMock extends MockUp<Files> {

    private Set<FileMock> files;

    public FilesMock(FileMock... files) {
        this.files = new HashSet<>(Arrays.asList(files));
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
                    paths.add(file.getPath());
                }
                return paths.iterator();
            }
        };
    }

    @Mock
    public FileTime getLastModifiedTime(Path path, LinkOption... options) {
        for (FileMock file : files) {
            if (file.getPath().equals(path)) {
                return file.getLastModifiedTime();
            }
        }
        return null;
    }

    @Mock
    public long size(Path path) {
        for (FileMock file : files) {
            if (file.getPath().equals(path)) {
                return file.size();
            }
        }
        return -1;
    }

    @Mock
    public boolean exists(Path path, LinkOption... options) {
        for (FileMock file : files) {
            if (file.getPath().equals(path)) {
                return true;
            }
        }
        return false;
    }

    @Mock
    public boolean deleteIfExists(Path path) throws IOException {
        if (Files.exists(path)) {
            Iterator<FileMock> i = files.iterator();
            while (i.hasNext()) {
                FileMock file = i.next();
                if (file.getPath().equals(path)) {
                    i.remove();
                    return true;
                }
            }
        }
        return false;
    }

    public void modifyFile1() {
        if (files.contains(FileMock.FILE_1)) {
            files.remove(FileMock.FILE_1);
            files.add(FileMock.MODIFIED_FILE_1);
        } else {
            throw new IllegalStateException("FILE_1 not found");
        }
    }

}
