package pl.grizwold.spotter.detection.pattern;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import lombok.extern.slf4j.Slf4j;
import pl.grizwold.spotter.model.Icon;
import pl.grizwold.spotter.model.Point;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Slf4j
public class PatternMatcher {
    private static final org.slf4j.Logger perfLog = org.slf4j.LoggerFactory.getLogger(PatternMatcher.class.getName() + "-performance");
    // pure magenta color - RGB(255, 0, 255)
    private static final int MASK = -65281;

    final BufferedImage image;
    final Icon pattern;

    public PatternMatcher(BufferedImage image, Icon pattern) {
        this.image = Objects.requireNonNull(image);
        this.pattern = Objects.requireNonNull(pattern);
    }

    public boolean testPattern(int x, int y) {
        if (!validPoint(x, y)) {
            return false;
        }

        BufferedImage subimage = image.getSubimage(x, y, pattern.getImage().getWidth(), pattern.getImage().getHeight());
        BiMap<Integer, Integer> colorMap = HashBiMap.create();
        Set<Integer> ignoredColors = new HashSet<>();

        for (int _x = 0; _x < pattern.getImage().getWidth(); _x++) {
            for (int _y = 0; _y < pattern.getImage().getHeight(); _y++) {
                int patternPixel = pattern.getImage().getRGB(_x, _y);
                int imagePixel = subimage.getRGB(_x, _y);

                if (patternPixel == MASK) {
                    if (colorMap.containsValue(imagePixel)) {
                        log.trace("Image did not match the pattern. Previously matched color {} was found under the MASK at {}", imagePixel, new Point(x, y).translate(_x, _y));
                        return false;
                    }
                    ignoredColors.add(imagePixel);
                    continue;
                }

                if (ignoredColors.contains(imagePixel)) {
                    log.trace("Image did not match the pattern. Already ignored color {} appeared on " +
                            "non-ignored area at {}", imagePixel, new Point(x, y).translate(_x, _y));
                    return false;
                }

                if (colorMap.containsKey(patternPixel) && colorMap.get(patternPixel) != imagePixel) {
                    log.trace("Image did not match the pattern. Previously matched color from pattern {} was not " +
                            "matched again, the color {} appeared at {}", patternPixel, colorMap.get(patternPixel), new Point(x, y).translate(_x, _y));
                    return false;
                }

                if (colorMap.containsValue(imagePixel) && colorMap.inverse().get(imagePixel) != patternPixel) {
                    log.trace("Image did not match the pattern. Previously matched color from the image {} was not " +
                            "matched again, the color {} appeared at {}", imagePixel, colorMap.inverse().get(imagePixel), new Point(x, y).translate(_x, _y));
                    return false;
                }

                colorMap.put(patternPixel, imagePixel);
            }
        }

        return true;
    }

    private boolean validPoint(int x, int y) {
        return patternFitsInImage() &&
                coordinatesAreInBoundsOfImage(x, y) &&
                patternDoesNotStickOutOfImage(x, y);
    }

    private boolean patternFitsInImage() {
        return image.getWidth() >= pattern.getImage().getWidth() && image.getHeight() >= pattern.getImage().getHeight();
    }

    private boolean coordinatesAreInBoundsOfImage(int x, int y) {
        return x >= 0 && x < this.image.getWidth() &&
                y >= 0 && y < this.image.getHeight();
    }

    private boolean patternDoesNotStickOutOfImage(int x, int y) {
        return x + pattern.getImage().getWidth() <= image.getWidth() &&
                y + pattern.getImage().getHeight() <= image.getHeight();
    }
}
