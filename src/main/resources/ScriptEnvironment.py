import string, random, time, math, re
import data.Engine as Engine
import data.FrameType as FrameType

CONTINUATION = FrameType.CONTINUATION
TEXT = FrameType.TEXT
BINARY = FrameType.BINARY
CLOSE = FrameType.CLOSE
PING = FrameType.PING
PONG = FrameType.PONG

def MatchRegex(regex):
    m = re.compile(unicode(regex), re.UNICODE|re.DOTALL|re.MULTILINE|re.IGNORECASE)
    def decorator(func):
        def handle_incoming_message(websocket_message):
            if m.match(websocket_message.getMessage()):
                func(websocket_message)
            else:
                websocket_message.getConnection().decIn()
        return handle_incoming_message
    return decorator

def MatchSize(*args):
    def decorator(func):
        def handle_incoming_message(websocket_message):
            if websocket_message.getLength() in args:
                func(websocket_message)
            else:
                websocket_message.getConnection().decIn()
        return handle_incoming_message
    return decorator

def MatchSizeRange(min, max):
    def decorator(func):
        def handle_incoming_message(websocket_message):
            if ((websocket_message.getLength() >= min) and (websocket_message.getLength() <= max)):
                func(websocket_message)
            else:
                websocket_message.getConnection().decIn()
        return handle_incoming_message
    return decorator

def MatchWordCount(*args):
    def decorator(func):
        def handle_incoming_message(websocket_message):
            if websocket_message.getWordCount() in args:
                func(websocket_message)
            else:
                websocket_message.getConnection().decIn()
        return handle_incoming_message
    return decorator

def MatchWordCountRange(min, max):
    def decorator(func):
        def handle_incoming_message(websocket_message):
            if ((websocket_message.getWordCount() >= min) and (websocket_message.getWordCount() <= max)):
                func(websocket_message)
            else:
                websocket_message.getConnection().decIn()
        return handle_incoming_message
    return decorator

def FilterSize(*args):
    def decorator(func):
        def handle_incoming_message(websocket_message):
            if websocket_message.getLength() in args:
                websocket_message.getConnection().decIn()
                return
            func(websocket_message)
        return handle_incoming_message
    return decorator

def FilterRegex(regex):
    m = re.compile(unicode(regex), re.UNICODE|re.DOTALL|re.MULTILINE|re.IGNORECASE)
    def decorator(func):
        def handle_incoming_message(websocket_message):
            if not m.match(websocket_message.getMessage()):
                func(websocket_message)
            else:
                websocket_message.getConnection().decIn()
        return handle_incoming_message
    return decorator

def FilterSizeRange(min, max):
    def decorator(func):
        def handle_incoming_message(websocket_message):
            if ((websocket_message.getLength()  >= min) and (websocket_message.getLength()  <= max)):
                websocket_message.getConnection().decIn()
                return
            func(websocket_message)
        return handle_incoming_message
    return decorator

def FilterWordCount(*args):
    def decorator(func):
        def handle_incoming_message(websocket_message):
            if websocket_message.getWordCount() in args:
                websocket_message.getConnection().decIn()
                return
            func(websocket_message)
        return handle_incoming_message
    return decorator

def FilterWordCountRange(min, max):
    def decorator(func):
        def handle_incoming_message(websocket_message):
            if ((websocket_message.getWordCount() >= min) and (websocket_message.getWordCount() <= max)):
                websocket_message.getConnection().decIn()
                return
            func(websocket_message)
        return handle_incoming_message
    return decorator

def Pong(pong):
    def decorator(func):
        def handle_outgoing_message(websocket_message):
            if not websocket_message.getMessage().startswith(pong):
                func(websocket_message)
        return handle_outgoing_message
    return decorator

def PingPong(ping, pong):
    def decorator(func):
        def handle_incoming_message(websocket_message):
            if (websocket_message.getMessage().startswith(ping)):
                websocket_message.getConnection().queue(pong)
                websocket_message.getConnection().decOut()
                websocket_message.getConnection().decIn()
            else:
                func(websocket_message)
        return handle_incoming_message
    return decorator

def mean(data):
    return sum(data)/len(data)

def stddev(data):
    if len(data) == 1:
        return 0
    avg = mean(data)
    base = sum((entry-avg)**2 for entry in data)
    return math.sqrt(base/(len(data)-1))

def randstr(length=12, allow_digits=True):
    candidates = string.ascii_lowercase
    if allow_digits:
        candidates += string.digits
    return ''.join(random.choice(candidates) for x in range(length))
