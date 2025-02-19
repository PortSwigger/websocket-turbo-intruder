import time
def queue_websockets(upgrade_request, message):
    connection1 = websocket_connection.create(upgrade_request, Engine.TURBO)
    connection1.queue("40")

@Pong("3")
def handle_outgoing_message(websocket_message, interesting):
    results_table.add(websocket_message)

def start(websocket_message):
    for i in range(1):
        websocket_message.getConnection().queue("42[\"message\",\"{}\"]".format(str(i)))
@PingPong("2","3")
def handle_incoming_message(websocket_message, interesting):
    if websocket_message.getMessage().startswith("40"):
        start(websocket_message)
    results_table.add(websocket_message)