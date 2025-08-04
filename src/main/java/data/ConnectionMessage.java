package data;

import burp.api.montoya.websocket.Direction;
import connection.Connection;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Represents a WebSocket message with comprehensive metadata and functionality.
 * This interface is used throughout the Turbo Intruder extension to handle both
 * incoming and outgoing WebSocket messages, providing access to message content,
 * timing information, analysis metrics, and associated connection details.
 *
 * <p>ConnectionMessage instances are passed to Python scripts in the
 * handle_incoming_message and handle_outgoing_message functions, allowing
 * scripts to analyze message properties and make decisions about further actions.
 *
 * <p>Key capabilities include:
 * <ul>
 *   <li>Message content access in multiple formats (String, ByteBuffer)</li>
 *   <li>Timing and performance metrics</li>
 *   <li>Content analysis (word count, key count, length)</li>
 *   <li>Direction tracking (incoming vs outgoing)</li>
 *   <li>Interest flagging for result filtering</li>
 *   <li>Connection management for bidirectional communication</li>
 * </ul>
 */
public interface ConnectionMessage {

    /**
     * Gets the message content as a string.
     * For text frames, this returns the decoded text content.
     * For binary frames, this returns a string representation of the binary data.
     *
     * @return the message content as a String
     */
    String getMessage();

    /**
     * Gets the direction of this message in the WebSocket communication.
     *
     * @return Direction.CLIENT_TO_SERVER for outgoing messages, Direction.SERVER_TO_CLIENT for incoming messages
     */
    Direction getDirection();

    /**
     * Gets the length of the message payload in bytes.
     * This represents the actual payload size, which may differ from the message length
     * for binary messages or messages with special encoding.
     *
     * @return the payload length in bytes
     */
    long getPayloadLength();

    /**
     * Gets the length of the message content.
     * For text messages, this is typically the character count.
     * For binary messages, this may represent the byte count or a processed length.
     *
     * @return the message length
     */
    int getMessageLength();

    /**
     * Gets the date and time when this message was processed.
     *
     * @return a LocalDateTime object representing when the message was handled
     */
    LocalDateTime getDateTime();

    /**
     * Gets the date and time as milliseconds since epoch.
     * Useful for timestamp calculations and comparisons.
     *
     * @return milliseconds since Unix epoch when the message was processed
     */
    long getDateTimeInMilliseconds();

    /**
     * Gets the response time or processing time for this message.
     * For incoming messages, this represents the time between sending a request
     * and receiving this response. For outgoing messages, this may represent
     * the time to send the message.
     *
     * @return the time in milliseconds
     */
    long getTime();

    /**
     * Sets the response time or processing time for this message.
     * Used internally to track timing metrics.
     *
     * @param time the time in milliseconds
     */
    void setTime(long time);

    /**
     * Gets any comment associated with this message.
     * Comments are typically set when using queueWithComment methods
     * to provide context or documentation for the message.
     *
     * @return the comment string, or null if no comment is set
     */
    String getComment();

    /**
     * Sets a comment for this message.
     * Useful for adding contextual information or test case descriptions.
     *
     * @param comment the comment to associate with this message
     */
    void setComment(String comment);

    /**
     * Gets the Connection object associated with this message.
     * This can be used in message handlers to send additional messages
     * on the same connection, enabling bidirectional communication patterns.
     *
     * @return the Connection instance that handled this message
     */
    Connection getConnection();

    /**
     * Sends this message over its associated connection.
     * This method allows re-sending or forwarding messages.
     */
    void send();

    /**
     * Gets the unique identifier for this message.
     * Used for tracking and correlation purposes across the application.
     *
     * @return a unique message ID
     */
    long getId();

    /**
     * Gets a hash value for this message.
     * Used for deduplication and comparison operations.
     *
     * @return a hash value representing the message content
     */
    long getHash();

    /**
     * Sets the hash value for this message.
     * Typically used internally for message identification and deduplication.
     *
     * @param id the hash value to set
     */
    void setHash(long id);

    /**
     * Gets the WebSocket frame type for this message.
     *
     * @return the FrameType (TEXT, BINARY, PING, PONG, CLOSE, etc.)
     */
    FrameType getCode();

//    /**
//     * Sets analysis attributes for this message.
//     * Used to store computed metrics and analysis results.
//     *
//     * @param attributes a map of WebSocketAttributeType to Integer values
//     */
//    void setAttributes(Map<WebSocketAttributeType, Integer> attributes);

//    /**
//     * Gets the number of words in the message content.
//     * Useful for content analysis and filtering based on message complexity.
//     * For JSON messages, this might count JSON keys or values.
//     *
//     * @return the word count in the message
//     */
//    int getWordCount();

//    /**
//     * Gets the number of keys in the message content.
//     * Primarily useful for structured data like JSON, where this
//     * represents the number of top-level keys or fields.
//     *
//     * @return the key count for structured messages
//     */
//    int getKeyCount();

    /**
     * Checks if this message has been flagged as interesting.
     * The interesting flag is used by analysis scripts to mark
     * messages that warrant further investigation or inclusion in results.
     *
     * @return true if the message is flagged as interesting, false otherwise
     */
    boolean isInteresting();

    /**
     * Sets the interesting flag for this message.
     * Scripts typically use this to mark messages that contain
     * unexpected responses, errors, or other noteworthy content.
     *
     * @param interesting true to flag as interesting, false otherwise
     */
    void setInteresting(boolean interesting);

    /**
     * Gets the message content as a ByteBuffer.
     * Provides direct access to the raw message bytes, useful for
     * binary message analysis or custom parsing operations.
     *
     * @return a ByteBuffer containing the raw message data
     */
    ByteBuffer getMessageByteBuffer();
}
