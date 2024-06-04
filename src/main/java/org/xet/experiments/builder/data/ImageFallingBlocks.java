package org.xet.experiments.builder.data;

public class ImageFallingBlocks {
    public int width;
    public int height;

    public int[][] heightMap;
    public String[][] colorMap;

    public ImageFallingBlocks(int width, int height, String[][] colorStrings, int[][] heightMap) {
        this.width = width;
        this.height = height;
        this.colorMap = colorStrings;
        this.heightMap = heightMap;
    }
}
