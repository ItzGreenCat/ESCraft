package dev.onyxcat.escraft.mixin;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.gl.*;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.client.util.TextureAllocationException;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengles.GLES20;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.function.Supplier;

@Mixin(value = GlBackend.class, remap = false)
public abstract class MixinGlBackend {

    @Shadow @Final private BufferManager bufferManager;
    @Shadow @Final private GpuBufferManager gpuBufferManager;
    @Shadow @Final private DebugLabelManager debugLabelManager;
    @Shadow @Mutable @Final private int uniformOffsetAlignment;
    @Shadow @Mutable @Final private int maxSupportedAnisotropy;



    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL;createCapabilities()Lorg/lwjgl/opengl/GLCapabilities;"))
    private GLCapabilities redirectCreateCapabilities() {
        return null;
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/DebugLabelManager;create(Lorg/lwjgl/opengl/GLCapabilities;ZLjava/util/Set;)Lnet/minecraft/client/gl/DebugLabelManager;"))
    private DebugLabelManager redirectDebugLabel(GLCapabilities caps, boolean render, Set<String> used) {
        return DebugLabelManager.create(null, render, used);
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/VertexBufferManager;create(Lorg/lwjgl/opengl/GLCapabilities;Lnet/minecraft/client/gl/DebugLabelManager;Ljava/util/Set;)Lnet/minecraft/client/gl/VertexBufferManager;"))
    private VertexBufferManager redirectVbo(GLCapabilities caps, DebugLabelManager dlm, Set<String> used) {
        return VertexBufferManager.create(null, dlm, used);
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/GpuBufferManager;create(Lorg/lwjgl/opengl/GLCapabilities;Ljava/util/Set;)Lnet/minecraft/client/gl/GpuBufferManager;"))
    private GpuBufferManager redirectGpuBuf(GLCapabilities caps, Set<String> used) {
        return GpuBufferManager.create(null, used);
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/BufferManager;create(Lorg/lwjgl/opengl/GLCapabilities;Ljava/util/Set;Lnet/minecraft/client/gl/GpuDeviceInfo;)Lnet/minecraft/client/gl/BufferManager;"))
    private BufferManager redirectBuf(GLCapabilities caps, Set<String> used, GpuDeviceInfo info) {
        return BufferManager.create(null, used, info);
    }

    @Redirect(method = "<init>", at = @At(value = "FIELD", target = "Lorg/lwjgl/opengl/GLCapabilities;GL_EXT_texture_filter_anisotropic:Z", opcode = 180))
    private boolean redirectAnisoField(GLCapabilities caps) {
        return false;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void postInit(CallbackInfo ci) {
        this.maxSupportedAnisotropy = 1;
        int align = GLES20.glGetInteger(35380);
        this.uniformOffsetAlignment = (align <= 0) ? 256 : align;
    }



    @Overwrite
    public GpuTexture createTexture(@Nullable String label, int usage, TextureFormat format, int width, int height, int depth, int mips) {
        if (mips < 1) throw new IllegalArgumentException("mipLevels must be at least 1");

        boolean isCubemap = (usage & 16) != 0;
        int textureId = GlStateManager._genTexture();
        String name = (label == null) ? String.valueOf(textureId) : label;

        int target = isCubemap ? 34067 : 3553;
        GLES20.glBindTexture(target, textureId);


        GLES20.glTexParameteri(target, 33085, mips - 1);
        GLES20.glTexParameteri(target, 33082, 0);


        boolean isDepthTexture = false;


        try {


            if (format != null) {
                String formatName = format.toString();
                isDepthTexture = formatName.contains("DEPTH") ||
                        formatName.contains("DEPTH_COMPONENT");
            }
        } catch (Exception e) {

        }

        if (isDepthTexture) {

            GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        }


        int internalFormat;
        int externalFormat;
        int type;

        if (isDepthTexture) {

            internalFormat = GLES20.GL_DEPTH_COMPONENT16;
            externalFormat = GLES20.GL_DEPTH_COMPONENT;
            type = GLES20.GL_UNSIGNED_SHORT;
        } else {
            try {

                Class<?> glConstClass = Class.forName("com.mojang.blaze3d.opengl.GlConst");
                java.lang.reflect.Method toGlInternalId = glConstClass.getDeclaredMethod("toGlInternalId", TextureFormat.class);
                java.lang.reflect.Method toGlExternalId = glConstClass.getDeclaredMethod("toGlExternalId", TextureFormat.class);
                java.lang.reflect.Method toGlType = glConstClass.getDeclaredMethod("toGlType", TextureFormat.class);

                internalFormat = (int) toGlInternalId.invoke(null, format);
                externalFormat = (int) toGlExternalId.invoke(null, format);
                type = (int) toGlType.invoke(null, format);
            } catch (Exception e) {

                System.err.println("[ESCraft] 格式转换失败，使用 RGBA 回退: " + e.getMessage());
                internalFormat = GLES20.GL_RGBA;
                externalFormat = GLES20.GL_RGBA;
                type = GLES20.GL_UNSIGNED_BYTE;
            }
        }


        int err = GLES20.glGetError();
        if (err != 0) {
            System.err.println("[ESCraft] 纹理创建前有未清除的 OpenGL 错误: " + err);
        }

        try {
            for (int level = 0; level < mips; ++level) {
                int w = Math.max(1, width >> level);
                int h = Math.max(1, height >> level);

                if (isCubemap) {
                    for (int face = 0; face < 6; face++) {
                        GLES20.glTexImage2D(34069 + face, level, internalFormat, w, h, 0, externalFormat, type, (ByteBuffer)null);
                    }
                } else {
                    GLES20.glTexImage2D(target, level, internalFormat, w, h, 0, externalFormat, type, (ByteBuffer)null);
                }


                err = GLES20.glGetError();
                if (err != 0) {
                    System.err.println("[ESCraft] Mip level " + level + " 创建失败: " + err);
                }
            }
        } catch (Exception e) {
            System.err.println("[ESCraft] 纹理创建异常: " + e.getMessage());
            e.printStackTrace();


            try {
                GLES20.glTexImage2D(target, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, (ByteBuffer)null);
            } catch (Exception e2) {
                throw new IllegalStateException("GLES 无法分配纹理，错误: " + GLES20.glGetError(), e2);
            }
        }

        err = GLES20.glGetError();
        if (err != 0) {
            System.err.println("[ESCraft] 纹理创建后仍有 OpenGL 错误: " + err);

            if (err == 1281) {
                System.err.println("[ESCraft] 尝试 RGBA8 兜底方案...");

                GLES20.glDeleteTextures(textureId);
                textureId = GlStateManager._genTexture();
                GLES20.glBindTexture(target, textureId);
                GLES20.glTexImage2D(target, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, (ByteBuffer)null);

                if (GLES20.glGetError() != 0) {
                    throw new IllegalStateException("GLES 无法分配基本纹理");
                }
            } else if (err == 1285) {
                throw new TextureAllocationException("显存溢出: " + name);
            } else {
                throw new IllegalStateException("OpenGL 错误: " + err);
            }
        }

        GlTexture glTexture = createGlTextureInstance(usage, name, format, width, height, depth, mips, textureId);

        if (glTexture != null) {
            this.debugLabelManager.labelGlTexture(glTexture);
        }

        return glTexture;
    }

    
    @Unique
    private GlTexture createGlTextureInstance(int usage, String name, TextureFormat format, int width, int height, int depth, int mips, int id) {
        try {
            java.lang.reflect.Constructor<GlTexture> c = GlTexture.class.getDeclaredConstructor(
                    int.class, String.class, TextureFormat.class, int.class, int.class, int.class, int.class, int.class
            );
            c.setAccessible(true);
            return c.newInstance(usage, name, format, width, height, depth, mips, id);
        } catch (Exception e) {
            System.err.println("[ESCraft] 无法通过反射创建 GlTexture 对象: " + e.getMessage());
            return null;
        }
    }



    @Overwrite
    public GpuBuffer createBuffer(@Nullable Supplier<String> supplier, int usage, long size) {
        while (GLES20.glGetError() != 0);
        long effectiveSize = size;
        if ((usage & 128) != 0) {
            int align = this.uniformOffsetAlignment > 0 ? this.uniformOffsetAlignment : 256;
            effectiveSize = (size + (long)align - 1L) & ~((long)align - 1L);
        }

        try {
            GlGpuBuffer buf = this.gpuBufferManager.createBuffer(this.bufferManager, supplier, usage, effectiveSize);
            if (GLES20.glGetError() != 0) {

                return this.gpuBufferManager.createBuffer(this.bufferManager, supplier, 32, effectiveSize);
            }
            this.debugLabelManager.labelGlGpuBuffer(buf);
            return buf;
        } catch (Exception e) {
            return createDummyBuffer(supplier, usage, size);
        }
    }

    @Overwrite
    public GpuBuffer createBuffer(@Nullable Supplier<String> supplier, int usage, ByteBuffer data) {
        while (GLES20.glGetError() != 0);
        try {
            GlGpuBuffer buf = this.gpuBufferManager.createBuffer(this.bufferManager, supplier, usage, data);
            if (GLES20.glGetError() != 0) {
                return this.gpuBufferManager.createBuffer(this.bufferManager, supplier, 32, data);
            }
            this.debugLabelManager.labelGlGpuBuffer(buf);
            return buf;
        } catch (Exception e) {
            return createDummyBuffer(supplier, usage, (long)data.remaining());
        }
    }



    @Unique
    private GlGpuBuffer createDummyBuffer(Supplier<String> supplier, int usage, long size) {
        try {
            java.lang.reflect.Constructor<GlGpuBuffer> c = GlGpuBuffer.class.getDeclaredConstructor(
                    Supplier.class, BufferManager.class, int.class, long.class, int.class, ByteBuffer.class
            );
            c.setAccessible(true);
            return c.newInstance(supplier, this.bufferManager, usage, size, 0, null);
        } catch (Exception e) { return null; }
    }

    @Overwrite
    private static int determineMaxTextureSize() {
        return GLES20.glGetInteger(GLES20.GL_MAX_TEXTURE_SIZE);
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glEnable(I)V"))
    private void redirectEnable(int target) {
        if (target == 34895 || target == 34370) return;
        GLES20.glEnable(target);
    }
}