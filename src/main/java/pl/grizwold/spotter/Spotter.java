package pl.grizwold.spotter;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import pl.grizwold.spotter.model.Icon;
import pl.grizwold.spotter.model.Point;
import pl.grizwold.spotter.detection.comparision.ImageComparator;
import pl.grizwold.spotter.detection.comparision.PixelByPixelImageLocator;
import pl.grizwold.spotter.detection.Locator;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

@Slf4j
public class Spotter {
    private static final long DEFAULT_TIMEOUT = Integer.MAX_VALUE;
    private static final long DEFAULT_ACTION_DELAY = 50;
    private static final BiConsumer<Icon, Spotter> DO_NOTHING = (_, _) -> {};
    private static final BiConsumer<List<Icon>, Spotter> GROUP_DO_NOTHING = (_, _) -> {};

    private final Robot robot;
    private final Point offset;
    private final Rectangle workingArea;

    @Getter
    private BufferedImage screenCapture;
    private PixelByPixelImageLocator imageLocator;
    private ImageComparator imageComparator;
    private long waitingLogTimeout = 10000;
    private BiConsumer<Icon, Spotter> defaultTimeoutHandler = DO_NOTHING;
    private BiConsumer<Icon, Spotter> defaultIconNotFoundHandler = DO_NOTHING;
    private BiConsumer<List<Icon>, Spotter> defaultGroupTimeoutHandler = GROUP_DO_NOTHING;
    private int colorTolerance = 30;
    private long actionDelay = DEFAULT_ACTION_DELAY;

    public Spotter() {
        this(GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration().getBounds(),
                GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice());
    }

    public Spotter(GraphicsDevice graphicsDevice) {
        this(graphicsDevice.getDefaultConfiguration().getBounds(), graphicsDevice);
    }

    public Spotter(Rectangle workingArea) {
        this(workingArea, GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice());
    }

    @SneakyThrows
    public Spotter(Rectangle workingArea, GraphicsDevice graphicsDevice) {
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
     * @see Locator#locateArea(Point, Rectangle)
     */
    public Optional<Rectangle> locateArea(Point point, Rectangle relativeArea) {
        return locator().locateArea(point, relativeArea);
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

    public Spotter refresh() {
        log.debug("Refreshing screenshot");
        this.screenCapture = robot.createScreenCapture(workingArea);
        this.imageLocator = new PixelByPixelImageLocator(screenCapture).withTolerance(colorTolerance);
        return this;
    }

    /**
     * @param key use {@link KeyEvent} constants
     */
    public Spotter press(int key) {
        robot.keyPress(key);
        halt(100);
        robot.keyRelease(key);
        return this;
    }

    public Spotter move(Point to) {
        to = addOffset(to);
        robot.mouseMove(to.x, to.y);
        return this;
    }

    public Spotter drag(Icon from, Icon to) {
        log.debug("Dragging from {} to {}", from.getFilename(), to.getFilename());
        return drag(from, to, defaultIconNotFoundHandler, defaultIconNotFoundHandler);
    }

    public Spotter drag(Icon from, Icon to, BiConsumer<Icon, Spotter> onNotFoundFrom, BiConsumer<Icon, Spotter> onNotFoundTo) {
        log.debug("Dragging from {} to {}", from.getFilename(), to.getFilename());
        locateMiddle(to)
                .ifPresentOrElse(
                        toPoint -> drag(from, toPoint, onNotFoundFrom),
                        () -> onNotFoundTo.accept(to, this)
                );
        return this;
    }

    public Spotter drag(Icon from, Point toPoint) {
        return drag(from, toPoint, defaultIconNotFoundHandler);
    }

    public Spotter drag(Icon from, Point toPoint, BiConsumer<Icon, Spotter> onNotFoundFrom) {
        log.debug("Dragging from {} to {}:{}", from.getFilename(), toPoint.x, toPoint.y);
        locateMiddle(from)
                .ifPresentOrElse(
                        fromPoint -> drag(fromPoint, toPoint),
                        () -> onNotFoundFrom.accept(from, this)
                );
        return this;
    }

    public Spotter drag(Point fromPoint, Icon to) {
        return drag(fromPoint, to, defaultIconNotFoundHandler);
    }

    public Spotter drag(Point fromPoint, Icon to, BiConsumer<Icon, Spotter> onNotFoundTo) {
        log.debug("Dragging from {}:{} to {}", fromPoint.x, fromPoint.y, to.getFilename());
        locateMiddle(to)
                .ifPresentOrElse(
                        toPoint -> drag(fromPoint, toPoint),
                        () -> onNotFoundTo.accept(to, this)
                );
        return this;
    }

    public Spotter drag(Point from, Point to) {
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

    public Spotter doubleClick(Icon icon) {
        return doubleClick(icon, defaultIconNotFoundHandler);
    }

    public Spotter doubleClick(Icon icon, BiConsumer<Icon, Spotter> onNotFound) {
        locateMiddle(icon)
                .ifPresentOrElse(
                        this::doubleClick,
                        () -> onNotFound.accept(icon, this)
                );
        return this;
    }

    public Spotter doubleClick(Point point) {
        return doubleClick(point, actionDelay);
    }

    public Spotter doubleClick(Point point, long delay) {
        log.debug("Double clicking {}:{} with delay {}", point.x, point.y, delay);
        this.click(point);
        halt(delay);
        this.click(point);
        return this;
    }

    public Spotter click(Icon icon) {
        return click(icon, defaultIconNotFoundHandler);
    }

    public Spotter click(Icon icon, BiConsumer<Icon, Spotter> onNotFound) {
        log.debug("Clicking {}", icon.getFilename());
        locateMiddle(icon)
                .ifPresentOrElse(
                        this::click,
                        () -> onNotFound.accept(icon, this)
                );
        return this;
    }

    public Spotter click(Point point) {
        log.debug("Clicking {}:{}", point.x, point.y);
        point = addOffset(point);

        robot.mouseMove(point.x, point.y);
        halt();
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        halt();
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        return this;
    }

    public Spotter rightClick(Icon icon) {
        return rightClick(icon, defaultIconNotFoundHandler);
    }

    public Spotter rightClick(Icon icon, BiConsumer<Icon, Spotter> onNotFound) {
        log.debug("Right clicking {}", icon.getFilename());
        locateMiddle(icon)
                .ifPresentOrElse(
                        this::rightClick,
                        () -> onNotFound.accept(icon, this)
                );
        return this;
    }

    public Spotter rightClick(Point point) {
        log.debug("Clicking {}:{}", point.x, point.y);
        point = addOffset(point);

        robot.mouseMove(point.x, point.y);
        halt();
        robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
        halt();
        robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
        return this;
    }

    public Spotter holdRMB() {
        robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
        return this;
    }

    public Spotter releaseRMB() {
        robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
        return this;
    }

    public Spotter holdLMB() {
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        return this;
    }

    public Spotter releaseLMB() {
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        return this;
    }

    public Spotter waitFor(Icon icon) {
        return waitFor(icon, DEFAULT_TIMEOUT);
    }

    public Spotter waitFor(Icon icon, long timeout) {
        return waitFor(icon, timeout, defaultTimeoutHandler);
    }

    public Spotter waitFor(Icon icon, BiConsumer<Icon, Spotter> onTimeout) {
        return waitFor(icon, DEFAULT_TIMEOUT, onTimeout);
    }

    public Spotter waitFor(Icon icon, long timeout, BiConsumer<Icon, Spotter> onTimeout) {
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

    public Optional<Icon> waitFor(List<Icon> icons, long timeout, BiConsumer<List<Icon>, Spotter> onTimeout) {
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

    public Spotter halt() {
        return halt(actionDelay);
    }

    @SneakyThrows
    public Spotter halt(long delay) {
        Thread.sleep(delay);
        return this;
    }

    public Spotter setWaitingLogTimeout(long waitingLogTimeout) {
        this.waitingLogTimeout = waitingLogTimeout;
        return this;
    }

    public Spotter withIgnoringTimeouts() {
        return doOnTimeout(DO_NOTHING);
    }

    public Spotter withIgnoringNotFound() {
        return doOnIconNotFound(DO_NOTHING);
    }

    public Spotter withLoggingTimeouts() {
        return doOnTimeout((i, _) -> log.warn("Waiting for {} timed out!", i.getFilename()));
    }

    public Spotter withLoggingNotFound() {
        return doOnIconNotFound((i, _) -> log.warn("Icon {} not found", i.getFilename()));
    }

    public Spotter doOnTimeout(BiConsumer<Icon, Spotter> defaultTimeoutHandler) {
        this.defaultTimeoutHandler = defaultTimeoutHandler;
        return this;
    }

    public Spotter doOnIconNotFound(BiConsumer<Icon, Spotter> defaultIconNotFoundHandler) {
        this.defaultIconNotFoundHandler = defaultIconNotFoundHandler;
        return this;
    }

    public Spotter doOnGroupTimeout(BiConsumer<List<Icon>, Spotter> defaultTimeoutHandler) {
        this.defaultGroupTimeoutHandler = defaultTimeoutHandler;
        return this;
    }

    public Spotter withColorTolerance(int range) {
        this.colorTolerance = range;
        this.imageLocator.withTolerance(range);
        this.imageComparator = new ImageComparator(range);
        return this;
    }

    public Spotter withActionDelay(long delay) {
        this.actionDelay = delay;
        return this;
    }

    public Spotter withDefaultActionDelay() {
        this.withActionDelay(DEFAULT_ACTION_DELAY);
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
