/*
 * Copyright (C) 2018 Kaloyan Raev
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

import java.nio.file.Path;

import io.goobox.sync.common.overlay.OverlayIcon;
import io.goobox.sync.common.overlay.OverlayIconProvider;
import io.goobox.sync.storj.db.DB;
import io.goobox.sync.storj.db.SyncFile;

public class StorjOverlayIconProvider implements OverlayIconProvider {

    @Override
    public OverlayIcon getIcon(Path path) {
        SyncFile file = DB.get(path);
        if (file == null || file.getState().isPending()) {
            return OverlayIcon.SYNCING;
        } else if (file.getState().isSynced()) {
            return OverlayIcon.OK;
        } else if (file.getState().isFailed()) {
            return OverlayIcon.ERROR;
        } else {
            return OverlayIcon.WARNING;
        }
    }

}
