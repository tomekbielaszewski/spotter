package pl.grizwold.screenautomation;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.awt.*;

@ToString
@EqualsAndHashCode
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

    public Point() {
        this(0, 0);
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
}
