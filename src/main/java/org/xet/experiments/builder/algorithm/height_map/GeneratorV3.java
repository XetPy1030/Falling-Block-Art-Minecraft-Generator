package org.xet.experiments.builder.algorithm.height_map;

public class GeneratorV3 extends GeneratorHeightMap {
    @Override
    public int[][] getHeightMap(int width, int height) {
        int[][] heightMap = new int[width][height];

        for (int x = 0; x < width; x++) {
            if (x % 2 == 0) {
                for (int y = 0; y < height; y++) {
                    heightMap[x][y] = x * height + y;
                }
            } else {
                for (int y = height - 1; y >= 0; y--) {
                    heightMap[x][y] = (x + 1) * height - y - 1;
                }
            }
        }

        return heightMap;
    }

    @Override
    public String getName() {
        return "v3";
    }
}
