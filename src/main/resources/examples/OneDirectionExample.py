def queue_websockets(upgrade_request, message):
    connection = websocket_connection.create(upgrade_request)

    for i in range(10):
        connection.queue(message)

def handle_outgoing_message(websocket_message):
    return

def handle_incoming_message(websocket_message):
    results_table.add(websocket_message)