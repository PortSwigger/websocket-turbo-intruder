package turbo;

import socket.TurboWebSocketInterface;

import java.util.ArrayList;
import java.util.List;

public class RawWebSocketMessagePublisher {
    private final List<TurboWebSocketInterface> subscribers;

    public RawWebSocketMessagePublisher() {
        this.subscribers = new ArrayList<>();
    }

    public void addSubscriber(TurboWebSocketInterface subscriber) {
        this.subscribers.add(subscriber);
    }

    public void removeSubscriber(TurboWebSocketInterface subscriber) {
        this.subscribers.remove(subscriber);
    }

    // Close the interpreter, notifying all subscribers
    public void close() {

        for (TurboWebSocketInterface subscriber : subscribers) {
            subscriber.stop();
        }

        subscribers.clear();
    }
}
