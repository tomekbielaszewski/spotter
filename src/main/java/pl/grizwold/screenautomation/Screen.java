package pl.grizwold.screenautomation;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
public class Screen {
    private final ImageLocator imageLocator;
    private final Robot robot;
    private final Point offset;
    private final Rectangle workingArea;

    @SneakyThrows
    public Screen(Rectangle workingArea) {
        this.offset = new Point(workingArea.getLocation());
        this.workingArea = workingArea;
        this.robot = new Robot();
        this.imageLocator = new ImageLocator(robot.createScreenCapture(workingArea));
    }

    /**
     * Locates given image icon on screen. When several icons are found on screen - one of them is returned.
     * The order may be random. Returned {@link Point} is location of left upper pixel of given icon. Returned {@link Point}
     * cooridnates are related to {@link Screen}.workingArea of enclosin {@link Screen} object (thus - subrectangle of
     * physical screen). In order to translate it to location on physical screen - translate it by {@link Screen}.offset
     *
     * @param icon small image which will be searched on the screen
     * @return location of upper left pixel of given icon
     */
    public Optional<Point> locate(Icon icon) {
        log.debug("Locating {}", icon.getFilename());
        return imageLocator.locate(icon)
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

            return Optional.of(new Rectangle(p1.toAwt(), p2.minus(p1).toDimension()));
        }

        return Optional.empty();
    }

    public List<Point> locateAll(Icon icon) {
        log.debug("Locating all {}", icon.getFilename());
        return imageLocator.locate(icon);
    }

    public Screen refresh() {
        log.debug("Refreshing screenshot");
        return new Screen(this.workingArea);
    }

    public boolean isVisible(Icon icon) {
        log.debug("Checking visibility of {}", icon.getFilename());
        return locate(icon)
                .isPresent();
    }

    public Screen drag(Icon from, Icon to) {
        log.debug("Dragging from {} to {}", from.getFilename(), to.getFilename());
        return drag(from, to,
                s -> log.error("Could not find icon to drag: " + from.getFilename()),
                s -> log.error("Could not find icon to drag: " + to.getFilename()));
    }

    public Screen drag(Icon from, Icon to, Consumer<Screen> onNotFoundFrom, Consumer<Screen> onNotFoundTo) {
        log.debug("Dragging from {} to {}", from.getFilename(), to.getFilename());
        locateMiddle(from)
                .ifPresentOrElse(
                        fromP -> locateMiddle(to)
                                .ifPresentOrElse(
                                        toP -> drag(fromP, toP),
                                        () -> onNotFoundTo.accept(this)
                                ),
                        () -> onNotFoundFrom.accept(this)
                );
        return this;
    }

    public Screen drag(Icon from, Point to, Consumer<Screen> onNotFoundFrom) {
        log.debug("Dragging from {} to {}:{}", from.getFilename(), to.x, to.y);
        locateMiddle(from)
                .ifPresentOrElse(
                        fromP -> drag(fromP, to),
                        () -> onNotFoundFrom.accept(this)
                );
        return this;
    }

    public Screen drag(Point from, Point to) {
        log.debug("Dragging from {}:{} to {}:{}", from.x, from.y, to.x, to.y);
        from = addOffset(from);
        to = addOffset(to);

        robot.mouseMove(from.x, from.y);
        this.halt();
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        this.halt();
        robot.mouseMove(to.x, to.y);
        this.halt();
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        return this;
    }

    public Screen doubleClick(Icon icon) {
        return doubleClick(icon, s -> log.error("Couldn't find icon to double click: " + icon.getFilename()));
    }

    public Screen doubleClick(Icon icon, Consumer<Screen> onNotFound) {
        locateMiddle(icon)
                .ifPresentOrElse(
                        this::doubleClick,
                        () -> onNotFound.accept(this)
                );
        return this;
    }

    public Screen doubleClick(Point point) {
        this.doubleClick(point, 50);
        return this;
    }

    public Screen doubleClick(Point point, long delay) {
        log.debug("Double clicking {}:{} with delay {}", point.x, point.y, delay);
        this.click(point);
        halt(delay);
        this.click(point);
        return this;
    }

    public Screen waitAndDrag(Icon waitFor, Point to, long timeout) {
        Screen screen = waitFor(waitFor, timeout);
        return screen.drag(waitFor, to,
                s -> log.error("Could not find icon to drag: " + waitFor.getFilename()));
    }

    public Screen waitAndClick(Icon icon, long timeout) {
        Screen screen = waitFor(icon, timeout);
        return screen.click(icon);
    }

    public Screen waitAndClick(Icon icon, long timeout, Consumer<Screen> onNotFound, Consumer<Screen> onTimeout) {
        Screen screen = waitFor(icon, timeout, onTimeout);
        return screen.click(icon, onNotFound);
    }

    public Screen click(Icon icon) {
        return click(icon, s -> log.error("Couldn't find icon to click: " + icon.getFilename()));
    }

    public Screen click(Icon icon, Consumer<Screen> onNotFound) {
        log.debug("Clicking {}", icon.getFilename());
        locateMiddle(icon)
                .ifPresentOrElse(
                        this::click,
                        () -> onNotFound.accept(this)
                );
        return this;
    }

    public Screen click(Point point) {
        log.debug("Clicking {}:{}", point.x, point.y);
        point = addOffset(point);

        robot.mouseMove(point.x, point.y);
        this.halt();
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        halt();
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        return this;
    }

    public Screen waitFor(Icon icon, long timeout) {
        return waitFor(icon, timeout, screen -> {
            throw new IllegalStateException("Timed out! Icon not found: " + icon.getFilename());
        });
    }

    public Screen waitFor(Icon icon, long timeout, Consumer<Screen> onTimeout) {
        log.debug("Waiting {}ms for {}", timeout, icon.getFilename());
        long start = System.currentTimeMillis();
        do {
            Screen refreshed = refresh();
            if (isVisible(icon))
                return refreshed;
            halt();
        } while ((System.currentTimeMillis() - start) < timeout);
        log.debug("Couldn't find {} in specified time of {}ms", icon.getFilename(), timeout);
        onTimeout.accept(this.refresh());
        return this;
    }

    public Screen halt() {
        return halt(50);
    }

    @SneakyThrows
    public Screen halt(long delay) {
        Thread.sleep(delay);
        return this;
    }

    private Point addOffset(Point p) {
        return p.translate(this.offset);
    }
}
