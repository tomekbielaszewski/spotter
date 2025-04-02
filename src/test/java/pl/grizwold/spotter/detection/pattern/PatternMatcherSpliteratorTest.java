package pl.grizwold.spotter.detection.pattern;

import org.junit.jupiter.api.Test;
import pl.grizwold.spotter.model.Icon;
import pl.grizwold.spotter.model.Point;
import pl.grizwold.spotter.util.ImageUtil;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

class PatternMatcherSpliteratorTest {
    private final BufferedImage base = ImageUtil.read("src/test/resources/pattern_matching/base_175x31.png");

    @Test
    void should_match_with_self() {
        Stream<Point> stream = PatternMatcherSpliterator.stream(new PatternMatcher(base, new Icon("src/test/resources/pattern_matching/base_175x31.png")));
        List<Point> list = stream.toList();

        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
        assertEquals(new Point(0, 0), list.getFirst());
    }

    @Test
    void should_not_find_matches_when_pattern_too_tall() {
        Stream<Point> stream = PatternMatcherSpliterator.stream(new PatternMatcher(base, new Icon("src/test/resources/pattern_matching/empty_pattern_40x40.png")));
        List<Point> list = stream.toList();

        assertTrue(list.isEmpty());
    }

    @Test
    void should_not_find_matches_when_pattern_too_wide() {
        Stream<Point> stream = PatternMatcherSpliterator.stream(new PatternMatcher(base, new Icon("src/test/resources/pattern_matching/empty_pattern_180x10.png")));
        List<Point> list = stream.toList();

        assertTrue(list.isEmpty());
    }

    @Test
    void should_match_everything_with_an_empty_pattern() {
        Stream<Point> stream = PatternMatcherSpliterator.stream(new PatternMatcher(base, new Icon("src/test/resources/pattern_matching/empty_pattern_10x10.png")));
        List<Point> list = stream.toList();

        int imageWidth = 175;
        int imageHeight = 31;
        var patternWidth = 10;
        var patternHeight = 10;

        assertFalse(list.isEmpty());
        assertEquals((imageWidth - patternWidth + 1) * (imageHeight - patternHeight + 1), list.size());
    }

    @Test
    void should_match_everything_with_an_empty_pattern_when_parallel() {
        Stream<Point> stream = PatternMatcherSpliterator.stream(new PatternMatcher(base, new Icon("src/test/resources/pattern_matching/empty_pattern_10x10.png"))).parallel();
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
        Stream<Point> stream = PatternMatcherSpliterator.stream(new PatternMatcher(base, new Icon("src/test/resources/pattern_matching/pattern_letter_eight.png")));
        List<Point> list = stream.toList();

        assertTrue(list.isEmpty());
    }

    @Test
    void should_match_number_three() {
        Stream<Point> stream = PatternMatcherSpliterator.stream(new PatternMatcher(base, new Icon("src/test/resources/pattern_matching/pattern_letter_three.png")));
        List<Point> list = stream.toList();

        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
        assertEquals(new Point(140, 10), list.getFirst());
    }

    @Test
    void should_match_comma_only_once() {
        Stream<Point> stream = PatternMatcherSpliterator.stream(new PatternMatcher(base, new Icon("src/test/resources/pattern_matching/pattern_letter_comma.png")));
        List<Point> list = stream.toList();

        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
        assertEquals(new Point(137, 10), list.getFirst());
    }

    @Test
    void should_find_two_hits() {
        BufferedImage twoIrons = ImageUtil.read("src/test/resources/pattern_matching/two_irons.png");
        Icon iron = new Icon("src/test/resources/pattern_matching/iron.png");
        Stream<Point> stream = PatternMatcherSpliterator.stream(new PatternMatcher(twoIrons, iron));

        List<Point> list = stream.toList();

        assertEquals(2, list.size());
        assertEquals(new Point(120, 84), list.getFirst());
        assertEquals(new Point(201, 230), list.getLast());
    }

    @Test
    void should_find_two_hits_when_pattern_has_altered_hue() {
        BufferedImage twoIrons = ImageUtil.read("src/test/resources/pattern_matching/two_irons.png");
        Icon iron = new Icon("src/test/resources/pattern_matching/iron_altered_hue.png");
        Stream<Point> stream = PatternMatcherSpliterator.stream(new PatternMatcher(twoIrons, iron));

        List<Point> list = stream.toList();

        assertEquals(2, list.size());
        assertEquals(new Point(120, 84), list.getFirst());
        assertEquals(new Point(201, 230), list.getLast());
    }

    @Test
    void should_not_match_second_half_when_split_and_first_half_used() {
        BufferedImage twoIrons = ImageUtil.read("src/test/resources/pattern_matching/two_irons.png");
        Icon iron = new Icon("src/test/resources/pattern_matching/iron.png");
        PatternMatcher matcher = new PatternMatcher(twoIrons, iron);
        PatternMatcherSpliterator spliterator = new PatternMatcherSpliterator(matcher);
        Spliterator<Point> prefixSplit = spliterator.trySplit();

        List<Point> listFromPrefixSpliterator = StreamSupport.stream(prefixSplit, false)
                .toList();

        assertEquals(1, listFromPrefixSpliterator.size());
        assertEquals(new Point(120, 84), listFromPrefixSpliterator.getFirst());
    }

    @Test
    void should_not_match_first_half_when_split_and_second_half_used() {
        BufferedImage twoIrons = ImageUtil.read("src/test/resources/pattern_matching/two_irons.png");
        Icon iron = new Icon("src/test/resources/pattern_matching/iron.png");
        PatternMatcherSpliterator matcher = new PatternMatcherSpliterator(new PatternMatcher(twoIrons, iron));
        Spliterator<Point> _ = matcher.trySplit();

        List<Point> listFromPrefixSpliterator = StreamSupport.stream(matcher, false)
                .toList();

        assertEquals(1, listFromPrefixSpliterator.size());
        assertEquals(new Point(201, 230), listFromPrefixSpliterator.getFirst());
    }

    @Test
    void should_amount_of_matches_after_split_sum_up_to_total_matches_of_one() {
        Icon pattern = new Icon("src/test/resources/pattern_matching/empty_pattern_10x10.png");

        Stream<Point> notSplitMatcher = PatternMatcherSpliterator.stream(new PatternMatcher(base, pattern));
        PatternMatcherSpliterator secondSplit = new PatternMatcherSpliterator(new PatternMatcher(base, pattern));
        Spliterator<Point> firstSplit = secondSplit.trySplit();

        List<Point> notSplit = notSplitMatcher.toList();
        List<Point> firstHalf = StreamSupport.stream(firstSplit, false).toList();
        List<Point> secondHalf = StreamSupport.stream(secondSplit, false).toList();

        assertEquals(firstHalf.size(), secondHalf.size());
        assertEquals(notSplit.size(), firstHalf.size() + secondHalf.size());
    }
}