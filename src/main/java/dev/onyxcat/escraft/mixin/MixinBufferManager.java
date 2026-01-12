package dev.onyxcat.escraft.mixin;

import net.minecraft.client.gl.BufferManager;
import net.minecraft.client.gl.GpuDeviceInfo;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengles.GLES20;
import org.lwjgl.opengles.GLES30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Set;

@Mixin(value = BufferManager.class, remap = false)
public abstract class MixinBufferManager {

    
    @Overwrite
    public static BufferManager create(GLCapabilities capabilities, Set<String> usedCapabilities, GpuDeviceInfo deviceInfo) {
        System.out.println("[ESCraft] 正在创建 GLES 兼容的 BufferManager");
        try {
            Class<?> defaultClass = Class.forName("net.minecraft.client.gl.BufferManager$DefaultBufferManager");
            java.lang.reflect.Constructor<?> constructor = defaultClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return (BufferManager) constructor.newInstance();
        } catch (Exception e) {
            try {
                Class<?> defaultClass = Class.forName("net.minecraft.class_10874$class_10930");
                java.lang.reflect.Constructor<?> constructor = defaultClass.getDeclaredConstructor();
                constructor.setAccessible(true);
                return (BufferManager) constructor.newInstance();
            } catch (Exception ex) {
                throw new RuntimeException("[ESCraft] 无法初始化 DefaultBufferManager", ex);
            }
        }
    }

    @Mixin(targets = "net.minecraft.client.gl.BufferManager$DefaultBufferManager", remap = false)
    public static class MixinDefaultBufferManager {


        @Unique private static final int TARGET_VBO = 34962;
        @Unique private static final int TARGET_IBO = 34963;
        @Unique private static final int TARGET_UBO = 35345;
        @Unique private static final int TARGET_COPY = 35866;

        @Unique
        private int getEscraftTarget(int usage) {
            if ((usage & 128) != 0) return TARGET_UBO;
            if ((usage & 64) != 0) return TARGET_IBO;
            if ((usage & 32) != 0) return TARGET_VBO;
            return TARGET_COPY;
        }

        @Unique
        private int getEscraftUsage(int usage) {


            return (usage & 3) != 0 ? 35048 : 35044;
        }

        @Overwrite
        public int createBuffer() {
            return GLES20.glGenBuffers();
        }

        @Overwrite
        public void setBufferData(int buffer, long size, int usage) {
            int target = getEscraftTarget(usage);
            int glUsage = getEscraftUsage(usage);

            GLES20.glBindBuffer(target, buffer);
            GLES20.glBufferData(target, size, glUsage);


            int err = GLES20.glGetError();
            if (err == 1280 && target == TARGET_UBO) {
                System.err.println("[ESCraft] 警告: 环境不支持 UBO (35345)，正在降级为 VBO 尝试修复 1280 错误");
                GLES20.glBindBuffer(TARGET_VBO, buffer);
                GLES20.glBufferData(TARGET_VBO, size, glUsage);
                GLES20.glBindBuffer(TARGET_VBO, 0);
            } else {
                GLES20.glBindBuffer(target, 0);
            }
        }

        @Overwrite
        public void setBufferData(int buffer, ByteBuffer data, int usage) {
            int target = getEscraftTarget(usage);
            int glUsage = getEscraftUsage(usage);

            GLES20.glBindBuffer(target, buffer);
            GLES20.glBufferData(target, data, glUsage);

            int err = GLES20.glGetError();
            if (err == 1280 && target == TARGET_UBO) {
                GLES20.glBindBuffer(TARGET_VBO, buffer);
                GLES20.glBufferData(TARGET_VBO, data, glUsage);
                GLES20.glBindBuffer(TARGET_VBO, 0);
            } else {
                GLES20.glBindBuffer(target, 0);
            }
        }

        @Overwrite
        public void setBufferSubData(int buffer, long offset, ByteBuffer data, int usage) {
            int target = getEscraftTarget(usage);
            GLES20.glBindBuffer(target, buffer);
            GLES20.glBufferSubData(target, offset, data);
            GLES20.glBindBuffer(target, 0);
        }

        @Overwrite
        public void setBufferStorage(int buffer, long size, int usage) {

            setBufferData(buffer, size, usage);
        }

        @Overwrite
        public void setBufferStorage(int buffer, ByteBuffer data, int usage) {
            setBufferData(buffer, data, usage);
        }

        @Overwrite
        @Nullable
        public ByteBuffer mapBufferRange(int buffer, long offset, long length, int access, int usage) {
            int target = getEscraftTarget(usage);
            GLES20.glBindBuffer(target, buffer);

            ByteBuffer result = GLES30.glMapBufferRange(target, offset, length, access);
            GLES20.glBindBuffer(target, 0);
            return result;
        }

        @Overwrite
        public void unmapBuffer(int buffer, int usage) {
            int target = getEscraftTarget(usage);
            GLES20.glBindBuffer(target, buffer);
            GLES30.glUnmapBuffer(target);
            GLES20.glBindBuffer(target, 0);
        }

        @Overwrite
        public void flushMappedBufferRange(int buffer, long offset, long length, int usage) {
            int target = getEscraftTarget(usage);
            GLES20.glBindBuffer(target, buffer);
            GLES30.glFlushMappedBufferRange(target, offset, length);
            GLES20.glBindBuffer(target, 0);
        }

        @Overwrite
        public void copyBufferSubData(int fromBuffer, int toBuffer, long readOffset, long writeOffset, long size) {

            GLES30.glBindBuffer(TARGET_COPY, fromBuffer);
            GLES30.glBindBuffer(35867, toBuffer);
            GLES30.glCopyBufferSubData(TARGET_COPY, 35867, readOffset, writeOffset, size);
            GLES30.glBindBuffer(TARGET_COPY, 0);
            GLES30.glBindBuffer(35867, 0);
        }



        @Overwrite
        public int createFramebuffer() {
            return GLES30.glGenFramebuffers();
        }

        @Overwrite
        public void setupFramebuffer(int framebuffer, int colorAttachment, int depthAttachment, int mipLevel, int bindTarget) {
            int target = (bindTarget == 0) ? 36160 : bindTarget;
            GLES30.glBindFramebuffer(target, framebuffer);
            GLES30.glFramebufferTexture2D(target, 36064, 3553, colorAttachment, mipLevel);
            GLES30.glFramebufferTexture2D(target, 36096, 3553, depthAttachment, mipLevel);
        }

        @Overwrite
        public void setupBlitFramebuffer(int readFramebuffer, int writeFramebuffer, int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
            GLES30.glBindFramebuffer(36008, readFramebuffer);
            GLES30.glBindFramebuffer(36009, writeFramebuffer);
            GLES30.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
        }
    }
}