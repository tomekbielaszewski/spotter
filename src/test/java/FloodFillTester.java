import pl.grizwold.spotter.model.Icon;
import pl.grizwold.spotter.detection.diff.FloodFill;
import pl.grizwold.spotter.util.ImageUtil;
import pl.grizwold.spotter.detection.diff.ScanlineStackBasedFloodFill;

import java.awt.*;

public class FloodFillTester {
    public static void main(String[] args) {
        Icon icon = new Icon("src/test/resources/floodfill.png");
        FloodFill floodFill = new ScanlineStackBasedFloodFill();

        Dimension dimension = icon.getDimension();
        int[][] matrix = new int[dimension.width][dimension.height];
        for (int x = 0; x < dimension.width; x++) {
            for (int y = 0; y < dimension.height; y++) {
                matrix[x][y] = icon.getImage().getRGB(x, y);
            }
        }

        int startX = 160;
        int startY = 125;
        int replacementColor = Color.MAGENTA.getRGB();

        long start = System.currentTimeMillis();
        floodFill.fill(startX, startY, icon.getImage().getRGB(startX, startY), replacementColor, matrix);
        long end = System.currentTimeMillis();
        System.out.println(end - start);

        for (int x = 0; x < dimension.width; x++) {
            for (int y = 0; y < dimension.height; y++) {
                icon.getImage().setRGB(x, y, matrix[x][y]);
            }
        }

        ImageUtil.save(icon.getImage(), "src/test/resources/floodfill_done.png");
    }
}
