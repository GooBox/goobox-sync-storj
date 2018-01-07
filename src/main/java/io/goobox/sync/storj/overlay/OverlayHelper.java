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
package io.goobox.sync.storj.overlay;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.DosFileAttributeView;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

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

import io.goobox.sync.storj.App;
import io.goobox.sync.storj.db.DB;

public class OverlayHelper implements FileIconControlCallback, ContextMenuControlCallback {

    private NativityControl nativityControl;
    private FileIconControl fileIconControl;

    private int globalStateIconId = OverlayIcon.NONE.id();

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

        globalStateIconId = OverlayIcon.OK.id();
        refresh();
    }

    public void setSynchronizing() {
        if (!OSDetector.isWindows()) {
            return;
        }

        globalStateIconId = OverlayIcon.SYNCING.id();
        refresh();
    }

    public void shutdown() {
        if (!OSDetector.isWindows()) {
            return;
        }

        globalStateIconId = OverlayIcon.NONE.id();
        refresh();
        nativityControl.disconnect();
    }

    public void refresh(Path path) {
        if (fileIconControl != null && path != null) {
            String[] pathAndParents = Stream.iterate(path, p -> p.getParent())
                    .limit(App.getInstance().getSyncDir().relativize(path).getNameCount())
                    .map(Path::toString)
                    .toArray(String[]::new);
            fileIconControl.refreshIcons(pathAndParents);
        }
    }

    private void refresh() {
        fileIconControl.refreshIcons(new String[] { App.getInstance().getSyncDir().toString() });
    }

    private void init() {
        if (!OSDetector.isWindows()) {
            return;
        }

        nativityControl = NativityControlUtil.getNativityControl();
        nativityControl.connect();

        // Setting filter folders is required for Mac's Finder Sync plugin
        nativityControl.setFilterFolder(App.getInstance().getSyncDir().toString());

        // Make Goobox a system folder
        DosFileAttributeView attr = Files.getFileAttributeView(App.getInstance().getSyncDir(),
                DosFileAttributeView.class);
        try {
            attr.setSystem(true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        fileIconControl = FileIconControlUtil.getFileIconControl(nativityControl, this);
        fileIconControl.enableFileIcons();

        /* Context Menus */
        ContextMenuControlUtil.getContextMenuControl(nativityControl, this);
    }

    /* FileIconControlCallback used by Windows and Mac */
    @Override
    public int getIconForFile(String path) {
        Path p = Paths.get(path);
        if (!p.startsWith(App.getInstance().getSyncDir())) {
            return OverlayIcon.NONE.id();
        } else if (App.getInstance().getSyncDir().equals(p)) {
            return globalStateIconId;
        } else {
            try {
                return Files.walk(p)
                        .map(DB::get)
                        .map(OverlayIcon::from)
                        .map(OverlayIcon::id)
                        .reduce(OverlayIcon.NONE.id(), Integer::max);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

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

}
