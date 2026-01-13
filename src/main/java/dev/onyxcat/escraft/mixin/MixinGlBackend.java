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
import org.lwjgl.opengles.GLES30;
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

        int maxPossibleMips = 1 + (int)(Math.log(Math.max(width, height)) / Math.log(2));
        if (mips > maxPossibleMips) {
            mips = maxPossibleMips;
        }

        boolean isCubemap = (usage & 16) != 0;
        int textureId = GlStateManager._genTexture();
        String name = (label == null) ? String.valueOf(textureId) : label;

        int target = isCubemap ? GLES30.GL_TEXTURE_CUBE_MAP : GLES30.GL_TEXTURE_2D;
        GLES30.glBindTexture(target, textureId);

        GLES30.glTexParameteri(target, GLES30.GL_TEXTURE_MAX_LEVEL, mips - 1);
        GLES30.glTexParameteri(target, GLES30.GL_TEXTURE_BASE_LEVEL, 0);

        boolean isDepth = false;
        if (format != null) {
            String fmt = format.toString();
            isDepth = fmt.contains("DEPTH");
        }

        int storageInternalFormat;
        int fallbackInternalFormat;
        int externalFormat;
        int type;

        if (isDepth) {
            storageInternalFormat = GLES30.GL_DEPTH_COMPONENT24;
            fallbackInternalFormat = GLES30.GL_DEPTH_COMPONENT;
            externalFormat = GLES30.GL_DEPTH_COMPONENT;
            type = GLES30.GL_UNSIGNED_INT;

            GLES30.glTexParameteri(target, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
            GLES30.glTexParameteri(target, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);
            GLES30.glTexParameteri(target, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
            GLES30.glTexParameteri(target, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        } else {
            storageInternalFormat = GLES30.GL_RGBA8;
            fallbackInternalFormat = GLES30.GL_RGBA;
            externalFormat = GLES30.GL_RGBA;
            type = GLES30.GL_UNSIGNED_BYTE;
        }

        while (GLES30.glGetError() != GLES30.GL_NO_ERROR);

        try {
            GLES30.glTexStorage2D(target, mips, storageInternalFormat, width, height);
        } catch (Exception | Error e) {
            System.out.println("[ESCraft] 纹理 " + name + " (ID:" + textureId + ") Storage 分配失败，转为 Image 模式");

            while (GLES30.glGetError() != GLES30.GL_NO_ERROR);

            for (int level = 0; level < mips; ++level) {
                int w = Math.max(1, width >> level);
                int h = Math.max(1, height >> level);

                if (isCubemap) {
                    for (int face = 0; face < 6; face++) {
                        GLES30.glTexImage2D(GLES30.GL_TEXTURE_CUBE_MAP_POSITIVE_X + face, level, fallbackInternalFormat, w, h, 0, externalFormat, type, (ByteBuffer)null);
                    }
                } else {
                    GLES30.glTexImage2D(target, level, fallbackInternalFormat, w, h, 0, externalFormat, type, (ByteBuffer)null);
                }
            }
        }

        int err = GLES30.glGetError();
        if (err != GLES30.GL_NO_ERROR) {
            System.err.println("[ESCraft] 严重：创建纹理 " + name + " 最终失败: " + err + " (Size: " + width + "x" + height + ", Mips: " + mips + ")");

            GLES30.glDeleteTextures(textureId);
            textureId = GlStateManager._genTexture();
            GLES30.glBindTexture(target, textureId);

            GLES30.glTexParameteri(target, GLES30.GL_TEXTURE_MAX_LEVEL, 0);
            GLES30.glTexImage2D(target, 0, GLES30.GL_RGBA, width, height, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, (ByteBuffer)null);
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
        int max = GLES20.glGetInteger(GLES20.GL_MAX_TEXTURE_SIZE);
        return Math.min(max, 16384);
    }
    @Redirect(
            method = "<init>",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glEnable(I)V"),
            remap = false
    )
    private void silenceInvalidEnables(int target) {
        if (target == 34895 || target == 34370) {
            return;
        }
        GLES20.glEnable(target);
    }

    @Redirect(
            method = "<init>",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glGetInteger(I)I"),
            remap = false
    )
    private int redirectGetInteger(int pname) {
        return GLES20.glGetInteger(pname);
    }
    @Redirect(
            method = "<init>",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glGetFloat(I)F"),
            remap = false
    )
    private float redirectGetFloat(int pname) {
        return GLES20.glGetFloat(pname);
    }
}