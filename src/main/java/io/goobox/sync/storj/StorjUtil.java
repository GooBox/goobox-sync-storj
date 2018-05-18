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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import io.goobox.sync.common.Utils;
import io.storj.libstorj.Storj;

public class StorjUtil {

    public static long getTime(String storjTimestamp) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = sdf.parse(storjTimestamp);
        return date.getTime();
    }

    public static Path getStorjPath(Path path) {
        return App.getInstance().getSyncDir().relativize(path);
    }

    public static String getStorjName(Path path) {
        String name = getStorjPath(path).toString();
        name = name.replace('\\', '/');
        if (Files.isDirectory(path)) {
            name += "/";
        }
        return name;
    }

    public static boolean isTemporaryError(int code) {
        switch (code) {
        case Storj.CURLE_COULDNT_RESOLVE_PROXY:
        case Storj.CURLE_COULDNT_RESOLVE_HOST:
        case Storj.CURLE_COULDNT_CONNECT:
        case Storj.CURLE_OPERATION_TIMEDOUT:
        case Storj.HTTP_INTERNAL_SERVER_ERROR:
        case Storj.HTTP_SERVICE_UNAVAILABLE:
        case Storj.STORJ_BRIDGE_REQUEST_ERROR:
        case Storj.STORJ_BRIDGE_TOKEN_ERROR:
        case Storj.STORJ_BRIDGE_TIMEOUT_ERROR:
        case Storj.STORJ_BRIDGE_INTERNAL_ERROR:
        case Storj.STORJ_BRIDGE_FRAME_ERROR:
        case Storj.STORJ_BRIDGE_POINTER_ERROR:
        case Storj.STORJ_BRIDGE_REPOINTER_ERROR:
        case Storj.STORJ_BRIDGE_OFFER_ERROR:
        case Storj.STORJ_FARMER_REQUEST_ERROR:
        case Storj.STORJ_FARMER_TIMEOUT_ERROR:
            return true;
        default:
            return false;
        }
    }

    public static boolean isExcluded(Path path) {
        if (Utils.isExcluded(path)) {
            return true;
        }

        try {
            if (Files.size(path) == 0) {
                return true;
            }
         } catch (IOException e) {
            return true;
        }

        return false;
    }

}
