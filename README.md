# WebSocket Turbo Intruder
An extension for fuzzing WebSocket messages using custom Python code.

## Usage

### Intruder tool
1. Right-click on a WebSocket message and navigate to Extensions > WebSocket Turbo Intruder > Send to WebSocket Turbo Intruder. 
If you highlight a portion of the message before sending, the highlighted content will be replaced with %s.
2. Select a template from the drop-down list.
3. Modify the Python code to fit your use case.
4. Start the attack.

Note: This will establish a new WebSocket connection for sending messages.

### Middleware tool
1. Right-click on a WebSocket message and navigate to Extensions > WebSocket Turbo Intruder > Send to WebSocket HTTP middleware.
2. Click the Start button to launch the HTTP server middleware.
3. For each unique WebSocket connection, the extension will create an HTTP POST endpoint that routes requests to the remote server.
4. Select the HTTP server from the table list and send requests through one of Burp Suite's tools.

## Documentation

### `queue_websockets(base_websocket, message)`
The `websocket_connection` object provides the following methods:
- `create(base_websocket)` creates a WebSocket connection using Burp Suite.
- `create(base_websocket, engine)` initializes a connection using one of the supported engines: `BURP`, `TURBO` or `THREADED`.
- `create(base_websocket, engine, config)` initializes a connection with manual settings using: `TURBO` and `THREADED` engines.
- `config()` creates default configuration used by `TURBO` and `THREADED` engines
- `config(connectionTimeout, sendingInterval, retries, number_of_threads, hello_message, send_ping_messages, use_chunks)`
- - `connectionTimeout` - connection timeout (Integer)
- - `sendingInterval` - delay between WebSocket frames in milliseconds (Integer)
- - `retries` - number of reconnection attempts if the connection closes (Integer)
- - `number_of_threads` - number of threads used by the `THREADED` engine (Integer)
- - `hello_message` - initial WebSocket message sent upon connection (String)
- - `send_ping_messages` - whether the engine should send ping messages (boolean)
- - `use_chunks` - whether the engine should send chunked requests (boolean)

Once a WebSocket connection is established, messages can be queued for sending.

Use the `queue()` method on this object.
- `queue(String message)`: Send the message
- `queue(String message, String replacement)`: Send message. `replacement` will replace all instances of `%s` in your message
- `queue(FrameType code, byte[] message)`: Sends a payload with a specified type code:
- - CONTINUATION
- - TEXT
- - BINARY
- - CLOSE
- - PING
- - PONG
- `queue(FrameType code, byte[] message, Integer legth)`: Sends a payload with a custom length and type code.
- `queue(FrameType code, byte[] message, String legth)`: Sends a payload with a custom length defined as a Long string.
- `queueWithComment(String message, String comment)`: Sends a message with an attached comment.
- `queueWithComment(String message, String replacement, String comment)`: Sends a message with a comment, with `replacement` replacing all `%s` instances in the message.

The `message` represents the contents of the WebSocket message editor at the top of the screen. It can be manually edited or modified within the Python script.

### `handle_outgoing_message(websocket_message)`
Use this method to conditionally add outgoing messages to the results table.

### `handle_incoming_message(websocket_message)`
Use this method to conditionally add incoming messages to the results table.

### `websocket_message`
Methods:
- `getMessage()`: Retrieves the message as a String.
- `getDirection()`: Retrieves the message direction (burp.api.montoya.websocket.Direction).
- `getLength()`: Retrieves the message length.
- `getDateTime()`: Retrieves the timestamp (java.time.LocalDateTime).
- `getComment()`: Retrieves the comment associated with the message.
- `setComment(String comment)`: Sets a comment on the message.
- `getConnection()`: Retrieves the connection, allowing additional messages to be queued.
- `getHash()`: Retrieves the message hash. Incoming and outgoing messages with the same hash are marked as request-response pairs.
- `setHash(long id)`: Sets a custom hash for the message.
- `getTime()`: Retrieves the response time in milliseconds.
- `isInteresting()`: Determines if the message attributes differ from other messages based on:
- - JSON key count
- - JSON key names
- - Backslash Powered Scanner keywords
- - Number of lines
- - Number of whitespaces
- - Message length

