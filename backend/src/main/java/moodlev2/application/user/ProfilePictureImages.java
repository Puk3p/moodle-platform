package moodlev2.application.user;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

/**
 * Turns an untrusted upload into a clean avatar. Nothing the uploader sent is stored as-is:
 *
 * <ul>
 *   <li>the type is decided by the file's magic bytes, never by its name or declared MIME type, and
 *       only JPEG and PNG are accepted (no SVG, which can carry script);
 *   <li>dimensions are read from the header before any pixels are decoded, so a small file that
 *       expands to billions of pixels (a decompression bomb) is refused cheaply;
 *   <li>the pixels are decoded, turned upright, centre-cropped to a square, scaled down and written
 *       out as a brand-new JPEG. That drops all metadata (EXIF, including GPS location) and
 *       anything smuggled in or after the image data (polyglot files).
 * </ul>
 */
final class ProfilePictureImages {

    static final int MAX_UPLOAD_BYTES = 2 * 1024 * 1024;
    static final int MAX_SIDE = 8000;
    static final long MAX_PIXELS = 40_000_000L;
    static final int MIN_SIDE = 32;
    static final int OUTPUT_SIDE = 512;
    static final String OUTPUT_CONTENT_TYPE = "image/jpeg";

    private static final float JPEG_QUALITY = 0.85f;

    private ProfilePictureImages() {}

    record Processed(byte[] jpeg, int side) {}

    static Processed process(byte[] input) {
        String format = sniffFormat(input);
        if (format == null) {
            throw new IllegalArgumentException("Only JPEG and PNG images are allowed.");
        }

        BufferedImage decoded = decode(input, format);
        int orientation = "jpeg".equals(format) ? ExifOrientation.read(input) : 1;
        BufferedImage upright = applyOrientation(decoded, orientation);
        BufferedImage square = cropAndScale(upright);
        return new Processed(encodeJpeg(square), square.getWidth());
    }

    /** "jpeg", "png", or null for anything else. */
    static String sniffFormat(byte[] b) {
        if (b == null) {
            return null;
        }
        if (b.length >= 3
                && (b[0] & 0xFF) == 0xFF
                && (b[1] & 0xFF) == 0xD8
                && (b[2] & 0xFF) == 0xFF) {
            return "jpeg";
        }
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
        if (b.length >= png.length) {
            for (int i = 0; i < png.length; i++) {
                if (b[i] != png[i]) {
                    return null;
                }
            }
            return "png";
        }
        return null;
    }

    private static BufferedImage decode(byte[] input, String format) {
        try (ImageInputStream in =
                ImageIO.createImageInputStream(new ByteArrayInputStream(input))) {
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(format);
            if (in == null || !readers.hasNext()) {
                throw unreadable();
            }
            ImageReader reader = readers.next();
            try {
                // ignoreMetadata: the decoder never parses EXIF/ICC/text chunks at all.
                reader.setInput(in, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width > MAX_SIDE || height > MAX_SIDE || (long) width * height > MAX_PIXELS) {
                    throw new IllegalArgumentException(
                            "The image is too large. Use one under "
                                    + MAX_SIDE
                                    + " pixels a side.");
                }
                if (width < MIN_SIDE || height < MIN_SIDE) {
                    throw new IllegalArgumentException(
                            "The image is too small. Use one at least "
                                    + MIN_SIDE
                                    + " pixels a side.");
                }
                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw unreadable();
                }
                return image;
            } finally {
                reader.dispose();
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            // Malformed files surface as IIOException or as runtime errors deep in the codec.
            throw unreadable();
        }
    }

    private static IllegalArgumentException unreadable() {
        return new IllegalArgumentException("The image could not be read. It may be damaged.");
    }

    /**
     * Applies an EXIF orientation (1-8). Phones store the pixels sideways and record the rotation
     * in EXIF; since the metadata is discarded, the rotation has to be baked into the pixels or
     * portrait photos end up lying on their side.
     */
    static BufferedImage applyOrientation(BufferedImage src, int orientation) {
        if (orientation < 2 || orientation > 8) {
            return src;
        }
        int w = src.getWidth();
        int h = src.getHeight();
        boolean swap = orientation >= 5;

        // Maps a source pixel (x, y) to its upright position.
        AffineTransform t =
                switch (orientation) {
                    case 2 -> new AffineTransform(-1, 0, 0, 1, w, 0); // mirror
                    case 3 -> new AffineTransform(-1, 0, 0, -1, w, h); // 180
                    case 4 -> new AffineTransform(1, 0, 0, -1, 0, h); // flip
                    case 5 -> new AffineTransform(0, 1, 1, 0, 0, 0); // transpose
                    case 6 -> new AffineTransform(0, 1, -1, 0, h, 0); // 90 clockwise
                    case 7 -> new AffineTransform(0, -1, -1, 0, h, w); // transverse
                    default -> new AffineTransform(0, -1, 1, 0, 0, w); // 8: 90 anticlockwise
                };

        BufferedImage out =
                new BufferedImage(swap ? h : w, swap ? w : h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        try {
            g.drawImage(src, t, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    /** Centre square, scaled down to at most OUTPUT_SIDE; small images are never upscaled. */
    private static BufferedImage cropAndScale(BufferedImage src) {
        int side = Math.min(src.getWidth(), src.getHeight());
        int x = (src.getWidth() - side) / 2;
        int y = (src.getHeight() - side) / 2;
        BufferedImage current = draw(src, x, y, side, side);

        // Halve repeatedly before the last step: one big bicubic jump from thousands of pixels
        // down to 512 aliases badly.
        int target = Math.min(OUTPUT_SIDE, side);
        while (current.getWidth() / 2 >= target) {
            int half = current.getWidth() / 2;
            current = draw(current, 0, 0, current.getWidth(), current.getHeight(), half);
        }
        if (current.getWidth() != target) {
            current = draw(current, 0, 0, current.getWidth(), current.getHeight(), target);
        }
        return current;
    }

    private static BufferedImage draw(BufferedImage src, int x, int y, int w, int h) {
        return draw(src, x, y, w, h, w);
    }

    /** Copies a region onto an opaque canvas: transparency is flattened onto white. */
    private static BufferedImage draw(BufferedImage src, int x, int y, int w, int h, int outSide) {
        BufferedImage out = new BufferedImage(outSide, outSide, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, outSide, outSide);
            g.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(src, 0, 0, outSide, outSide, x, y, x + w, y + h, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    private static byte[] encodeJpeg(BufferedImage image) {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IllegalStateException("No JPEG encoder available");
        }
        ImageWriter writer = writers.next();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ImageOutputStream out = ImageIO.createImageOutputStream(bytes)) {
            writer.setOutput(out);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);
            // No metadata object: the output carries nothing but the pixels.
            writer.write(null, new IIOImage(image, null, null), param);
        } catch (IOException e) {
            throw new IllegalStateException("Could not encode the picture", e);
        } finally {
            writer.dispose();
        }
        return bytes.toByteArray();
    }

    /**
     * Reads the EXIF orientation tag (0x0112) straight from the JPEG bytes. Every offset is bounds
     * checked; anything unexpected means "no rotation" rather than an error, because this data is
     * attacker-controlled.
     */
    static final class ExifOrientation {

        private ExifOrientation() {}

        static int read(byte[] b) {
            try {
                int i = 2; // after SOI
                while (i + 4 <= b.length) {
                    if ((b[i] & 0xFF) != 0xFF) {
                        return 1;
                    }
                    int marker = b[i + 1] & 0xFF;
                    if (marker == 0xDA || marker == 0xD9) {
                        return 1; // image data starts: no more metadata segments
                    }
                    int length = u16(b, i + 2, false);
                    if (length < 2 || i + 2 + length > b.length) {
                        return 1;
                    }
                    if (marker == 0xE1 && length >= 8 && isExifHeader(b, i + 4)) {
                        return fromTiff(b, i + 10, i + 2 + length);
                    }
                    i += 2 + length;
                }
            } catch (RuntimeException e) {
                return 1;
            }
            return 1;
        }

        private static boolean isExifHeader(byte[] b, int at) {
            byte[] exif = "Exif\0\0".getBytes(StandardCharsets.US_ASCII);
            if (at + exif.length > b.length) {
                return false;
            }
            for (int k = 0; k < exif.length; k++) {
                if (b[at + k] != exif[k]) {
                    return false;
                }
            }
            return true;
        }

        private static int fromTiff(byte[] b, int tiff, int end) {
            if (tiff + 8 > end) {
                return 1;
            }
            boolean little;
            if (b[tiff] == 'I' && b[tiff + 1] == 'I') {
                little = true;
            } else if (b[tiff] == 'M' && b[tiff + 1] == 'M') {
                little = false;
            } else {
                return 1;
            }
            if (u16(b, tiff + 2, little) != 42) {
                return 1;
            }
            long ifd = tiff + u32(b, tiff + 4, little);
            if (ifd < tiff || ifd + 2 > end) {
                return 1;
            }
            int entries = u16(b, (int) ifd, little);
            for (int e = 0; e < entries; e++) {
                int at = (int) ifd + 2 + e * 12;
                if (at + 12 > end) {
                    return 1;
                }
                if (u16(b, at, little) == 0x0112 && u16(b, at + 2, little) == 3) {
                    int value = u16(b, at + 8, little);
                    return value >= 1 && value <= 8 ? value : 1;
                }
            }
            return 1;
        }

        private static int u16(byte[] b, int at, boolean little) {
            int a = b[at] & 0xFF;
            int c = b[at + 1] & 0xFF;
            return little ? (c << 8) | a : (a << 8) | c;
        }

        private static long u32(byte[] b, int at, boolean little) {
            long v = 0;
            for (int k = 0; k < 4; k++) {
                int shift = little ? 8 * k : 8 * (3 - k);
                v |= (long) (b[at + k] & 0xFF) << shift;
            }
            return v;
        }
    }
}
