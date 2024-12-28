package pl.grizwold.spotter.processing;

import lombok.extern.slf4j.Slf4j;

import java.awt.image.BufferedImage;
import java.time.Instant;

import static java.time.Duration.between;
import static java.time.Instant.now;

@Slf4j
public class ImageComparator {
    private static final int MASK = -65281; //pure magenta color
    private final int tolerance;

    public ImageComparator(int tolerance) {
        this.tolerance = tolerance;
    }

    public ImageComparator() {
        this(0);
    }

    public boolean areTheSame(BufferedImage img1, BufferedImage img2) {
        Instant start = now();

        if (img1.getWidth() != img2.getWidth() &&
                img1.getHeight() != img2.getHeight())
            throw new IllegalArgumentException("Image dimensions should be the same!");

        for (int x = 0; x < img1.getWidth(); x++) {
            for (int y = 0; y < img1.getHeight(); y++) {
                int rgb1 = img1.getRGB(x, y);
                int rgb2 = img2.getRGB(x, y);
                if (rgb1 == MASK || rgb2 == MASK) continue;
                double diff = colorDifference(rgb1, rgb2);

                if (diff > tolerance) return false;
            }
        }

        log.debug("Images compared in {}ms", between(start, now()).toMinutes());
        return true;
    }

    private double colorDifference(int samplePixelColor, int basePixelColor) {
        int[] sampleRGB = destructARGB(samplePixelColor);
        int[] baseRGB = destructARGB(basePixelColor);
        return cartesianPowerDistance(sampleRGB, baseRGB);
    }

    private double cartesianPowerDistance(int[] a, int[] b) {
        return (a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1]) + (a[2] - b[2]) * (a[2] - b[2]);
    }

    private int[] destructARGB(int argb) {
        return new int[]{
                (argb >> 24) & 0xff,
                (argb >> 16) & 0xff,
                (argb >> 8) & 0xff,
                argb & 0xff
        };
    }
}
