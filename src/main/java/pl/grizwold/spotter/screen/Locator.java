package pl.grizwold.spotter.screen;

import lombok.extern.slf4j.Slf4j;
import pl.grizwold.spotter.detection.comparision.ImageLocator;
import pl.grizwold.spotter.model.Icon;
import pl.grizwold.spotter.model.Point;
import pl.grizwold.spotter.detection.comparision.ImageComparator;
import pl.grizwold.spotter.detection.comparision.PixelByPixelImageLocator;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;

@Slf4j
public class Locator {
    private final BufferedImage base;
    private final ImageComparator imageComparator;
    private final ImageLocator imageLocator;
    private final Point offset;

    public Locator(BufferedImage base, ImageComparator imageComparator, ImageLocator imageLocator, Point offset) {
        this.base = base;
        this.imageComparator = imageComparator;
        this.imageLocator = imageLocator;
        this.offset = offset;
    }

    /**
     * Locates given image icon on {@link Locator}.base image. When several icons are found on {@link Locator}.base - one of them is returned.
     * The order may be random. Returned {@link Point} is location of left upper pixel of given icon. Returned {@link Point}
     * cooridnates are related to {@link Screen}.workingArea of enclosin {@link Screen} object (thus - subrectangle of
     * physical screen). In order to translate it to location on physical screen - translate it by {@link Screen}.offset
     *
     * @param icon small image which will be searched on the screen
     * @return location of upper left pixel of given icon
     */
    public Optional<Point> locate(Icon icon) {
        log.debug("Locating {}", icon.getFilename());
        return locateAll(icon)
                .stream()
                .findFirst();
    }

    /**
     * Locates given image icon on screen. When several icons are found on screen - one of them is returned.
     * The order may be random. Returned {@link Point} is location of center pixel of given icon. Returned {@link Point}
     * cooridnates are related to {@link Screen}.workingArea of enclosin {@link Screen} object (thus - subrectangle of
     * physical screen). In order to translate it to location on physical screen - translate it by {@link Screen}.offset
     * <br/><br/>
     * This method is similar to: {@link #locate(Icon)}
     *
     * @param icon small image which will be searched on the screen
     * @return location of center pixel of given icon
     */
    public Optional<Point> locateMiddle(Icon icon) {
        log.debug("Locating middle {}", icon.getFilename());
        BufferedImage iconImage = icon.getImage();
        return locate(icon)
                .map(p -> p.translate(iconImage.getWidth() / 2, iconImage.getHeight() / 2));
    }

    public Optional<Rectangle> locateArea(Icon upperLeft, Icon lowerRight) {
        log.debug("Locating area between {} and {}", upperLeft.getFilename(), lowerRight.getFilename());
        Optional<Point> upperLeftPoint = locate(upperLeft);
        Optional<Point> lowerRightPoint = locate(lowerRight);

        if (upperLeftPoint.isPresent() && lowerRightPoint.isPresent()) {
            BufferedImage lowerRightImage = lowerRight.getImage();

            Point p1 = upperLeftPoint.get();
            Point p2 = lowerRightPoint.get().translate(lowerRightImage.getWidth(), lowerRightImage.getHeight());
            p1 = addOffset(p1);
            p2 = addOffset(p2);

            return Optional.of(new Rectangle(p1.toAwt(), p2.minus(p1).toDimension()));
        }

        return Optional.empty();
    }

    public boolean isLocatedAtCenterOf(Icon icon, Rectangle area) {
        log.debug("Checking if {} is located at center of {}", icon.getFilename(), area.toString());
        Point iconLocation = new Point(area.getCenterX(), area.getCenterY())
                .minus(icon.getCenter())
                .translate(area.getLocation());
        return isLocatedAt(icon, iconLocation);
    }

    public boolean isLocatedAt(Icon icon, Point p) {
        log.debug("Checking if {} is located at {}", icon.getFilename(), p.toString());
        BufferedImage image = icon.getImage();

        if (p.x < 0 || p.y < 0 || p.x + image.getWidth() > this.base.getWidth() || p.y + image.getHeight() > this.base.getHeight()) {
            return false;
        }

        BufferedImage screen = this.base.getSubimage(p.x, p.y, image.getWidth(), image.getHeight());
        return this.imageComparator.areTheSame(image, screen);
    }

    /**
     * Accepts a Point from the current working area and translates it to its relative rectangle.
     */
    public Optional<Rectangle> locateArea(Point from, Rectangle relativeArea) {
        log.debug("Locating area from {} with relative area {}", from, relativeArea.toString());
        return Optional.ofNullable(from)
                .map(p -> new Point(relativeArea.getLocation())
                        .translate(p)
                        .translate(this::addOffset)
                        .toRectangle(relativeArea.getSize()));
    }

    /**
     * @see Locator#locateArea(Point, Rectangle)
     */
    public Optional<Rectangle> locateArea(Icon icon, Rectangle relativeArea) {
        log.debug("Locating area from {} with relative area {}", icon.getFilename(), relativeArea.toString());
        return locate(icon)
                .flatMap(p -> this.locateArea(p, relativeArea));
    }

    public List<Point> locateAll(Icon icon) {
        log.debug("Locating all {}", icon.getFilename());
        return imageLocator.locate(icon);
    }

    public boolean isVisible(Icon icon) {
        log.debug("Checking visibility of {}", icon.getFilename());
        return locate(icon)
                .isPresent();
    }

    private Point addOffset(Point p) {
        return p.translate(this.offset);
    }
}
