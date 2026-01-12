package dev.onyxcat.escraft.mixin;

import org.lwjgl.opengles.GLES20;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;


@Mixin(targets = "com.mojang.blaze3d.opengl.GlStateManager$CapabilityTracker")
public class MixinCapabilityTracker {

    @Shadow @Final private int cap;
    @Shadow private boolean state;


    private static final int GL_ALPHA_TEST = 0x0BC0;
    private static final int GL_LIGHTING = 0x0B50;
    private static final int GL_COLOR_MATERIAL = 0x0B57;
    private static final int GL_TEXTURE_2D = 0x0DE1;
    private static final int GL_NORMALIZE = 0x0BA1;
    private static final int GL_AUTO_NORMAL = 0x0D80;

    
    @Overwrite
    public void setState(boolean state) {


        if (state != this.state) {
            this.state = state;


            if (this.cap == GL_ALPHA_TEST || this.cap == GL_LIGHTING || this.cap == GL_COLOR_MATERIAL
                    || this.cap == GL_NORMALIZE || this.cap == GL_AUTO_NORMAL || this.cap == GL_TEXTURE_2D) {
                return;
            }

            if (state) {
                GLES20.glEnable(this.cap);
            } else {
                GLES20.glDisable(this.cap);
            }
        }
    }
}