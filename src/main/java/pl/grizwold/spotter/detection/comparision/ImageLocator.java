package pl.grizwold.spotter.detection.comparision;

import pl.grizwold.spotter.model.Icon;
import pl.grizwold.spotter.model.Point;

import javax.annotation.Nonnull;
import java.util.List;

public interface ImageLocator {
    List<Point> locate(@Nonnull final Icon icon);
}
