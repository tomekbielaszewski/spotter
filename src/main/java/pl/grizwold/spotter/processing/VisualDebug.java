package pl.grizwold.spotter.processing;

import lombok.extern.slf4j.Slf4j;

import java.awt.image.BufferedImage;
import java.io.File;
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
            log.debug("Saving debug image to {}", fileName);
            Instant instant = Instant.ofEpochMilli(System.currentTimeMillis());
            String minutes = DateTimeFormatter.ofPattern("HH-mm").format(LocalDateTime.ofInstant(
                    instant, TimeZone.getDefault().toZoneId()));
            String millis = DateTimeFormatter.ofPattern("ss-SSS-").format(LocalDateTime.ofInstant(
                    instant, TimeZone.getDefault().toZoneId()));

            String saveDir = directory + minutes + "/" + millis + fileName + ".png";
            ImageUtil.save(imageProvider.getImage(), saveDir);
        }
    }

    public void clear() {
        if (this.enabled) {
            File file = new File(directory);
            if (file.exists()) {
                boolean deleted = file.delete();
                log.debug("Cleared up debug location: {}", deleted);
            }
        }
    }

    public interface DebugImageProvider {
        BufferedImage getImage();
    }
}
