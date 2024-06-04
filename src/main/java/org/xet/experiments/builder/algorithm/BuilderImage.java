package org.xet.experiments.builder.algorithm;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.xet.experiments.builder.algorithm.color.ColorMap;
import org.xet.experiments.builder.algorithm.height_map.GeneratorHeightMap;
import org.xet.experiments.builder.algorithm.height_map.RegistryGeneratorsHeightMap;
import org.xet.experiments.builder.data.ImageFallingBlocks;

import java.awt.image.BufferedImage;
import java.io.IOException;

public class BuilderImage {
    ImageFallingBlocks fallingBlocks;

    public BuilderImage(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String fileName = context.getArgument("file", String.class);
        int width = context.getArgument("width", Integer.class);
        int height = context.getArgument("height", Integer.class);
        String generatorName = context.getArgument("typeHeightMap", String.class);

        fallingBlocks = getImageFallingBlocks(fileName, width, height, generatorName);
    }

    public void build(Vec3d pos, ServerWorld serverWorld) {
        buildImage(pos, serverWorld, fallingBlocks);
    }

    private ImageFallingBlocks getImageFallingBlocks(String fileName, int width, int height, String generatorName) throws CommandSyntaxException {
        BufferedImage image;
        try {
            image = ImageGetter.getImage(fileName);
        } catch (IOException e) {
            Text textError = Text.literal(e.getMessage());
            throw new SimpleCommandExceptionType(textError).create();
        }

        ColorMap colorMap = new ColorMap(image);
        String[][] colorStrings = colorMap.getColorMap(width, height);

        GeneratorHeightMap generator = RegistryGeneratorsHeightMap.getGeneratorByName(generatorName);
        int[][] heightMap = generator.getHeightMap(width, height);

        return new ImageFallingBlocks(width, height, colorStrings, heightMap);
    }



    private static void buildImage(Vec3d pos, ServerWorld serverWorld, ImageFallingBlocks image) {
        for (int i = 0; i < image.width; i++) {
            for (int j = 0; j < image.height; j++) {
                int x = (int) pos.x + i;
                int y = (int) pos.y + image.heightMap[i][j];
                int z = (int) pos.z + j;

                if (image.heightMap[i][j] == 0) {
                    serverWorld.setBlockState(new BlockPos(x, y, z), Blocks.TORCH.getDefaultState());
                }

                y += 1;

                serverWorld.setBlockState(new BlockPos(x, y, z), getBlockWithColor(image.colorMap[i][j]));

                if (i + 1 < image.width && image.heightMap[i + 1][j] - 1 == image.heightMap[i][j]) {
                    BlockState state = Blocks.WALL_TORCH.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.EAST);
                    serverWorld.setBlockState(new BlockPos(x + 1, y, z), state);
                }
                if (i - 1 >= 0 && image.heightMap[i - 1][j] - 1 == image.heightMap[i][j]) {
                    BlockState state = Blocks.WALL_TORCH.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.WEST);
                    serverWorld.setBlockState(new BlockPos(x - 1, y, z), state);
                }
                if (j + 1 < image.height && image.heightMap[i][j + 1] - 1 == image.heightMap[i][j]) {
                    BlockState state = Blocks.WALL_TORCH.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.SOUTH);
                    serverWorld.setBlockState(new BlockPos(x, y, z + 1), state);
                }
                if (j - 1 >= 0 && image.heightMap[i][j - 1] - 1 == image.heightMap[i][j]) {
                    BlockState state = Blocks.WALL_TORCH.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH);
                    serverWorld.setBlockState(new BlockPos(x, y, z - 1), state);
                }

            }
        }
    }

    private static BlockState getBlockWithColor(String color) {
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
}
