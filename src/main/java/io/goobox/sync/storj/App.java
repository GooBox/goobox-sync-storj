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
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.liferay.nativity.control.NativityControl;
import com.liferay.nativity.control.NativityControlUtil;
import com.liferay.nativity.modules.fileicon.FileIconControl;
import com.liferay.nativity.modules.fileicon.FileIconControlCallback;
import com.liferay.nativity.modules.fileicon.FileIconControlUtil;
import com.liferay.nativity.util.OSDetector;

import io.goobox.sync.common.Utils;
import io.goobox.sync.common.systemtray.ShutdownListener;
import io.goobox.sync.common.systemtray.SystemTrayHelper;
import io.goobox.sync.storj.db.DB;
import io.goobox.sync.storj.ipc.StdinReader;
import io.goobox.sync.storj.overlay.OverlayHelper;
import io.storj.libstorj.Bucket;
import io.storj.libstorj.CreateBucketCallback;
import io.storj.libstorj.GetBucketsCallback;
import io.storj.libstorj.KeysNotFoundException;
import io.storj.libstorj.Storj;

public class App implements ShutdownListener {

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    private static App instance;

    private Path syncDir;

    private Storj storj;
    private Bucket gooboxBucket;
    private TaskQueue tasks;
    private TaskExecutor taskExecutor;
    private FileWatcher fileWatcher;
    private StdinReader stdinReader;

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
                .longOpt("sync-dir")
                .hasArg()
                .desc("set the sync dir")
                .build());

        try {
            CommandLine cmd = new DefaultParser().parse(opts, args);

            if (cmd.hasOption("reset-db")) {
                DB.reset();
            }

            if (cmd.hasOption("sync-dir")) {
                instance = new App(Paths.get(cmd.getParsedOptionValue("sync-dir").toString()));
            } else {
                instance = new App();
            }
        } catch (ParseException e) {
            logger.error("Failed to parse command line options", e);
            System.exit(1);
        }

        instance.init();

        NativityControl nativityControl = NativityControlUtil.getNativityControl();
        nativityControl.connect();

        // Setting filter folders is required for Mac's Finder Sync plugin
        // nativityControl.setFilterFolder(Utils.getSyncDir().toString());

        /* File Icons */

        int testIconId = 1;

        // FileIconControlCallback used by Windows and Mac
        FileIconControlCallback fileIconControlCallback = new FileIconControlCallback() {
            @Override
            public int getIconForFile(String path) {
                return 1; // testIconId;
            }
        };

        FileIconControl fileIconControl = FileIconControlUtil.getFileIconControl(nativityControl,
                fileIconControlCallback);

        fileIconControl.enableFileIcons();

        String testFilePath = instance.getSyncDir().toString();

        if (OSDetector.isWindows()) {
            // This id is determined when building the DLL
            testIconId = 1;
        } else if (OSDetector.isMinimumAppleVersion(OSDetector.MAC_YOSEMITE_10_10)) {
            // Used by Mac Finder Sync. This unique id can be set at runtime.
            testIconId = 1;

            fileIconControl.registerIconWithId("/tmp/goobox.icns",
                    "test label", "" + testIconId);
        } else if (OSDetector.isLinux()) {
            // Used by Mac Injector and Linux
            testIconId = fileIconControl.registerIcon("/tmp/git-clean.png");
        }

        // FileIconControl.setFileIcon() method only used by Linux
        fileIconControl.setFileIcon(testFilePath, testIconId);
        nativityControl.disconnect();
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

    public TaskQueue getTaskQueue() {
        return tasks;
    }

    public TaskExecutor getTaskExecutor() {
        return taskExecutor;
    }

    public FileWatcher getFileWatcher() {
        return fileWatcher;
    }

    private void init() {
        SystemTrayHelper.init(syncDir);
        SystemTrayHelper.setShutdownListener(this);

        storj = new Storj();
        storj.setConfigDirectory(StorjUtil.getStorjConfigDir().toFile());
        storj.setDownloadDirectory(syncDir.toFile());

        stdinReader = new StdinReader();
        stdinReader.start();

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

        tasks = new TaskQueue();
        tasks.add(new CheckStateTask());

        taskExecutor = new TaskExecutor(tasks);
        fileWatcher = new FileWatcher();

        fileWatcher.start();
        taskExecutor.start();
    }

    @Override
    public void shutdown() {
        // TODO graceful shutdown
        OverlayHelper.getInstance().shutdown();
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

        try {
            while (result[0] == null) {
                final CountDownLatch latch = new CountDownLatch(1);

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
                            public void onError(String message) {
                                logger.error("Failed creating cloud Goobox bucket: {}", message);
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
                    public void onError(String message) {
                        logger.error(message);
                        latch.countDown();
                    }
                });

                latch.await();

                if (result[0] == null) {
                    // error - wait 3 seconds before trying again
                    Thread.sleep(3000);
                }
            }
        } catch (KeysNotFoundException e) {
            logger.error(
                    "No keys found. Have your imported your keys using libstorj? Make sure you don't specify a passcode.");
        } catch (InterruptedException e) {
            // do nothing
        }

        return result[0];
    }
}
