package utils;

import burp.WebSocketFuzzer;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.core.Range;
import config.FileLocationConfiguration;
import data.FrameData;
import data.FrameType;
import logger.Logger;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static burp.api.montoya.core.ByteArray.byteArray;

public class Utilities {
    public static final int MAGIC = 0xD4D;
    // Map of WebSocket close codes to reasons
    private static final Map<Integer, String> STATUS_CODE_REASONS = new HashMap<>();
    private static final String ENVIRONMENT = "ScriptEnvironment.py";

    static {
        STATUS_CODE_REASONS.put(1000, "Normal Closure: The connection successfully completed.");
        STATUS_CODE_REASONS.put(1001, "Going Away: The endpoint is going away, such as a server shutdown or browser navigation.");
        STATUS_CODE_REASONS.put(1002, "Protocol Error: The endpoint is terminating the connection due to a protocol error.");
        STATUS_CODE_REASONS.put(1003, "Unsupported Data: The connection is closed because it received unsupported data.");
        STATUS_CODE_REASONS.put(1005, "No Status Received: Reserved. Indicates no status code was provided.");
        STATUS_CODE_REASONS.put(1006, "Abnormal Closure: Reserved. Indicates the connection was closed abnormally.");
        STATUS_CODE_REASONS.put(1007, "Invalid Payload Data: The endpoint received inconsistent or invalid data.");
        STATUS_CODE_REASONS.put(1008, "Policy Violation: The endpoint is terminating the connection due to policy violations.");
        STATUS_CODE_REASONS.put(1009, "Message Too Big: The message size exceeds the allowed limit.");
        STATUS_CODE_REASONS.put(1010, "Mandatory Extension Missing: The client requested an extension that is not available.");
        STATUS_CODE_REASONS.put(1011, "Internal Server Error: The server encountered an unexpected condition.");
        STATUS_CODE_REASONS.put(1012, "Service Restart: The server is restarted.");
        STATUS_CODE_REASONS.put(1013, "Try Again Later: The service is overloaded.");
        STATUS_CODE_REASONS.put(1014, "Bad Gateway: The server received an invalid response from the upstream server.");
        STATUS_CODE_REASONS.put(1015, "TLS Handshake Failure: Reserved. Indicates a failure during TLS handshake.");
    }

    public static String loadScriptEnvironment() {
        try {
            InputStream inputStream = Utilities.class.getClassLoader().getResourceAsStream(ENVIRONMENT);

            if (inputStream == null) {
                return "";
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (Exception ignored) {
            return "";
        }
    }

    public static ByteBuffer encodeFrame(ByteBuffer buffer) {
        if (buffer == null || buffer.remaining() == 0) {
            throw new IllegalArgumentException("Input buffer must not be null or empty.");
        }

        if (buffer.remaining() <= Long.BYTES) {
            throw new IllegalArgumentException("Input buffer is too short.");
        }

        byte code = buffer.get();
        byte[] lengthBytes = new byte[Long.BYTES];
        buffer.get(lengthBytes);
        long length = ByteBuffer.wrap(lengthBytes).getLong();

        byte[] payload = new byte[buffer.remaining()];
        if (payload.length > 0) {
            buffer.get(payload); // Read payload only if there are remaining bytes
        }

        FrameType type = FrameType.from(code);
        if (type == null) {
            return ByteBuffer.wrap(payload);
        }

        return encodeFrame(new FrameData(type, payload, length));
    }

//    public static ByteBuffer encodeFrame(ByteArray array) {
//        if (array == null || array.length() == 0) {
//            throw new IllegalArgumentException("Input array must not be null or empty.");
//        }
//
//        if (array.length() <= Long.BYTES) {
//            throw new IllegalArgumentException("Input array is too short.");
//        }
//
//        ByteArray code = array.subArray(0, 1);
//        ByteArray length = array.subArray(1, Long.BYTES + 1);
//        ByteArray payload;
//        if (array.length() > Long.BYTES + 1) {
//            payload = array.subArray(Long.BYTES + 1, array.length());
//        } else {
//            payload = ByteArray.byteArrayOfLength(0);
//        }
//
//        FrameType type = FrameType.from(code.getByte(0));
//        if (type == null) {
//            return ByteBuffer.wrap(payload.getBytes());
//        }
//
//        return encodeFrame(new FrameData(type, payload.getBytes(), ByteBuffer.wrap(length.getBytes()).getLong()));
//    }

    public static ByteBuffer encodeFrame(FrameData framedata) {

        byte[] payload = framedata.getPayload();
        long payloadLength = framedata.getLength();
        int offset = 2; // Minimum header size

        if (payloadLength < 0) {
            offset += 8 + 4 + payload.length; // 2-byte header + optional mask key
        } else if (payloadLength <= 125) {
            offset += 4 + payload.length; // 2-byte header + optional mask key
        } else if (payloadLength <= 65535) {
            offset += 2 + 4 + payload.length; // 4-byte header + optional mask key
        } else {
            offset += 8 + 4 + payload.length; // 10-byte header + optional mask key
        }

        ByteBuffer frameBuffer = ByteBuffer.allocate(offset);

        // Set FIN and opcode (FIN=1, Text Frame opcode=0x1)
        byte firstByte = (byte) (0x80 | framedata.getOpcode().value); // Set FIN flag (0x80) and the opcode
        frameBuffer.put(firstByte);

        // Payload length
        if (payloadLength < 0) {
            frameBuffer.put((byte) 0xFF);
            frameBuffer.putLong(payloadLength);
        } else if (payloadLength <= 125) {
            frameBuffer.put((byte) (0x80 | payloadLength));
        } else if (payloadLength <= 65535) {
            frameBuffer.put((byte) 0xFE);
            frameBuffer.putShort((short) payloadLength);
        } else {
            frameBuffer.put((byte) 0xFF);
            frameBuffer.putLong(payloadLength);
        }

        // Mask key
        byte[] maskKey = {0x00, 0x00, 0x00, 0x00};
        frameBuffer.put(maskKey);

        // Mask and write payload
        for (int i = 0; i < payload.length; i++) {
            byte maskedByte = payload[i];
            maskedByte ^= maskKey[i % 4];
            frameBuffer.put(maskedByte);
        }

        frameBuffer.flip(); // Prepare the buffer for reading
        return frameBuffer;
    }

    public static JMenu generateMenu(Logger logger, FileLocationConfiguration fileLocationConfiguration, Runnable unloadAction) {
        JMenuItem resetDefaultScriptsMenuItem = new JMenuItem("Reset scripts directory to default.");
        resetDefaultScriptsMenuItem.addActionListener(l -> fileLocationConfiguration.setWebSocketScriptPath());

        JMenuItem closeAllFramesMenuItem = new JMenuItem("Close all " + WebSocketFuzzer.EXTENSION_NAME + " windows.");
        closeAllFramesMenuItem.addActionListener(l -> unloadAction.run());

        JCheckBoxMenuItem loggingLevelDebug = new JCheckBoxMenuItem("Debug mode", logger.isDebugLogLevel());
        loggingLevelDebug.addActionListener(l -> logger.setDebugLogLevel(loggingLevelDebug.getState()));

        JMenu menu = new JMenu(WebSocketFuzzer.EXTENSION_NAME);
        menu.add(resetDefaultScriptsMenuItem);
        menu.add(closeAllFramesMenuItem);
        menu.add(loggingLevelDebug);

        return menu;
    }

    public static ByteArray insertPlaceholder(ByteArray initialPayload, Range range, String placeholder) {
        ByteArray prependArr = range.startIndexInclusive() == 0 ? byteArray() : initialPayload.subArray(0, range.startIndexInclusive());
        ByteArray postpendArr = range.endIndexExclusive() == initialPayload.length() ? byteArray() : initialPayload.subArray(range.endIndexExclusive(), initialPayload.length());

        return prependArr.withAppended(placeholder).withAppended(postpendArr);
    }

    public static String decodeWebSocketCloseFrame(byte[] closeFrame) {
        if (closeFrame == null || closeFrame.length < 2) {
            return "Invalid frame: No close status code provided.";
        }

        ByteBuffer buffer = ByteBuffer.wrap(closeFrame);

        // Extract the status code
        int statusCode = buffer.getShort() & 0xFFFF; // Convert to unsigned short

        // Extract the optional reason phrase
        byte[] reasonBytes = new byte[buffer.remaining()];
        buffer.get(reasonBytes);
        String reason = new String(reasonBytes);

        // Map status code to predefined reason
        String predefinedReason = STATUS_CODE_REASONS.getOrDefault(
                statusCode,
                "Unknown status code: This code is not defined in the WebSocket specification."
        );

        return String.format("Status Code: %d - %s%s", statusCode, predefinedReason, reason.isEmpty() ? "" : String.format(" Reason: %s", reason));
    }

    public static boolean startsWith(byte[] array, byte[] prefix) {
        // Check if prefix is longer than the array
        if (prefix == null || array == null || prefix.length > array.length) {
            return false;
        }

        // Compare each element of the prefix with the array
        for (int i = 0; i < prefix.length; i++) {
            if (array[i] != prefix[i]) {
                return false;
            }
        }

        return true;
    }

    public static long generateID(int high, int low) {
//        return ((long) high * 1000) + (low);
        return ((long) high << 32) + (low & 0xFFFFFFFFL);
    }

    public static byte[] magicPing(long ts) {
        ByteBuffer tsBuffer = ByteBuffer.allocate(Long.BYTES + Integer.BYTES);
        tsBuffer.putInt(MAGIC);
        tsBuffer.putLong(ts);
        return tsBuffer.array();
    }

    public static long magicPong(byte[] payload) throws IllegalArgumentException {
        if (payload == null || payload.length != Long.BYTES + Integer.BYTES) {
            throw new IllegalArgumentException("The byte array must be non-null and exactly 12 bytes.");
        }
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        int magic = buffer.getInt();
        long time = buffer.getLong();
        if (magic != MAGIC) throw new IllegalArgumentException("Not magic Pong!");
        return time;
    }

    public static byte[] magicTime(long ts, int index) {
        ByteBuffer tsBuffer = ByteBuffer.allocate(Long.BYTES + Integer.BYTES + Integer.BYTES);
        tsBuffer.putInt(MAGIC);
        tsBuffer.putInt(index);
        tsBuffer.putLong(ts);
        return tsBuffer.array();
    }

    public static int magicIndex(byte[] payload) throws IllegalArgumentException {
        if (payload == null || payload.length != Long.BYTES + Integer.BYTES + Integer.BYTES) {
            throw new IllegalArgumentException("The byte array must be non-null and exactly 16 bytes.");
        }
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        int magic = buffer.getInt();
        int index = buffer.getInt();
        long time = buffer.getLong();
        return index;
    }

    public static long magicToLong(byte[] payload) throws IllegalArgumentException {
        if (payload == null || payload.length != Long.BYTES + Integer.BYTES + Integer.BYTES) {
            throw new IllegalArgumentException("The byte array must be non-null and exactly 16 bytes.");
        }
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        int magic = buffer.getInt();
        int index = buffer.getInt();
        return buffer.getLong();
    }
}
