package pl.grizwold.spotter.detection.diff;

import lombok.extern.slf4j.Slf4j;
import pl.grizwold.spotter.util.ImageUtil;
import pl.grizwold.spotter.util.VisualDebug;
import pl.grizwold.spotter.model.Point;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Credits to: https://github.com/romankh3/image-comparison which is licensed under Apache License 2.0
 */
@Slf4j
public class ImageDiff {
    private static final int DIFFERENCE_MARKER = 1;

    private final BufferedImage original;
    private final VisualDebug debug;
    private double pixelToleranceLevel = 0.0;
    private double differenceConstant;
    private int minimalRectangleSize = 10;

    public ImageDiff(BufferedImage original) {
        this.original = original;
        this.differenceConstant = calculateDifferenceConstant();
        this.debug = new VisualDebug();
    }

    public List<Rectangle> getDiffBounds(BufferedImage sample) {
        return this.getDiffBounds(sample, new Point(0, 0));
    }

    public List<Rectangle> getDiffBounds(BufferedImage sample, Point locationOnOriginal) {
        long start = System.currentTimeMillis();

        Rectangle bounds = new Rectangle(locationOnOriginal.x, locationOnOriginal.y, sample.getWidth(), sample.getHeight());
        BufferedImage originalSubImage = getSubImage(original, bounds);
        validateSameSize(originalSubImage, sample);
        int[][] diffMatrix = getDifferenceMatrix(original, sample);
        int regionCount = groupRegions(diffMatrix);
        debug_saveRegionVisualization(diffMatrix, sample);

        List<Rectangle> rectangles = getRegionBoundaries(regionCount, diffMatrix);
        rectangles = filterOutSmallOnes(rectangles);
        debug_saveBoundariesVisualization(rectangles, sample);

        log.debug("Detecting difference between images of size {}x{} took: {}ms", sample.getWidth(), sample.getHeight(), (System.currentTimeMillis() - start));
        log.debug("Difference detection found {} rectangles", rectangles.size());
        return rectangles;
    }

    public ImageDiff setPixelToleranceLevel(double pixelToleranceLevel) {
        if (0.0 <= pixelToleranceLevel && pixelToleranceLevel < 1) {
            this.pixelToleranceLevel = pixelToleranceLevel;
            differenceConstant = calculateDifferenceConstant();
        }
        return this;
    }

    private BufferedImage getSubImage(BufferedImage base, Rectangle bounds) {
        return base.getSubimage(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    private void validateSameSize(BufferedImage originalSubImage, BufferedImage sample) {
        if (originalSubImage.getWidth() != sample.getWidth() || originalSubImage.getHeight() != sample.getHeight()) {
            throw new IllegalArgumentException("Compared images are not the same size!");
        }
    }

    private int[][] getDifferenceMatrix(BufferedImage original, BufferedImage sample) {
        int[][] matrix = new int[original.getHeight()][original.getWidth()];
        for (int y = 0; y < original.getHeight(); y++) {
            for (int x = 0; x < original.getWidth(); x++) {
                matrix[y][x] = areColorsDifferent(original.getRGB(x, y), sample.getRGB(x, y)) ? DIFFERENCE_MARKER : 0;
            }
        }
        return matrix;
    }

    private int groupRegions(int[][] matrix) {
        int regionId = DIFFERENCE_MARKER + 1; // 0 is same pixels on both, 1 is difference detected, regions start on 2
        for (int y = 0; y < matrix.length; y++) {
            for (int x = 0; x < matrix[y].length; x++) {
                if (matrix[y][x] == DIFFERENCE_MARKER) {
                    new ScanlineStackBasedFloodFill()
                            .fill(x, y, DIFFERENCE_MARKER, regionId, matrix);
                    regionId++;
                }
            }
        }
        int regionCount = regionId - 2;
        return regionCount;
    }

    private List<Rectangle> getRegionBoundaries(int regionCount, int[][] diffMatrix) {
        List<Rectangle> rectangles = new ArrayList<>();

        for (int regionId = 2; regionId < regionCount + 2; regionId++) {
            Rectangle rectangle = getRegionBoundary(diffMatrix, regionId);
            if (!rectangle.isEmpty()) {
                rectangles.add(rectangle);
            }
        }

        return rectangles;
    }

    private List<Rectangle> filterOutSmallOnes(List<Rectangle> rectangles) {
        return rectangles.stream()
                .filter(this::isMinimalSizeMet)
                .collect(Collectors.toList());
    }

    private Rectangle getRegionBoundary(int[][] matrix, int region) {
        int xMax = Integer.MIN_VALUE;
        int yMax = Integer.MIN_VALUE;
        int xMin = Integer.MAX_VALUE;
        int yMin = Integer.MAX_VALUE;
        for (int y = 0; y < matrix.length; y++) {
            for (int x = 0; x < matrix[0].length; x++) {
                if (matrix[y][x] == region) {
                    xMax = Math.max(xMax, x);
                    yMax = Math.max(yMax, y);
                    xMin = Math.min(xMin, x);
                    yMin = Math.min(yMin, y);
                }
            }
        }
        return new Rectangle(xMin, yMin, (xMax - xMin), (yMax - yMin));
    }

    private boolean areColorsDifferent(int rgb1, int rgb2) {
        if (rgb1 == rgb2) {
            return false;
        } else if (pixelToleranceLevel == 0.0) {
            return true;
        }

        int red1 = (rgb1 >> 16) & 0xff;
        int green1 = (rgb1 >> 8) & 0xff;
        int blue1 = (rgb1) & 0xff;
        int red2 = (rgb2 >> 16) & 0xff;
        int green2 = (rgb2 >> 8) & 0xff;
        int blue2 = (rgb2) & 0xff;

        return (Math.pow(red2 - red1, 2) + Math.pow(green2 - green1, 2) + Math.pow(blue2 - blue1, 2))
                > differenceConstant;
    }

    private double calculateDifferenceConstant() {
        return Math.pow(pixelToleranceLevel * Math.sqrt(Math.pow(255, 2) * 3), 2);
    }

    private boolean isMinimalSizeMet(Rectangle rectangle) {
        return rectangle.width * rectangle.height > minimalRectangleSize;
    }

    private void debug_saveRegionVisualization(int[][] diffMatrix, BufferedImage sample) {
        String fileName = "image-diff-regions.png";
        this.debug.saveDebugImage(() -> this.createImageWithRegions(diffMatrix, sample), fileName);
    }

    private void debug_saveBoundariesVisualization(List<Rectangle> rectangles, BufferedImage sample) {
        String fileName = "image-diff-boundaries.png";
        this.debug.saveDebugImage(() -> this.createImageWithBoundaries(sample, rectangles), fileName);
    }

    private BufferedImage createImageWithRegions(int[][] diffMatrix, BufferedImage sample) {
        BufferedImage copy = ImageUtil.copy(sample);
        List<Color> colors = Arrays.asList(
                Color.MAGENTA,
                Color.BLUE,
                Color.CYAN,
                Color.GREEN,
                Color.ORANGE,
                Color.RED,
                Color.WHITE,
                Color.YELLOW,
                Color.LIGHT_GRAY
        );

        Graphics2D g = copy.createGraphics();
        for (int x = 0; x < copy.getWidth(); x++) {
            for (int y = 0; y < copy.getHeight(); y++) {
                int region = diffMatrix[y][x] - 2;
                if (region >= 0) {
                    int colorIndex = region % (colors.size() - 1);
                    g.setColor(colors.get(colorIndex));
                } else {
                    g.setColor(Color.BLACK);
                }
                g.drawLine(x, y, x, y);
            }
        }
        g.dispose();

        return copy;
    }

    private BufferedImage createImageWithBoundaries(BufferedImage sample, List<Rectangle> rectangles) {
        BufferedImage copy = ImageUtil.copy(sample);

        Graphics2D g = copy.createGraphics();
        g.setColor(Color.MAGENTA);
        for (Rectangle r : rectangles) {
            g.drawRect(r.x, r.y, r.width, r.height);
        }
        g.dispose();

        return copy;
    }
}
