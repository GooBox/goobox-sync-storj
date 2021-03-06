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
package io.goobox.sync.storj.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.dizitart.no2.Nitrite;
import org.dizitart.no2.objects.ObjectFilter;
import org.dizitart.no2.objects.ObjectRepository;
import org.dizitart.no2.objects.filters.ObjectFilters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobox.sync.common.Utils;
import io.goobox.sync.storj.App;
import io.goobox.sync.storj.StorjUtil;
import io.storj.libstorj.File;

public class DB {

    private static final Logger logger = LoggerFactory.getLogger(DB.class);

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
        return Nitrite.builder()
                .compressed()
                .filePath(getDBPath().toFile())
                .openOrCreate();
    }

    public static void reset() {
        logger.info("Resetting sync DB");
        try {
            Files.deleteIfExists(getDBPath());
        } catch (IOException e) {
            logger.error("Failed deleting DB file", e);
        }
    }

    private static Path getDBPath() {
        return Utils.getDataDir().resolve("sync.db");
    }

    public static String getName(File file) {
        return file.getName().replaceAll("/+$", ""); // remove trailing slash
    }

    public static String getName(Path path) {
        return StorjUtil.getStorjName(path).replaceAll("/+$", ""); // remove trailing slash
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
        return contains(getName(file));
    }

    public synchronized static boolean contains(Path path) {
        return contains(getName(path));
    }

    public synchronized static boolean contains(String fileName) {
        return get(fileName) != null;
    }

    public synchronized static SyncFile get(File file) {
        return get(getName(file));
    }

    public synchronized static SyncFile get(Path path) {
        return get(getName(path));
    }

    public synchronized static SyncFile get(String fileName) {
        return repo().find(withName(fileName)).firstOrDefault();
    }

    private synchronized static SyncFile getOrCreate(File file) {
        return getOrCreate(getName(file));
    }

    private synchronized static SyncFile getOrCreate(Path path) {
        return getOrCreate(getName(path));
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
        remove(getName(file));
    }

    public synchronized static void remove(Path path) {
        remove(getName(path));
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
        App.getInstance().getOverlayHelper().refresh(localFile);
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
        App.getInstance().getOverlayHelper().refresh(localFile);
    }

    public synchronized static void addForUpload(Path path) throws IOException {
        remove(path);
        SyncFile syncFile = getOrCreate(path);
        syncFile.setLocalData(path);
        syncFile.setState(SyncState.FOR_UPLOAD);
        repo().update(syncFile);
        App.getInstance().getOverlayHelper().refresh(path);
    }

    public synchronized static void addForUpload(File storjFile, Path localFile) throws IOException {
        SyncFile syncFile = getOrCreate(localFile);
        syncFile.setCloudData(storjFile);
        syncFile.setLocalData(localFile);
        syncFile.setState(SyncState.FOR_UPLOAD);
        repo().update(syncFile);
        App.getInstance().getOverlayHelper().refresh(localFile);
    }

    public synchronized static void setDownloadFailed(File storjFile, Path localFile) throws IOException {
        SyncFile syncFile = get(storjFile);
        syncFile.setCloudData(storjFile);
        if (localFile != null && Files.exists(localFile)) {
            syncFile.setLocalData(localFile);
        }
        syncFile.setState(SyncState.DOWNLOAD_FAILED);
        repo().update(syncFile);
        App.getInstance().getOverlayHelper().refresh(localFile);
    }

    public synchronized static void setUploadFailed(Path path) throws IOException {
        SyncFile syncFile = get(path);
        if (Files.exists(path)) {
            syncFile.setLocalData(path);
        }
        syncFile.setState(SyncState.UPLOAD_FAILED);
        repo().update(syncFile);
        App.getInstance().getOverlayHelper().refresh(path);
    }

    public synchronized static void setForLocalDelete(Path path) throws IOException {
        SyncFile syncFile = get(path);
        syncFile.setLocalData(path);
        syncFile.setState(SyncState.FOR_LOCAL_DELETE);
        repo().update(syncFile);
        App.getInstance().getOverlayHelper().refresh(path);
    }

    public synchronized static void setForCloudDelete(File file) {
        SyncFile syncFile = get(file);
        syncFile.setCloudData(file);
        syncFile.setState(SyncState.FOR_CLOUD_DELETE);
        repo().update(syncFile);
    }

    public synchronized static void addForLocalCreateDir(File file) throws IOException {
        SyncFile syncFile = getOrCreate(file);
        syncFile.setCloudData(file);
        syncFile.setState(SyncState.FOR_LOCAL_CREATE_DIR);
        repo().update(syncFile);
    }

    public synchronized static void addForCloudCreateDir(Path path) throws IOException {
        SyncFile syncFile = getOrCreate(path);
        syncFile.setLocalData(path);
        syncFile.setState(SyncState.FOR_CLOUD_CREATE_DIR);
        repo().update(syncFile);
        App.getInstance().getOverlayHelper().refresh(path);
    }

    public synchronized static void setConflict(File storjFile, Path localFile) throws IOException {
        SyncFile syncFile = getOrCreate(storjFile);
        syncFile.setCloudData(storjFile);
        syncFile.setLocalData(localFile);
        syncFile.setState(SyncState.CONFLICT);
        repo().update(syncFile);
        App.getInstance().getOverlayHelper().refresh(localFile);
    }

    public static void main(String[] args) {
        List<SyncFile> files = repo().find().toList();
        for (SyncFile file : files) {
            System.out.println(file);
        }
    }

}
