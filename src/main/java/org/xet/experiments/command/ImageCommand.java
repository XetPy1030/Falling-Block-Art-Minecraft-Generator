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
import net.minecraft.util.math.Vec3d;
import org.xet.experiments.builder.algorithm.BuilderImage;
import org.xet.experiments.builder.algorithm.ImageGetter;
import org.xet.experiments.builder.algorithm.height_map.RegistryGeneratorsHeightMap;

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

    private static int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        assert player != null;

        Vec3d pos = player.getPos();
        ServerWorld serverWorld = player.getWorld();

        BuilderImage builderImage = new BuilderImage(context);
        builderImage.build(pos, serverWorld);

        // ToDo: в треды?
//        org.xet.experiments.ImageCommand.Image image = openFile(context);


//        Thread thread = new Thread(() -> {
//            buildImage(pos, serverWorld, image);
//        });
//        thread.start();
//
//        buildImage(pos, serverWorld, image);

        ItemStack stack = new ItemStack(Items.DIAMOND);
        player.giveItemStack(stack);

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
