package dev.onyxcat.escraft.mixin;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.GpuQuery;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.onyxcat.escraft.mixin.ESCraftAccessors.*;
import net.minecraft.client.gl.*;
import net.minecraft.client.render.CloudRenderer;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.client.texture.GlTextureView;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengles.GLES20;
import org.lwjgl.opengles.GLES30;
import org.lwjgl.opengles.GLES32;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

@Mixin(GlCommandEncoder.class)
public abstract class MixinGlCommandEncoder {

    @Shadow @Nullable private GlTimerQuery timerQuery;
    @Shadow @Nullable private ShaderProgram currentProgram;
    @Shadow @Nullable private RenderPipeline currentPipeline;

    @Shadow protected abstract void setPipelineAndApplyState(RenderPipeline pipeline);
    @Shadow protected abstract GlBackend getBackend();

    @Shadow
    private boolean renderPassOpen;
    @Unique private static boolean HAS_GLES32 = false;
    @Unique private static boolean CHECKED_GLES32 = false;

    @Unique
    private static boolean isGles32() {
        if (!CHECKED_GLES32) {
            String version = GLES20.glGetString(GLES20.GL_VERSION);
            HAS_GLES32 = version != null && (version.contains("OpenGL ES 3.2") || version.contains("OpenGL ES 3.3"));
            CHECKED_GLES32 = true;
        }
        return HAS_GLES32;
    }

    @Unique
    private int getBufferId(GpuBuffer buffer) {
        return ((GlGpuBufferAccessor) (Object) buffer).getIdField();
    }

    @Unique
    private int getTextureId(GlTexture texture) {
        return ((GlTextureAccessor) texture).getGlIdField();
    }

    @Unique
    private int getSamplerId(GlSampler sampler) {
        return ((GlSamplerAccessor) sampler).getSamplerIdField();
    }





    @Overwrite
    public GpuBuffer.MappedView mapBuffer(GpuBufferSlice gpuBufferSlice, boolean read, boolean write) {
        GlGpuBuffer glGpuBuffer = (GlGpuBuffer) gpuBufferSlice.buffer();
        if (glGpuBuffer.isClosed()) {
            throw new IllegalStateException("Buffer already closed");
        }

        int access = 0;
        if (read) access |= 0x0001;
        if (write) access |= 0x0002;
        if (write) {
            access |= 0x0004;
            access |= 0x0020;
        }

        int bufferId = getBufferId(glGpuBuffer);

        ByteBuffer mappedData = mapBufferRangeRetry(bufferId, gpuBufferSlice.offset(), gpuBufferSlice.length(), access);

        if (mappedData == null) {
            throw new IllegalStateException("Failed to map buffer (GLES driver returned NULL)");
        }

        return new GpuBuffer.MappedView() {
            @Override
            public ByteBuffer data() {
                return mappedData;
            }

            @Override
            public void close() {
                GLES20.glBindBuffer(GLES30.GL_COPY_WRITE_BUFFER, bufferId);
                GLES30.glUnmapBuffer(GLES30.GL_COPY_WRITE_BUFFER);
                GLES20.glBindBuffer(GLES30.GL_COPY_WRITE_BUFFER, 0);
            }
        };
    }

    @Unique
    private ByteBuffer mapBufferRangeRetry(int bufferId, long offset, long length, int access) {
        GLES20.glBindBuffer(GLES30.GL_COPY_WRITE_BUFFER, bufferId);

        ByteBuffer buffer = GLES30.glMapBufferRange(GLES30.GL_COPY_WRITE_BUFFER, offset, length, access);
        if (buffer != null) return buffer;

        int basicAccess = access & (0x0001 | 0x0002);
        if (basicAccess != access) {
            buffer = GLES30.glMapBufferRange(GLES30.GL_COPY_WRITE_BUFFER, offset, length, basicAccess);
            if (buffer != null) return buffer;
        }

        buffer = GLES30.glMapBufferRange(GLES30.GL_COPY_WRITE_BUFFER, offset, length, 0x0001 | 0x0002);

        if (buffer == null) {
            int err = GLES20.glGetError();
            System.err.println("[ESCraft] Critical: Failed to map buffer " + bufferId + ". GL Error: " + err);
        }

        return buffer;
    }





    @Overwrite
    public <T> void drawObjectsWithRenderPass(RenderPassImpl pass, Collection<RenderPass.RenderObject<T>> objects, @Nullable GpuBuffer indexBuffer, VertexFormat.@Nullable IndexType indexType, Collection<String> validationSkippedUniforms, T object) {
        if (this.setupRenderPass(pass, validationSkippedUniforms)) {
            if (indexType == null) {
                indexType = VertexFormat.IndexType.SHORT;
            }

            for (RenderPass.RenderObject<T> renderObject : objects) {
                VertexFormat.IndexType indexType2 = renderObject.indexType() == null ? indexType : renderObject.indexType();

                pass.setIndexBuffer(renderObject.indexBuffer() == null ? indexBuffer : renderObject.indexBuffer(), indexType2);
                pass.setVertexBuffer(renderObject.slot(), renderObject.vertexBuffer());

                BiConsumer<T, RenderPass.UniformUploader> biConsumer = renderObject.uniformUploaderConsumer();
                if (biConsumer != null) {
                    biConsumer.accept(object, (name, gpuBufferSlice) -> {
                        CompiledShaderPipeline pipeline = ((RenderPassImplAccessor)pass).getPipeline();
                        ShaderProgram program = ((CompiledShaderPipelineAccessor)(Object)pipeline).getProgram();
                        GlUniform glUniform = program.getUniform(name);

                        if (glUniform instanceof GlUniform.UniformBuffer) {
                            int binding = ((UniformBufferAccessor)(Object)glUniform).getBlockBinding();
                            int bufferId = getBufferId(gpuBufferSlice.buffer());
                            GLES30.glBindBufferRange(GLES30.GL_UNIFORM_BUFFER, binding, bufferId, gpuBufferSlice.offset(), gpuBufferSlice.length());
                        }
                    });
                }

                this.drawObjectWithRenderPass(pass, 0, renderObject.firstIndex(), renderObject.indexCount(), indexType2, ((RenderPassImplAccessor)pass).getPipeline(), 1);
            }
        }
    }

    @Overwrite
    private boolean setupRenderPass(RenderPassImpl passObj, Collection<String> validationSkippedUniforms) {
        RenderPassImplAccessor pass = (RenderPassImplAccessor) passObj;
        CompiledShaderPipeline pipeline = pass.getPipeline();

        if (pipeline == null || pipeline.program() == ShaderProgram.INVALID) {
            return false;
        }

        RenderPipeline renderPipeline = pipeline.info();
        ShaderProgram shaderProgram = pipeline.program();
        this.setPipelineAndApplyState(renderPipeline);

        boolean programChanged = this.currentProgram != shaderProgram;
        if (programChanged) {
            GlStateManager._glUseProgram(shaderProgram.getGlRef());
            this.currentProgram = shaderProgram;
        }
        GLES32.glMemoryBarrier(GLES32.GL_BUFFER_UPDATE_BARRIER_BIT | GLES32.GL_UNIFORM_BARRIER_BIT | GLES32.GL_TEXTURE_FETCH_BARRIER_BIT);

        Map<String, GlUniform> uniforms = shaderProgram.getUniforms();
        Set<String> setSimpleUniforms = pass.getSetSimpleUniforms();
        HashMap<String, GpuBufferSlice> simpleUniforms = pass.getSimpleUniforms();

        for (Map.Entry<String, GlUniform> entry : uniforms.entrySet()) {
            String name = entry.getKey();
            boolean isUniformSet = setSimpleUniforms.contains(name);
            GlUniform glUniform = entry.getValue();

            if (glUniform instanceof GlUniform.UniformBuffer ubo) {
                if (isUniformSet) {
                    GpuBufferSlice slice = simpleUniforms.get(name);
                    if (slice != null) {
                        int binding = ((UniformBufferAccessor)(Object)ubo).getBlockBinding();
                        GLES32.glBindBufferRange(GLES32.GL_UNIFORM_BUFFER, binding, getBufferId(slice.buffer()), slice.offset(), slice.length());
                    }
                }
            }
            else if (glUniform instanceof GlUniform.TexelBuffer tbo) {
                TexelBufferAccessor tboAcc = (TexelBufferAccessor)(Object) tbo;
                int samplerIndex = tboAcc.getSamplerIndex();

                if (programChanged || isUniformSet) {
                    GlStateManager._glUniform1i(tboAcc.getLocation(), samplerIndex);
                }

                GLES32.glBindSampler(samplerIndex, 0);
                GlStateManager._activeTexture(GLES32.GL_TEXTURE0 + samplerIndex);
                GLES32.glBindTexture(GLES32.GL_TEXTURE_BUFFER, tboAcc.getTexture());

                if (isUniformSet) {
                    GpuBufferSlice slice = simpleUniforms.get(name);
                    if (slice != null) {
                        int internalFormat = name.equals("CloudFaces") ? GLES32.GL_R8I : getGlesInternalFormat(tboAcc.getFormat());
                        int bufferId = getBufferId(slice.buffer());

                        if (slice.offset() == 0) {
                            GLES32.glTexBuffer(GLES32.GL_TEXTURE_BUFFER, internalFormat, bufferId);
                        } else {
                            GLES32.glTexBufferRange(GLES32.GL_TEXTURE_BUFFER, internalFormat, bufferId, slice.offset(), slice.length());
                        }
                    }
                }
            }
            else if (glUniform instanceof GlUniform.Sampler sampler) {
                SamplerAccessor samplerAcc = (SamplerAccessor)(Object)sampler;
                String uniformName = entry.getKey();
                HashMap<String, ?> samplerUniformsMap = pass.getSamplerUniforms();
                Object samplerUniformObj = samplerUniformsMap.get(uniformName);

                if (samplerUniformObj != null) {
                    SamplerUniformAccessor samplerUniformAcc = (SamplerUniformAccessor) samplerUniformObj;
                    GlTextureView textureView = samplerUniformAcc.getView();
                    int location = samplerAcc.getLocation();
                    int samplerIndex = samplerAcc.getSamplerIndex();

                    if (programChanged || isUniformSet) {
                        GlStateManager._glUniform1i(location, samplerIndex);
                    }

                    GlStateManager._activeTexture(GLES32.GL_TEXTURE0 + samplerIndex);
                    GlTexture glTexture = (GlTexture) textureView.texture();
                    int target = ((glTexture.usage() & 16) != 0) ? GLES32.GL_TEXTURE_CUBE_MAP : GLES32.GL_TEXTURE_2D;
                    int glId = ((GlTextureAccessor)glTexture).getGlIdField();

                    if (target == GLES32.GL_TEXTURE_CUBE_MAP) {
                        GLES32.glBindTexture(GLES32.GL_TEXTURE_CUBE_MAP, glId);
                    } else {
                        GlStateManager._bindTexture(glId);
                    }

                    int samplerId = ((GlSamplerAccessor) samplerUniformAcc.getSampler()).getSamplerIdField();
                    GLES32.glBindSampler(samplerIndex, samplerId);

                    int baseLevel = textureView.baseMipLevel();
                    int maxLevel = baseLevel + textureView.mipLevels() - 1;
                    GlStateManager._texParameter(target, GLES32.GL_TEXTURE_BASE_LEVEL, baseLevel);
                    GlStateManager._texParameter(target, GLES32.GL_TEXTURE_MAX_LEVEL, maxLevel);
                }
            }
        }

        setSimpleUniforms.clear();
        ScissorState scissor = pass.getScissorState();
        if (scissor.isEnabled()) {
            GlStateManager._enableScissorTest();
            GlStateManager._scissorBox(scissor.getX(), scissor.getY(), scissor.getWidth(), scissor.getHeight());
        } else {
            GlStateManager._disableScissorTest();
        }
        return true;
    }

    @Unique
    private int getGlesInternalFormat(TextureFormat format) {
        String name = format.toString();
        if (name.contains("R8")) {
            return name.contains("UI") ? GLES32.GL_R8UI : GLES32.GL_R8;
        } else if (name.contains("R32")) {
            return name.contains("UI") ? GLES32.GL_R32UI : GLES32.GL_R32F;
        }
        return GLES32.GL_RGBA8;
    }

    @Overwrite
    public GpuQuery timerQueryBegin() {
        RenderSystem.assertOnRenderThread();
        if (this.timerQuery != null) throw new IllegalStateException("Query active");
        try {
            IntBuffer ids = IntBuffer.allocate(1);
            GLES30.glGenQueries(ids);
            int i = ids.get(0);
            GLES30.glBeginQuery(0x88BF, i);
            this.timerQuery = GlTimerQueryInvoker.create(i);
            return this.timerQuery;
        } catch (Exception e) { return null; }
    }

    @Overwrite
    public void timerQueryEnd(GpuQuery gpuQuery) {
        RenderSystem.assertOnRenderThread();
        if (gpuQuery == null) return;
        if (gpuQuery != this.timerQuery) throw new IllegalStateException("Mismatch");
        try { GLES30.glEndQuery(0x88BF); } catch (Exception ignored) {}
        this.timerQuery = null;
    }

    @Overwrite
    private void drawObjectWithRenderPass(RenderPassImpl pass, int baseVertex, int firstIndex, int count, VertexFormat.@Nullable IndexType indexType, CompiledShaderPipeline pipeline, int instanceCount) {
        GpuBuffer[] vertexBuffers = ((RenderPassImplAccessor) pass).getVertexBuffers();
        GpuBuffer indexBuffer = ((RenderPassImplAccessor) pass).getIndexBuffer();
        VertexFormat vertexFormat = ((CompiledShaderPipelineAccessor) (Object) pipeline).getInfo().getVertexFormat();

        this.getBackend().getVertexBufferManager().setupBuffer(vertexFormat, (GlGpuBuffer) vertexBuffers[0]);

        int mode = toGlMode(((CompiledShaderPipelineAccessor) (Object) pipeline).getInfo().getVertexFormatMode());

        if (indexType != null) {
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, getBufferId(indexBuffer));
            int glIndexType = toGlIndexType(indexType);
            long offset = (long)firstIndex * (long)indexType.size;

            if (instanceCount > 1) {
                if (baseVertex > 0) {
                    if (isGles32()) GLES32.glDrawElementsInstancedBaseVertex(mode, count, glIndexType, offset, instanceCount, baseVertex);
                    else GLES30.glDrawElementsInstanced(mode, count, glIndexType, offset, instanceCount);
                } else {
                    GLES30.glDrawElementsInstanced(mode, count, glIndexType, offset, instanceCount);
                }
            } else if (baseVertex > 0) {
                if (isGles32()) GLES32.glDrawElementsBaseVertex(mode, count, glIndexType, offset, baseVertex);
                else GLES20.glDrawElements(mode, count, glIndexType, offset);
            } else {
                GLES20.glDrawElements(mode, count, glIndexType, offset);
            }
        } else if (instanceCount > 1) {
            GLES30.glDrawArraysInstanced(mode, baseVertex, count, instanceCount);
        } else {
            GLES20.glDrawArrays(mode, baseVertex, count);
        }
    }

    @Unique
    private int toGlMode(VertexFormat.DrawMode mode) {
        return switch (mode) {
            case LINES, DEBUG_LINES -> GLES20.GL_LINES;
            case DEBUG_LINE_STRIP -> GLES20.GL_LINE_STRIP;
            case POINTS -> GLES20.GL_POINTS;
            case TRIANGLES -> GLES20.GL_TRIANGLES;
            case TRIANGLE_STRIP -> GLES20.GL_TRIANGLE_STRIP;
            case TRIANGLE_FAN -> GLES20.GL_TRIANGLE_FAN;
            case QUADS -> GLES20.GL_TRIANGLES;
        };
    }

    @Unique
    private int toGlIndexType(VertexFormat.IndexType indexType) {
        return switch (indexType) {
            case SHORT -> GLES20.GL_UNSIGNED_SHORT;
            case INT -> GLES30.GL_UNSIGNED_INT;
        };
    }

    @Unique
    private int toGlInternalId(TextureFormat format) {
        if (format == null) return GLES20.GL_RGBA;
        String name = format.toString();
        if (name.contains("RGBA8")) return GLES30.GL_RGBA8;
        if (name.contains("R8")) return GLES30.GL_R8;
        if (name.contains("R32UI")) return GLES30.GL_R32UI;
        return GLES20.GL_RGBA;
    }





    @Overwrite
    public void copyTextureToBuffer(GpuTexture gpuTexture, GpuBuffer gpuBuffer, long offset, Runnable callback, int mipLevel) {
        if (this.renderPassOpen) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        } else {
            this.copyTextureToBuffer(gpuTexture, gpuBuffer, offset, callback, mipLevel, 0, 0, gpuTexture.getWidth(mipLevel), gpuTexture.getHeight(mipLevel));
        }
    }

    @Overwrite
    public void copyTextureToBuffer(GpuTexture gpuTexture, GpuBuffer gpuBuffer, long offset, Runnable callback, int mipLevel, int x, int y, int width, int height) {


        if (this.renderPassOpen) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        }


        if (mipLevel >= 0 && mipLevel < gpuTexture.getMipLevels()) {
            long requiredSize = (long)(gpuTexture.getWidth(mipLevel) * gpuTexture.getHeight(mipLevel) * gpuTexture.getFormat().pixelSize()) + offset;





            if (x + width <= gpuTexture.getWidth(mipLevel) && y + height <= gpuTexture.getHeight(mipLevel)) {
                if (gpuTexture.isClosed()) throw new IllegalStateException("Source texture is closed");
                if (gpuBuffer.isClosed()) throw new IllegalStateException("Destination buffer is closed");






                GlStateManager._glBindFramebuffer(36008,((BufferManagerAccessor)getBackend().getBufferManager()).getCreateFramebuffer());





                int fbo = GLES30.glGenFramebuffers();
                GLES30.glBindFramebuffer(GLES30.GL_READ_FRAMEBUFFER, fbo);
                GLES30.glFramebufferTexture2D(GLES30.GL_READ_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, getTextureId((GlTexture)gpuTexture), mipLevel);



                GLES30.glBindBuffer(35051, getBufferId(gpuBuffer));


                GLES20.glPixelStorei(GLES20.GL_PACK_ALIGNMENT, 1);




                int format = GLES20.GL_RGBA;
                int type = GLES20.GL_UNSIGNED_BYTE;


                String fmtName = gpuTexture.getFormat().toString();
                if (fmtName.contains("R8")) { format = GLES30.GL_RED; }
                else if (fmtName.contains("RGBA")) { format = GLES20.GL_RGBA; }


                GLES20.nglReadPixels(x, y, width, height, format, type, offset);


                RenderSystem.queueFencedTask(callback);


                GLES30.glBindFramebuffer(GLES30.GL_READ_FRAMEBUFFER, 0);
                GLES30.glDeleteFramebuffers(fbo);
                GLES30.glBindBuffer(35051, 0);

            } else {
                throw new IllegalArgumentException("Rect out of bounds");
            }
        } else {
            throw new IllegalArgumentException("Invalid mipLevel");
        }
    }
    @Redirect(method = {
            "createRenderPass",
            "clearColorAndDepthTextures(Lcom/mojang/blaze3d/textures/GpuTexture;ILcom/mojang/blaze3d/textures/GpuTexture;D)V",
            "clearColorAndDepthTextures(Lcom/mojang/blaze3d/textures/GpuTexture;ILcom/mojang/blaze3d/textures/GpuTexture;DIIII)V",
            "clearDepthTexture"
    }, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glClearDepth(D)V"))
    private void redirectClearDepth(double depth) {
        GLES20.glClearDepthf((float) depth);
    }

    @Redirect(method = {
            "createRenderPass",
            "clearColorTexture",
            "clearColorAndDepthTextures(Lcom/mojang/blaze3d/textures/GpuTexture;ILcom/mojang/blaze3d/textures/GpuTexture;D)V",
            "clearColorAndDepthTextures(Lcom/mojang/blaze3d/textures/GpuTexture;ILcom/mojang/blaze3d/textures/GpuTexture;DIIII)V"
    }, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glClearColor(FFFF)V"))
    private void redirectClearColor(float red, float green, float blue, float alpha) {
        GLES20.glClearColor(red, green, blue, alpha);
    }

    @Redirect(method = "clearDepthTexture",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glDrawBuffer(I)V"))
    private void redirectDrawBuffer(int mode) {
        try (org.lwjgl.system.MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush()) {
            java.nio.IntBuffer buf = stack.ints(mode);
            GLES30.glDrawBuffers(buf);
        }
    }
}