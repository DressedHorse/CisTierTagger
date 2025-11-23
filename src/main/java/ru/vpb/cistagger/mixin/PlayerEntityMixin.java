package ru.vpb.cistagger.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import ru.vpb.cistagger.TierTagger;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @ModifyReturnValue(method = "getDisplayName", at = @At("RETURN"))
    public Text appendTier(Text original) {
        PlayerEntity self = (PlayerEntity) (Object) this;

        return TierTagger.appendTier(self.getName(), original);
    }
}
