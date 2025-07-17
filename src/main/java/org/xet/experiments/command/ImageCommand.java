package org.xet.experiments.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xet.experiments.builder.algorithm.BuilderImage;
import org.xet.experiments.builder.algorithm.ImageGetter;
import org.xet.experiments.builder.algorithm.height_map.RegistryGeneratorsHeightMap;

import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;

public class ImageCommand {
    private static final Logger LOGGER = LogManager.getLogger(ImageCommand.class);

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

    private static int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        assert player != null;

        Vec3d pos = player.getPos();
        ServerWorld serverWorld = player.getWorld();
        ServerCommandSource source = context.getSource();

        LOGGER.info("Игрок {} выполняет команду генерации изображения в позиции {}", 
                   player.getName().getString(), pos);

        try {
            BuilderImage builderImage = new BuilderImage(context);
            
            source.sendMessage(Text.literal("§eЗапуск генерации изображения..."));
            
            builderImage.buildAsync(pos, serverWorld, source)
                .exceptionally(throwable -> {
                    LOGGER.error("Критическая ошибка при выполнении команды image", throwable);
                    source.sendMessage(Text.literal("§cКритическая ошибка: " + throwable.getMessage()));
                    return null;
                });

            ItemStack stack = new ItemStack(Items.DIAMOND);
            player.giveItemStack(stack);
            
            source.sendMessage(Text.literal("§aКоманда успешно запущена! Генерация выполняется в фоновом режиме."));

        } catch (Exception e) {
            LOGGER.error("Ошибка при создании BuilderImage", e);
            source.sendMessage(Text.literal("§cОшибка при инициализации: " + e.getMessage()));
            return 0;
        }

        return 1;
    }

    static class AttributeTypeHeightSuggestionProvider implements SuggestionProvider<ServerCommandSource> {
        private final String[] names = RegistryGeneratorsHeightMap.getAvailableNames();

        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
            for (String name : names) {
                builder.suggest(name);
            }
            return builder.buildFuture();
        }
    }

    static class AttributeFileSuggestionProvider implements SuggestionProvider<ServerCommandSource> {

        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
            for (String file : ImageGetter.getAvailableImages()) {
                builder.suggest(file);
            }

            return builder.buildFuture();
        }
    }

}
