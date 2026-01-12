package dev.onyxcat.escraft.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(targets = "com.mojang.blaze3d.opengl.GlConst", remap = false)
public class MixinGlConst {
    
    @Overwrite
    public static int bufferUsageToGlEnum(int usage) {

        if ((usage & 2) != 0) {
            return 35048;
        }

        if ((usage & 1) != 0) {
            return 35040;
        }
        return 35044;
    }
}