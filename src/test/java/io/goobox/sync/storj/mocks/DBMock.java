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
package io.goobox.sync.storj.mocks;

import org.dizitart.no2.Nitrite;

import io.goobox.sync.storj.db.DB;
import mockit.Mock;
import mockit.MockUp;

public class DBMock extends MockUp<DB> {

    @Mock
    private Nitrite open() {
        return Nitrite.builder().compressed().openOrCreate();
    }

}
