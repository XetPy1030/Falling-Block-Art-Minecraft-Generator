package org.xet.experiments.builder.algorithm.color;

import org.xet.experiments.builder.data.ImageRGB;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class ColorMap {
    BufferedImage image;

    public ColorMap(BufferedImage image) {
        this.image = image;
    }

    public String[][] getColorMap(int width, int height) {
        BufferedImage resized = null;
        try {
            resized = resizeImage(this.image, width, height);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int[] data = resized.getRGB(0, 0, width, height, null, 0, width);
        Color[][] colors = new Color[width][height];
        for (int i = 0; i < data.length; i++) {
            int x = i % width;
            int y = i / width;
            colors[x][y] = new Color(data[i]);
        }

        String[][] colorStrings = new String[width][height];
        for (int i = 0; i < colors.length; i++) {
            for (int j = 0; j < colors[0].length; j++) {
                Color color = colors[i][j];
                colorStrings[i][j] = getStringColorFromRGB(color.getRed(), color.getGreen(), color.getBlue());
            }
        }

        return colorStrings;
    }

    private static String getStringColorFromRGB(int r, int g, int b) {
        Color targetColor = new Color(r, g, b);

        ImageRGB[] colors = {
                new ImageRGB(Color.WHITE, "white"),
                new ImageRGB(Color.ORANGE, "orange"),
                new ImageRGB(Color.MAGENTA, "magenta"),
                new ImageRGB(Color.LIGHT_GRAY, "light_gray"),
                new ImageRGB(Color.YELLOW, "yellow"),
                new ImageRGB(new Color(204, 255, 0), "lime"),
                new ImageRGB(Color.PINK, "pink"),
                new ImageRGB(Color.GRAY, "gray"),
                new ImageRGB(Color.CYAN, "cyan"),
                new ImageRGB(new Color(153, 0, 153), "purple"),
                new ImageRGB(Color.BLUE, "blue"),
                new ImageRGB(new Color(153, 102, 51), "brown"),
                new ImageRGB(new Color(63, 63, 63), "gray"),
                new ImageRGB(Color.GREEN, "green"),
                new ImageRGB(Color.RED, "red"),
                new ImageRGB(Color.BLACK, "black"),
        };


        Color closestColor = null;
        int minDistance = Integer.MAX_VALUE;

        for (ImageRGB color : colors) {
            int distance = getColorDistance(targetColor, color.color);

            if (distance < minDistance) {
                minDistance = distance;
                closestColor = color.color;
            }
        }

        if (closestColor != null) {
            for (ImageRGB color : colors) {
                if (color.color.equals(closestColor)) {
                    return color.stringColor;
                }
            }
        }

        return "unknown";
    }

    private static int getColorDistance(Color c1, Color c2) {
        int rDiff = c1.getRed() - c2.getRed();
        int gDiff = c1.getGreen() - c2.getGreen();
        int bDiff = c1.getBlue() - c2.getBlue();

        return rDiff * rDiff + gDiff * gDiff + bDiff * bDiff;
    }

    private static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) throws IOException {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = resizedImage.createGraphics();
        graphics2D.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        graphics2D.dispose();

        // save image
//        File pathFile = new File(System.getenv("AppData") + "/.minecraft/testresized.png");
//        ImageIO.write(resizedImage, "png", pathFile);

        return resizedImage;
    }
}
