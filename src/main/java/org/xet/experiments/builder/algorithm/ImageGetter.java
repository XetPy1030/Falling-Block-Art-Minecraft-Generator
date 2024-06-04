package org.xet.experiments.builder.algorithm;

import net.fabricmc.loader.api.FabricLoader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class ImageGetter {

    public static BufferedImage getImage(String fileName) throws IOException {
        String imagePath = getBasePath() + "/" + fileName;

        BufferedImage image;
        try {
            image = ImageIO.read(new File(imagePath));
        } catch (IOException e) {
            throw new IOException("File not found");
        }

        return image;
    }

    public static String[] getAvailableImages() {
        File folder = new File(getBasePath());
        File[] files = folder.listFiles();
        assert files != null;

        ArrayList<String> images = new ArrayList<>();
        for (File file : files) {
            if (file.isDirectory()) {
                continue;
            }

            if (file.getName().endsWith(".png") || file.getName().endsWith(".jpg"))
                images.add(file.getName());
        }

        return images.toArray(new String[0]);
    }

    private static String getBasePath() {
        return FabricLoader.getInstance().getGameDir().toString();
    }
}
