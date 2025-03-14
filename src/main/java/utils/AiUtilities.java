package utils;

import java.lang.reflect.Method;

public class AiUtilities {
    public static final String SYSTEM_PROMPT =
            "Help to write a python2 script for the WebSocket Turbo Intruder Burp extension. It will run by jython. " +
                    "The script must have three mandatory functions. " +
                    "Main function: def queue_websockets(upgrade_request, message): " +
                    "upgrade_request is HttpRequest object, message is a user defined string" +
                    "It should create connection object with " +
                    "connection = websocket_connection.create(upgrade_request, Engine.TURBO) " +
                    "Use that connection to send websocket messages:\n" +
                    "function connection.queue(message, payload) use second payload parameter nad keep first message as is. \n" +
                    "Second function def handle_outgoing_message(websocket_message, interesting): " +
                    "should be used to add sent websocket_message to the result_table with " +
                    "results_table.add(websocket_message)" +
                    "Third function def handle_incoming_message(websocket_message, interesting): " +
                    "should be used to add websocket_message to the result_table " +
                    "interesting parameter is boolean value that can be used to filter out boring responses." +
                    "websocket_message variable implements ConnectionMessage interface with following methods: \n" +
                    "String getMessage(); // websocket message\n" +
                    "long getTime(); // time to response" +
                    "int getWordCount(); // number of words in message\n" +
                    "int getMessageLength(); // length of the message\n" +
                    "Connection getConnection(); // will return active websocket connection that can be used to send additional requests inside handle_incoming_message function\n" +
                    "Use interesting flag to add only interesting findings to the result table.\n" +
                    "Do not import Engine and websocket_connection as they were already imported.\n" +
                    "Do not describe or explain anything. Do not use Markdown. Output plain text.";

    public static boolean methodExists(Class<?> clazz, String methodName) {
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(methodName)) {
                return true;
            }
        }
        return false;
    }
}
