package ru.vpb.cistagger.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.vpb.cistagger.config.ConfigManager;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method = "stop", at = @At("HEAD"))
    private void hookOnStop(CallbackInfo ci) {
        try {
            ConfigManager.save();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
