package pl.grizwold.spotter.detection.comparision;

import lombok.extern.slf4j.Slf4j;
import pl.grizwold.spotter.util.ImageUtil;
import pl.grizwold.spotter.util.VisualDebug;
import pl.grizwold.spotter.model.Icon;
import pl.grizwold.spotter.model.Point;

import javax.annotation.Nonnull;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class PixelByPixelImageLocator implements ImageLocator {
    // pure magenta color - RGB(255, 0, 255)
    private static final int MASK = -65281;

    private final BufferedImage base;
    private final VisualDebug debug;

    private int colorTolerance = 1;

    public PixelByPixelImageLocator(BufferedImage base) {
        this.debug = new VisualDebug();
        this.base = base;
    }

    @Nonnull
    public List<Point> locate(@Nonnull final Icon icon_) {
        long start = System.currentTimeMillis();
        final List<Point> locations = new ArrayList<>();
        final BufferedImage sample = icon_.getImage();

        Point sampleOffsetVector = getSampleOffset(sample)
                .orElseThrow(() -> new IllegalArgumentException("Sample icon cannot be all magenta!"));
        for (int x = 0; x < base.getWidth(); x++) {
            for (int y = 0; y < base.getHeight(); y++) {
                if (foundSample(base, x, y, sample, sampleOffsetVector)) {
                    locations.add(new Point(x, y));
                    x += sample.getWidth() - 1;
                }
            }
        }

        long algoTime = System.currentTimeMillis() - start;
        if (algoTime > 100) {
            log.warn("Locating icon \"{}\" took: {} ms", icon_.getFilename(), algoTime);
        } else {
            log.debug("Locating icon \"{}\" took: {} ms", icon_.getFilename(), algoTime);
        }
        saveResultVisualization(this.base, icon_, locations);
        return locations;
    }

    private boolean foundSample(BufferedImage base, int baseX, int baseY, BufferedImage sample, Point v) {
        int maxX = base.getWidth() - 1;
        int maxY = base.getHeight() - 1;
        for (int sampleX = v.x; sampleX < sample.getWidth(); sampleX++) {
            for (int sampleY = v.y; sampleY < sample.getHeight(); sampleY++) {
                int samplePixelColor = sample.getRGB(sampleX, sampleY);
                if (samplePixelColor == MASK) continue;

                int baseSampledX = baseX + sampleX;
                int baseSampledY = baseY + sampleY;

                if (baseSampledX > maxX || baseSampledY > maxY) {
                    return false;
                }

                int basePixelColor = base.getRGB(baseSampledX, baseSampledY);
                if (samplePixelColor == basePixelColor) {
                    continue;
                }

                double colorDifference = calcColorDifference(samplePixelColor, basePixelColor);
                if (colorDifference > colorTolerance * colorTolerance) {
                    return false;
                }
            }
        }
        return true;
    }

    private double calcColorDifference(int samplePixelColor, int basePixelColor) {
        int[] sampleRGB = destructARGB(samplePixelColor);
        int[] baseRGB = destructARGB(basePixelColor);
        return cartesianPowerDistance(sampleRGB, baseRGB);
    }

    private double cartesianPowerDistance(int[] a, int[] b) {
        return (a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1]) + (a[2] - b[2]) * (a[2] - b[2]);
    }

    private Optional<Point> getSampleOffset(BufferedImage sample) {
        for (int x = 0; x < sample.getWidth(); x++) {
            for (int y = 0; y < sample.getHeight(); y++) {
                int color = sample.getRGB(x, y);
                if (color != MASK) return Optional.of(new Point(x, y));
            }
        }
        return Optional.empty();
    }

    public PixelByPixelImageLocator withTolerance(int colorTolerance) {
        this.colorTolerance = colorTolerance;
        return this;
    }

    private int[] destructARGB(int argb) {
        return new int[]{
                (argb >> 24) & 0xff,
                (argb >> 16) & 0xff,
                (argb >> 8) & 0xff,
                argb & 0xff
        };
    }

    private int constructARGB(int[] argb) {
        return (argb[0] << 24) | (argb[1] << 16) | (argb[2] << 8) | argb[3];
    }

    private void saveResultVisualization(BufferedImage base, Icon icon, List<Point> locations) {
        String fileName = icon.getFilename().substring(0, icon.getFilename().length() - 4) // remove original ".png"
                // do not create subdirectories if icon is loaded from deeper directory
                .replaceAll("/", "-") // on linux
                .replaceAll("\\\\", "-") // on windows
                + (locations.isEmpty() ? "_NOT_FOUND" : "");
        this.debug.saveDebugImage(this.provideImageWithFoundAreas(base, icon, locations), fileName);
    }

    private VisualDebug.DebugImageProvider provideImageWithFoundAreas(BufferedImage base, Icon icon, List<Point> locations) {
        return () -> {
            List<Rectangle> rectangles = locations.stream()
                    .map(p -> new Rectangle(p.toAwt(), icon.getDimension()))
                    .toList();
            BufferedImage copy = ImageUtil.copy(base);

            Graphics2D g = copy.createGraphics();
            g.setColor(Color.MAGENTA);
            for (Rectangle p : rectangles) {
                g.drawRect(p.x, p.y, p.width, p.height);
            }
            g.dispose();
            return copy;
        };
    }
}
