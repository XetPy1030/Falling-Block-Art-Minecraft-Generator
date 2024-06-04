package org.xet.experiments.builder.algorithm.height_map;

public class GeneratorV1 extends GeneratorHeightMap {
    @Override
    public int[][] getHeightMap(int width, int height) {
        int[][] heightMap = new int[width][height];

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                heightMap[i][j] = Math.abs(i - width / 2) + Math.abs(j - height / 2);
            }
        }

        return heightMap;
    }

    @Override
    public String getName() {
        return "v1";
    }
}
