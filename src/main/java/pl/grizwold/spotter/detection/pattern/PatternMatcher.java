package pl.grizwold.spotter.detection.pattern;

import lombok.extern.slf4j.Slf4j;
import pl.grizwold.spotter.model.Icon;
import pl.grizwold.spotter.model.Point;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
public class PatternMatcher implements Spliterator<Point> {
    // pure magenta color - RGB(255, 0, 255)
    private static final int MASK = -65281;

    private final BufferedImage image;
    private final Icon pattern;
    private final int size;
    private int offset;

    public static Stream<Point> stream(BufferedImage image, Icon pattern) {
        return StreamSupport.stream(new PatternMatcher(image, pattern), false);
    }

    public PatternMatcher(BufferedImage image, Icon pattern) {
        this.image = Objects.requireNonNull(image);
        this.pattern = Objects.requireNonNull(pattern);
        size = lastPossibleMatchingPoint();
    }

    private PatternMatcher(BufferedImage image, Icon pattern, int offset, int size) {
        this.image = image;
        this.pattern = pattern;
        this.offset = offset;
        this.size = size;
    }

    @Override
    public boolean tryAdvance(Consumer<? super Point> action) {
        long start = System.currentTimeMillis();
        while (patternFitsInImage()) {
            var thisOffset = offset;
            boolean found = testPattern();
            increaseOffset();

            if (found) {
                action.accept(toPoint(thisOffset));
                log.debug("Pattern {} found in {}ms", pattern, System.currentTimeMillis() - start);
                return true;
            }
        }
        log.debug("Pattern {} NOT found after {}ms", pattern, System.currentTimeMillis() - start);
        return false;
    }

    private boolean testPattern() {
        BufferedImage subimage = image.getSubimage(x(), y(), pattern.getImage().getWidth(), pattern.getImage().getHeight());
        HashMap<Integer, Integer> patternToImageColorMap = new HashMap<>();
        Set<Integer> ignoredColors = new HashSet<>();
        Set<Integer> matchedColors = new HashSet<>();

        for (int x = 0; x < pattern.getImage().getWidth(); x++) {
            for (int y = 0; y < pattern.getImage().getHeight(); y++) {
                int patternPixel = pattern.getImage().getRGB(x, y);
                int imagePixel = subimage.getRGB(x, y);
                if (patternPixel == MASK) {
                    if (matchedColors.contains(imagePixel)) {
                        log.trace("Image did not match the pattern. Previously matched color {} was found under the MASK at {}", imagePixel, toPoint(offset).translate(x, y));
                        return false;
                    }
                    ignoredColors.add(imagePixel);
                    continue;
                }
                if (ignoredColors.contains(imagePixel)) {
                    log.trace("Image did not match the pattern. Already ignored color {} appeared on non-ignored area at {}", imagePixel, toPoint(offset).translate(x, y));
                    return false;
                }
                Integer previousImagePixel = patternToImageColorMap.putIfAbsent(patternPixel, imagePixel);
                if (previousImagePixel != null && previousImagePixel != imagePixel) {
                    log.trace("Image did not match the pattern. Previously matched color {} was not matched again, the color {} appeared at {}", previousImagePixel, imagePixel, toPoint(offset).translate(x, y));
                    return false;
                }
                matchedColors.add(imagePixel);
            }
        }

        return true;
    }

    private boolean patternFitsInImage() {
        boolean patternFitsTheImage = image.getWidth() >= pattern.getImage().getWidth() && image.getHeight() >= pattern.getImage().getHeight();
        boolean thereAreStillChecksToBeDone = offset <= size;
        return patternFitsTheImage && thereAreStillChecksToBeDone;
    }

    private void increaseOffset() {
        if (x() >= image.getWidth() - pattern.getImage().getWidth()) {
            // pattern right side is at the right end of an image
            // it won't fit anymore if we progress further to the right
            // we need to scan next line below from the beginning
            offset = (y() + 1) * image.getWidth();
        } else {
            offset++;
        }
    }

    private int lastPossibleMatchingPoint() {
        return (image.getHeight() * image.getWidth())
                - (pattern.getImage().getHeight() * image.getWidth())
                + (image.getWidth() - pattern.getImage().getWidth());
    }

    private Point toPoint(int offset) {
        return new Point(x(offset), y(offset));
    }

    private int y(int offset) {
        return offset / image.getWidth();
    }

    private int x(int offset) {
        return offset % image.getWidth();
    }

    private int y() {
        return y(offset);
    }

    private int x() {
        return x(offset);
    }

    @Override
    public Spliterator<Point> trySplit() {
        if (!patternFitsInImage()) {
            return null;
        }
        var remaining = lastPossibleMatchingPoint() - offset;
        if (remaining < 3) {
            return null;
        }
        PatternMatcher prefixSpliterator = new PatternMatcher(image, pattern, offset, (remaining / 2) + offset);
        this.offset = (remaining / 2) + offset + 1;
        return prefixSpliterator;
    }

    @Override
    public long estimateSize() {
        return 0;
    }

    @Override
    public int characteristics() {
        return ORDERED | DISTINCT | NONNULL | IMMUTABLE;
    }
}
