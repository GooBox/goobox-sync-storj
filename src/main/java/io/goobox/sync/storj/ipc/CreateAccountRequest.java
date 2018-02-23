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
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobox.sync.storj.App;
import io.storj.libstorj.RegisterCallback;
import io.storj.libstorj.Storj;

public class CreateAccountRequest {

    private static final Logger logger = LoggerFactory.getLogger(CreateAccountRequest.class);

    public static final String METHOD = "createAccount";

    private String email;
    private String password;

    public CreateAccountRequest(Map<String, String> args) {
        this(args.get("email"), args.get("password"));
    }

    public CreateAccountRequest(String email, String password) {
        this.email = email;
        this.password = password;
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

        Storj storj = App.getInstance().getStorj();

        final CountDownLatch latch = new CountDownLatch(1);
        final String[] error = { null };

        storj.register(email, password, new RegisterCallback() {
            @Override
            public void onConfirmationPending(String email) {
                latch.countDown();
            }

            @Override
            public void onError(int code, String message) {
                error[0] = message;
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            error[0] = "Interrupted";
        }

        if (error[0] != null) {
            return new CommandResult(Status.ERROR, error[0]);
        }

        return new GenerateMnemonicRequest().execute();
    }

}
