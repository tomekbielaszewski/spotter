package pl.grizwold.spotter.screen;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import pl.grizwold.spotter.model.Icon;
import pl.grizwold.spotter.model.Point;
import pl.grizwold.spotter.processing.ImageComparator;
import pl.grizwold.spotter.processing.PixelByPixelImageLocator;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

@Slf4j
public class Screen {
    private static final long DEFAULT_TIMEOUT = Integer.MAX_VALUE;
    private static final long DEFAULT_ACTION_DELAY = 50;
    private static final BiConsumer<Icon, Screen> DO_NOTHING = (_, _) -> {};
    private static final BiConsumer<List<Icon>, Screen> GROUP_DO_NOTHING = (_, _) -> {};

    private final Robot robot;
    private final Point offset;
    private final Rectangle workingArea;

    @Getter
    private BufferedImage screenCapture;
    private PixelByPixelImageLocator imageLocator;
    private ImageComparator imageComparator;
    private long waitingLogTimeout = 10000;
    private BiConsumer<Icon, Screen> defaultTimeoutHandler = DO_NOTHING;
    private BiConsumer<Icon, Screen> defaultIconNotFoundHandler = DO_NOTHING;
    private BiConsumer<List<Icon>, Screen> defaultGroupTimeoutHandler = GROUP_DO_NOTHING;
    private int colorTolerance = 30;

    public Screen() {
        this(GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration().getBounds(),
                GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice());
    }

    public Screen(GraphicsDevice graphicsDevice) {
        this(graphicsDevice.getDefaultConfiguration().getBounds(), graphicsDevice);
    }

    public Screen(Rectangle workingArea) {
        this(workingArea, GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice());
    }

    @SneakyThrows
    public Screen(Rectangle workingArea, GraphicsDevice graphicsDevice) {
        log.debug("Working area set to: " + workingArea.toString());
        this.offset = new Point(workingArea.getLocation());
        this.workingArea = workingArea;
        this.robot = new Robot(graphicsDevice);
        this.imageComparator = new ImageComparator();
        refresh();
        withLoggingNotFound();
        withLoggingTimeouts();
    }

    /**
     * @see Locator#locate(Icon)
     */
    public Optional<Point> locate(Icon icon) {
        return locator().locate(icon);
    }

    /**
     * @see Locator#locateMiddle(Icon)
     */
    public Optional<Point> locateMiddle(Icon icon) {
        return locator().locateMiddle(icon);
    }

    /**
     * @see Locator#locateArea(Icon, Icon)
     */
    public Optional<Rectangle> locateArea(Icon upperLeft, Icon lowerRight) {
        return locator().locateArea(upperLeft, lowerRight);
    }

    /**
     * @see Locator#isLocatedAtCenterOf(Icon, Rectangle)
     */
    public boolean isLocatedAtCenterOf(Icon icon, Rectangle area) {
        return locator().isLocatedAtCenterOf(icon, area);
    }

    /**
     * @see Locator#isLocatedAt(Icon, Point)
     */
    public boolean isLocatedAt(Icon icon, Point p) {
        return locator().isLocatedAt(icon, p);
    }

    /**
     * @see Locator#locateArea(Icon, Rectangle)
     */
    public Optional<Rectangle> locateArea(Icon icon, Rectangle relativeArea) {
        return locator().locateArea(icon, relativeArea);
    }

    /**
     * @see Locator#locateAll(Icon)
     */
    public List<Point> locateAll(Icon icon) {
        return locator().locateAll(icon);
    }

    /**
     * @see Locator#isVisible(Icon)
     */
    public boolean isVisible(Icon icon) {
        return locator().isVisible(icon);
    }

    public Screen refresh() {
        log.debug("Refreshing screenshot");
        this.screenCapture = robot.createScreenCapture(workingArea);
        this.imageLocator = new PixelByPixelImageLocator(screenCapture).withTolerance(colorTolerance);
        return this;
    }

    /**
     * @param key use {@link KeyEvent} constants
     */
    public Screen press(int key) {
        robot.keyPress(key);
        halt(100);
        robot.keyRelease(key);
        return this;
    }

    public Screen move(Point to) {
        to = addOffset(to);
        robot.mouseMove(to.x, to.y);
        return this;
    }

    public Screen drag(Icon from, Icon to) {
        log.debug("Dragging from {} to {}", from.getFilename(), to.getFilename());
        return drag(from, to, defaultIconNotFoundHandler, defaultIconNotFoundHandler);
    }

    public Screen drag(Icon from, Icon to, BiConsumer<Icon, Screen> onNotFoundFrom, BiConsumer<Icon, Screen> onNotFoundTo) {
        log.debug("Dragging from {} to {}", from.getFilename(), to.getFilename());
        locateMiddle(to)
                .ifPresentOrElse(
                        toPoint -> drag(from, toPoint, onNotFoundFrom),
                        () -> onNotFoundTo.accept(to, this)
                );
        return this;
    }

    public Screen drag(Icon from, Point toPoint) {
        return drag(from, toPoint, defaultIconNotFoundHandler);
    }

    public Screen drag(Icon from, Point toPoint, BiConsumer<Icon, Screen> onNotFoundFrom) {
        log.debug("Dragging from {} to {}:{}", from.getFilename(), toPoint.x, toPoint.y);
        locateMiddle(from)
                .ifPresentOrElse(
                        fromPoint -> drag(fromPoint, toPoint),
                        () -> onNotFoundFrom.accept(from, this)
                );
        return this;
    }

    public Screen drag(Point fromPoint, Icon to) {
        return drag(fromPoint, to, defaultIconNotFoundHandler);
    }

    public Screen drag(Point fromPoint, Icon to, BiConsumer<Icon, Screen> onNotFoundTo) {
        log.debug("Dragging from {}:{} to {}", fromPoint.x, fromPoint.y, to.getFilename());
        locateMiddle(to)
                .ifPresentOrElse(
                        toPoint -> drag(fromPoint, toPoint),
                        () -> onNotFoundTo.accept(to, this)
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
        return doubleClick(icon, defaultIconNotFoundHandler);
    }

    public Screen doubleClick(Icon icon, BiConsumer<Icon, Screen> onNotFound) {
        locateMiddle(icon)
                .ifPresentOrElse(
                        this::doubleClick,
                        () -> onNotFound.accept(icon, this)
                );
        return this;
    }

    public Screen doubleClick(Point point) {
        return doubleClick(point, DEFAULT_ACTION_DELAY);
    }

    public Screen doubleClick(Point point, long delay) {
        log.debug("Double clicking {}:{} with delay {}", point.x, point.y, delay);
        this.click(point);
        halt(delay);
        this.click(point);
        return this;
    }

    public Screen click(Icon icon) {
        return click(icon, defaultIconNotFoundHandler);
    }

    public Screen click(Icon icon, BiConsumer<Icon, Screen> onNotFound) {
        log.debug("Clicking {}", icon.getFilename());
        locateMiddle(icon)
                .ifPresentOrElse(
                        this::click,
                        () -> onNotFound.accept(icon, this)
                );
        return this;
    }

    public Screen click(Point point) {
        log.debug("Clicking {}:{}", point.x, point.y);
        point = addOffset(point);

        robot.mouseMove(point.x, point.y);
        halt();
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        halt();
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        return this;
    }

    public Screen rightClick(Icon icon) {
        return rightClick(icon, defaultIconNotFoundHandler);
    }

    public Screen rightClick(Icon icon, BiConsumer<Icon, Screen> onNotFound) {
        log.debug("Right clicking {}", icon.getFilename());
        locateMiddle(icon)
                .ifPresentOrElse(
                        this::rightClick,
                        () -> onNotFound.accept(icon, this)
                );
        return this;
    }

    public Screen rightClick(Point point) {
        log.debug("Clicking {}:{}", point.x, point.y);
        point = addOffset(point);

        robot.mouseMove(point.x, point.y);
        halt();
        robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
        halt();
        robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
        return this;
    }

    public Screen holdRMB() {
        robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
        return this;
    }

    public Screen releaseRMB() {
        robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
        return this;
    }

    public Screen holdLMB() {
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        return this;
    }

    public Screen releaseLMB() {
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        return this;
    }

    public Screen waitFor(Icon icon) {
        return waitFor(icon, DEFAULT_TIMEOUT);
    }

    public Screen waitFor(Icon icon, long timeout) {
        return waitFor(icon, timeout, defaultTimeoutHandler);
    }

    public Screen waitFor(Icon icon, BiConsumer<Icon, Screen> onTimeout) {
        return waitFor(icon, DEFAULT_TIMEOUT, onTimeout);
    }

    public Screen waitFor(Icon icon, long timeout, BiConsumer<Icon, Screen> onTimeout) {
        log.debug("Waiting {}ms for {}", timeout, icon.getFilename());
        long start = System.currentTimeMillis();
        boolean longWaitLogged = false;
        do {
            refresh();
            if (isVisible(icon))
                return this;
            if ((System.currentTimeMillis() - start) > waitingLogTimeout && !longWaitLogged) {
                log.info("Waiting for {} takes more than {}ms", icon.getFilename(), waitingLogTimeout);
                longWaitLogged = true;
            }
            halt();
        } while ((System.currentTimeMillis() - start) < timeout);
        log.debug("Couldn't find {} in specified time of {}ms", icon.getFilename(), timeout);
        onTimeout.accept(icon, this);
        return this;
    }

    public Optional<Icon> waitFor(List<Icon> icons) {
        return waitFor(icons, DEFAULT_TIMEOUT);
    }

    public Optional<Icon> waitFor(List<Icon> icons, long timeout) {
        return waitFor(icons, timeout, defaultGroupTimeoutHandler);
    }

    public Optional<Icon> waitFor(List<Icon> icons, long timeout, BiConsumer<List<Icon>, Screen> onTimeout) {
        log.debug("Waiting {}ms for {} icons", timeout, icons.size());
        long start = System.currentTimeMillis();
        boolean longWaitLogged = false;
        do {
            refresh();
            for (Icon icon : icons) {
                if (isVisible(icon))
                    return Optional.of(icon);
            }
            if ((System.currentTimeMillis() - start) > waitingLogTimeout && !longWaitLogged) {
                log.info("Waiting for one of the {} icons takes more than {}ms", icons.size(), waitingLogTimeout);
                longWaitLogged = true;
            }
            halt();
        } while ((System.currentTimeMillis() - start) < timeout);
        log.debug("Couldn't find any of the icons in specified time of {}ms", timeout);
        onTimeout.accept(icons, this);
        return Optional.empty();
    }

    public Screen halt() {
        return halt(DEFAULT_ACTION_DELAY);
    }

    @SneakyThrows
    public Screen halt(long delay) {
        Thread.sleep(delay);
        return this;
    }

    public Screen activateDebugging() {
        this.imageLocator.activateDebugging();
        return this;
    }

    public Screen deactivateDebugging() {
        this.imageLocator.deactivateDebugging();
        return this;
    }

    public Screen setWaitingLogTimeout(long waitingLogTimeout) {
        this.waitingLogTimeout = waitingLogTimeout;
        return this;
    }

    public Screen withIgnoringTimeouts() {
        return doOnTimeout(DO_NOTHING);
    }

    public Screen withIgnoringNotFound() {
        return doOnIconNotFound(DO_NOTHING);
    }

    public Screen withLoggingTimeouts() {
        return doOnTimeout((i, _) -> log.warn("Waiting for {} timed out!", i.getFilename()));
    }

    public Screen withLoggingNotFound() {
        return doOnIconNotFound((i, _) -> log.warn("Icon {} not found", i.getFilename()));
    }

    public Screen doOnTimeout(BiConsumer<Icon, Screen> defaultTimeoutHandler) {
        this.defaultTimeoutHandler = defaultTimeoutHandler;
        return this;
    }

    public Screen doOnIconNotFound(BiConsumer<Icon, Screen> defaultIconNotFoundHandler) {
        this.defaultIconNotFoundHandler = defaultIconNotFoundHandler;
        return this;
    }

    public Screen doOnGroupTimeout(BiConsumer<List<Icon>, Screen> defaultTimeoutHandler) {
        this.defaultGroupTimeoutHandler = defaultTimeoutHandler;
        return this;
    }

    public Screen withColorTolerance(int range) {
        this.colorTolerance = range;
        this.imageLocator.withTolerance(range);
        this.imageComparator = new ImageComparator(range);
        return this;
    }

    private Point addOffset(Point p) {
        return p.translate(this.offset);
    }

    private Locator locator() {
        return new Locator(this.screenCapture, this.imageComparator, this.imageLocator, this.offset);
    }

    public Point getMouseLocation() {
        return new Point(MouseInfo.getPointerInfo().getLocation())
                .minus(offset);
    }
}
