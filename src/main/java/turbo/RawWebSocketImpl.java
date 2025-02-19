package turbo;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.core.Range;
import data.FrameData;
import data.FrameType;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static utils.Utilities.*;

public class RawWebSocketImpl {
    private static final int MAX_FRAME_SIZE = 1024 * 1024;
    /**
     * Queue of buffers that need to be sent to the client.
     */
    public final BlockingQueue<ByteBuffer> outQueue;
    /**
     * Queue of buffers that need to be processed
     */
    public final BlockingQueue<ByteBuffer> inQueue;
    public final AtomicInteger outCounter = new AtomicInteger(0);
    public final AtomicInteger inCounter = new AtomicInteger(0);
    public final RawExtensionWebSocket webSocket;

    public RawWebSocketImpl(RawExtensionWebSocket wsl) {
        this.outQueue = new LinkedBlockingQueue<>();
        this.inQueue = new LinkedBlockingQueue<>();
        this.webSocket = wsl;
    }

//    public List<FrameData> decodeFrames(ByteBuffer socketBuffer) {
//        List<FrameData> frames = new ArrayList<>();
//
//        while (socketBuffer.remaining() >= 2) {
//                // Check the frame header
//                byte firstByte = socketBuffer.get();
//                byte secondByte = socketBuffer.get();
//
//                boolean isFinalFrame = (firstByte & 0x80) != 0; // FIN flag
//                FrameType frameType = FrameType.fromByte(firstByte);
//
//                int payloadLength = secondByte & 0x7F;
//
//                // Handle payload length
//                if (payloadLength == 126) {
//                    payloadLength = socketBuffer.getShort() & 0xFFFF;
//                } else if (payloadLength == 127) {
//                    payloadLength = (int) socketBuffer.getLong();
//                }
//
//                // Make sure the frame size does not exceed the maximum allowed size
//                if (payloadLength > MAX_FRAME_SIZE) {
//                    throw new IllegalArgumentException("Frame size exceeds maximum allowed size");
//                }
//
//                if (socketBuffer.remaining() < payloadLength) break;
//
//                byte[] payload = new byte[payloadLength];
//                socketBuffer.get(payload);
//
//                // Create the frame object and add it to the list
//                FrameData framedata = new FrameData(frameType, payload);
//                frames.add(framedata);
//
//                // Handle any other flags (e.g., handling fragmented frames)
//                if (!isFinalFrame) {
//                    System.out.println("Warning: Fragmented frames are not fully implemented.");
//                }
//
//        }
//        return frames;
//    }

    public void decode(ByteBuffer socketBuffer) {
        assert (socketBuffer.hasRemaining());
        decodeAndProcessFrames(socketBuffer);
    }

    /**
     * Decodes as many complete WebSocket frames as possible from the buffer.
     * Leaves any incomplete frame data in the buffer.
     *
     * @param buffer The ByteBuffer containing accumulated data.
     * @return A list of decoded FrameData objects.
     */
    public List<FrameData> decodeFrames(ByteBuffer buffer) {
        List<FrameData> frames = new ArrayList<>();

        while (buffer.remaining() >= 2) { // Minimum frame header size
            buffer.mark(); // Mark the current position

            // Read the first two bytes
            byte firstByte = buffer.get();
            byte secondByte = buffer.get();

            boolean fin = (firstByte & 0x80) != 0;
            boolean rsv1 = (firstByte & 0x40) != 0;
            boolean rsv2 = (firstByte & 0x20) != 0;
            boolean rsv3 = (firstByte & 0x10) != 0;
            int opcodeValue = firstByte & 0x0F;

            FrameType frameType;
            try {
                frameType = FrameType.fromByte(firstByte);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unsupported opcode: " + opcodeValue);
            }

            boolean masked = (secondByte & 0x80) != 0;
            long payloadLength = secondByte & 0x7F;

            // Validate RSV bits (assuming no extensions)
            if (rsv1 || rsv2 || rsv3) {
                throw new IllegalArgumentException("RSV bits must be 0 unless an extension is negotiated.");
            }

            // Determine actual payload length
            if (payloadLength == 126) {
                if (buffer.remaining() < 2) {
                    buffer.reset();
                    break; // Incomplete extended payload length
                }
                payloadLength = buffer.getShort() & 0xFFFF;
            } else if (payloadLength == 127) {
                if (buffer.remaining() < 8) {
                    buffer.reset();
                    break; // Incomplete extended payload length
                }
                payloadLength = buffer.getLong();
                if (payloadLength < 0) {
                    throw new IllegalArgumentException("Invalid payload length: " + payloadLength);
                }
            }

            // Ensure payload length does not exceed maximum allowed size
            if (payloadLength > MAX_FRAME_SIZE) {
                throw new IllegalArgumentException("Frame size exceeds maximum allowed size: " + payloadLength);
            }

            // Read masking key if present
            byte[] maskingKey = null;
            if (masked) {
                if (buffer.remaining() < 4) {
                    buffer.reset();
                    break; // Incomplete masking key
                }
                maskingKey = new byte[4];
                buffer.get(maskingKey);
            }

            // Check if the buffer has enough bytes for the payload
            if (buffer.remaining() < payloadLength) {
                buffer.reset();
                break; // Incomplete payload
            }

            // Read payload data
            byte[] payloadData = new byte[(int) payloadLength];
            buffer.get(payloadData);

            // Unmask payload if masked
            if (masked && maskingKey != null) {
                for (int i = 0; i < payloadData.length; i++) {
                    payloadData[i] ^= maskingKey[i % 4];
                }
            }

            // Create FrameData object
            FrameData frame = new FrameData(frameType, payloadData);

            // Handle fragmented messages if necessary

            frames.add(frame);
        }

        return frames;
    }

    public void decodeAndProcessFrames(ByteBuffer socketBuffer) {
        decodeFrames(socketBuffer).forEach(this::processFrame);
    }

    public void processFrame(FrameData frame) {
        FrameType code = frame.getOpcode();
        switch (code) {
            case PING -> outQueue.add(encodeFrame(FrameData.pongFrame(frame.getPayload())));
            case PONG -> {
                ByteBuffer magic = ByteBuffer.allocate(Integer.BYTES);
                magic.putInt(MAGIC);
                if (startsWith(frame.getPayload(), magic.array())) {
                    try {
                        long l = magicToLong(frame.getPayload());
                        webSocket.updateLatency(l);
                    } catch (IllegalArgumentException ignored) {
                    }
                }

            }
            case CLOSE -> {
                webSocket.closeConnection(decodeWebSocketCloseFrame(frame.getPayload()).getBytes(StandardCharsets.UTF_8));
                throw new RawException("Raw Connection was closed by server!");
            }
            default -> webSocket.messageReceived(frame);
        }
    }

}
