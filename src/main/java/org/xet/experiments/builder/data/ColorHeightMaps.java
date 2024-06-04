package org.xet.experiments.builder.data;

import org.xet.experiments.builder.algorithm.color.ImageColorEnum;

public record ColorHeightMaps(int width, int height, ImageColorEnum[][] colorMap, int[][] heightMap) {
}
