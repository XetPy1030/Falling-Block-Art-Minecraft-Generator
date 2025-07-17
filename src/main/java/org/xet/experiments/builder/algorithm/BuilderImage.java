package org.xet.experiments.builder.algorithm;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xet.experiments.builder.algorithm.color.ColorMap;
import org.xet.experiments.builder.algorithm.color.ImageColorEnum;
import org.xet.experiments.builder.algorithm.height_map.GeneratorHeightMap;
import org.xet.experiments.builder.algorithm.height_map.RegistryGeneratorsHeightMap;
import org.xet.experiments.builder.data.ColorHeightMaps;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;

public class BuilderImage {
    private static final Logger LOGGER = LogManager.getLogger(BuilderImage.class);
    private static final int BLOCKS_PER_TICK = 15;
    private static final int GENERATION_TIMEOUT_SECONDS = 30;
    
    private ColorHeightMaps fallingBlocks;
    private final String fileName;
    private final int width;
    private final int height;
    private final String generatorName;
    
    // Для постепенного размещения блоков
    private static final List<BlockPlacementTask> activeTasks = new ArrayList<>();
    private static boolean tickListenerRegistered = false;

    public BuilderImage(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        this.fileName = context.getArgument("file", String.class);
        this.width = context.getArgument("width", Integer.class);
        this.height = context.getArgument("height", Integer.class);
        this.generatorName = context.getArgument("typeHeightMap", String.class);
        
        LOGGER.info("Создание BuilderImage: файл={}, размер={}x{}", fileName, width, height);
    }

    /**
     * Асинхронная генерация с таймаутом и последующее постепенное размещение блоков
     */
    public CompletableFuture<Void> buildAsync(Vec3d pos, ServerWorld serverWorld, ServerCommandSource source) {
        LOGGER.info("Запуск асинхронной генерации изображения...");
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOGGER.info("Начало генерации...");
                ColorHeightMaps result = getImageFallingBlocks(fileName, width, height, generatorName);
                LOGGER.info("Генерация завершена");
                return result;
            } catch (CommandSyntaxException e) {
                LOGGER.error("Ошибка при генерации: {}", e.getMessage());
                throw new RuntimeException(e);
            }
        }, Executors.newSingleThreadExecutor())
        .orTimeout(GENERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .thenAccept(result -> {
            this.fallingBlocks = result;
            source.sendMessage(Text.literal("§aГенерация завершена! Начинаем размещение блоков..."));
            
            // Запускаем постепенное размещение блоков
            startGradualBlockPlacement(pos, serverWorld, source);
        })
        .exceptionally(throwable -> {
            if (throwable instanceof TimeoutException) {
                LOGGER.error("Генерация превысила таймаут {} секунд", GENERATION_TIMEOUT_SECONDS);
                source.sendMessage(Text.literal("§cОшибка: Генерация превысила таймаут " + GENERATION_TIMEOUT_SECONDS + " секунд"));
            } else {
                LOGGER.error("Ошибка при генерации", throwable);
                source.sendMessage(Text.literal("§cОшибка при генерации: " + throwable.getMessage()));
            }
            return null;
        });
    }

    /**
     * Запуск постепенного размещения блоков
     */
    private void startGradualBlockPlacement(Vec3d pos, ServerWorld serverWorld, ServerCommandSource source) {
        if (!tickListenerRegistered) {
            registerTickListener();
        }
        
        List<BlockPlacement> blocks = prepareBlockPlacements(pos, fallingBlocks);
        
        BlockPlacementTask task = new BlockPlacementTask(blocks, serverWorld, source);
        activeTasks.add(task);
        
        source.sendMessage(Text.literal("§eНачинаем размещение " + blocks.size() + " блоков..."));
    }

    /**
     * Регистрация обработчика тиков сервера
     */
    private static void registerTickListener() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            activeTasks.removeIf(task -> {
                if (task.isCompleted()) {
                    return true;
                }
                
                task.processNextBatch();
                return false;
            });
        });
        tickListenerRegistered = true;
    }

    /**
     * Подготовка списка блоков для размещения (снизу вверх)
     */
    private List<BlockPlacement> prepareBlockPlacements(Vec3d pos, ColorHeightMaps image) {
        List<BlockPlacement> blocks = new ArrayList<>();
        
        for (int i = 0; i < image.width(); i++) {
            for (int j = 0; j < image.height(); j++) {
                int x = (int) pos.x + i;
                int y = (int) pos.y + image.heightMap()[i][j];
                int z = (int) pos.z + j;

                // Основной блок
                if (image.heightMap()[i][j] == 0) {
                    blocks.add(new BlockPlacement(
                        new BlockPos(x, y, z), 
                        Blocks.TORCH.getDefaultState()
                    ));
                }

                y += 1;
                blocks.add(new BlockPlacement(
                    new BlockPos(x, y, z), 
                    getBlockWithColor(image.colorMap()[i][j])
                ));

                // Настенные факелы
                addWallTorchIfNeeded(blocks, image, i, j, x, y, z);
            }
        }
        
        // Сортируем блоки по Y координате (снизу вверх)
        blocks.sort(Comparator.comparingInt(a -> a.pos.getY()));
        
        return blocks;
    }

    /**
     * Добавление настенных факелов при необходимости
     */
    private void addWallTorchIfNeeded(List<BlockPlacement> blocks, ColorHeightMaps image, int i, int j, int x, int y, int z) {
        if (i + 1 < image.width() && image.heightMap()[i + 1][j] - 1 == image.heightMap()[i][j]) {
            BlockState state = Blocks.WALL_TORCH.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.EAST);
            blocks.add(new BlockPlacement(new BlockPos(x + 1, y, z), state));
        }
        if (i - 1 >= 0 && image.heightMap()[i - 1][j] - 1 == image.heightMap()[i][j]) {
            BlockState state = Blocks.WALL_TORCH.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.WEST);
            blocks.add(new BlockPlacement(new BlockPos(x - 1, y, z), state));
        }
        if (j + 1 < image.height() && image.heightMap()[i][j + 1] - 1 == image.heightMap()[i][j]) {
            BlockState state = Blocks.WALL_TORCH.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.SOUTH);
            blocks.add(new BlockPlacement(new BlockPos(x, y, z + 1), state));
        }
        if (j - 1 >= 0 && image.heightMap()[i][j - 1] - 1 == image.heightMap()[i][j]) {
            BlockState state = Blocks.WALL_TORCH.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH);
            blocks.add(new BlockPlacement(new BlockPos(x, y, z - 1), state));
        }
    }

    private static BlockState getBlockWithColor(ImageColorEnum color) {
        return switch (color) {
            case BLACK -> Blocks.BLACK_CONCRETE_POWDER.getDefaultState();
            case BLUE -> Blocks.BLUE_CONCRETE_POWDER.getDefaultState();
            case BROWN -> Blocks.BROWN_CONCRETE_POWDER.getDefaultState();
            case CYAN -> Blocks.CYAN_CONCRETE_POWDER.getDefaultState();
            case GRAY -> Blocks.GRAY_CONCRETE_POWDER.getDefaultState();
            case GREEN -> Blocks.GREEN_CONCRETE_POWDER.getDefaultState();
            case LIGHT_GRAY -> Blocks.LIGHT_GRAY_CONCRETE_POWDER.getDefaultState();
            case LIME -> Blocks.LIME_CONCRETE_POWDER.getDefaultState();
            case MAGENTA -> Blocks.MAGENTA_CONCRETE_POWDER.getDefaultState();
            case ORANGE -> Blocks.ORANGE_CONCRETE_POWDER.getDefaultState();
            case PINK -> Blocks.PINK_CONCRETE_POWDER.getDefaultState();
            case PURPLE -> Blocks.PURPLE_CONCRETE_POWDER.getDefaultState();
            case RED -> Blocks.RED_CONCRETE_POWDER.getDefaultState();
            case WHITE -> Blocks.WHITE_CONCRETE_POWDER.getDefaultState();
            case YELLOW -> Blocks.YELLOW_CONCRETE_POWDER.getDefaultState();
            default -> Blocks.AIR.getDefaultState();
        };
    }

    private ColorHeightMaps getImageFallingBlocks(String fileName, int width, int height, String generatorName) throws CommandSyntaxException {
        BufferedImage image;
        try {
            image = ImageGetter.getImage(fileName);
        } catch (IOException e) {
            LOGGER.error("Ошибка загрузки изображения: {}", e.getMessage());
            Text textError = Text.literal(e.getMessage());
            throw new SimpleCommandExceptionType(textError).create();
        }

        ColorMap colorMap = new ColorMap(image);
        ImageColorEnum[][] colorStrings = colorMap.getColorMap(width, height);

        GeneratorHeightMap generator = RegistryGeneratorsHeightMap.getGeneratorByName(generatorName);
        int[][] heightMap = generator.getHeightMap(width, height);

        return new ColorHeightMaps(width, height, colorStrings, heightMap);
    }

    /**
     * Класс для хранения информации о размещении блока
     */
    private static class BlockPlacement {
        final BlockPos pos;
        final BlockState state;

        BlockPlacement(BlockPos pos, BlockState state) {
            this.pos = pos;
            this.state = state;
        }
    }

    /**
     * Задача для постепенного размещения блоков
     */
    private static class BlockPlacementTask {
        private final List<BlockPlacement> blocks;
        private final ServerWorld serverWorld;
        private final ServerCommandSource source;
        private int currentIndex = 0;
        private final int totalBlocks;
        private int lastReportedPercent = 0;

        BlockPlacementTask(List<BlockPlacement> blocks, ServerWorld serverWorld, ServerCommandSource source) {
            this.blocks = blocks;
            this.serverWorld = serverWorld;
            this.source = source;
            this.totalBlocks = blocks.size();
        }

        void processNextBatch() {
            int processed = 0;
            
            while (currentIndex < blocks.size() && processed < BLOCKS_PER_TICK) {
                BlockPlacement placement = blocks.get(currentIndex);
                serverWorld.setBlockState(placement.pos, placement.state);
                currentIndex++;
                processed++;
            }

            // Отчет о прогрессе каждые 25%
            int currentPercent = (currentIndex * 100) / totalBlocks;
            if (currentPercent >= lastReportedPercent + 25) {
                lastReportedPercent = currentPercent;
                source.sendMessage(Text.literal("§eПрогресс размещения: " + currentPercent + "%"));
            }

            if (isCompleted()) {
                source.sendMessage(Text.literal("§aРазмещение блоков завершено!"));
            }
        }

        boolean isCompleted() {
            return currentIndex >= blocks.size();
        }
    }
}
