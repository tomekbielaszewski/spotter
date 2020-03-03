package pl.grizwold.screenautomation;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
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

/**
 * {@link ImageLocator} class is responsible for locating given {@link Icon} on bigger "base" image provided in constructor.<br/>
 * Pure magenta pixels on subimage are ignored. rgb(255,0,255)<br/>
 * Finding subimages which pixel colors are much less common on base image is much faster. Especially when most unique
 * pixel color is located on upper left corner of subimage.<br/><br/>
 *
 * Algorithm for locating {@link Icon} subimage goes as follows:<br/>
 * - Preprocess base image by mapping each pixels color to all locations on screen. Map<PixelColor, LocationOnScreen><br/>
 * - locate first non-magenta pixel on subimage starting from upper left corner and going down<br/>
 * - find all locations of found pixel color on base image<br/>
 * - for every next non-magenta pixel:<br/>
 *   - calculate vector between location of first non-magenta pixel (in subimage coordinates) and this pixel (in subimage coordinates)<br/>
 *   - find all locations of this pixel color on base image<br/>
 *   - filter out all first pixel locations which are not equal to next pixel location subtracted by vector calculated above<br/>
 **/
@Slf4j
public class ImageLocator {
    private static final int MASK = -65281; //pure magenta color

    private final BufferedImage base;

    private Map<Integer, List<Point>> colourMap = new HashMap<>();
    private boolean debug_saveSteps;
    private String debug_saveStepsDirectory;
    private int amountOfLastFoundPixels = -1;

    /**
     * @param base image on which Icon will be searched
     */
    public ImageLocator(@Nonnull BufferedImage base) {
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

    /**
     * Founds all locations of subimage on base image.
     * @param subimage {@link BufferedImage} wrapper of searched image
     * @return all found locations of given {@link Icon} subimage found on base image
     */
    @Nonnull
    public List<Point> locate(@Nonnull final Icon subimage) {
        long start = System.currentTimeMillis();
        final BufferedImage sample = subimage.getImage();
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

                debugImage(this.base, subimage, possibleFirstPixels, x + "_" + y, start);

                if (possibleFirstPixels.isEmpty()) {
                    log.debug("Icon {} not found", subimage.getFilename());
                    return possibleFirstPixels;
                }
            }
        }

        log.debug("Locating icon \"{}\" took: {} ms", subimage.getFilename(), (System.currentTimeMillis() - start));

        possibleFirstPixels = possibleFirstPixels.stream()
                .map(p -> p.minus(firstSampledLocation[0]))
                .collect(Collectors.toList());

        return possibleFirstPixels;
    }

    /**
     * Activating image debugging will visualize each iteration of image locating algorithm by saving images with
     * located possible first pixels in given directory. Possible first pixels are marked by magenta color and every next
     * algorithm iteration should filter out some of the pixels (see algorithm description on {@link ImageLocator}).
     * Last saved iteration will be clear base image in case of not finding the subimage, or single magenta pixel in case of
     * finding single location or multiple pixels in case of finding many subimages on base image.
     * @param directory where algorithm iteration steps will be visualized by saving image files
     */
    public void activateDebugging(String directory) {
        this.debug_saveSteps = true;
        this.debug_saveStepsDirectory = Optional.ofNullable(directory).orElse("debug");
    }

    /**
     * Deactivates algorithm visualizer. See: activateDebugging
     */
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

    @Nonnull
    private BufferedImage copy(@Nonnull BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }
}
