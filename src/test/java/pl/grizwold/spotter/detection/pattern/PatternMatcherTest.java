package pl.grizwold.spotter.detection.pattern;

import org.junit.jupiter.api.Test;
import pl.grizwold.spotter.model.Icon;
import pl.grizwold.spotter.model.Point;
import pl.grizwold.spotter.util.ImageUtil;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class PatternMatcherTest {
    private final BufferedImage base = ImageUtil.read("src/test/resources/pattern_matching/base_175x31.png");

    @Test
    void should_match_with_self() {
        Stream<Point> stream = PatternMatcher.stream(base, new Icon("src/test/resources/pattern_matching/base_175x31.png"));
        List<Point> list = stream.toList();

        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
        assertEquals(new Point(0, 0), list.getFirst());
    }

    @Test
    void should_not_find_matches_when_pattern_too_tall() {
        Stream<Point> stream = PatternMatcher.stream(base, new Icon("src/test/resources/pattern_matching/empty_pattern_40x40.png"));
        List<Point> list = stream.toList();

        assertTrue(list.isEmpty());
    }

    @Test
    void should_not_find_matches_when_pattern_too_wide() {
        Stream<Point> stream = PatternMatcher.stream(base, new Icon("src/test/resources/pattern_matching/empty_pattern_180x10.png"));
        List<Point> list = stream.toList();

        assertTrue(list.isEmpty());
    }

    @Test
    void should_match_everything_with_an_empty_pattern() {
        Stream<Point> stream = PatternMatcher.stream(base, new Icon("src/test/resources/pattern_matching/empty_pattern_10x10.png"));
        List<Point> list = stream.toList();

        int imageWidth = 175;
        int imageHeight = 31;
        var patternWidth = 10;
        var patternHeight = 10;

        assertFalse(list.isEmpty());
        assertEquals((imageWidth - patternWidth + 1) * (imageHeight - patternHeight + 1), list.size());
    }

    @Test
    void should_not_match_eight_in_place_of_three() {
        Stream<Point> stream = PatternMatcher.stream(base, new Icon("src/test/resources/pattern_matching/pattern_letter_eight.png"));
        List<Point> list = stream.toList();

        assertTrue(list.isEmpty());
    }

    @Test
    void should_match_number_three() {
        Stream<Point> stream = PatternMatcher.stream(base, new Icon("src/test/resources/pattern_matching/pattern_letter_three.png"));
        List<Point> list = stream.toList();

        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
        assertEquals(new Point(140, 10), list.getFirst());
    }

    @Test
    void should_match_comma_only_once() {
        Stream<Point> stream = PatternMatcher.stream(base, new Icon("src/test/resources/pattern_matching/pattern_letter_comma.png"));
        List<Point> list = stream.toList();

        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
        assertEquals(new Point(137, 10), list.getFirst());
    }


}