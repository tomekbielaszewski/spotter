package pl.grizwold.screenautomation;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class ImageLocator {
    private final BufferedImage base;
    private Map<Integer, List<Point>> colourMap = new HashMap<>();

    private boolean debug_saveSteps;
    private boolean debug_saveStepsVerbose;
    private String debug_saveStepsDirectory;

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

    public List<Point> locate(final Icon sample) {
        long start = System.currentTimeMillis();

        final BufferedImage icon = sample.getImage();
        int firstPixel = icon.getRGB(0, 0);
        List<Point> possibleFirstPixels = colourMap.computeIfAbsent(firstPixel, k -> new ArrayList<>());

        if (debug_saveSteps && debug_saveStepsVerbose) {
            saveImageWithFoundPixels(possibleFirstPixels, start, 0);
        }

        for (int x = 1; x < icon.getWidth(); x++) {
            for (int y = 0; y < icon.getHeight(); y++) {
                int pixel = icon.getRGB(x, y);
                final int _x = x;
                final int _y = y;
                possibleFirstPixels = possibleFirstPixels.stream()
                        .filter(fpix -> colourMap.computeIfAbsent(pixel, k -> new ArrayList<>()).stream()
                                .anyMatch(p -> fpix.translate(_x, _y).equals(p)))
                        .collect(Collectors.toList());
                if (debug_saveSteps && debug_saveStepsVerbose) {
                    saveImageWithFoundPixels(possibleFirstPixels, start, y * icon.getHeight() + x);
                }
            }
        }

        log.debug("Locating icon \"{}\" took: \t\t{} ms", sample.getFilename(), (System.currentTimeMillis() - start));

        return possibleFirstPixels;
    }

    private void setupDebugFlags() {
        Optional.ofNullable(System.getProperty("pl.grizwold.screenautomation.ImageLocator.save.steps"))
                .ifPresent(p -> this.debug_saveSteps = true);
        Optional.ofNullable(System.getProperty("pl.grizwold.screenautomation.ImageLocator.save.steps.directory"))
                .ifPresent(p -> this.debug_saveStepsDirectory = p);
        Optional.ofNullable(System.getProperty("pl.grizwold.screenautomation.ImageLocator.save.steps.verbose"))
                .ifPresent(p -> this.debug_saveStepsVerbose = true);
    }

    @SneakyThrows
    private void saveImageWithFoundPixels(List<Point> possibleFirstPixels, long timestamp, int iteration) {
        BufferedImage copy = copy(this.base);
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
        pathStr += String.valueOf(timestamp) + "_" + String.valueOf(iteration) + ".png";
        File file = new File(pathStr);
        ImageIO.write(copy, "png", file);
    }

    private BufferedImage copy(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

}
