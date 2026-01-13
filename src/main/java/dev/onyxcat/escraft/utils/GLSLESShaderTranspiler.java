package dev.onyxcat.escraft.utils;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Scanner;

import static org.lwjgl.opengles.GLES20.GL_VERTEX_SHADER;
import static org.lwjgl.util.shaderc.Shaderc.*;
import static org.lwjgl.util.spvc.Spvc.*;

public class GLSLESShaderTranspiler {

    public static String transpile(String originalSource, int shaderType) {
        if (originalSource == null || originalSource.isEmpty()) return originalSource;
        String workingSource = sanitizeSource(originalSource);

        long compiler = 0;
        long options = 0;
        long result = 0;

        try {
            compiler = shaderc_compiler_initialize();
            options = shaderc_compile_options_initialize();

            shaderc_compile_options_set_target_env(options, shaderc_target_env_opengl, shaderc_env_version_opengl_4_5);
            shaderc_compile_options_set_forced_version_profile(options, 330, shaderc_profile_core);


            shaderc_compile_options_set_optimization_level(options, shaderc_optimization_level_zero);

            shaderc_compile_options_set_auto_map_locations(options, true);
            shaderc_compile_options_set_auto_bind_uniforms(options, true);

            int kind = (shaderType == GL_VERTEX_SHADER) ? shaderc_glsl_vertex_shader : shaderc_glsl_fragment_shader;
            result = shaderc_compile_into_spv(compiler, workingSource, kind, "mc_shader", "main", options);

            if (shaderc_result_get_compilation_status(result) != shaderc_compilation_status_success) {
                String error = shaderc_result_get_error_message(result);
                System.err.println("[SPIRV] Shaderc Compile failed: " + error);
                return originalSource;
            }

            ByteBuffer spirvData = shaderc_result_get_bytes(result);
            IntBuffer spirvIntBuffer = MemoryUtil.memAllocInt(spirvData.remaining() / 4);
            spirvIntBuffer.put(spirvData.asIntBuffer());
            spirvIntBuffer.flip();

            String glesSource = crossCompileToESSL(spirvIntBuffer);
            MemoryUtil.memFree(spirvIntBuffer);
            String glesSourceTemp = glesSource != null ? glesSource.replace("uniform highp isampler2D CloudFaces","uniform highp isamplerBuffer CloudFaces") : originalSource;
            glesSourceTemp = glesSourceTemp.replace("texelFetch(CloudFaces, ivec2(index, 0), 0)","texelFetch(CloudFaces, index)");
            glesSourceTemp = glesSourceTemp.replace("texelFetch(CloudFaces, ivec2(index + 1, 0), 0)", "texelFetch(CloudFaces, index + 1)");
            glesSourceTemp = glesSourceTemp.replace("texelFetch(CloudFaces, ivec2(index + 2, 0), 0)","texelFetch(CloudFaces, index + 2)");
            return glesSourceTemp;

        } catch (Exception e) {
            System.err.println("[SPIRV] Transpiler Exception: " + e.getMessage());
            e.printStackTrace();
            return originalSource;
        } finally {
            if (result != 0) shaderc_result_release(result);
            if (options != 0) shaderc_compile_options_release(options);
            if (compiler != 0) shaderc_compiler_release(compiler);
        }
    }

    private static String sanitizeSource(String source) {
        StringBuilder sb = new StringBuilder();
        try (Scanner scanner = new Scanner(source)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.trim().startsWith("#version")) continue;
                if (line.trim().startsWith("#line")) continue;
                sb.append(line).append("\n");
            }
        }
        sb.append("\n");
        return sb.toString();
    }

    private static String crossCompileToESSL(IntBuffer spirv) {
        long context = 0;
        String resultSource = null;
        PointerBuffer contextPtr = null;
        PointerBuffer irPtr = null;
        PointerBuffer compilerPtr = null;
        PointerBuffer sourcePtr = null;
        PointerBuffer optionsPtr = null;

        try {
            contextPtr = MemoryUtil.memAllocPointer(1);
            check(spvc_context_create(contextPtr));
            context = contextPtr.get(0);

            irPtr = MemoryUtil.memAllocPointer(1);
            check(spvc_context_parse_spirv(context, spirv, spirv.remaining(), irPtr));
            long ir = irPtr.get(0);

            compilerPtr = MemoryUtil.memAllocPointer(1);
            check(spvc_context_create_compiler(context, SPVC_BACKEND_GLSL, ir, SPVC_CAPTURE_MODE_TAKE_OWNERSHIP, compilerPtr));
            long compiler = compilerPtr.get(0);

            optionsPtr = MemoryUtil.memAllocPointer(1);
            check(spvc_compiler_create_compiler_options(compiler, optionsPtr));
            long options = optionsPtr.get(0);

            check(spvc_compiler_options_set_uint(options, SPVC_COMPILER_OPTION_GLSL_VERSION, 310));
            check(spvc_compiler_options_set_bool(options, SPVC_COMPILER_OPTION_GLSL_ES, true));
            check(spvc_compiler_options_set_bool(options, SPVC_COMPILER_OPTION_GLSL_ENABLE_420PACK_EXTENSION, false));

            try {
                check(spvc_compiler_options_set_bool(options, 44, false));
            } catch (Exception ignored) {}

            check(spvc_compiler_install_compiler_options(compiler, options));

            sourcePtr = MemoryUtil.memAllocPointer(1);
            check(spvc_compiler_compile(compiler, sourcePtr));
            resultSource = sourcePtr.getStringUTF8(0);

            if (resultSource != null) {

                if (resultSource.contains("samplerBuffer")) {
                    resultSource = resultSource.replace("samplerBuffer", "sampler2D");
                    resultSource = resultSource.replace("isamplerBuffer", "isampler2D");
                    resultSource = resultSource.replace("usamplerBuffer", "usampler2D");
                    resultSource = resultSource.replaceAll("texelFetch\\(([^,]+), ([^,)]+)\\)", "texelFetch($1, ivec2($2, 0), 0)");
                    resultSource = resultSource.replaceAll("textureSize\\(([^,)]+)\\)", "textureSize($1, 0).x");
                }


                StringBuilder cleanSource = new StringBuilder();
                String[] lines = resultSource.split("\n");

                for (String line : lines) {

                    if (line.contains("mediump")) {
                        line = line.replace("mediump", "highp");
                    }
                    if (line.contains("lowp")) {
                        line = line.replace("lowp", "highp");
                    }

                    if (line.contains("layout")) {


                        line = line.replaceAll("binding\\s*=\\s*\\d+", "");

                        line = line.replaceAll("location\\s*=\\s*\\d+", "");

                        line = line.replaceAll("set\\s*=\\s*\\d+", "");





                        line = line.replace(", ,", ",");
                        line = line.replace("(,", "(");
                        line = line.replace(",)", ")");
                        line = line.replace(", )", ")");



                        line = line.replaceAll("layout\\s*\\(\\s*\\)", "");


                    }
                    cleanSource.append(line).append("\n");
                }
                resultSource = cleanSource.toString();
            }

        } catch (Exception e) {
            System.err.println("[SPIRV-Cross] Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (contextPtr != null) MemoryUtil.memFree(contextPtr);
            if (irPtr != null) MemoryUtil.memFree(irPtr);
            if (compilerPtr != null) MemoryUtil.memFree(compilerPtr);
            if (sourcePtr != null) MemoryUtil.memFree(sourcePtr);
            if (optionsPtr != null) MemoryUtil.memFree(optionsPtr);
            if (context != 0) spvc_context_destroy(context);
        }

        return resultSource;
    }

    private static void check(int result) {
        if (result != SPVC_SUCCESS) {
            throw new RuntimeException("SPIRV-Cross failed with error code: " + result);
        }
    }
}