package pl.grizwold.spotter.processing;

import lombok.extern.slf4j.Slf4j;
import pl.grizwold.spotter.model.Icon;
import pl.grizwold.spotter.model.Point;

import javax.annotation.Nonnull;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class PixelByPixelImageLocator {
    private static final int MASK = -65281; //pure magenta color

    private final BufferedImage base;

    private boolean debug_saveSteps;
    private String debug_saveStepsDirectory = "image_locator_debug";
    private int colorTolerance = 1;

    public PixelByPixelImageLocator(BufferedImage base) {
        this.base = base;
        setupDebugFlags();
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
        saveResultVisualization(this.base, icon_, locations, start);
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

    public PixelByPixelImageLocator activateDebugging() {
        return this.activateDebugging(null);
    }

    public PixelByPixelImageLocator activateDebugging(String directory) {
        this.debug_saveSteps = true;
        this.debug_saveStepsDirectory = Optional.ofNullable(directory).orElse("image_locator_debug");
        return this;
    }

    public PixelByPixelImageLocator deactivateDebugging() {
        this.debug_saveSteps = false;
        this.debug_saveStepsDirectory = null;
        return this;
    }

    private void saveResultVisualization(BufferedImage base, Icon icon, List<Point> possibleFirstPixels, long timestamp) {
        if (debug_saveSteps) {
            if (!possibleFirstPixels.isEmpty()) {
                List<Rectangle> rectangles = possibleFirstPixels.stream()
                        .map(p -> new Rectangle(p.toAwt(), icon.getDimension()))
                        .collect(Collectors.toList());
                saveImageWithFoundAreas(rectangles, base, icon.getFilename(), timestamp);
            } else {
                saveImageWithFoundAreas(Collections.emptyList(), base, icon.getFilename() + "_NOT_FOUND", timestamp);
            }
        }
    }

    private void setupDebugFlags() {
        Optional.ofNullable(System.getProperty("pl.grizwold.spotter.ImageLocator.save.steps.enabled"))
                .filter("true"::equals)
                .ifPresent(_ -> this.debug_saveSteps = true);
        Optional.ofNullable(System.getProperty("pl.grizwold.spotter.ImageLocator.save.steps.directory"))
                .filter(p -> !p.isEmpty())
                .ifPresent(p -> this.debug_saveStepsDirectory = p);
    }

    private void saveImageWithFoundAreas(List<Rectangle> rectangles, BufferedImage base, String iconFileName, long timestamp) {
        BufferedImage copy = ImageUtil.copy(base);

        Graphics2D g = copy.createGraphics();
        g.setColor(Color.MAGENTA);
        for (Rectangle p : rectangles) {
            g.drawRect(p.x, p.y, p.width, p.height);
        }
        g.dispose();

        String pathStr = debug_saveStepsDirectory;
        pathStr += pathStr.endsWith("/") ? "" : "/";
        String iconName = iconFileName.substring(0, iconFileName.length() - 4);
        pathStr += timestamp + "/" + iconName + "_final.png";
        ImageUtil.save(copy, pathStr);
    }
}
