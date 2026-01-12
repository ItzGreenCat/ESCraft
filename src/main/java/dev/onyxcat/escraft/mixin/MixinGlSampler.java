package dev.onyxcat.escraft.mixin;

import net.minecraft.client.gl.GlSampler;
import org.lwjgl.opengles.GLES30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GlSampler.class)
public class MixinGlSampler {

    @Shadow
    private int samplerId;

    @Shadow
    private boolean closed;

    
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL33C;glGenSamplers()I"))
    private int redirectGenSamplers() {
        return GLES30.glGenSamplers();
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL33C;glSamplerParameteri(III)V"))
    private void redirectSamplerParameteri(int sampler, int pname, int param) {
        GLES30.glSamplerParameteri(sampler, pname, param);
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL33C;glSamplerParameterf(IIF)V"))
    private void redirectSamplerParameterf(int sampler, int pname, float param) {
        GLES30.glSamplerParameterf(sampler, pname, param);
    }

    
    @Overwrite
    public void close() {
        if (!this.closed) {
            this.closed = true;
            GLES30.glDeleteSamplers(this.samplerId);
        }
    }
}