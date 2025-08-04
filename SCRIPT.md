# WebSocket Turbo Intruder - Python Script Guide

## Overview

WebSocket Turbo Intruder uses Python2 scripts (executed via Jython) to perform advanced WebSocket testing and fuzzing. 
Every script must implement three mandatory functions to handle the testing lifecycle.

## Intruder Script Requirements

### Mandatory Functions

Every script must implement these three functions:

```python
def queue_websockets(upgrade_request, message):
    # Main function - sets up connections and queues messages
    connection = websocket_connection.create(upgrade_request)
    connection.queue(message)

def handle_outgoing_message(websocket_message):
    # Handles sent messages - typically adds them to results
    results_table.add(websocket_message)

def handle_incoming_message(websocket_message):
    # Handles received messages - processes responses and adds to results
    results_table.add(websocket_message)
```

## HTTP Middleware Server Script Requirements

### Mandatory Functions

Every HTTP Middleware server script must implement these three functions:

```python
def create_connection(upgrade_request):
    # Main function - sets up connections
    connection = websocket_connection.create(upgrade_request, Engine.TURBO)
    return connection

def handle_outgoing_message(websocket_message):
    # Handles sent messages - typically adds them to results
    results_table.add(websocket_message)

def handle_incoming_message(websocket_message):
    # Handles received messages - processes responses and adds to results
    results_table.add(websocket_message)
```

WebAcademy example

```python
def create_connection(upgrade_request):
    connection = websocket_connection.create(upgrade_request, Engine.TURBO)
    return connection

def handle_outgoing_message(websocket_message):
    results_table.add(websocket_message)

@MatchRegex(r"{\"user\":\"Hal Pline\".*")
def handle_incoming_message(websocket_message):
    results_table.add(websocket_message)
```

## Connection Management

### Creating Connections

The `websocket_connection` factory provides multiple ways to create connections:

```python
# Default connection (auto-selects best engine)
connection = websocket_connection.create(upgrade_request)

# High-performance single-threaded connection
connection = websocket_connection.create(upgrade_request, Engine.TURBO)

# Multi-threaded connection for concurrent operations
connection = websocket_connection.create(upgrade_request, Engine.THREADED)

# Custom configuration
config = websocket_connection.config(
    connectionTimeout=5000,    # Connection timeout in ms
    sendingInterval=100,       # Delay between messages in ms
    retries=3,                 # Reconnection attempts
    nThreads=10,               # Number of threads (THREADED engine)
    helloMessage="hello",      # Initial message after connection
    isPinging=True,            # Enable ping/pong keep-alive
    useChunks=False            # Use chunked message sending
)
connection = websocket_connection.create(upgrade_request, Engine.TURBO, config)
```

### Engine Types

- **BURP**: Burp Suite default engine
- **TURBO**: High-performance single-threaded engine for speed
- **THREADED**: Multi-threaded engine for concurrent operations

## Message Operations

### Sending Messages

```python
# Send text message
connection.queue("Hello World")

# Send with payload substitution (fuzzing)
connection.queue(message, "replacement_value")

# Send with comment for documentation
connection.queueWithComment(message, "Testing basic functionality")

# Send specific frame types
connection.queue(TEXT, "text_payload")
connection.queue(BINARY, binary_data)

# Send with length limit
connection.queue(TEXT, large_payload, 1000)

```

### Connection Control

```python

# Get original HTTP upgrade request
request = connection.upgradeRequest()

# Get message counters
incoming_count = connection.in()
outgoing_count = connection.out()

# Manually adjust counters for paring
connection.incIn()   # Increment incoming counter
connection.decIn()   # Decrement incoming counter
connection.incOut()  # Increment outgoing counter
connection.decOut()  # Decrement outgoing counter
```

## Message Analysis

### ConnectionMessage Methods

The `websocket_message` parameter in handlers provides comprehensive message information:

#### Content Access
```python
# Get message content
content = websocket_message.getMessage()                   # String content
raw_bytes = websocket_message.getMessageByteBuffer()       # Raw bytes
length = websocket_message.getMessageLength()              # Character/byte count
```

#### Analysis Metrics
```python
# Content analysis
word_count = websocket_message.getWordCount()              # Word count
key_count = websocket_message.getKeyCount()                # JSON keys count
message_hash = websocket_message.getHash()                 # Message hash
message_id = websocket_message.getId()                     # Unique ID
```

#### Timing & Direction
```python
# Timing information
response_time = websocket_message.getTime()                # Response time in ms
datetime_obj = websocket_message.getDateTime()             # LocalDateTime object
timestamp = websocket_message.getDateTimeInMilliseconds()  # Epoch timestamp

# Message direction
direction = websocket_message.getDirection()               # CLIENT_TO_SERVER or SERVER_TO_CLIENT
```

#### Metadata & Control
```python
# Comments and documentation
comment = websocket_message.getComment()                   # Get comment

# Interest flagging
is_interesting = websocket_message.isInteresting()         # Check if flagged

# Frame type
frame_type = websocket_message.getCode()                   # CONTINUATION, TEXT, BINARY, PING, PONG, CLOSE
```

#### Connection Operations
```python
# Get associated connection for bidirectional communication
connection = websocket_message.getConnection()
```

## Advanced Features

### Using ScriptEnvironment Decorators

The `ScriptEnvironment.py` provides helpful decorators for filtering and matching:

```python
# Filter messages by size
@FilterSize(404, 500)
def handle_incoming_message(websocket_message):
    # This won't process messages with length 404 or 500
    results_table.add(websocket_message)

# Match specific regex patterns
@MatchRegex(r"error|exception|failed")
def handle_incoming_message(websocket_message):
    # Only process messages matching the regex
    websocket_message.setInteresting(True)
    results_table.add(websocket_message)

# Filter by word count range
@FilterWordCountRange(1, 5)
def handle_incoming_message(websocket_message):
    # Skip messages with 1-5 words (likely boring)
    results_table.add(websocket_message)
```

### Custom Configuration Examples

```python
# Advanced configuration  
stealth_config = websocket_connection.config(
    connectionTimeout=10000,   # Long timeout
    sendingInterval=100,      # Slow sending
    retries=3,                 # More retries
    nThreads=2,                # 2 threads
    helloMessage="40",         # Initial message 40
    isPinging=True,            # Send Ping frames
    useChunks=False            # Send one frame per an interval
)
```

## Best Practices

Use Appropriate Engines:

- **BURP**: Default Burp Suite engine
- **TURBO**: For high-speed single-threaded testing
- **THREADED**: For concurrent operations or when timing matters


The following are automatically available in your scripts:

- `Engine`: Enum with BURP, TURBO, THREADED values
- `FrameType`: Enum with CONTINUATION, TEXT, BINARY, PING, PONG, CLOSE values  
