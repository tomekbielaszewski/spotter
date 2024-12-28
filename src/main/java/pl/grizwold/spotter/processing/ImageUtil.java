package pl.grizwold.spotter.processing;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

@UtilityClass
public class ImageUtil {
    @Nonnull
    public static BufferedImage copy(@Nonnull BufferedImage image) {
        ColorModel cm = image.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = image.copyData(image.getRaster().createCompatibleWritableRaster());
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    @SneakyThrows
    public static void save(@Nonnull BufferedImage image, @Nonnull String filepath) {
        File file = new File(filepath);
        if (file.exists()) {
            file.delete();
        }
        Files.createDirectories(Paths.get(filepath));
        ImageIO.write(image, "png", file);
    }

    @SneakyThrows
    public static BufferedImage read(String filepath) {
        return ImageIO.read(Paths.get(filepath).toFile());
    }
}
