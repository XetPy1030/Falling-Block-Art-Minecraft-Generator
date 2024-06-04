package org.xet.experiments.builder.algorithm.height_map;

public class GeneratorV1 extends GeneratorHeightMap {
    @Override
    public int[][] getHeightMap(int width, int height) {
        int[][] heightMap = new int[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                heightMap[x][y] = Math.abs(x - width / 2) + Math.abs(y - height / 2);
            }
        }

        return heightMap;
    }

    @Override
    public String getName() {
        return "v1";
    }
}
