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
package io.goobox.sync.storj.helpers;

import io.goobox.sync.storj.App;
import io.storj.libstorj.DeleteFileCallback;
import io.storj.libstorj.File;

public class StorjUtil {

    public static void deleteFile(File file) {
        App.getInstance().getStorj().deleteFile(null, file, new TestDeleteFileCallback());
    }

    private static class TestDeleteFileCallback implements DeleteFileCallback {

        @Override
        public void onFileDeleted() {
        }

        @Override
        public void onError(int code, String message) {
            String msg = String.format("%s (%d)", message, code);
            throw new IllegalStateException(msg);
        }

    }

}
