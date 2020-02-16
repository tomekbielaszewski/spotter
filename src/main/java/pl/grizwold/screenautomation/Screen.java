package pl.grizwold.screenautomation;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.awt.event.InputEvent;
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
        this.offset = workingArea.getLocation();
        this.workingArea = workingArea;
        this.robot = new Robot();
        this.imageLocator = new ImageLocator(robot.createScreenCapture(workingArea));
    }

    public Screen refresh() {
        return new Screen(this.workingArea);
    }

    public Screen drag(Icon from, Icon to) {
        return drag(from, to,
                s -> log.error("Could not find icon to drag: " + from.getFilename()),
                s -> log.error("Could not find icon to drag: " + to.getFilename()));
    }

    public Screen drag(Icon from, Icon to, Consumer<Screen> onNotFoundFrom, Consumer<Screen> onNotFoundTo) {
        locate(from)
                .ifPresentOrElse(
                        fromP -> locate(to)
                                .ifPresentOrElse(
                                        toP -> drag(fromP, toP),
                                        () -> onNotFoundTo.accept(this)
                                ),
                        () -> onNotFoundFrom.accept(this)
                );
        return this;
    }

    public Screen drag(Point from, Point to) {
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
        locate(icon)
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
        this.click(point);
        halt(delay);
        this.click(point);
        return this;
    }

    public Screen waitAndDrag(Icon waitFor, Icon dragTo, long timeout) {
        Screen screen = waitFor(waitFor, timeout);
        return screen.drag(waitFor, dragTo);
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
        locate(icon)
                .ifPresentOrElse(
                        this::click,
                        () -> onNotFound.accept(this)
                );
        return this;
    }

    public Screen click(Point point) {
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
        long start = System.currentTimeMillis();
        do {
            Screen refreshed = refresh();
            Optional<Point> location = refreshed.locate(icon);
            if (location.isPresent())
                return refreshed;
            halt();
        } while ((System.currentTimeMillis() - start) < timeout);
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

    private Optional<Point> locate(Icon icon) {
        return imageLocator.locate(icon)
                .stream()
                .findFirst();
    }

    private Point addOffset(Point p) {
        Point copy = p.getLocation();
        copy.translate(this.offset.x, this.offset.y);
        return copy;
    }
}
