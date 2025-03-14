def queue_websockets(upgrade_request, message):
    """
    queue(Integer code, byte[] message, String length)
    Sends a binary message with a custom length defined as a Long string.
    Supported codes:
    - 1 - text
    - 2 - binary
    - 8 - close
    - 9 - ping
    - 10 - pong
    """
    connection1 = websocket_connection.create(upgrade_request, Engine.TURBO)
    connection1.queue(1, b"", "2147483647")

def handle_outgoing_message(websocket_message, interesting):
    results_table.add(websocket_message)

def handle_incoming_message(websocket_message, interesting):
    results_table.add(websocket_message)