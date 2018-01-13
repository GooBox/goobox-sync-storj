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

public class IpcExecutor extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(IpcExecutor.class);

    private Gson gson = new Gson();

    @Override
    public void run() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                String input = scanner.nextLine();
                logger.debug("Command input received: {}", input);

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

                sendResult(result);
            }
        }
    }

    private void sendResult(CommandResult result) {
        send(result, "Command result sent");
    }

    public void sendIdleEvent() {
        sendEvent(new SyncStateEvent("idle"));
    }

    public void sendSyncEvent() {
        sendEvent(new SyncStateEvent("synchronizing"));
    }

    private void sendEvent(Event event) {
        send(event, "Event sent");
    }

    private void send(Object obj, String debugMessage) {
        if (obj != null) {
            String output = gson.toJson(obj);
            logger.debug("{}: {}", debugMessage, output);
            send(output);
        }
    }

    private void send(String msg) {
        synchronized (this) {
            System.out.println(msg);
        }
    }

}
