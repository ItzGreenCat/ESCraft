package dev.onyxcat.escraft.mixin;

import com.mojang.blaze3d.opengl.GlStateManager;
import dev.onyxcat.escraft.utils.GLSLESShaderTranspiler;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengles.GLES20;
import org.lwjgl.opengles.GLES30;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Mixin(GlStateManager.class)
public abstract class MixinGlStateManager {

    @Shadow private static int readFbo;
    @Shadow private static int writeFbo;
    @Shadow private static int activeTexture;





    
    @Overwrite
    public static ByteBuffer _glMapBufferRange(int target, long offset, long range, int access) {

        ByteBuffer buffer = GLES30.glMapBufferRange(target, offset, range, access);


        if (buffer != null) {
            return buffer;
        }


        int error = GLES20.glGetError();

        System.err.println("[ESCraft] glMapBufferRange 失败! NULL returned.");
        System.err.println("Target: " + target + " Offset: " + offset + " Range: " + range + " Access: " + access + " GL Error: " + error);




        int basicAccess = access & (0x0001 | 0x0002);

        if (basicAccess != access) {
            System.err.println("[ESCraft] Retrying with simplified access flags: " + basicAccess);
            buffer = GLES30.glMapBufferRange(target, offset, range, basicAccess);
            if (buffer != null) return buffer;
        }



        System.err.println("[ESCraft] Retrying with READ | WRITE...");
        buffer = GLES30.glMapBufferRange(target, offset, range, 0x0001 | 0x0002);

        if (buffer != null) return buffer;


        System.err.println("[ESCraft] Critical: All MapBufferRange attempts failed.");
        return null;
    }

    @Overwrite
    public static void _glBufferData(int target, long size, int usage) {



        GLES20.glBufferData(target, size, usage);

        int err = GLES20.glGetError();
        if (err != 0) {
            System.err.println("[ESCraft] glBufferData(Size) 失败! Error: " + err + " Target: " + target + " Size: " + size);
        }
    }

    @Overwrite
    public static void _glBufferData(int target, ByteBuffer data, int usage) {

        int glesUsage = usage;



        if (usage >= 35040 && usage <= 35042) {
            glesUsage = 35040;
        }

        else if (usage >= 35044 && usage <= 35046) {
            glesUsage = 35044;
        }

        else if (usage >= 35048 && usage <= 35050) {
            glesUsage = 35048;
        }

        GLES20.glBufferData(target, data, glesUsage);

        int err = GLES20.glGetError();
        if (err != 0) {
            System.err.println("[ESCraft] glBufferData(Data) 失败! Error: " + err);
        }
    }





    @Overwrite
    public static void _scissorBox(int x, int y, int width, int height) {
        GLES20.glScissor(x, y, width, height);
    }

    @Overwrite
    public static void _depthFunc(int func) {
        GLES20.glDepthFunc(func);
    }

    @Overwrite
    public static void _depthMask(boolean mask) {
        GLES20.glDepthMask(mask);
    }

    @Overwrite
    public static void glBlendFuncSeparate(int srcFactorRgb, int dstFactorRgb, int srcFactorAlpha, int dstFactorAlpha) {
        GLES20.glBlendFuncSeparate(srcFactorRgb, dstFactorRgb, srcFactorAlpha, dstFactorAlpha);
    }

    @Overwrite
    public static int glGetProgrami(int program, int pname) {
        return GLES20.glGetProgrami(program, pname);
    }

    @Overwrite
    public static void glAttachShader(int program, int shader) {
        GLES20.glAttachShader(program, shader);
    }

    @Overwrite
    public static int glCreateShader(int type) {
        return GLES20.glCreateShader(type);
    }

    @Overwrite
    public static void glShaderSource(int shader, String source) {
        String transpiledSource = source;
        try {
            int shaderType = inferShaderType(source);
            transpiledSource = GLSLESShaderTranspiler.transpile(source, shaderType);
        } catch (Exception e) {
            System.err.println("[ESCraft] Transpile failed: " + e.getMessage());
            e.printStackTrace();
            transpiledSource = source;
        }

        byte[] bs = transpiledSource.getBytes(StandardCharsets.UTF_8);
        ByteBuffer byteBuffer = MemoryUtil.memAlloc(bs.length + 1);
        byteBuffer.put(bs);
        byteBuffer.put((byte)0);
        byteBuffer.flip();

        try {
            MemoryStack memoryStack = MemoryStack.stackPush();
            try {
                PointerBuffer pointerBuffer = memoryStack.mallocPointer(1);
                pointerBuffer.put(byteBuffer);
                GLES20.nglShaderSource(shader, 1, pointerBuffer.address0(), 0L);
            } catch (Throwable var12) {
                if (memoryStack != null) try { memoryStack.close(); } catch (Throwable var11) { var12.addSuppressed(var11); }
                throw var12;
            }
            if (memoryStack != null) memoryStack.close();
        } finally {
            MemoryUtil.memFree(byteBuffer);
        }
    }

    private static int inferShaderType(String source) {
        if (source.contains("gl_Position")) return GLES20.GL_VERTEX_SHADER;
        return GLES20.GL_FRAGMENT_SHADER;
    }

    @Overwrite
    public static void glDeleteShader(int shader) {
        GLES20.glDeleteShader(shader);
    }

    @Overwrite
    public static void glCompileShader(int shader) {
        GLES20.glCompileShader(shader);
    }

    @Overwrite
    public static int glGetShaderi(int shader, int pname) {
        return GLES20.glGetShaderi(shader, pname);
    }

    @Overwrite
    public static void _glUseProgram(int program) {
        GLES20.glUseProgram(program);
    }

    @Overwrite
    public static int glCreateProgram() {
        return GLES20.glCreateProgram();
    }

    @Overwrite
    public static void glDeleteProgram(int program) {
        GLES20.glDeleteProgram(program);
    }

    @Overwrite
    public static void glLinkProgram(int program) {
        GLES20.glLinkProgram(program);
    }

    @Overwrite
    public static int _glGetUniformLocation(int program, CharSequence name) {
        return GLES20.glGetUniformLocation(program, name);
    }

    @Overwrite
    public static void _glUniform1i(int location, int value) {
        GLES20.glUniform1i(location, value);
    }

    @Overwrite
    public static void _glBindAttribLocation(int program, int index, CharSequence name) {
        GLES20.glBindAttribLocation(program, index, name);
    }

    @Overwrite
    public static int _glGenBuffers() {
        return GLES20.glGenBuffers();
    }

    @Overwrite
    public static int _glGenVertexArrays() {
        return GLES30.glGenVertexArrays();
    }

    @Overwrite
    public static void _glBindBuffer(int target, int buffer) {
        GLES20.glBindBuffer(target, buffer);
    }

    @Overwrite
    public static void _glBindVertexArray(int array) {
        GLES30.glBindVertexArray(array);
    }

    @Overwrite
    public static void _glBufferSubData(int target, long offset, ByteBuffer data) {
        GLES20.glBufferSubData(target, offset, data);
    }

    @Overwrite
    public static void _glUnmapBuffer(int target) {
        GLES30.glUnmapBuffer(target);
    }

    @Overwrite
    public static void _glDeleteBuffers(int buffer) {
        GLES20.glDeleteBuffers(buffer);
    }

    @Overwrite
    public static void _glBlitFrameBuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
        GLES30.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
    }

    @Overwrite
    public static void _glDeleteFramebuffers(int framebuffer) {
        GLES30.glDeleteFramebuffers(framebuffer);
        if (readFbo == framebuffer) readFbo = 0;
        if (writeFbo == framebuffer) writeFbo = 0;
    }

    @Overwrite
    public static int glGenFramebuffers() {
        return GLES30.glGenFramebuffers();
    }

    @Overwrite
    public static void _glFramebufferTexture2D(int target, int attachment, int textureTarget, int texture, int level) {
        GLES30.glFramebufferTexture2D(target, attachment, textureTarget, texture, level);
    }

    @Overwrite
    public static String glGetShaderInfoLog(int shader, int maxLength) {
        return GLES20.glGetShaderInfoLog(shader, maxLength);
    }

    @Overwrite
    public static String glGetProgramInfoLog(int program, int maxLength) {
        return GLES20.glGetProgramInfoLog(program, maxLength);
    }

    @Overwrite
    public static void _polygonMode(int face, int mode) { }

    @Overwrite
    public static void _polygonOffset(float factor, float units) {
        GLES20.glPolygonOffset(factor, units);
    }

    @Overwrite
    public static void _logicOp(int op) { }

    @Overwrite
    public static void _glBindFramebuffer(int target, int framebuffer) {
        GLES30.glBindFramebuffer(target, framebuffer);
        if (target == 36008 || target == 36160) readFbo = framebuffer;
        if (target == 36009 || target == 36160) writeFbo = framebuffer;
    }

    @Overwrite
    public static void _activeTexture(int texture) {
        GLES20.glActiveTexture(texture);
        activeTexture = texture - 33984;
    }

    @Overwrite
    public static void _texParameter(int target, int pname, int param) {
        GLES20.glTexParameteri(target, pname, param);
    }

    @Overwrite
    public static int _getTexLevelParameter(int target, int level, int pname) {
        return 0;
    }

    @Overwrite
    public static int _genTexture() {
        return GLES20.glGenTextures();
    }

    @Overwrite
    public static void _deleteTexture(int texture) {
        GLES20.glDeleteTextures(texture);
    }

    @Overwrite
    public static void _bindTexture(int texture) {
        GLES20.glBindTexture(3553, texture);
    }

    @Overwrite
    public static void _texImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, ByteBuffer byteBuffer) {
        GLES20.glTexImage2D(target, level, internalFormat, width, height, border, format, type, byteBuffer);
    }

    @Overwrite
    public static void _texSubImage2D(int target, int level, int offsetX, int offsetY, int width, int height, int format, int type, long pixels) {
        GLES20.nglTexSubImage2D(target, level, offsetX, offsetY, width, height, format, type, pixels);
    }

    @Overwrite
    public static void _texSubImage2D(int target, int level, int offsetX, int offsetY, int width, int height, int format, int type, ByteBuffer byteBuffer) {
        GLES20.glTexSubImage2D(target, level, offsetX, offsetY, width, height, format, type, byteBuffer);
    }

    @Overwrite
    public static void _viewport(int x, int y, int width, int height) {
        GLES20.glViewport(x, y, width, height);
    }

    @Overwrite
    public static void _colorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        GLES20.glColorMask(red, green, blue, alpha);
    }

    @Overwrite
    public static void _clear(int mask) {
        GLES20.glClear(mask);
    }

    @Overwrite
    public static void _vertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer) {
        GLES20.glVertexAttribPointer(index, size, type, normalized, stride, pointer);
    }

    @Overwrite
    public static void _vertexAttribIPointer(int index, int size, int type, int stride, long pointer) {
        GLES30.glVertexAttribIPointer(index, size, type, stride, pointer);
    }

    @Overwrite
    public static void _enableVertexAttribArray(int index) {
        GLES20.glEnableVertexAttribArray(index);
    }

    @Overwrite
    public static void _drawElements(int mode, int type, int count, long indices) {
        GLES20.glDrawElements(mode, type, count, indices);
    }

    @Overwrite
    public static void _drawArrays(int mode, int first, int count) {
        GLES20.glDrawArrays(mode, first, count);
    }

    @Overwrite
    public static void _pixelStore(int pname, int param) {
        GLES30.glPixelStorei(pname, param);
    }

    @Overwrite
    public static void _readPixels(int x, int y, int width, int height, int format, int type, long pixels) {
        GLES20.nglReadPixels(x, y, width, height, format, type, pixels);
    }

    @Overwrite
    public static int _getError() {
        return GLES20.glGetError();
    }

    @Overwrite
    public static String _getString(int name) {
        return GLES20.glGetString(name);
    }

    @Overwrite
    public static int _getInteger(int pname) {
        return GLES20.glGetInteger(pname);
    }

    @Overwrite
    public static long _glFenceSync(int condition, int flags) {
        return GLES30.glFenceSync(condition, flags);
    }

    @Overwrite
    public static int _glClientWaitSync(long sync, int flags, long timeout) {
        return GLES30.glClientWaitSync(sync, flags, timeout);
    }

    @Overwrite
    public static void _glDeleteSync(long sync) {
        GLES30.glDeleteSync(sync);
    }
}