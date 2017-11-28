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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.dizitart.no2.Nitrite;
import org.dizitart.no2.objects.ObjectFilter;
import org.dizitart.no2.objects.ObjectRepository;
import org.dizitart.no2.objects.filters.ObjectFilters;

import io.goobox.sync.storj.Utils;
import io.storj.libstorj.File;

public class DB {

    private static Nitrite db;

    private static Nitrite db() {
        if (db == null || db.isClosed()) {
            db = open();
        }
        return db;
    }

    private static ObjectRepository<SyncFile> repo() {
        return db().getRepository(SyncFile.class);
    }

    private static ObjectFilter withName(String fileName) {
        return ObjectFilters.eq("name", fileName);
    }

    private static Nitrite open() {
        Path dbPath = Utils.getDataDir().resolve("sync.db");
        return Nitrite.builder()
                .compressed()
                .filePath(dbPath.toFile())
                .openOrCreate();
    }

    public synchronized static void close() {
        db().close();
    }

    public synchronized static void commit() {
        db().commit();
    }

    public synchronized static List<SyncFile> all() {
        return repo().find().toList();
    }

    public synchronized static boolean contains(File file) {
        return contains(file.getName());
    }

    public synchronized static boolean contains(Path path) {
        return contains(path.getFileName().toString());
    }

    public synchronized static boolean contains(String fileName) {
        return get(fileName) != null;
    }

    public synchronized static SyncFile get(File file) {
        return get(file.getName());
    }

    public synchronized static SyncFile get(Path path) {
        return get(path.getFileName().toString());
    }

    public synchronized static SyncFile get(String fileName) {
        return repo().find(withName(fileName)).firstOrDefault();
    }

    private synchronized static SyncFile getOrCreate(File file) {
        return getOrCreate(file.getName());
    }

    private synchronized static SyncFile getOrCreate(Path path) {
        return getOrCreate(path.getFileName().toString());
    }

    private synchronized static SyncFile getOrCreate(String fileName) {
        SyncFile syncFile = get(fileName);
        if (syncFile == null) {
            syncFile = new SyncFile();
            syncFile.setName(fileName);
            repo().insert(syncFile);
        }
        return syncFile;
    }

    public synchronized static void remove(File file) {
        remove(file.getName());
    }

    public synchronized static void remove(Path path) {
        remove(path.getFileName().toString());
    }

    public synchronized static void remove(String fileName) {
        repo().remove(withName(fileName));
    }

    public synchronized static long size() {
        return repo().size();
    }

    public synchronized static void setSynced(File storjFile, Path localFile) throws IOException {
        SyncFile syncFile = getOrCreate(storjFile);
        syncFile.setCloudData(storjFile);
        syncFile.setLocalData(localFile);
        syncFile.setState(SyncState.SYNCED);
        repo().update(syncFile);
    }

    public synchronized static void addForDownload(File file) {
        remove(file);
        SyncFile syncFile = getOrCreate(file);
        syncFile.setCloudData(file);
        syncFile.setState(SyncState.FOR_DOWNLOAD);
        repo().update(syncFile);
    }

    public synchronized static void addForDownload(File storjFile, Path localFile) throws IOException {
        SyncFile syncFile = getOrCreate(storjFile);
        syncFile.setCloudData(storjFile);
        syncFile.setLocalData(localFile);
        syncFile.setState(SyncState.FOR_DOWNLOAD);
        repo().update(syncFile);
    }

    public synchronized static void addForUpload(Path path) throws IOException {
        remove(path);
        SyncFile syncFile = getOrCreate(path);
        syncFile.setLocalData(path);
        syncFile.setState(SyncState.FOR_UPLOAD);
        repo().update(syncFile);
    }

    public synchronized static void addForUpload(File storjFile, Path localFile) throws IOException {
        SyncFile syncFile = getOrCreate(localFile);
        syncFile.setCloudData(storjFile);
        syncFile.setLocalData(localFile);
        syncFile.setState(SyncState.FOR_UPLOAD);
        repo().update(syncFile);
    }

    public synchronized static void setDownloadFailed(File storjFile, Path localFile) throws IOException {
        SyncFile syncFile = get(storjFile);
        syncFile.setCloudData(storjFile);
        if (Files.exists(localFile)) {
            syncFile.setLocalData(localFile);
        }
        syncFile.setState(SyncState.DOWNLOAD_FAILED);
        repo().update(syncFile);
    }

    public synchronized static void setUploadFailed(Path path) throws IOException {
        SyncFile syncFile = get(path);
        if (Files.exists(path)) {
            syncFile.setLocalData(path);
        }
        syncFile.setState(SyncState.UPLOAD_FAILED);
        repo().update(syncFile);
    }

    public synchronized static void setForLocalDelete(Path path) throws IOException {
        SyncFile syncFile = get(path);
        syncFile.setLocalData(path);
        syncFile.setState(SyncState.FOR_LOCAL_DELETE);
        repo().update(syncFile);
    }

    public synchronized static void setForCloudDelete(File file) {
        SyncFile syncFile = get(file);
        syncFile.setCloudData(file);
        syncFile.setState(SyncState.FOR_CLOUD_DELETE);
        repo().update(syncFile);
    }

    public synchronized static void setConflict(File storjFile, Path localFile) throws IOException {
        SyncFile syncFile = getOrCreate(storjFile);
        syncFile.setCloudData(storjFile);
        syncFile.setLocalData(localFile);
        syncFile.setState(SyncState.CONFLICT);
        repo().update(syncFile);
    }

    public static void main(String[] args) {
        List<SyncFile> files = repo().find().toList();
        for (SyncFile file : files) {
            System.out.println(file);
        }
    }

}
