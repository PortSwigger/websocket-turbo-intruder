package utils;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;
import static utils.Utilities.encodeFrame;

class ByteBufferEncodingTest {

    @Test
    void testEncodeFrame_EmptyBuffer_ShouldThrowException() {
        ByteBuffer emptyBuffer = ByteBuffer.allocate(0);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> encodeFrame(emptyBuffer));
        assertEquals("Input buffer must not be null or empty.", exception.getMessage());
    }

    @Test
    void testEncodeFrame_TooShortBuffer_ShouldThrowException() {
        ByteBuffer shortBuffer = ByteBuffer.allocate(Long.BYTES); // Less than required bytes
        Exception exception = assertThrows(IllegalArgumentException.class, () -> encodeFrame(shortBuffer));
        assertEquals("Input buffer is too short.", exception.getMessage());
    }

    @Test
    void testEncodeFrame_EmptyBuffer_ShouldNotThrowException() {
        ByteBuffer buffer = ByteBuffer.allocate(1 + Long.BYTES + 5)
                .put((byte) 9) // FrameType Code
                .putLong(0) // Payload length
                .put(new byte[]{}); // Payload

        buffer.flip(); // Reset position before passing

        assertDoesNotThrow(() -> encodeFrame(buffer));
    }

    @Test
    void testEncodeFrame_ExploitBuffer_ShouldNotThrowException() {
        ByteBuffer buffer = ByteBuffer.allocate(1 + Long.BYTES + 5)
                .put((byte) 9) // FrameType Code
                .putLong(-1) // Payload length
                .put(new byte[]{}); // Payload

        buffer.flip(); // Reset position before passing

        assertDoesNotThrow(() -> encodeFrame(buffer));
    }

    @Test
    void testEncodeFrame_ValidBuffer_ShouldNotThrowException() {
        ByteBuffer buffer = ByteBuffer.allocate(1 + Long.BYTES + 5)
                .put((byte) 1) // FrameType Code
                .putLong(5) // Payload length
                .put(new byte[]{10, 20, 30, 40, 50}); // Payload

        buffer.flip(); // Reset position before passing

        assertDoesNotThrow(() -> encodeFrame(buffer));
    }

    @Test
    void testEncodeFrame_FrameTypeNull_ShouldReturnPayloadDirectly() {
        ByteBuffer buffer = ByteBuffer.allocate(1 + Long.BYTES + 3)
                .put((byte) -1) // Invalid FrameType Code
                .putLong(3) // Payload length
                .put(new byte[]{5, 10, 15}); // Payload

        buffer.flip();

        ByteBuffer result = encodeFrame(buffer);
        byte[] expectedPayload = new byte[]{5, 10, 15};
        assertArrayEquals(expectedPayload, result.array());
    }

    @Test
    void testEncodeFrame_ValidFrame_ShouldProcessCorrectly() {
        ByteBuffer buffer = ByteBuffer.allocate(1 + Long.BYTES + 3)
                .put((byte) 1) // Valid FrameType Code
                .putLong(3) // Payload length
                .put(new byte[]{7, 8, 9}); // Payload

        buffer.flip();

        ByteBuffer result = encodeFrame(buffer);
        assertNotNull(result);
        assertTrue(result.remaining() > 0); // Ensure it's not empty
    }

    @Test
    void testByteBufferConstruction_ShouldBeReadable() {
        ByteBuffer buffer = ByteBuffer.allocate(1 + Long.BYTES + 5)
                .put((byte) 2)
                .putLong(5)
                .put(new byte[]{10, 20, 30, 40, 50});

        buffer.flip(); // Ensure it's set to read mode

        assertEquals(1 + Long.BYTES + 5, buffer.remaining());
        assertEquals(2, buffer.get()); // Read frame type
        assertEquals(5, buffer.getLong()); // Read payload length

        byte[] payload = new byte[5];
        buffer.get(payload);
        assertArrayEquals(new byte[]{10, 20, 30, 40, 50}, payload);
    }

    @Test
    void testEmptyPayload_ShouldNotThrowException() {
        ByteBuffer buffer = ByteBuffer.allocate(1 + Long.BYTES)
                .put((byte) 1) // Valid FrameType
                .putLong(0); // Payload length = 0

        buffer.flip();

        assertDoesNotThrow(() -> encodeFrame(buffer));
    }
}

