package pl.grizwold.spotter.util;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.TimeZone;

@Slf4j
public class VisualDebug {
    private static final String DEBUG_ENABLE_ENV_VAR = "SPOTTER_DEBUG_ENABLED";
    private static final String DEBUG_DIRECTORY_ENV_VAR = "SPOTTER_DEBUG_DIRECTORY";

    private static final String DEFAULT_DIRECTORY = "visual-debug/";
    private static final boolean DEFAULT_DEBUG = false;

    private final boolean enabled;
    private final String directory;

    public VisualDebug() {
        this.enabled = Optional.ofNullable(System.getenv(DEBUG_ENABLE_ENV_VAR))
                .map(Boolean::parseBoolean)
                .orElse(DEFAULT_DEBUG);
        this.directory = Optional.ofNullable(System.getenv(DEBUG_DIRECTORY_ENV_VAR))
                .map(directory -> directory + (directory.endsWith("/") ? "" : "/"))
                .orElse(DEFAULT_DIRECTORY);
    }

    public void saveDebugImage(BufferedImage image, String fileName) {
        this.saveDebugImage(() -> image, fileName);
    }

    public void saveDebugImage(DebugImageProvider imageProvider, String fileName) {
        if (this.enabled) {
            long start = System.currentTimeMillis();
            Instant instant = Instant.ofEpochMilli(start);
            String minutes = DateTimeFormatter.ofPattern("HH-mm").format(LocalDateTime.ofInstant(
                    instant, TimeZone.getDefault().toZoneId()));
            String millis = DateTimeFormatter.ofPattern("ss-SSS-").format(LocalDateTime.ofInstant(
                    instant, TimeZone.getDefault().toZoneId()));

            fileName = millis + fileName + ".png";
            String saveDir = directory + minutes + "/" + fileName;
            ImageUtil.save(imageProvider.getImage(), saveDir);
            log.debug("Saving debug image to \"{}\" took {}ms", fileName, (System.currentTimeMillis() - start));
        }
    }

    @SneakyThrows
    public void clear() {
        if (this.enabled) {
            Path debugDirectory = Paths.get(directory);
            FileUtils.deleteDirectory(debugDirectory.toFile());
            log.debug("Cleared up debug location: {}", debugDirectory.toAbsolutePath());
        }
    }

    public interface DebugImageProvider {
        BufferedImage getImage();
    }
}