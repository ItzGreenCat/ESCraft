package dev.onyxcat.escraft.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.client.gl.GlTimerQuery;

@Mixin(GlTimerQuery.class)
public interface GlTimerQueryInvoker {

    @Invoker("<init>")
    static GlTimerQuery create(int id) {
        throw new AssertionError();
    }
}