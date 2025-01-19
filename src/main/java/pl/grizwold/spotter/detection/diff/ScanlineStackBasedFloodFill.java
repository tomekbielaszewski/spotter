package pl.grizwold.spotter.detection.diff;

import java.util.Stack;

/**
 * Credits to: https://lodev.org/cgtutor/floodfill.html
 */
public class ScanlineStackBasedFloodFill implements FloodFill {
    @Override
    public void fill(int x, int y, int searchedColor, int replacementColor, int[][] matrix) {
        if (searchedColor == replacementColor) {
            return;
        }
        if (!valid(x, y, matrix)) {
            return;
        }

        int x1;
        boolean spanAbove, spanBelow;

        Stack<Integer> stack = new Stack<>();
        stack.push(x);
        stack.push(y);

        while (!stack.isEmpty()) {
            y = stack.pop();
            x = stack.pop();

            x1 = x;
            while (x1 >= 0 && matrix[y][x1] == searchedColor) x1--;
            x1++;
            spanAbove = spanBelow = false;

            while (valid(x1, y, matrix) && matrix[y][x1] == searchedColor) {
                matrix[y][x1] = replacementColor;

                if (!spanAbove && y > 0 && matrix[y - 1][x1] == searchedColor) {
                    stack.push(x1);
                    stack.push(y - 1);
                    spanAbove = true;
                } else if (spanAbove && y > 0 && matrix[y - 1][x1] != searchedColor) {
                    spanAbove = false;
                }

                if (!spanBelow && y < matrix.length - 1 && matrix[y + 1][x1] == searchedColor) {
                    stack.push(x1);
                    stack.push(y + 1);
                    spanBelow = true;
                } else if (spanBelow && y < matrix.length - 1 && matrix[y + 1][x1] != searchedColor) {
                    spanBelow = false;
                }

                x1++;
            }
        }
    }

    private boolean valid(int x, int y, int[][] matrix) {
        return y >= 0 && y < matrix.length && x >= 0 && x < matrix[y].length;
    }
}
