import base64
def queue_websockets(upgrade_request, message):
    connection1 = websocket_connection.create(upgrade_request, Engine.TURBO)
    with open('/Users/zakhar.fedotkin/ResearchProjects/SecLists/Passwords/Common-Credentials/10-million-password-list-top-10000.txt') as file:
        for line in file:
            line = line.strip()
            connection1.queue(message, base64.b64encode(line))

def handle_outgoing_message(websocket_message, interesting):
    pass

def handle_incoming_message(websocket_message, interesting):
    if interesting:
        results_table.add(websocket_message)