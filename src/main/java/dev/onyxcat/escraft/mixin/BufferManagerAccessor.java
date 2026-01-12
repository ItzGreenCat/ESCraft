package dev.onyxcat.escraft.mixin;

import net.minecraft.client.gl.BufferManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BufferManager.class)
public interface BufferManagerAccessor {
    @Invoker("createFramebuffer")
    int getCreateFramebuffer();
}
