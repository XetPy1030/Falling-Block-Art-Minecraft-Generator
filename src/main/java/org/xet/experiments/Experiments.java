package org.xet.experiments;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.xet.experiments.command.ImageCommand;

public class Experiments implements ModInitializer {
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(ImageCommand::register);
    }
}
