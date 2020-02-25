package pl.grizwold.screenautomation;

import java.awt.*;
import java.util.Objects;

public class Point {
    public final int x;
    public final int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Point(Point toCopy) {
        this.x = toCopy.x;
        this.y = toCopy.y;
    }

    public Point(java.awt.Point from) {
        this(from.x, from.y);
    }

    public Point translate(int dx, int dy) {
        return new Point(this.x + dx, this.y + dy);
    }

    public Point translate(Point vector) {
        return translate(vector.x, vector.y);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Point point = (Point) o;
        return x == point.x &&
                y == point.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}
