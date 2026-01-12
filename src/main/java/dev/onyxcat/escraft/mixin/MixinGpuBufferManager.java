package dev.onyxcat.escraft.mixin;

import net.minecraft.client.gl.GpuBufferManager;
import org.lwjgl.opengl.GLCapabilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import java.util.Set;

@Mixin(value = GpuBufferManager.class, remap = false)
public abstract class MixinGpuBufferManager {

    
    @Overwrite
    public static GpuBufferManager create(GLCapabilities capabilities, Set<String> usedCapabilities) {



        System.out.println("[ESCraft] 正在为 GLES 创建 GpuBufferManager (强制使用 Direct 模式)");

        try {

            Class<?> directClass = Class.forName("net.minecraft.client.gl.GpuBufferManager$DirectGpuBufferManager");
            java.lang.reflect.Constructor<?> constructor = directClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return (GpuBufferManager) constructor.newInstance();
        } catch (Exception e) {
            try {

                Class<?> directClass = Class.forName("net.minecraft.class_11266$class_11268");
                java.lang.reflect.Constructor<?> constructor = directClass.getDeclaredConstructor();
                constructor.setAccessible(true);
                return (GpuBufferManager) constructor.newInstance();
            } catch (Exception ex) {
                System.err.println("[ESCraft] 无法实例化 DirectGpuBufferManager!");
                ex.printStackTrace();
                return null;
            }
        }
    }
}