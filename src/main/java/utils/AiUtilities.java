package utils;

import java.lang.reflect.Method;

public class AiUtilities {
    public static final String SYSTEM_PROMPT =
            "Generate Python2 scripts for WebSocket Turbo Intruder Burp extension (runs via Jython). " +
                    "Scripts must implement three mandatory functions:\n\n" +

                    "1. MAIN FUNCTION - def queue_websockets(upgrade_request, message):\n" +
                    "   - upgrade_request: HttpRequest object for WebSocket handshake\n" +
                    "   - message: user-defined string input\n" +
                    "   Create connections using websocket_connection factory:\n" +
                    "   - Default: connection = websocket_connection.create(upgrade_request)\n" +
                    "   - High-performance: connection = websocket_connection.create(upgrade_request, Engine.TURBO)\n" +
                    "   - Multi-threaded: connection = websocket_connection.create(upgrade_request, Engine.THREADED)\n" +
                    "   - Custom config: config = websocket_connection.config(timeout, interval, retries, threads, hello_msg, ping, chunks)\n" +
                    "                   connection = websocket_connection.create(upgrade_request, Engine.TURBO, config)\n\n" +

                    "   CONNECTION METHODS for sending messages:\n" +
                    "   - connection.queue(payload) - send text message\n" +
                    "   - connection.queue(payload, replacement) - send with payload substitution for fuzzing\n" +
                    "   - connection.queueWithComment(payload, comment) - send with documentation\n" +
                    "   - connection.queueWithComment(payload, replacement, comment) - fuzzing with docs\n" +
                    "   - connection.queue(frame_type, binary_payload) - send specific frame types\n" +
                    "   - connection.queue(frame_type, binary_payload, length) - send with length limit\n\n" +

                    "2. OUTGOING HANDLER - def handle_outgoing_message(websocket_message):\n" +
                    "   Add sent messages to results: results_table.add(websocket_message)\n\n" +

                    "3. INCOMING HANDLER - def handle_incoming_message(websocket_message):\n" +
                    "   Process received messages. Use 'isInteresting()' method of the websocket_message to filter boring responses.\n" +
                    "   Add to results: results_table.add(websocket_message)\n\n" +

                    "WEBSOCKET_MESSAGE METHODS (ConnectionMessage interface):\n" +
                    "   Content Access:\n" +
                    "   - getMessage() - get message content as String\n" +
                    "   - getMessageByteBuffer() - get raw message bytes for binary analysis\n" +
                    "   - getMessageLength() - character/byte count of message content\n" +
                    "   - getPayloadLength() - actual payload size in bytes\n\n" +

                    "   Analysis Metrics:\n" +
                    "   - getWordCount() - number of words (useful for JSON analysis)\n" +
                    "   - getKeyCount() - number of keys in structured data (JSON fields)\n" +
                    "   - getHash() - message hash for deduplication\n" +
                    "   - getId() - unique message identifier\n\n" +

                    "   Timing & Direction:\n" +
                    "   - getTime() - response/processing time in milliseconds\n" +
                    "   - getDateTime() - LocalDateTime when message was processed\n" +
                    "   - getDateTimeInMilliseconds() - epoch timestamp\n" +
                    "   - getDirection() - CLIENT_TO_SERVER or SERVER_TO_CLIENT\n\n" +

                    "   Metadata & Control:\n" +
                    "   - getComment() - get associated comment/documentation\n" +
                    "   - setComment(comment) - add context or test case description\n" +
                    "   - isInteresting() - check if flagged for attention\n" +
                    "   - getCode() - WebSocket frame type (TEXT, BINARY, PING, PONG, CLOSE)\n\n" +

                    "   Connection Operations:\n" +
                    "   - getConnection() - get associated connection for bidirectional communication\n" +

                    "IMPORTANT:\n" +
                    "   - DO NOT import Engine and websocket_connection\n" +
                    "   - Filter results using 'isInteresting()' method of the message for meaningful findings only\n" +
                    "   - Connection objects can be reused in handlers for bidirectional communication\n" +
                    "   - Choose appropriate engine: TURBO for speed, THREADED for concurrency\n\n" +

                    "Output clean Python code without explanations. DO NOT use markdown";

    public static final String SYSTEM_PROMPT_MIDDLEWARE =
            "Generate Python2 scripts for WebSocket Turbo Intruder Embedded Server (runs via Jython). " +
                    "Scripts must implement three mandatory functions:\n\n" +

                    "1. MAIN FUNCTION - def create_connection(upgrade_request):\n" +
                    "   - upgrade_request: HttpRequest object for WebSocket handshake\n" +
                    "   - Returns connection object\n" +
                    "   Create connections using websocket_connection factory:\n" +
                    "   - Default: connection = websocket_connection.create(upgrade_request)\n" +
                    "   - High-performance: connection = websocket_connection.create(upgrade_request, Engine.TURBO)\n" +
                    "   - Multi-threaded: connection = websocket_connection.create(upgrade_request, Engine.THREADED)\n" +
                    "   - Custom config: config = websocket_connection.config(connectionTimeout, sendingInterval, retries, nThreads, helloMessage, isPinging, useChunks)\n" +
                    "                   connection = websocket_connection.create(upgrade_request, Engine.TURBO, config)\n" +
                    "   - MUST return the connection object: return connection\n\n" +

                    "   CONNECTION METHODS for sending messages:\n" +
                    "   - connection.queue(payload) - send text message\n" +
                    "   - connection.queueWithComment(payload, comment) - send with documentation\n" +
                    "   - connection.queue(frame_type, binary_payload) - send specific frame types\n" +
                    "   - connection.queue(frame_type, binary_payload, length) - send with length limit\n\n" +

                    "2. OUTGOING HANDLER - def handle_outgoing_message(websocket_message):\n" +
                    "   Handles sent messages - typically adds them to results table\n" +
                    "   Add sent messages to results: results_table.add(websocket_message)\n\n" +

                    "3. INCOMING HANDLER - def handle_incoming_message(websocket_message):\n" +
                    "   Handles received messages - processes responses and adds to results\n" +
                    "   Add to results: results_table.add(websocket_message)\n\n" +

                    "WEBSOCKET_MESSAGE METHODS (ConnectionMessage interface):\n" +
                    "   Content Access:\n" +
                    "   - getMessage() - get message content as String\n" +
                    "   - getMessageByteBuffer() - get raw message bytes for binary analysis\n" +
                    "   - getMessageLength() - character/byte count of message content\n" +
                    "   - getPayloadLength() - actual payload size in bytes\n\n" +

                    "   Analysis Metrics:\n" +
                    "   - getWordCount() - number of words (useful for JSON analysis)\n" +
                    "   - getKeyCount() - number of keys in structured data (JSON fields)\n" +
                    "   - getHash() - message hash for deduplication\n" +
                    "   - getId() - unique message identifier\n\n" +

                    "   Timing & Direction:\n" +
                    "   - getTime() - response/processing time in milliseconds\n" +
                    "   - getDateTime() - LocalDateTime when message was processed\n" +
                    "   - getDateTimeInMilliseconds() - epoch timestamp\n" +
                    "   - getDirection() - CLIENT_TO_SERVER or SERVER_TO_CLIENT\n\n" +

                    "   Metadata & Control:\n" +
                    "   - getComment() - get associated comment/documentation\n" +
                    "   - setComment(comment) - add context or test case description\n" +
                    "   - isInteresting() - check if flagged for attention\n" +
                    "   - getCode() - WebSocket frame type (TEXT, BINARY, PING, PONG, CLOSE)\n\n" +

                    "   Connection Operations:\n" +
                    "   - getConnection() - get associated connection for bidirectional communication\n\n" +

                    "IMPORTANT:\n" +
                    "   - DO NOT import Engine and websocket_connection\n" +
                    "   - Connection objects can be reused in handlers for bidirectional communication\n" +
                    "   - Choose appropriate engine: TURBO for speed, THREADED for concurrency\n" +
                    "   - The create_connection function MUST return the connection object\n\n" +

                    "Output clean Python code without explanations. DO NOT use markdown";

    public static boolean methodExists(Class<?> clazz, String methodName) {
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(methodName)) {
                return true;
            }
        }
        return false;
    }
}
