import base64
def queue_websockets(upgrade_request, message):
    conn = websocket_connection.create(upgrade_request, Engine.THREADED)
    with open('/usr/share/dict/words') as f:
        for i, line in enumerate(f):
            if i >= 100000: break
            conn.queue(message, base64.b64encode(line.strip().encode()).decode())

def handle_outgoing_message(websocket_message):
    results_table.add(websocket_message)

def handle_incoming_message(websocket_message):
    if websocket_message.isInteresting():
        results_table.add(websocket_message)