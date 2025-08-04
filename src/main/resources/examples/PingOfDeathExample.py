def queue_websockets(upgrade_request, message):
    """
    queue(FrameType code, byte[] message, String length)
    Sends a binary message with a custom length defined as a Long string.
    Supported codes:
    - CONTINUATION
    - TEXT
    - BINARY
    - CLOSE
    - PING
    - PONG
    """
    connection = websocket_connection.create(upgrade_request, Engine.TURBO)
    connection.queue(TEXT, b"", "2147483647")

def handle_outgoing_message(websocket_message):
    results_table.add(websocket_message)

def handle_incoming_message(websocket_message):
    results_table.add(websocket_message)