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

import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class StdinReader extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(StdinReader.class);

    @Override
    public void run() {
        Gson gson = new Gson();
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                String input = scanner.nextLine();
                logger.debug("Command input received: " + input);

                CommandResult result = null;
                try {
                    Command cmd = gson.fromJson(input, Command.class);
                    if (cmd != null) {
                        result = cmd.execute();
                    }
                } catch (JsonSyntaxException e) {
                    String msg = "Invalid command input: " + input;
                    logger.error(msg, e);
                    result = new CommandResult(Status.ERROR, msg);
                }

                if (result != null) {
                    String output = gson.toJson(result);
                    logger.debug("Command result sent: " + output);
                    System.out.println(output);
                }
            }
        }
    }

}
