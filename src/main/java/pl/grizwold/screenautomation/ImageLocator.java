package pl.grizwold.screenautomation;

import lombok.extern.slf4j.Slf4j;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class ImageLocator {
    private final BufferedImage base;
    private Map<Integer, List<Point>> colourMap = new HashMap<>();

    public ImageLocator(BufferedImage base) {
        this.base = base;
        buildColorMap();
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

        for (int x = 1; x < icon.getWidth(); x++) {
            for (int y = 0; y < icon.getHeight(); y++) {
                int pixel = icon.getRGB(x, y);
                final int _x = x;
                final int _y = y;
                possibleFirstPixels = possibleFirstPixels.stream()
                        .filter(fpix -> colourMap.computeIfAbsent(pixel, k -> new ArrayList<>()).stream()
                                .anyMatch(p -> fpix.translate(_x, _y).equals(p)))
                        .collect(Collectors.toList());
            }
        }

        log.debug("Locating icon \"{}\" took: \t\t{} ms", sample.getFilename(), (System.currentTimeMillis() - start));

        return possibleFirstPixels;
    }
}
