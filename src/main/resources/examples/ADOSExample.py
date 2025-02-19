import base64
def queue_websockets(upgrade_request, message):
    connection1 = websocket_connection.create(upgrade_request, Engine.BURP)
    connection1.queue(1, b"", "2147483647")

def handle_outgoing_message(websocket_message, interesting):
    results_table.add(websocket_message)

def handle_incoming_message(websocket_message, interesting):
    results_table.add(websocket_message)