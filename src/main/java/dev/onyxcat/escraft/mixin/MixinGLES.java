package dev.onyxcat.escraft.mixin;

import org.lwjgl.opengles.GLES;
import org.lwjgl.system.FunctionProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = GLES.class, remap = false)
public class MixinGLES {


    @Shadow
    private static FunctionProvider functionProvider;

    
    @Overwrite
    public static void create(FunctionProvider provider) {

        if (functionProvider != null) {
            throw new IllegalStateException("OpenGL ES has already been created.");
        } else {

            functionProvider = provider;

            System.out.println("[ESCraft] GLES.create() 劫持成功：已跳过 ThreadLocal 状态检查。");
        }
    }
}