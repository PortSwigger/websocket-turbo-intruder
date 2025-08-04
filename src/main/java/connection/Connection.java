package connection;

import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.websocket.extension.ExtensionWebSocketCreationStatus;
import data.ConnectionMessage;
import data.FrameType;

/**
 * WebSocket connection interface for the Turbo Intruder extension.
 * Provides methods for queuing messages, managing connection state, and tracking message statistics.
 * <p>
 * This interface supports various message queuing strategies with different frame types,
 * payload formats, and replacement mechanisms for fuzzing operations.
 */
public interface Connection {

    /**
     * Queues a text message for sending over the WebSocket connection.
     *
     * @param payload the text payload to send
     * @return the message ID for tracking purposes
     */
    int queue(String payload);

    /**
     * Queues a binary message for sending over the WebSocket connection.
     *
     * @param payload the binary payload to send
     * @return the message ID for tracking purposes
     */
    int queue(byte[] payload);

    /**
     * Queues a message with a specific frame type.
     *
     * @param code    the WebSocket frame type (TEXT, BINARY, PING, PONG, etc.)
     * @param payload the binary payload to send
     * @return the message ID for tracking purposes
     */
    int queue(FrameType code, byte[] payload);

    /**
     * Queues a message with a specific frame type and length limit.
     *
     * @param code    the WebSocket frame type
     * @param payload the binary payload to send
     * @param length  the maximum length to send from the payload
     * @return the message ID for tracking purposes
     */
    int queue(FrameType code, byte[] payload, int length);

    /**
     * Queues a message with a specific frame type and string-based length specification.
     *
     * @param code    the WebSocket frame type
     * @param payload the binary payload to send
     * @param length  the length specification as a string (for dynamic length calculation)
     * @return the message ID for tracking purposes
     */
    int queue(FrameType code, byte[] payload, String length);

    /**
     * Queues a message with payload replacement for fuzzing operations.
     * This method allows substituting parts of the original payload with the replacement value.
     *
     * @param payload     the original text payload (may contain placeholders)
     * @param replacement the replacement value to substitute into the payload
     * @return the message ID for tracking purposes
     */
    int queue(String payload, String replacement);

    /**
     * Queues a message with an associated comment for documentation purposes.
     *
     * @param payload the text payload to send
     * @param comment a descriptive comment for this message (for logging/reporting)
     */
    void queueWithComment(String payload, String comment);

    /**
     * Queues a message with payload replacement and an associated comment.
     * Combines fuzzing capabilities with documentation for tracking test scenarios.
     *
     * @param payload     the original text payload (may contain placeholders)
     * @param replacement the replacement value to substitute into the payload
     * @param comment     a descriptive comment for this message (for logging/reporting)
     */
    void queueWithComment(String payload, String replacement, String comment);

    /**
     * Sends a pre-constructed connection message immediately.
     *
     * @param message the ConnectionMessage object containing all message details
     */
    void sendMessage(ConnectionMessage message);

    /**
     * Closes the WebSocket connection gracefully.
     * This will terminate the connection and clean up associated resources.
     */
    void close();

    /**
     * Retrieves the original HTTP upgrade request used to establish this WebSocket connection.
     *
     * @return the HttpRequest object that initiated the WebSocket handshake
     */
    HttpRequest upgradeRequest();

    /**
     * Gets the current status of the WebSocket connection creation.
     *
     * @return the ExtensionWebSocketCreationStatus indicating connection state
     */
    ExtensionWebSocketCreationStatus status();

    /**
     * Decrements the incoming message counter and returns the new value.
     * Used for tracking message flow statistics.
     *
     * @return the decremented incoming message count
     */
    int decIn();

    /**
     * Decrements the outgoing message counter and returns the new value.
     * Used for tracking message flow statistics.
     *
     * @return the decremented outgoing message count
     */
    int decOut();

    /**
     * Increments the incoming message counter and returns the new value.
     * Used for tracking message flow statistics.
     *
     * @return the incremented incoming message count
     */
    int incIn();

    /**
     * Increments the outgoing message counter and returns the new value.
     * Used for tracking message flow statistics.
     *
     * @return the incremented outgoing message count
     */
    int incOut();

    /**
     * Gets the current count of incoming messages.
     *
     * @return the total number of messages received on this connection
     */
    int in();

    /**
     * Gets the current count of outgoing messages.
     *
     * @return the total number of messages sent on this connection
     */
    int out();
}
