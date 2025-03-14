import base64
def queue_websockets(upgrade_request, message):
    """
    Create 10 WebSocket connections by default.
    Config can be changed using following method
    websocket_connection.create(upgrade_request, Engine.THREADED, websocket_connection.config(0, 10, 1, 10, None, True, True))
    - `websocket_connection.config(connectionTimeout, sendingInterval, retries, number_of_threads, hello_message, send_ping_messages, use_chunks)`
    - - `connectionTimeout` - connection timeout (Integer)
    - - `sendingInterval` - delay between WebSocket frames in milliseconds (Integer)
    - - `retries` - number of reconnection attempts if the connection closes (Integer)
    - - `number_of_threads` - number of threads used by the `Threaded` engine (Integer)
    - - `hello_message` - initial WebSocket message sent upon connection (String)
    - - `send_ping_messages` - whether the engine should send ping messages (boolean)
    - - `use_chunks` - whether the engine should send chunked requests (boolean)
    """
    connection1 = websocket_connection.create(upgrade_request, Engine.THREADED)
    with open('/usr/share/dict/words') as file:
        for line in file:
            line = line.strip()
            connection1.queue(message, base64.b64encode(line))

def handle_outgoing_message(websocket_message, interesting):
    results_table.add(websocket_message)

def handle_incoming_message(websocket_message, interesting):
    if interesting:
        results_table.add(websocket_message)