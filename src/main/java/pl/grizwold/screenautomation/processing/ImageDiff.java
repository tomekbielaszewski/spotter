package pl.grizwold.screenautomation.processing;

import lombok.extern.slf4j.Slf4j;
import pl.grizwold.screenautomation.model.Point;

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
    private final BufferedImage original;
    private double pixelToleranceLevel = 0.0;
    private double differenceConstant;
    private int minimalRectangleSize = 10;
    private boolean debug = false;

    public ImageDiff(BufferedImage original) {
        this.original = original;
        differenceConstant = calculateDifferenceConstant();
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
        if (debug) {
            saveRegionVisualization(diffMatrix, sample, start);
        }
        List<Rectangle> rectangles = getRegionBoundaries(regionCount, diffMatrix);
        rectangles = filterOutSmallOnes(rectangles);
        if (debug) {
            saveBoundariesVisualization(rectangles, sample, start);
        }

        log.debug("Detecting difference between images of size {}x{} took: {}ms", sample.getWidth(), sample.getHeight(), (System.currentTimeMillis() - start));
        log.debug("Difference detection found {} rectangles", rectangles.size());
        return rectangles;
    }

    public ImageDiff debug(boolean debug) {
        this.debug = debug;
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
                matrix[y][x] = areColorsDifferent(original.getRGB(x, y), sample.getRGB(x, y)) ? 1 : 0;
            }
        }
        return matrix;
    }

    private int groupRegions(int[][] matrix) {
        int regionId = 2; // 0 is same pixels on both, 1 is difference detected, regions start on 2
        for (int y = 0; y < matrix.length; y++) {
            for (int x = 0; x < matrix[y].length; x++) {
                if (matrix[y][x] == 1) {
                    joinToRegion(x, y, regionId, matrix);
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

    private void joinToRegion(int x, int y, int region, int[][] matrix) {
        if (isJumpRejected(x, y, matrix)) {
            return;
        }

        matrix[y][x] = region;

        joinToRegion(x + 1, y, region, matrix);
        joinToRegion(x, y + 1, region, matrix);
        joinToRegion(x + 1, y - 1, region, matrix);
        joinToRegion(x - 1, y + 1, region, matrix);
        joinToRegion(x + 1, y + 1, region, matrix);
        joinToRegion(x - 1, y - 1, region, matrix);
        joinToRegion(x, y - 1, region, matrix);
        joinToRegion(x - 1, y, region, matrix);
    }

    private boolean isJumpRejected(int x, int y, int[][] matrix) {
        return y < 0 || y >= matrix.length || x < 0 || x >= matrix[y].length || matrix[y][x] != 1;
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

    private void saveRegionVisualization(int[][] diffMatrix, BufferedImage sample, long timestamp) {
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
        drawRegions(diffMatrix, copy, colors);
        ImageUtil.save(copy, "image_diff_debug/" + timestamp + "/regions.png");
    }

    private void saveBoundariesVisualization(List<Rectangle> rectangles, BufferedImage sample, long timestamp) {
        BufferedImage copy = ImageUtil.copy(sample);
        drawBoundaries(copy, rectangles);
        ImageUtil.save(copy, "image_diff_debug/" + timestamp + "/boundaries.png");
    }

    private void drawRegions(int[][] diffMatrix, BufferedImage copy, List<Color> colors) {
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
    }

    private void drawBoundaries(BufferedImage copy, List<Rectangle> rectangles) {
        Graphics2D g = copy.createGraphics();
        g.setColor(Color.MAGENTA);
        for (Rectangle r : rectangles) {
            g.drawRect(r.x, r.y, r.width, r.height);
        }
        g.dispose();
    }
}
