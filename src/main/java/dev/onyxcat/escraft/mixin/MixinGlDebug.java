package dev.onyxcat.escraft.mixin;

import net.minecraft.client.gl.GlDebug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.jspecify.annotations.Nullable;
import java.util.Set;

@Mixin(value = GlDebug.class, remap = false)
public class MixinGlDebug {

    
    @Overwrite
    public static @Nullable GlDebug enableDebug(int verbosity, boolean sync, Set<String> usedGlCaps) {
        System.out.println("[ESCraft] 已跳过 GlDebug 初始化 (GLES 环境)");
        return null;
    }
}