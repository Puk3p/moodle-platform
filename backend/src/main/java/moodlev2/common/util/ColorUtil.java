package moodlev2.common.util;

import java.util.Random;

public final class ColorUtil {

    private static final Random RANDOM = new Random();

    private ColorUtil() {}

    public static String randomPastelColor() {
        int r = (RANDOM.nextInt(128) + 127);
        int g = (RANDOM.nextInt(128) + 127);
        int b = (RANDOM.nextInt(128) + 127);

        return String.format("#%02x%02x%02x", r, g, b);
    }
}
