def create_connection(upgrade_request):
    connection = websocket_connection.create(upgrade_request, Engine.TURBO)
    return connection

def handle_outgoing_message(websocket_message):
    results_table.add(websocket_message)

def handle_incoming_message(websocket_message):
    results_table.add(websocket_message)