package org.xet.experiments;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;

public class Experiments implements ModInitializer {
    @Override
    public void onInitialize() {
        System.out.println("Hello Fabric world!");

        CommandRegistrationCallback.EVENT.register(ImageCommand::register);
    }
}
