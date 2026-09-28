package moodlev2.application.user;

import static moodlev2.application.user.PictureTestData.contains;
import static moodlev2.application.user.PictureTestData.jpeg;
import static moodlev2.application.user.PictureTestData.png;
import static moodlev2.application.user.PictureTestData.pngClaiming;
import static moodlev2.application.user.PictureTestData.twoColour;
import static moodlev2.application.user.PictureTestData.withExif;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ProfilePictureImagesTest {

    @BeforeAll
    static void headless() {
        System.setProperty("java.awt.headless", "true");
    }

    private static BufferedImage decode(byte[] bytes) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }

    // ── What comes out ───────────────────────────────────────────────────────

    @Test
    void anyAcceptedUploadBecomesASquareJpegOfAtMost512() throws IOException {
        ProfilePictureImages.Processed out = ProfilePictureImages.process(png(800, 600));

        assertThat(ProfilePictureImages.sniffFormat(out.jpeg())).isEqualTo("jpeg");
        BufferedImage img = decode(out.jpeg());
        assertThat(img.getWidth()).isEqualTo(512);
        assertThat(img.getHeight()).isEqualTo(512);
    }

    @Test
    void smallImagesAreCroppedButNeverUpscaled() throws IOException {
        BufferedImage img = decode(ProfilePictureImages.process(png(100, 80)).jpeg());

        assertThat(img.getWidth()).isEqualTo(80);
        assertThat(img.getHeight()).isEqualTo(80);
    }

    @Test
    void exifAndLocationMetadataAreStripped() {
        byte[] upload =
                withExif(
                        jpeg(twoColour(200, 200, Color.RED, Color.BLUE)),
                        1,
                        "GPS 44.4268N 26.1025E SECRET");
        assertThat(contains(upload, "SECRET")).isTrue();

        byte[] stored = ProfilePictureImages.process(upload).jpeg();

        assertThat(contains(stored, "Exif")).isFalse();
        assertThat(contains(stored, "SECRET")).isFalse();
    }

    @Test
    void anythingSmuggledAfterTheImageDataIsGone() {
        byte[] polyglot = concat(png(120, 120), "<script>alert(document.cookie)</script>");

        byte[] stored = ProfilePictureImages.process(polyglot).jpeg();

        assertThat(contains(stored, "<script>")).isFalse();
    }

    @Test
    void sidewaysPhonePhotoIsStoredUpright() throws IOException {
        // Landscape pixels, red on the left, tagged "rotate 90 clockwise" like a phone portrait.
        byte[] upload = withExif(jpeg(twoColour(60, 40, Color.RED, Color.BLUE)), 6, "x");

        BufferedImage img = decode(ProfilePictureImages.process(upload).jpeg());

        // Rotated clockwise, the red left half ends up on top.
        assertThat(isReddish(img.getRGB(img.getWidth() / 2, 4))).isTrue();
        assertThat(isReddish(img.getRGB(img.getWidth() / 2, img.getHeight() - 4))).isFalse();
    }

    @Test
    void everyExifOrientationProducesTheRightShape() {
        BufferedImage wide = twoColour(60, 40, Color.RED, Color.BLUE);
        for (int o = 1; o <= 8; o++) {
            BufferedImage out = ProfilePictureImages.applyOrientation(wide, o);
            boolean swapped = o >= 5;
            assertThat(out.getWidth()).as("orientation %d", o).isEqualTo(swapped ? 40 : 60);
            assertThat(out.getHeight()).as("orientation %d", o).isEqualTo(swapped ? 60 : 40);
        }
    }

    @Test
    void malformedExifFallsBackToNoRotation() {
        byte[] truncated =
                Arrays.copyOf(withExif(jpeg(twoColour(40, 40, Color.RED, Color.BLUE)), 6, "x"), 30);

        assertThat(ProfilePictureImages.ExifOrientation.read(truncated)).isEqualTo(1);
        assertThat(ProfilePictureImages.ExifOrientation.read(new byte[] {(byte) 0xFF, (byte) 0xD8}))
                .isEqualTo(1);
    }

    // ── What is refused ──────────────────────────────────────────────────────

    @Test
    void svgIsRefusedEvenThoughBrowsersRenderIt() {
        byte[] svg =
                "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>"
                        .getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> ProfilePictureImages.process(svg))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only JPEG and PNG");
    }

    @Test
    void otherFormatsAndNonImagesAreRefusedByContentNotByName() {
        byte[] gif = PictureTestData.encode(twoColour(40, 40, Color.RED, Color.BLUE), "gif");
        byte[] html = "<html><body>hi</body></html>".getBytes(StandardCharsets.UTF_8);

        for (byte[] bytes : new byte[][] {gif, html, new byte[0], new byte[] {1, 2, 3}}) {
            assertThatThrownBy(() -> ProfilePictureImages.process(bytes))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Only JPEG and PNG");
        }
    }

    @Test
    void decompressionBombIsRefusedFromItsHeaderWithoutDecoding() {
        byte[] bomb = pngClaiming(20_000, 20_000);
        assertThat(bomb.length).isLessThan(100);

        assertThatThrownBy(() -> ProfilePictureImages.process(bomb))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("too large");
    }

    @Test
    void truncatedImageIsRefusedCleanly() {
        byte[] broken = Arrays.copyOf(png(200, 200), 60);

        assertThatThrownBy(() -> ProfilePictureImages.process(broken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("could not be read");
    }

    @Test
    void tinyImagesAreRefused() {
        assertThatThrownBy(() -> ProfilePictureImages.process(png(10, 10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("too small");
    }

    private static boolean isReddish(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int b = rgb & 0xFF;
        return r > 150 && b < 100;
    }

    private static byte[] concat(byte[] a, String b) {
        byte[] tail = b.getBytes(StandardCharsets.UTF_8);
        byte[] out = Arrays.copyOf(a, a.length + tail.length);
        System.arraycopy(tail, 0, out, a.length, tail.length);
        return out;
    }
}
