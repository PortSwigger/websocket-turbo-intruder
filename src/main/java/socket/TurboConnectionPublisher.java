package socket;

import connection.Connection;

import java.util.ArrayList;
import java.util.List;

public class TurboConnectionPublisher {
    private final List<Connection> subscribers;

    public TurboConnectionPublisher() {
        this.subscribers = new ArrayList<>();
    }

    public void addSubscriber(Connection subscriber) {
        this.subscribers.add(subscriber);
    }

    public void removeSubscriber(Connection subscriber) {
        this.subscribers.remove(subscriber);
    }

    // Close the interpreter, notifying all subscribers
    public void close() {

        for (Connection subscriber : subscribers) {
            subscriber.close();
        }

        subscribers.clear();
    }
}
