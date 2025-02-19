import string, random, time, math, re
import data.Engine as Engine

def MatchRegex(regex):
    m = re.compile(unicode(regex), re.UNICODE|re.DOTALL|re.MULTILINE|re.IGNORECASE)
    def decorator(func):
        def handle_incoming_message(websocket_message,interesting):
            if m.match(websocket_message.getMessage()):
                func(websocket_message,interesting)
        return handle_incoming_message
    return decorator

def MatchSize(*args):
    def decorator(func):
        def handle_incoming_message(websocket_message,interesting):
            if websocket_message.getLength() in args:
                func(websocket_message,interesting)
        return handle_incoming_message
    return decorator

def MatchSizeRange(min, max):
    def decorator(func):
        def handle_incoming_message(websocket_message,interesting):
            if ((websocket_message.getLength() >= min) and (websocket_message.getLength() <= max)):
                func(websocket_message,interesting)
        return handle_incoming_message
    return decorator

def MatchWordCount(*args):
    def decorator(func):
        def handle_incoming_message(websocket_message,interesting):
            if websocket_message.getWordCount() in args:
                func(websocket_message,interesting)
        return handle_incoming_message
    return decorator

def MatchWordCountRange(min, max):
    def decorator(func):
        def handle_incoming_message(websocket_message,interesting):
            if ((websocket_message.getWordCount() >= min) and (websocket_message.getWordCount() <= max)):
                func(websocket_message,interesting)
        return handle_incoming_message
    return decorator

def FilterSize(*args):
    def decorator(func):
        def handle_incoming_message(websocket_message,interesting):
            if websocket_message.getLength() in args:
                return
            func(websocket_message,interesting)
        return handle_incoming_message
    return decorator

def FilterRegex(regex):
    m = re.compile(unicode(regex), re.UNICODE|re.DOTALL|re.MULTILINE|re.IGNORECASE)
    def decorator(func):
        def handle_incoming_message(websocket_message,interesting):
            if not m.match(websocket_message.getMessage()):
                func(websocket_message,interesting)
        return handle_incoming_message
    return decorator

def FilterSizeRange(min, max):
    def decorator(func):
        def handle_incoming_message(websocket_message,interesting):
            if ((websocket_message.getLength()  >= min) and (websocket_message.getLength()  <= max)):
                return
            func(websocket_message,interesting)
        return handle_incoming_message
    return decorator

def FilterWordCount(*args):
    def decorator(func):
        def handle_incoming_message(websocket_message,interesting):
            if websocket_message.getWordCount() in args:
                return
            func(websocket_message,interesting)
        return handle_incoming_message
    return decorator

def FilterWordCountRange(min, max):
    def decorator(func):
        def handle_incoming_message(websocket_message,interesting):
            if ((websocket_message.getWordCount() >= min) and (websocket_message.getWordCount() <= max)):
                return
            func(websocket_message,interesting)
        return handle_incoming_message
    return decorator

def Pong(pong):
    def decorator(func):
        def handle_outgoing_message(websocket_message,interesting):
            global PingCount
            try:
                PingCount
            except:
                PingCount = 0
            if not websocket_message.getMessage().startswith(pong):
                websocket_message.setHash(websocket_message.getHash() - PingCount)
                func(websocket_message,interesting)
        return handle_outgoing_message
    return decorator

def PingPong(ping, pong):
    def decorator(func):
        def handle_incoming_message(websocket_message,interesting):
            global PingCount
            try:
                PingCount
            except:
                PingCount = 0
            if (websocket_message.getMessage().startswith(ping)):
                PingCount += 1
                websocket_message.getConnection().queue(pong)
            else:
                websocket_message.setHash(websocket_message.getHash() - PingCount)
                func(websocket_message,interesting)
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
