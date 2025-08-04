def queue_websockets(upgrade_request, message):
    connection = websocket_connection.create(upgrade_request, Engine.TURBO)

    for i in range(10):
        connection.queue(message, str(i))

def handle_outgoing_message(websocket_message):
    results_table.add(websocket_message)

def handle_incoming_message(websocket_message):
    results_table.add(websocket_message)