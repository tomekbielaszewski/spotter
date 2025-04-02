package pl.grizwold.spotter.detection.pattern;

import lombok.extern.slf4j.Slf4j;
import pl.grizwold.spotter.model.Point;

import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
public class PatternMatcherSpliterator implements Spliterator<Point> {
    private static final org.slf4j.Logger perfLog = org.slf4j.LoggerFactory.getLogger(PatternMatcher.class.getName() + "-performance");

    private final PatternMatcher matcher;
    private final int size;
    private int offset;

    public static Stream<Point> stream(PatternMatcher matcher) {
        return StreamSupport.stream(new PatternMatcherSpliterator(matcher), false);
    }

    public PatternMatcherSpliterator(PatternMatcher matcher) {
        this.matcher = matcher;
        this.size = lastPossibleMatchingPoint();
    }

    private PatternMatcherSpliterator(PatternMatcher matcher, int offset, int size) {
        this.matcher = matcher;
        this.offset = offset;
        this.size = size;
    }

    @Override
    public boolean tryAdvance(Consumer<? super Point> action) {
        var start = System.currentTimeMillis();
        while (patternFitsInImage()) {
            var thisOffset = offset;
            var found = this.matcher.testPattern(x(), y());
            increaseOffset();

            if (found) {
                action.accept(new Point(x(thisOffset), y(thisOffset)));
                perfLog.debug("Pattern {} found in {}ms", matcher.pattern, System.currentTimeMillis() - start);
                return true;
            }
        }
        perfLog.debug("Pattern {} NOT found after {}ms", matcher.pattern, System.currentTimeMillis() - start);
        return false;
    }

    private boolean patternFitsInImage() {
        var patternFitsTheImage = matcher.image.getWidth() >= matcher.pattern.getImage().getWidth() &&
                matcher.image.getHeight() >= matcher.pattern.getImage().getHeight();
        var thereAreStillChecksToBeDone = offset <= size;
        return patternFitsTheImage && thereAreStillChecksToBeDone;
    }

    private void increaseOffset() {
        if (x() >= matcher.image.getWidth() - matcher.pattern.getImage().getWidth()) {
            // pattern right side is at the right end of an image
            // it won't fit anymore if we progress further to the right
            // we need to scan next line below from the beginning
            offset = (y() + 1) * matcher.image.getWidth();
        } else {
            offset++;
        }
    }

    private int lastPossibleMatchingPoint() {
        return (matcher.image.getHeight() * matcher.image.getWidth())
                - (matcher.pattern.getImage().getHeight() * matcher.image.getWidth())
                + (matcher.image.getWidth() - matcher.pattern.getImage().getWidth());
    }

    private int y(int offset) {
        return offset / matcher.image.getWidth();
    }

    private int x(int offset) {
        return offset % matcher.image.getWidth();
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

        PatternMatcherSpliterator prefixSpliterator = new PatternMatcherSpliterator(matcher, offset, offset + (remaining / 2));

        this.offset = offset + (remaining / 2);
        this.increaseOffset();

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
