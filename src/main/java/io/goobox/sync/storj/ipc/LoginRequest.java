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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobox.sync.storj.App;
import io.storj.libstorj.Keys;
import io.storj.libstorj.Storj;

public class LoginRequest {

    private static final Logger logger = LoggerFactory.getLogger(LoginRequest.class);

    private String email;
    private String password;
    private String encryptionKey;

    public LoginRequest(String email, String password, String encryptionKey) {
        this.email = email;
        this.password = password;
        this.encryptionKey = encryptionKey;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public CommandResult execute() {
        if (email == null) {
            String msg = "Missing email argument";
            logger.error(msg);
            return new CommandResult(Status.ERROR, msg);
        }

        if (password == null) {
            String msg = "Missing password argument";
            logger.error(msg);
            return new CommandResult(Status.ERROR, msg);
        }

        if (encryptionKey == null) {
            String msg = "Missing encryptionKey argument";
            logger.error(msg);
            return new CommandResult(Status.ERROR, msg);
        }

        Storj storj = App.getInstance().getStorj();
        Keys keys = new Keys(email, password, encryptionKey);

        boolean result = storj.verifyKeys(email, password);
        if (!result) {
            String msg = "Email and password do not match";
            logger.error(msg);
            return new CommandResult(Status.ERROR, msg);
        }

        result = storj.importKeys(keys, "");
        if (!result) {
            String msg = "Failed to write authentication file";
            logger.error(msg);
            return new CommandResult(Status.ERROR, msg);
        }

        return new CommandResult(Status.OK, null);
    }

}
