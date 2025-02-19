def queue_websockets(upgrade_request, message):
    connection1 = websocket_connection.create(upgrade_request, websocket_connection.config("MULTI", 0, 100, 1, 10, None, False, True, True))
    for l in range(0, 100):
        connection1.queue("TEST" + str(l))

def handle_outgoing_message(websocket_message, interesting):
    results_table.add(websocket_message)

def handle_incoming_message(websocket_message, interesting):
    if websocket_message.isInteresting():
        results_table.add(websocket_message)