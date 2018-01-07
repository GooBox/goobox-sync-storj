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
package io.goobox.sync.storj;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.goobox.sync.common.Utils;

@RunWith(JUnit4.class)
public class AppTest {

    @Test
    public void defaultSyncDir() throws Exception {
        App app = new App();
        Assert.assertEquals(Utils.getSyncDir(), app.getSyncDir());
    }

    @Test
    public void customSyncDir() throws Exception {
        Path customDir = Paths.get("/custom/sync/dir");
        App app = new App(customDir);
        Assert.assertEquals(customDir, app.getSyncDir());
    }

}
