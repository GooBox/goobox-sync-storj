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
package io.goobox.sync.storj.db;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;

import org.dizitart.no2.objects.Id;

import io.goobox.sync.storj.Utils;
import io.storj.libstorj.File;

@SuppressWarnings("serial")
public class SyncFile implements Serializable {

    @Id
    private String name;

    private String storjId;

    private long storjCreatedTime;

    private long storjSize;

    private long localModifiedTime;

    private long localSize;

    private SyncState state;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStorjId() {
        return storjId;
    }

    public void setStorjId(String storjId) {
        this.storjId = storjId;
    }

    public long getStorjCreatedTime() {
        return storjCreatedTime;
    }

    public void setStorjCreatedTime(long storjCreatedTime) {
        this.storjCreatedTime = storjCreatedTime;
    }

    public long getStorjSize() {
        return storjSize;
    }

    public void setStorjSize(long storjSize) {
        this.storjSize = storjSize;
    }

    public long getLocalModifiedTime() {
        return localModifiedTime;
    }

    public void setLocalModifiedTime(long localModifiedTime) {
        this.localModifiedTime = localModifiedTime;
    }

    public long getLocalSize() {
        return localSize;
    }

    public void setLocalSize(long localSize) {
        this.localSize = localSize;
    }

    public SyncState getState() {
        return state;
    }

    public void setState(SyncState state) {
        this.state = state;
    }

    public void setCloudData(File file) {
        setStorjId(file.getId());
        try {
            setStorjCreatedTime(Utils.getTime(file.getCreated()));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        setStorjSize(file.getSize());
    }

    public void setLocalData(Path path) throws IOException {
        setLocalModifiedTime(Files.getLastModifiedTime(path).toMillis());
        setLocalSize(Files.size(path));
    }

    @Override
    public String toString() {
        return new StringBuilder().append("SyncFile[")
                .append("name = ").append(name)
                .append(", state = ").append(state)
                .append(", storjCreated = ").append(storjCreatedTime)
                .append(", localModified = ").append(localModifiedTime)
                .append(", storjSize = ").append(storjSize)
                .append(", localSize = ").append(localSize)
                .append(", storjId = ").append(storjId)
                .append("]")
                .toString();
    }

}
