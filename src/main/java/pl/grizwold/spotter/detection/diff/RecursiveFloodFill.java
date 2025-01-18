package pl.grizwold.spotter.detection.diff;

public class RecursiveFloodFill implements FloodFill {
    @Override
    public void fill(int x, int y, int searchedColor, int replacementColor, int[][] matrix) {
        if (searchedColor == replacementColor) {
            return;
        }

        if (isJumpRejected(x, y, searchedColor, matrix)) {
            return;
        }

        matrix[y][x] = replacementColor;

        fill(x + 1, y, searchedColor, replacementColor, matrix);
        fill(x, y + 1, searchedColor, replacementColor, matrix);
        fill(x, y - 1, searchedColor, replacementColor, matrix);
        fill(x - 1, y, searchedColor, replacementColor, matrix);
        fill(x + 1, y - 1, searchedColor, replacementColor, matrix);
        fill(x - 1, y + 1, searchedColor, replacementColor, matrix);
        fill(x + 1, y + 1, searchedColor, replacementColor, matrix);
        fill(x - 1, y - 1, searchedColor, replacementColor, matrix);
    }

    private boolean isJumpRejected(int x, int y, int targetGroup, int[][] matrix) {
        return y < 0 || y >= matrix.length || x < 0 || x >= matrix[y].length || matrix[y][x] != targetGroup;
    }
}
