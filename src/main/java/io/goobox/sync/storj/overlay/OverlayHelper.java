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
package io.goobox.sync.storj.overlay;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.DosFileAttributeView;
import java.util.ArrayList;
import java.util.List;

import com.liferay.nativity.control.NativityControl;
import com.liferay.nativity.control.NativityControlUtil;
import com.liferay.nativity.modules.contextmenu.ContextMenuControlCallback;
import com.liferay.nativity.modules.contextmenu.ContextMenuControlUtil;
import com.liferay.nativity.modules.contextmenu.model.ContextMenuAction;
import com.liferay.nativity.modules.contextmenu.model.ContextMenuItem;
import com.liferay.nativity.modules.fileicon.FileIconControl;
import com.liferay.nativity.modules.fileicon.FileIconControlCallback;
import com.liferay.nativity.modules.fileicon.FileIconControlUtil;
import com.liferay.nativity.util.OSDetector;

import io.goobox.sync.common.Utils;

public class OverlayHelper {

    private NativityControl nativityControl;
    private FileIconControl fileIconControl;

    private int stateIconId = 0;

    private static OverlayHelper instance;

    public static OverlayHelper getInstance() {
        if (instance == null) {
            instance = new OverlayHelper();
        }
        return instance;
    }

    public OverlayHelper() {
        init();
    }

    public void setOK() {
        if (!OSDetector.isWindows()) {
            return;
        }

        stateIconId = 1;
        refresh();
    }

    public void setSynchronizing() {
        if (!OSDetector.isWindows()) {
            return;
        }

        stateIconId = 2;
        refresh();
    }

    public void shutdown() {
        if (!OSDetector.isWindows()) {
            return;
        }

        stateIconId = 0;
        refresh();
        nativityControl.disconnect();
    }

    private void refresh() {
        fileIconControl.refreshIcons(new String[] { Utils.getSyncDir().toString() });
    }

    private void init() {
        if (!OSDetector.isWindows()) {
            return;
        }

        nativityControl = NativityControlUtil.getNativityControl();
        nativityControl.connect();

        // Setting filter folders is required for Mac's Finder Sync plugin
        nativityControl.setFilterFolder(Utils.getSyncDir().toString());

        DosFileAttributeView attr = Files.getFileAttributeView(Utils.getSyncDir(), DosFileAttributeView.class);
        try {
            attr.setSystem(true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // FileIconControlCallback used by Windows and Mac
        FileIconControlCallback fileIconControlCallback = new FileIconControlCallback() {
            @Override
            public int getIconForFile(String path) {
                if (Utils.getSyncDir().toString().equals(path)) {
                    return stateIconId;
                }
                return 0;
            }
        };

        fileIconControl = FileIconControlUtil.getFileIconControl(nativityControl, fileIconControlCallback);

        fileIconControl.enableFileIcons();

        /* Context Menus */

        ContextMenuControlCallback contextMenuControlCallback = new ContextMenuControlCallback() {
            @Override
            public List<ContextMenuItem> getContextMenuItems(String[] paths) {
                ContextMenuItem contextMenuItem = new ContextMenuItem("Goobox");

                ContextMenuAction contextMenuAction = new ContextMenuAction() {
                    @Override
                    public void onSelection(String[] paths) {
                        System.out.println("Context menu selection: " + String.join("; ", paths));
                    }
                };

                contextMenuItem.setContextMenuAction(contextMenuAction);

                List<ContextMenuItem> contextMenuItems = new ArrayList<ContextMenuItem>();
                contextMenuItems.add(contextMenuItem);

                // Mac Finder Sync will only show the parent level of context menus
                return contextMenuItems;
            }
        };

        ContextMenuControlUtil.getContextMenuControl(nativityControl, contextMenuControlCallback);
    }

}
