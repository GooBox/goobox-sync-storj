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

import io.goobox.sync.storj.App;
import io.storj.libstorj.Keys;
import io.storj.libstorj.Storj;

public class LoginRequest {

    private static final Logger logger = LoggerFactory.getLogger(LoginRequest.class);

    public static final String METHOD = "login";

    private String email;
    private String password;
    private String encryptionKey;

    public LoginRequest(Map<String, String> args) {
        this(args.get("email"), args.get("password"), args.get("encryptionKey"));
    }

    public LoginRequest(String email, String password, String encryptionKey) {
        this.email = email;
        this.password = password;
        this.encryptionKey = encryptionKey;
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

        try {
            int result = storj.verifyKeys(new Keys(email, password, encryptionKey));
            if (result != Storj.NO_ERROR) {
                String msg;

                switch (result) {
                case Storj.HTTP_UNAUTHORIZED:
                    msg = "Email and password do not match";
                    break;
                case Storj.STORJ_META_DECRYPTION_ERROR:
                    msg = "Encryption key cannot decrypt content";
                    break;
                default:
                    msg = Storj.getErrorMessage(result);
                }

                logger.error(msg);
                return new CommandResult(Status.ERROR, msg);
            }
        } catch (InterruptedException e) {
            String msg = "Login interrupted";
            logger.error(msg, e);
            return new CommandResult(Status.ERROR, msg);
        }

        if (!storj.importKeys(keys, "")) {
            String msg = "Failed to write authentication file";
            logger.error(msg);
            return new CommandResult(Status.ERROR, msg);
        }

        return new CommandResult(Status.OK, null);
    }

}
