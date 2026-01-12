package dev.onyxcat.escraft.mixin;

import net.minecraft.client.gl.ShaderProgram;
import org.lwjgl.opengles.GLES30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ShaderProgram.class)
public class MixinShaderProgram {

    
    @Redirect(method = "set", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL31;glGetUniformBlockIndex(ILjava/lang/CharSequence;)I"))
    private int redirectGetUniformBlockIndex(int program, CharSequence uniformBlockName) {
        return GLES30.glGetUniformBlockIndex(program, uniformBlockName);
    }

    
    @Redirect(method = "set", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL31;glUniformBlockBinding(III)V"))
    private void redirectUniformBlockBinding(int program, int uniformBlockIndex, int uniformBlockBinding) {
        GLES30.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding);
    }

    
    @Redirect(method = "set", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL31;glGetActiveUniformBlockName(II)Ljava/lang/String;"))
    private String redirectGetActiveUniformBlockName(int program, int uniformBlockIndex) {
        return GLES30.glGetActiveUniformBlockName(program, uniformBlockIndex);
    }
}