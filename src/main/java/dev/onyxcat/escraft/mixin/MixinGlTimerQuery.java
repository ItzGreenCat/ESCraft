package dev.onyxcat.escraft.mixin;

import com.mojang.blaze3d.systems.GpuQuery;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.GlTimerQuery;
import org.lwjgl.opengles.GLES30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.OptionalLong;

@Mixin(GlTimerQuery.class)
public abstract class MixinGlTimerQuery implements GpuQuery {

    @Shadow
    private int id;

    @Shadow
    private boolean closed;

    @Shadow
    private OptionalLong value;

    
    @Overwrite
    public OptionalLong getValue() {
        RenderSystem.assertOnRenderThread();

        if (this.closed) {
            throw new IllegalStateException("GlTimerQuery is closed");
        } else if (this.value.isPresent()) {
            return this.value;
        } else {


            int[] available = new int[1];
            GLES30.glGetQueryObjectuiv(this.id, GLES30.GL_QUERY_RESULT_AVAILABLE, available);

            if (available[0] != 0) {

                int[] result = new int[1];
                GLES30.glGetQueryObjectuiv(this.id, GLES30.GL_QUERY_RESULT, result);



                long resultLong = result[0] & 0xFFFFFFFFL;

                this.value = OptionalLong.of(resultLong);
                return this.value;
            } else {
                return OptionalLong.empty();
            }
        }
    }

    
    @Overwrite
    public void close() {
        RenderSystem.assertOnRenderThread();
        if (!this.closed) {
            this.closed = true;



            GLES30.glDeleteQueries(this.id);



        }
    }
}