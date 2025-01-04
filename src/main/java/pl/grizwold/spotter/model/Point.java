package pl.grizwold.spotter.model;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.function.Function;

@ToString
@EqualsAndHashCode
public class Point {
    public final int x;
    public final int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Point(double x, double y) {
        this.x = (int) x;
        this.y = (int) y;
    }

    public Point(Point toCopy) {
        this(toCopy.x, toCopy.y);
    }

    public Point(java.awt.Point from) {
        this(from.x, from.y);
    }

    public Point() {
        this(0, 0);
    }

    public Point translate(int dx, int dy) {
        return new Point(this.x + dx, this.y + dy);
    }

    public Point translate(Point vector) {
        return translate(vector.x, vector.y);
    }

    public Point translate(java.awt.Point vector) {
        return translate(vector.x, vector.y);
    }

    public Point translate(Function<Point, Point> pointModifier) {
        return pointModifier.apply(this);
    }

    public Point scale(Dimension dimension) {
        return new Point(this.x * dimension.width, this.y * dimension.height);
    }

    public Point minus(Point vector) {
        return translate(-vector.x, -vector.y);
    }

    public java.awt.Point toAwt() {
        return new java.awt.Point(this.x, this.y);
    }

    public Dimension toDimension() {
        return new Dimension(this.x, this.y);
    }

    public Rectangle toRectangle(Dimension size) {
        return new Rectangle(this.toAwt(), size);
    }

    public static Point toCenterOf(Rectangle rectangle) {
        return new Point(rectangle.getCenterX(), rectangle.getCenterY());
    }

    public int distance(Point p) {
        return (int) Point2D.distance(this.x, this.y, p.x, p.y);
    }
}
