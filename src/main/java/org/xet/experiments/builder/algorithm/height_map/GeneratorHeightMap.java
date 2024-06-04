package org.xet.experiments.builder.algorithm.height_map;

public abstract class GeneratorHeightMap {
    public abstract int[][] getHeightMap(int width, int height);

    public abstract String getName();
}
