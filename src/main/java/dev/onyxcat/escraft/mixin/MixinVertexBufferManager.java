package dev.onyxcat.escraft.mixin;

import net.minecraft.client.gl.DebugLabelManager;
import net.minecraft.client.gl.VertexBufferManager;
import org.lwjgl.opengl.GLCapabilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import java.util.Set;

@Mixin(value = VertexBufferManager.class, remap = false)
public abstract class MixinVertexBufferManager {

    
    @Overwrite
    public static VertexBufferManager create(GLCapabilities capabilities, DebugLabelManager labeler, Set<String> usedCapabilities) {
        System.out.println("[ESCraft] 正在为 GLES 创建 VertexBufferManager (强制使用 Default 模式)");
        try {
            Class<?> defaultClass = Class.forName("net.minecraft.client.gl.VertexBufferManager$DefaultVertexBufferManager");
            java.lang.reflect.Constructor<?> constructor = defaultClass.getDeclaredConstructor(DebugLabelManager.class);
            constructor.setAccessible(true);
            return (VertexBufferManager) constructor.newInstance(labeler);
        } catch (Exception e) {
            try {
                Class<?> defaultClass = Class.forName("net.minecraft.class_10869$class_10870");
                java.lang.reflect.Constructor<?> constructor = defaultClass.getDeclaredConstructor(DebugLabelManager.class);
                constructor.setAccessible(true);
                return (VertexBufferManager) constructor.newInstance(labeler);
            } catch (Exception ex) {
                System.err.println("[ESCraft] 无法实例化 DefaultVertexBufferManager!");
                ex.printStackTrace();
                return null;
            }
        }
    }
}