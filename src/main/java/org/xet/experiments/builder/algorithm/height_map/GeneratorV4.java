package org.xet.experiments.builder.algorithm.height_map;

public class GeneratorV4 extends GeneratorHeightMap {
    @Override
    public int[][] getHeightMap(int width, int height) {
        int[][] heightMap = new int[width][height];

        int minRow = 0;
        int maxRow = width - 1;
        int minCol = 0;
        int maxCol = height - 1;

        int num = 0;

        while (minRow <= maxRow && minCol <= maxCol) {
            for (int j = minCol; j <= maxCol; j++) {
                heightMap[minRow][j] = num;
                num++;
            }
            minRow++;

            for (int i = minRow; i <= maxRow; i++) {
                heightMap[i][maxCol] = num;
                num++;
            }
            maxCol--;

            if (minRow <= maxRow) {
                for (int j = maxCol; j >= minCol; j--) {
                    heightMap[maxRow][j] = num;
                    num++;
                }
                maxRow--;
            }

            if (minCol <= maxCol) {
                for (int i = maxRow; i >= minRow; i--) {
                    heightMap[i][minCol] = num;
                    num++;
                }
                minCol++;
            }
        }

        return heightMap;
    }

    @Override
    public String getName() {
        return "v4";
    }
}
