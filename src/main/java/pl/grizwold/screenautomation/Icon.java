package pl.grizwold.screenautomation;

import lombok.SneakyThrows;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Icon {
    private Path path;
    private BufferedImage image;

    @SneakyThrows
    public Icon(String path) {
        this.path = Paths.get(path);
        this.image = ImageIO.read(this.path.toFile());
    }

    public BufferedImage getImage() {
        return image;
    }

    public String getFilename() {
        return path.getFileName().toString();
    }
}
