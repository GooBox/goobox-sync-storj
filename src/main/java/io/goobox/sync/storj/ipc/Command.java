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

public class Command {

    private static final Logger logger = LoggerFactory.getLogger(Command.class);

    protected String method;
    protected Map<String, String> args;

    public CommandResult execute() {
        if (method == null) {
            return errorResult("Method missing");
        }

        switch (method) {
        case LoginRequest.METHOD:
            return new LoginRequest(args).execute();
        case CreateAccountRequest.METHOD:
            return new CreateAccountRequest(args).execute();
        case CheckMnemonicRequest.METHOD:
            return new CheckMnemonicRequest(args).execute();
        case QuitCommand.METHOD:
            return new QuitCommand().execute();
        default:
            return errorResult("Invalid command method: " + method);
        }
    }

    public CommandResult errorResult(String msg) {
        logger.error(msg);
        return new CommandResult(Status.ERROR, msg);
    }

}
