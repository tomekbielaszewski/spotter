package pl.grizwold.screenautomation;

public class LoggerConfig {
    public static void logLevel(String level) {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", level);
    }
}
