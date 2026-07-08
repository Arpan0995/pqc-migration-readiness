package org.pqcreadiness.agility.crypto;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal length-prefixed framing shared by the hybrid primitives: each block is a
 * 4-byte big-endian length followed by its bytes. Deliberately simple — composite
 * PQC encodings are still being standardised, so this avoids inventing a clever format.
 */
final class Wire {

    private Wire() {
    }

    static byte[] concat(List<byte[]> blocks) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] block : blocks) {
            out.writeBytes(ByteBuffer.allocate(4).putInt(block.length).array());
            out.writeBytes(block);
        }
        return out.toByteArray();
    }

    static List<byte[]> split(byte[] wire) {
        List<byte[]> blocks = new ArrayList<>();
        ByteBuffer buf = ByteBuffer.wrap(wire);
        while (buf.remaining() >= 4) {
            int len = buf.getInt();
            if (len < 0 || len > buf.remaining()) {
                break;
            }
            byte[] block = new byte[len];
            buf.get(block);
            blocks.add(block);
        }
        return blocks;
    }
}
