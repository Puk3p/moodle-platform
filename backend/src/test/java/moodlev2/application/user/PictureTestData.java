package moodlev2.application.user;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;

/** Builds real (and deliberately hostile) image bytes for the picture tests. */
final class PictureTestData {

    private PictureTestData() {}

    static BufferedImage twoColour(int w, int h, Color left, Color right) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(left);
        g.fillRect(0, 0, w / 2, h);
        g.setColor(right);
        g.fillRect(w / 2, 0, w - w / 2, h);
        g.dispose();
        return img;
    }

    static byte[] png(int w, int h) {
        return encode(twoColour(w, h, Color.RED, Color.BLUE), "png");
    }

    static byte[] jpeg(BufferedImage img) {
        return encode(img, "jpg");
    }

    static byte[] encode(BufferedImage img, String format) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, format, out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Inserts an EXIF APP1 segment right after SOI with the given orientation, plus a trailing
     * marker string standing in for GPS coordinates or other private metadata.
     */
    static byte[] withExif(byte[] jpeg, int orientation, String secret) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.US_ASCII);
        ByteBuffer tiff = ByteBuffer.allocate(8 + 2 + 12 + 4 + secretBytes.length);
        tiff.put((byte) 'M').put((byte) 'M').putShort((short) 42).putInt(8); // big-endian header
        tiff.putShort((short) 1); // one IFD entry
        tiff.putShort((short) 0x0112).putShort((short) 3).putInt(1); // Orientation, SHORT, count 1
        tiff.putShort((short) orientation).putShort((short) 0);
        tiff.putInt(0); // no next IFD
        tiff.put(secretBytes);

        byte[] exifHeader = "Exif\0\0".getBytes(StandardCharsets.US_ASCII);
        int segmentLength = 2 + exifHeader.length + tiff.capacity();

        ByteBuffer out = ByteBuffer.allocate(jpeg.length + 2 + segmentLength);
        out.put(jpeg, 0, 2); // SOI
        out.put((byte) 0xFF).put((byte) 0xE1).putShort((short) segmentLength);
        out.put(exifHeader).put(tiff.array());
        out.put(jpeg, 2, jpeg.length - 2);
        return out.array();
    }

    /** A structurally valid PNG header that claims enormous dimensions (a decompression bomb). */
    static byte[] pngClaiming(int width, int height) {
        ByteBuffer ihdr = ByteBuffer.allocate(13);
        ihdr.putInt(width)
                .putInt(height)
                .put((byte) 8)
                .put((byte) 2)
                .put((byte) 0)
                .put((byte) 0)
                .put((byte) 0);
        ByteBuffer out = ByteBuffer.allocate(8 + chunkSize(13) + chunkSize(0));
        out.put(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});
        putChunk(out, "IHDR", ihdr.array());
        putChunk(out, "IEND", new byte[0]);
        return out.array();
    }

    private static int chunkSize(int dataLength) {
        return 4 + 4 + dataLength + 4;
    }

    private static void putChunk(ByteBuffer out, String type, byte[] data) {
        byte[] typeBytes = type.getBytes(StandardCharsets.US_ASCII);
        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);
        out.putInt(data.length).put(typeBytes).put(data).putInt((int) crc.getValue());
    }

    static boolean contains(byte[] haystack, String needle) {
        return new String(haystack, StandardCharsets.ISO_8859_1).contains(needle);
    }
}
