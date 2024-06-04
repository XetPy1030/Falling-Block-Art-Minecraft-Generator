package org.xet.experiments.builder.algorithm.height_map;

public class GeneratorV2 extends GeneratorHeightMap {
    @Override
    public int[][] getHeightMap(int width, int height) {
        int[][] heightMap = new int[width][height];

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                heightMap[i][j] = Math.abs(i) + Math.abs(j);
            }
        }

        return heightMap;
    }

    @Override
    public String getName() {
        return "v2";
    }
}
