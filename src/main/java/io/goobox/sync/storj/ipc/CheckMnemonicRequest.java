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
package io.goobox.sync.storj.ipc;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.storj.libstorj.Storj;

public class CheckMnemonicRequest {

    private static final Logger logger = LoggerFactory.getLogger(CheckMnemonicRequest.class);

    public static final String METHOD = "checkMnemonic";

    private String encryptionKey;

    public CheckMnemonicRequest(Map<String, String> args) {
        this(args.get("encryptionKey"));
    }

    public CheckMnemonicRequest(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public CommandResult execute() {
        if (encryptionKey == null) {
            String msg = "Missing encryptionKey argument";
            logger.error(msg);
            return new CommandResult(Status.ERROR, msg);
        }

        boolean result = Storj.checkMnemonic(encryptionKey);
        if (!result) {
            String msg = "Invalid encryption key";
            logger.error(msg);
            return new CommandResult(Status.ERROR, msg);
        }

        return new CommandResult(Status.OK, null);
    }

}
