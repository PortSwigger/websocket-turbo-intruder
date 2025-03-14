def queue_websockets(upgrade_request, message):
    """
    Create two WebSocket connections manually.
    """
    connection1 = websocket_connection.create(upgrade_request)
    connection2 = websocket_connection.create(upgrade_request.withHeader("Cookie", "session=foo"))

    connection1.queue("READY")
    connection2.queue("foo")

def handle_outgoing_message(websocket_message, interesting):
    """
    Log outgoing messages for debugging or inspection.
    """
    results_table.add(websocket_message)

def handle_incoming_message(websocket_message, interesting):
    """
    Handle incoming messages.
    Interesting boolean variable determines if the message attributes differ from other messages based on:
    - - Number of lines
    - - Number of words
    - - Message length
    """
    results_table.add(websocket_message)