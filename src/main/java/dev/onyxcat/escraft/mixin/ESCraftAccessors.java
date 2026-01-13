package dev.onyxcat.escraft.mixin;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.gl.*;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.client.texture.GlTextureView;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ESCraftAccessors {





    @Mixin(RenderPassImpl.class)
    public interface RenderPassImplAccessor {
        @Accessor("pipeline") CompiledShaderPipeline getPipeline();

        @Accessor("vertexBuffers") GpuBuffer[] getVertexBuffers();

        @Accessor("indexBuffer") GpuBuffer getIndexBuffer();
        @Accessor("setSimpleUniforms") Set<String> getSetSimpleUniforms();


        @Accessor("simpleUniforms")
        HashMap<String, GpuBufferSlice> getSimpleUniforms();
        @Accessor("scissorState")
        net.minecraft.client.gl.ScissorState getScissorState();
        @Accessor("samplerUniforms") HashMap<String, ?> getSamplerUniforms();
    }
    @Mixin(targets = "net.minecraft.client.gl.RenderPassImpl$SamplerUniform")
    public interface SamplerUniformAccessor {

        @Accessor("view") GlTextureView getView();
        @Accessor("sampler") GlSampler getSampler();
    }
    @Mixin(CompiledShaderPipeline.class)
    public interface CompiledShaderPipelineAccessor {
        @Accessor("info") RenderPipeline getInfo();
        @Accessor("program") ShaderProgram getProgram();
    }

    @Mixin(ShaderProgram.class)
    public interface ShaderProgramAccessor {
        @Accessor("uniformsByName") Map<String, GlUniform> getUniformsField();
    }





    @Mixin(GlTexture.class)
    public interface GlTextureAccessor {


        @Accessor("glId")
        int getGlIdField();
    }

    @Mixin(GlTimerQuery.class)
    public interface GlTimerQueryInvoker {
        @Invoker("<init>")
        static GlTimerQuery create(int id) { throw new AssertionError(); }
    }

    @Mixin(targets = "net.minecraft.client.gl.GlGpuBuffer")
    public interface GlGpuBufferAccessor {

        @Accessor("id")
        int getIdField();
    }
    @Mixin(targets = "net.minecraft.client.gl.GlUniform$UniformBuffer")
    public interface UniformBufferAccessor {
        @Accessor("blockBinding") int getBlockBinding();
    }

    @Mixin(targets = "net.minecraft.client.gl.GlUniform$TexelBuffer")
    public interface TexelBufferAccessor {
        @Accessor("location") int getLocation();
        @Accessor("samplerIndex") int getSamplerIndex();
        @Accessor("format") TextureFormat getFormat();
        @Accessor("texture") int getTexture();
    }


    @Mixin(targets = "net.minecraft.client.gl.GlUniform$Sampler")
    public interface SamplerAccessor {
        @Accessor("location") int getLocation();
        @Accessor("samplerIndex") int getSamplerIndex();
    }

    @Mixin(GlSampler.class)
    public interface GlSamplerAccessor {

        @Accessor("samplerId")
        int getSamplerIdField();
    }
}