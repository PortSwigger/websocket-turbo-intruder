def queue_websockets(upgrade_request, message):
    """
    Create a WebSocket connection and start testing
    """
    connection1 = websocket_connection.create(upgrade_request)

    for i in range(10):
        connection1.queueWithComment(message, "foo")

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
    websocket_message.setComment("bar")
    results_table.add(websocket_message)