package org.xet.experiments;

import com.google.gson.Gson;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;


public class ImageCommand {
    public static void register(CommandDispatcher<ServerCommandSource> serverCommandSourceCommandDispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
        serverCommandSourceCommandDispatcher.register(CommandManager.literal("image")
                .then(argument("width", IntegerArgumentType.integer())
                        .then(argument("height", IntegerArgumentType.integer())
                                .then(argument("typeHeightMap", StringArgumentType.word())
                                        .suggests(new AttributeTypeHeightSuggestionProvider())
                                        .then(argument("file", StringArgumentType.word())
                                                .suggests(new AttributeFileSuggestionProvider())
                                                .executes(ImageCommand::run)
                                        )
                                )
                        )
                )
        );
    }

    static class AttributeTypeHeightSuggestionProvider implements SuggestionProvider<ServerCommandSource> {
        private final String[] array = {"v1", "v2", "v3", "v4"};

        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
            for (String s : array) {
                builder.suggest(s);
            }
            return builder.buildFuture();
        }
    }

    static class AttributeFileSuggestionProvider implements SuggestionProvider<ServerCommandSource> {

        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
            String path = System.getenv("AppData") + "/.minecraft";
            File folder = new File(path);

            File[] files = folder.listFiles();
            assert files != null;
            for (final File fileEntry : files) {
                if (!fileEntry.isDirectory()) {
                    if (fileEntry.getName().endsWith(".png") || fileEntry.getName().endsWith(".jpg"))
                        builder.suggest(fileEntry.getName());
                }
            }

            return builder.buildFuture();
        }
    }

    private static int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        assert player != null;
        Vec3d pos = player.getPos();
        ServerWorld serverWorld = player.getWorld();
        ItemStack stack = new ItemStack(Items.DIAMOND);
        player.giveItemStack(stack);

        Image image = openFile(context);

        Thread thread = new Thread(() -> {
            buildImage(pos, serverWorld, image);
        });
        thread.start();

        buildImage(pos, serverWorld, image);

        return 1;
    }

    private static void buildImage(Vec3d pos, ServerWorld serverWorld, Image image) {
        for (int i = 0; i < image.width; i++) {
            for (int j = 0; j < image.height; j++) {
                int x = (int) pos.x + i;
                int y = (int) pos.y + image.heightMap[i][j];
                int z = (int) pos.z + j;

                if (image.heightMap[i][j] == 0) {
                    serverWorld.setBlockState(new BlockPos(x, y, z), Blocks.TORCH.getDefaultState());
                }

                y += 1;

//                serverWorld.setBlockState(new BlockPos(x, y, z), Blocks.SAND.getDefaultState());
                serverWorld.setBlockState(new BlockPos(x, y, z), getBlockWithColor(image.colorMap[i][j]));

                if (i+1 < image.width && image.heightMap[i+1][j]-1 == image.heightMap[i][j]) {
                    BlockState state = Blocks.WALL_TORCH.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.EAST);
                        serverWorld.setBlockState(new BlockPos(x+1, y, z), state);
                }
                if (i-1 >= 0 && image.heightMap[i-1][j]-1 == image.heightMap[i][j]) {
                    BlockState state = Blocks.WALL_TORCH.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.WEST);
                    serverWorld.setBlockState(new BlockPos(x-1, y, z), state);
                }
                if (j+1 < image.height && image.heightMap[i][j+1]-1 == image.heightMap[i][j]) {
                    BlockState state = Blocks.WALL_TORCH.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.SOUTH);
                    serverWorld.setBlockState(new BlockPos(x, y, z+1), state);
                }
                if (j-1 >= 0 && image.heightMap[i][j-1]-1 == image.heightMap[i][j]) {
                    BlockState state = Blocks.WALL_TORCH.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH);
                    serverWorld.setBlockState(new BlockPos(x, y, z-1), state);
                }

            }
        }
    }

    public static BlockState getBlockWithColor(String color) {
        return switch (color) {
            case "black" -> Blocks.BLACK_CONCRETE_POWDER.getDefaultState();
            case "blue" -> Blocks.BLUE_CONCRETE_POWDER.getDefaultState();
            case "brown" -> Blocks.BROWN_CONCRETE_POWDER.getDefaultState();
            case "cyan" -> Blocks.CYAN_CONCRETE_POWDER.getDefaultState();
            case "gray" -> Blocks.GRAY_CONCRETE_POWDER.getDefaultState();
            case "green" -> Blocks.GREEN_CONCRETE_POWDER.getDefaultState();
            case "light_blue" -> Blocks.LIGHT_BLUE_CONCRETE_POWDER.getDefaultState();
            case "light_gray" -> Blocks.LIGHT_GRAY_CONCRETE_POWDER.getDefaultState();
            case "lime" -> Blocks.LIME_CONCRETE_POWDER.getDefaultState();
            case "magenta" -> Blocks.MAGENTA_CONCRETE_POWDER.getDefaultState();
            case "orange" -> Blocks.ORANGE_CONCRETE_POWDER.getDefaultState();
            case "pink" -> Blocks.PINK_CONCRETE_POWDER.getDefaultState();
            case "purple" -> Blocks.PURPLE_CONCRETE_POWDER.getDefaultState();
            case "red" -> Blocks.RED_CONCRETE_POWDER.getDefaultState();
            case "white" -> Blocks.WHITE_CONCRETE_POWDER.getDefaultState();
            case "yellow" -> Blocks.YELLOW_CONCRETE_POWDER.getDefaultState();
            default -> Blocks.AIR.getDefaultState();
        };
    }

    public static Image openFile(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String fileName = context.getArgument("file", String.class);
        String imagePath = System.getenv("AppData") + "/.minecraft/" + fileName;
        System.out.println(System.getenv("AppData"));
        System.out.println(imagePath);


        BufferedImage image = null;
        try {
            image = ImageIO.read(new File(imagePath));
        } catch (IOException e) {
            Text textError = Text.literal("File not found");
            throw new SimpleCommandExceptionType(textError).create();
        }


        try {
            int resizeWidth = context.getArgument("width", Integer.class);
            int resizeHeight = context.getArgument("height", Integer.class);
            image = resizeImage(image, resizeWidth, resizeHeight);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }



        int width = image.getWidth();
        int height = image.getHeight();
        int[] data = image.getRGB(0, 0, width, height, null, 0, width);
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

        int[][] heightMap = generateHeightMap(context.getArgument("typeHeightMap", String.class), width, height);


        return new Image(width, height, colorStrings, heightMap);
    }

    public static int[][] generateHeightMap(String type, int width, int height) {
        return switch (type) {
            case "v1" -> generatorHeightMapV1(width, height);
            case "v2" -> generatorHeightMapV2(width, height);
            case "v3" -> generatorHeightMapV3(width, height);
            case "v4" -> generatorHeightMapV4(width, height);
            default -> null;
        };
    }

    public static int[][] generatorHeightMapV1(int width, int height) {
        int[][] heightMap = new int[width][height];

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                heightMap[i][j] = Math.abs(i - width / 2) + Math.abs(j - height / 2);
            }
        }

        return heightMap;
    }

    public static int[][] generatorHeightMapV2(int width, int height) {
        int[][] heightMap = new int[width][height];

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                heightMap[i][j] = Math.abs(i) + Math.abs(j);
            }
        }

        return heightMap;
    }

    public static int[][] generatorHeightMapV3(int width, int height) {
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

    public static int[][] generatorHeightMapV4(int width, int height) {
        int[][] heightMap = new int[width][height];

        int minRow = 0;
        int maxRow = width - 1;
        int minCol = 0;
        int maxCol = height - 1;

        int num = 0;

        while (minRow <= maxRow && minCol <= maxCol) {
            for (int j = minCol; j <= maxCol; j++) {
                heightMap[minRow][j] = num;
                num++;
            }
            minRow++;

            for (int i = minRow; i <= maxRow; i++) {
                heightMap[i][maxCol] = num;
                num++;
            }
            maxCol--;

            if (minRow <= maxRow) {
                for (int j = maxCol; j >= minCol; j--) {
                    heightMap[maxRow][j] = num;
                    num++;
                }
                maxRow--;
            }

            if (minCol <= maxCol) {
                for (int i = maxRow; i >= minRow; i--) {
                    heightMap[i][minCol] = num;
                    num++;
                }
                minCol++;
            }
        }

        return heightMap;
    }

    public static class ImageRGB {
        public Color color;
        public String stringColor;

        public ImageRGB(Color color, String stringColor) {
            this.color = color;
            this.stringColor = stringColor;
        }
    }

    public static String getStringColorFromRGB(int r, int g, int b) {
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

    public static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) throws IOException {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = resizedImage.createGraphics();
        graphics2D.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        graphics2D.dispose();

        // save image
        File pathFile = new File(System.getenv("AppData") + "/.minecraft/testresized.png");
        ImageIO.write(resizedImage, "png", pathFile);
        return resizedImage;
    }

    public static String readFile(File file) {
        String text = "";
        try(BufferedReader br = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            text = sb.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return text;
    }

    public static class Image {
        public int width;
        public int height;
        public int[][] heightMap;

        public String[][] colorMap;

        public Image(int width, int height, String[][] colorStrings, int[][] heightMap) {
            this.width = width;
            this.height = height;
            this.colorMap = colorStrings;
            this.heightMap = heightMap;
        }
    }
}