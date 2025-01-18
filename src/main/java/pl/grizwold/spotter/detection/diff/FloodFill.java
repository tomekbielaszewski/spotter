package pl.grizwold.spotter.detection.diff;

public interface FloodFill {
    void fill(int x, int y, int searchedColor, int replacementColor, int[][] matrix);
}
