package ru.vpb.cistagger.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.vpb.cistagger.TierTagger;

import java.util.List;

@Mixin(DisplayEntity.TextDisplayEntity.class)
public abstract class TextDisplayEntityRenderer {
    @ModifyReturnValue(method = "getText", at = @At("RETURN"))
    private Text akdhjg(Text original) {

        String[] a = Formatting.strip(original.getString()).split(" ");
        if (a.length == 0 || a[0].length() < 3) {
            return original;
        }

        return TierTagger.appendTier(Text.literal(a[0]), original);
    }
}
