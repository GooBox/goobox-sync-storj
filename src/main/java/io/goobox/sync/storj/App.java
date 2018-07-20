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
package io.goobox.sync.storj;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobox.sync.common.ShutdownListener;
import io.goobox.sync.common.Utils;
import io.goobox.sync.common.overlay.OverlayHelper;
import io.goobox.sync.storj.db.DB;
import io.goobox.sync.storj.ipc.IpcExecutor;
import io.goobox.sync.storj.overlay.StorjOverlayIconProvider;
import io.storj.libstorj.Bucket;
import io.storj.libstorj.CreateBucketCallback;
import io.storj.libstorj.GetBucketsCallback;
import io.storj.libstorj.KeysNotFoundException;
import io.storj.libstorj.Storj;

public class App implements ShutdownListener {

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    private static App instance;

    private static int NUM_THREADS = Runtime.getRuntime().availableProcessors();

    private Path syncDir;

    private Storj storj;
    private Bucket gooboxBucket;
    private TaskQueue tasks;
    private TaskExecutor taskExecutor;
    private FileWatcher fileWatcher;
    private IpcExecutor ipcExecutor;
    private OverlayHelper overlayHelper;

    private StorjExecutorService storjExecutorService;

    public App() {
        this.syncDir = Utils.getSyncDir();
    }

    public App(Path syncDir) {
        this.syncDir = syncDir;
    }

    public static void main(String[] args) {
        Options opts = new Options();
        opts.addOption(Option.builder()
                .longOpt("reset-db")
                .desc("reset sync DB")
                .build());
        opts.addOption(Option.builder()
                .longOpt("reset-auth-file")
                .desc("reset auth file")
                .build());
        opts.addOption(Option.builder()
                .longOpt("sync-dir")
                .hasArg()
                .desc("set the sync dir")
                .build());

        try {
            CommandLine cmd = new DefaultParser().parse(opts, args);

            if (cmd.hasOption("reset-db")) {
                DB.reset();
            }

            boolean resetAuthFile = cmd.hasOption("reset-auth-file");

            if (cmd.hasOption("sync-dir")) {
                String syncDirParam = (String) cmd.getParsedOptionValue("sync-dir");
                try {
                    Path syncDir = new java.io.File(syncDirParam).getCanonicalFile().toPath();
                    instance = new App(syncDir);
                } catch (IOException e) {
                    logger.error("Cannot resolve sync dir path: " + syncDirParam);
                    System.exit(1);
                }
            } else {
                instance = new App();
            }

            instance.init(resetAuthFile);
        } catch (ParseException e) {
            logger.error("Failed to parse command line options", e);
            System.exit(1);
        }
    }

    public static App getInstance() {
        return instance;
    }

    public Path getSyncDir() {
        return syncDir;
    }

    public Storj getStorj() {
        return storj;
    }

    public Bucket getGooboxBucket() {
        return gooboxBucket;
    }

    public IpcExecutor getIpcExecutor() {
        return ipcExecutor;
    }

    public TaskQueue getTaskQueue() {
        return tasks;
    }

    public TaskExecutor getTaskExecutor() {
        return taskExecutor;
    }

    public FileWatcher getFileWatcher() {
        return fileWatcher;
    }

    public OverlayHelper getOverlayHelper() {
        return overlayHelper;
    }

    private void init(boolean resetAuthFile) {
        storj = new Storj();
        storj.setConfigDirectory(Utils.getDataDir().toFile());
        storj.setDownloadDirectory(syncDir.toFile());

        if (resetAuthFile) {
            storj.deleteKeys();
        }

        ipcExecutor = new IpcExecutor();
        ipcExecutor.start();

        if (!checkAndCreateSyncDir()) {
            System.exit(1);
        }

        if (!checkAndCreateDataDir()) {
            System.exit(1);
        }

        gooboxBucket = checkAndCreateCloudBucket();
        if (gooboxBucket == null) {
            System.exit(1);
        }

        overlayHelper = new OverlayHelper(syncDir, new StorjOverlayIconProvider());
        storjExecutorService = new StorjExecutorService(NUM_THREADS, new LinkedBlockingQueue<Runnable>());

        tasks = new TaskQueue();
        tasks.add(new CheckStateTask());

        taskExecutor = new TaskExecutor(tasks, storjExecutorService);
        fileWatcher = new FileWatcher();

        fileWatcher.start();
        taskExecutor.start();
    }

    @Override
    public void shutdown() {
        logger.info("Shutting down");

        if (overlayHelper != null) {
            overlayHelper.shutdown();
        }

        if (storjExecutorService != null) {
            storjExecutorService.shutdownNow();
            logger.info("Storj executor service shutdown");
        }

        System.exit(0);
    }

    private boolean checkAndCreateSyncDir() {
        logger.info("Checking if local Goobox sync folder exists");
        return checkAndCreateFolder(getSyncDir());
    }

    private boolean checkAndCreateDataDir() {
        logger.info("Checking if Goobox data folder exists");
        return checkAndCreateFolder(Utils.getDataDir());
    }

    private boolean checkAndCreateFolder(Path path) {
        if (Files.exists(path)) {
            logger.info("Folder exists");
            return true;
        } else {
            logger.info("Folder does not exist");
            try {
                Files.createDirectory(path);
                logger.info("Folder created");
                return true;
            } catch (IOException e) {
                logger.error("Failed creating folder", e);
                return false;
            }
        }
    }

    private Bucket checkAndCreateCloudBucket() {
        logger.info("Checking if cloud Goobox bucket exists");
        final Bucket[] result = { null };

        while (result[0] == null) {
            final CountDownLatch latch = new CountDownLatch(1);

            try {
                storj.getBuckets(new GetBucketsCallback() {
                    @Override
                    public void onBucketsReceived(Bucket[] buckets) {
                        for (Bucket bucket : buckets) {
                            if ("Goobox".equals(bucket.getName())) {
                                result[0] = bucket;
                                logger.info("Goobox bucket exists");
                                latch.countDown();
                                return;
                            }
                        }

                        logger.info("Goobox bucket does not exist");
                        storj.createBucket("Goobox", new CreateBucketCallback() {
                            @Override
                            public void onError(String bucketName, int code, String message) {
                                logger.error("Failed creating cloud Goobox bucket: {} ({})", message, code);
                                latch.countDown();
                            }

                            @Override
                            public void onBucketCreated(Bucket bucket) {
                                logger.info("Cloud Goobox bucket created");
                                result[0] = bucket;
                                latch.countDown();
                            }
                        });
                    }

                    @Override
                    public void onError(int code, String message) {
                        logger.error("{} ({})", message, code);
                        latch.countDown();
                    }
                });
            } catch (KeysNotFoundException e) {
                logger.info("No keys found at {}. Waiting for keys to be imported.", Utils.getDataDir().toFile());
                latch.countDown();
            }

            try {
                latch.await();

                if (result[0] == null) {
                    // error - wait 3 seconds before trying again
                    Thread.sleep(3000);
                }
            } catch (InterruptedException e) {
                break;
            }
        }

        return result[0];
    }
}
