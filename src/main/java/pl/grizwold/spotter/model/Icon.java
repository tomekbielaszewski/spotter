package pl.grizwold.spotter.model;

import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.sun.jna.Platform.isWindows;

@Slf4j
public class Icon {
    private Path path;
    private BufferedImage image;

    public Icon(Path path) {
        this.path = path;
        try {
            this.image = ImageIO.read(this.path.toFile());
        } catch (IOException e) {
            log.error("Couldn't load image from file {}", path, e);
        }
    }

    public Icon(String path) {
        this(Paths.get(linuxFix(path)));
    }

    public BufferedImage getImage() {
        return image;
    }

    public Point getCenter() {
        Rectangle imageBounds = getBounds();
        return new Point(imageBounds.getCenterX(), imageBounds.getCenterY());
    }

    public Rectangle getBounds() {
        return new Rectangle(new Dimension(image.getWidth(), image.getHeight()));
    }

    public String getFilename() {
        return path.getFileName().toString();
    }

    public Dimension getDimension() {
        return new Dimension(image.getWidth(), image.getHeight());
    }

    private static String linuxFix(String path) {
        if (!isWindows()) {
            path = path.replaceAll("\\\\", "/");
        }
        return path;
    }
}
