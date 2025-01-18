package pl.grizwold.spotter.detection.comparision;

import lombok.extern.slf4j.Slf4j;
import pl.grizwold.spotter.ImageUtil;
import pl.grizwold.spotter.model.Icon;
import pl.grizwold.spotter.model.Point;

import javax.annotation.Nonnull;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class ColorMapImageLocator implements ImageLocator {
    private static final int MASK = -65281; //pure magenta color

    private final BufferedImage base;
    private final Map<Integer, List<Point>> colorMap = new HashMap<>();
    private final ImageUtil.VisualDebug debug;

    private int amountOfLastFoundPixels = -1;

    public ColorMapImageLocator(BufferedImage base) {
        this.base = base;
        this.debug = new ImageUtil.VisualDebug();
        buildColorMap();
    }

    private void buildColorMap() {
        long start = System.currentTimeMillis();

        for (int x = 0; x < base.getWidth(); x++) {
            for (int y = 0; y < base.getHeight(); y++) {
                int pixel = base.getRGB(x, y);
                List<Point> points = colorMap.computeIfAbsent(pixel, _ -> new ArrayList<>());
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
                    possibleFirstPixels = colorMap.computeIfAbsent(pixel, _ -> new ArrayList<>());
                    startedSampling = true;
                    firstSampledLocation[0] = location;
                } else {
                    possibleFirstPixels = possibleFirstPixels.stream()
                            .filter(fpix -> colorMap.computeIfAbsent(pixel, _ -> new ArrayList<>()).stream()
                                    .anyMatch(p -> fpix.translate(location.minus(firstSampledLocation[0]))
                                            .equals(p)))
                            .collect(Collectors.toList());
                }

                saveStepVisualization(this.base, icon_, possibleFirstPixels, x + "x" + y);

                if (possibleFirstPixels.isEmpty()) {
                    long time = System.currentTimeMillis() - start;
                    log.debug("Icon {} not found in {}ms", icon_.getFilename(), time);
                    return possibleFirstPixels;
                }
            }
        }

        possibleFirstPixels = possibleFirstPixels.stream()
                .map(p -> p.minus(firstSampledLocation[0]))
                .collect(Collectors.toList());

        saveResultVisualization(this.base, icon_, possibleFirstPixels);

        long algoTime = System.currentTimeMillis() - start;
        log.debug("Locating icon \"{}\" took: {} ms", icon_.getFilename(), algoTime);
        if (algoTime > 100) {
            log.warn("Locating icon \"{}\" took: {} ms which is more than 100ms. Consider optimizing the icon image.", icon_.getFilename(), algoTime);
        }
        return possibleFirstPixels;
    }

    private void saveStepVisualization(BufferedImage baseImage, Icon icon, List<Point> pixelsToHighlight, String iteration) {
        if (pixelsToHighlight.size() != amountOfLastFoundPixels) {
            String fileName = icon.getFilename().substring(0, icon.getFilename().length() - 4) // remove original ".png"
                    // do not create subdirectories if icon is loaded from deeper directory
                    .replaceAll("/", "-") // on linux
                    .replaceAll("\\\\", "-") // on windows
                    + "-" + iteration;
            this.debug.saveDebugImage(() -> provideImageWithFoundPixels(pixelsToHighlight, baseImage), fileName);
            amountOfLastFoundPixels = pixelsToHighlight.size();
        }
    }

    private void saveResultVisualization(BufferedImage base, Icon icon, List<Point> possibleFirstPixels) {
        if (!possibleFirstPixels.isEmpty()) {
            String fileName = icon.getFilename().substring(0, icon.getFilename().length() - 4) // remove original ".png"
                    // do not create subdirectories if icon is loaded from deeper directory
                    .replaceAll("/", "-") // on linux
                    .replaceAll("\\\\", "-") // on windows
                    + "-final";
            this.debug.saveDebugImage(() -> provideImageWithFoundAreas(possibleFirstPixels, base, icon), fileName);
        }
    }

    private BufferedImage provideImageWithFoundAreas(List<Point> possibleFirstPixels, BufferedImage base, Icon icon) {
        List<Rectangle> rectangles = possibleFirstPixels.stream()
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
    }

    private BufferedImage provideImageWithFoundPixels(@Nonnull List<Point> possibleFirstPixels, BufferedImage baseImage) {
        BufferedImage copy = ImageUtil.copy(baseImage);

        Graphics2D g = copy.createGraphics();
        g.setColor(Color.MAGENTA);
        for (Point p : possibleFirstPixels) {
            g.drawLine(p.x, p.y, p.x, p.y);
        }
        g.dispose();

        return copy;
    }
}
