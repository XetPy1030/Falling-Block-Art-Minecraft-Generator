package org.xet.experiments.builder.algorithm.height_map;

public class GeneratorV3 extends GeneratorHeightMap {
    @Override
    public int[][] getHeightMap(int width, int height) {
        int[][] heightMap = new int[width][height];

        for (int i = 0; i < width; i++) {
            if (i % 2 == 0) {
                for (int j = 0; j < height; j++) {
                    heightMap[i][j] = i * height + j;
                }
            } else {
                for (int j = height - 1; j >= 0; j--) {
                    heightMap[i][j] = i * height + (height - j - 1);
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
