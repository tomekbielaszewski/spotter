package pl.grizwold.screenautomation;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class ImageLocator {
    private static final int MASK = -65281; //pure magenta color

    private final BufferedImage base;

    private Map<Integer, List<Point>> colourMap = new HashMap<>();
    private boolean debug_saveSteps;
    private String debug_saveStepsDirectory;
    private int amountOfLastFoundPixels = -1;

    public ImageLocator(BufferedImage base) {
        this.base = base;
        buildColorMap();
        setupDebugFlags();
    }

    private void buildColorMap() {
        long start = System.currentTimeMillis();

        for (int x = 0; x < base.getWidth(); x++) {
            for (int y = 0; y < base.getHeight(); y++) {
                int pixel = base.getRGB(x, y);
                List<Point> points = colourMap.computeIfAbsent(pixel, k -> new ArrayList<>());
                points.add(new Point(x, y));
            }
        }

        log.debug("Building screen color map took: {} ms", (System.currentTimeMillis() - start));
    }

    @Nonnull
    public List<Point> locate(@Nonnull final Icon icon_) {
        long start = System.currentTimeMillis();
        final BufferedImage sample = icon_.getImage();
        List<Point> possibleFirstPixels = new ArrayList<>();
        boolean startedSampling = false;
        Point[] firstSampledLocation = new Point[1];

        for (int x = 0; x < sample.getWidth(); x++) {
            for (int y = 0; y < sample.getHeight(); y++) {
                int pixel = sample.getRGB(x, y);
                final Point location = new Point(x, y);

                if (pixel == MASK) continue;
                if (!startedSampling) {
                    possibleFirstPixels = colourMap.computeIfAbsent(pixel, k -> new ArrayList<>());
                    startedSampling = true;
                    firstSampledLocation[0] = location;
                } else {
                    possibleFirstPixels = possibleFirstPixels.stream()
                            .filter(fpix -> colourMap.computeIfAbsent(pixel, k -> new ArrayList<>()).stream()
                                    .anyMatch(p -> fpix.translate(location.minus(firstSampledLocation[0]))
                                            .equals(p)))
                            .collect(Collectors.toList());
                }

                debugImage(this.base, icon_, possibleFirstPixels, x + "_" + y, start);

                if (possibleFirstPixels.isEmpty()) {
                    log.debug("Icon {} not found", icon_.getFilename());
                    return possibleFirstPixels;
                }
            }
        }

        log.debug("Locating icon \"{}\" took: {} ms", icon_.getFilename(), (System.currentTimeMillis() - start));

        possibleFirstPixels = possibleFirstPixels.stream()
                .map(p -> p.minus(firstSampledLocation[0]))
                .collect(Collectors.toList());

        return possibleFirstPixels;
    }

    public void activateDebugging() {
        this.activateDebugging(null);
    }

    public void activateDebugging(String directory) {
        this.debug_saveSteps = true;
        this.debug_saveStepsDirectory = Optional.ofNullable(directory).orElse("image_locator_debug");
    }

    public void deactivateDebugging() {
        this.debug_saveSteps = false;
        this.debug_saveStepsDirectory = null;
    }

    private void debugImage(BufferedImage baseImage, Icon sample, List<Point> pixelsToHighlight, String iteration, long timestamp) {
        if (debug_saveSteps && pixelsToHighlight.size() != amountOfLastFoundPixels) {
            saveImageWithFoundPixels(pixelsToHighlight, timestamp, iteration, baseImage, sample);
            amountOfLastFoundPixels = pixelsToHighlight.size();
        }
    }

    private void setupDebugFlags() {
        Optional.ofNullable(System.getProperty("pl.grizwold.screenautomation.ImageLocator.save.steps"))
                .filter("true"::equals)
                .ifPresent(p -> this.debug_saveSteps = true);
        Optional.ofNullable(System.getProperty("pl.grizwold.screenautomation.ImageLocator.save.steps.directory"))
                .filter(p -> p.length() > 0)
                .ifPresent(p -> this.debug_saveStepsDirectory = p);
    }

    @SneakyThrows
    private void saveImageWithFoundPixels(@Nonnull List<Point> possibleFirstPixels, long timestamp, String iteration, BufferedImage baseImage, Icon sample) {
        BufferedImage copy = ImageUtil.copy(baseImage);
        Graphics2D g = copy.createGraphics();
        g.setColor(Color.MAGENTA);
        for (Point p : possibleFirstPixels) {
            g.drawLine(p.x, p.y, p.x, p.y);
        }
        g.dispose();

        String pathStr = "";
        if (this.debug_saveStepsDirectory != null) {
            pathStr = debug_saveStepsDirectory;
            pathStr += pathStr.endsWith("/") ? "" : "/";
        }
        String iconName = sample.getFilename().substring(0, sample.getFilename().length() - 4);
        pathStr += timestamp + "/" + iconName + "_" + iteration + ".png";
        Files.createDirectories(Paths.get(pathStr));
        File file = new File(pathStr);
        ImageIO.write(copy, "png", file);
    }
}
