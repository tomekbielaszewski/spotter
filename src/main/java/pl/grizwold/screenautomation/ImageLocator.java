package pl.grizwold.screenautomation;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class ImageLocator {
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

    public List<Point> locate(final Icon sample) {
        long start = System.currentTimeMillis();

        final BufferedImage icon = sample.getImage();
        int firstPixel = icon.getRGB(0, 0);
        List<Point> possibleFirstPixels = colourMap.computeIfAbsent(firstPixel, k -> new ArrayList<>());

        debugImage(this.base, sample, possibleFirstPixels, "0_0", start);

        if (possibleFirstPixels.isEmpty()) {
            log.debug("Icon {} not found", sample.getFilename());
            return possibleFirstPixels;
        }

        for (int x = 0; x < icon.getWidth(); x++) {
            for (int y = 0; y < icon.getHeight(); y++) {
                if (x == 0 && y == 0) continue;

                int pixel = icon.getRGB(x, y);
                final int _x = x;
                final int _y = y;

                possibleFirstPixels = possibleFirstPixels.stream()
                        .filter(fpix -> colourMap.computeIfAbsent(pixel, k -> new ArrayList<>()).stream()
                                .anyMatch(p -> fpix.translate(_x, _y).equals(p)))
                        .collect(Collectors.toList());

                debugImage(this.base, sample, possibleFirstPixels, x + "_" + y, start);

                if (possibleFirstPixels.isEmpty()) {
                    log.debug("Icon {} not found", sample.getFilename());
                    return possibleFirstPixels;
                }
            }
        }

        log.debug("Locating icon \"{}\" took: \t\t{} ms", sample.getFilename(), (System.currentTimeMillis() - start));

        return possibleFirstPixels;
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
    private void saveImageWithFoundPixels(List<Point> possibleFirstPixels, long timestamp, String iteration, BufferedImage baseImage, Icon sample) {
        BufferedImage copy = copy(baseImage);
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

    private BufferedImage copy(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

}
