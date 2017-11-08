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
package io.goobox.sync.storj;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Utils {

    private static String OS = null;

    public static Path getHomeDir() {
        String path = System.getProperty("user.home");
        if (isWindows() && !isPureAscii(path)) {
            try {
                path = getMSDOSPath(path);
            } catch (IOException | InterruptedException e) {
                throw new IllegalStateException("Cannot determine user home dir", e);
            }
        }
        return Paths.get(path);
    }

    public static Path getConfigDir() {
        return Utils.getHomeDir().resolve(".goobox");
    }

    public static Path getSyncDir() {
        return Utils.getHomeDir().resolve("Goobox");
    }

    public static Path getStorjConfigDir() {
        return Utils.getHomeDir().resolve(".storj");
    }

    public static long getTime(String storjTimestamp) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = sdf.parse(storjTimestamp);
        return date.getTime();
    }

    private static String getOsName() {
        if (OS == null) {
            OS = System.getProperty("os.name");
        }
        return OS;
    }

    private static boolean isWindows() {
        return getOsName().startsWith("Windows");
    }

    private static boolean isPureAscii(String path) {
        return StandardCharsets.US_ASCII.newEncoder().canEncode(path);
    }

    private static String getMSDOSPath(String path) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(
                "cmd /c for %I in (\"" + path + "\") do @echo %~fsI");

        process.waitFor();

        byte[] data = new byte[65536];
        int size = process.getInputStream().read(data);

        if (size <= 0)
            return null;

        return new String(data, 0, size).replaceAll("\\r\\n", "");
    }

}
