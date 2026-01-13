/*package dev.onyxcat.escraft.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderSourceGetter;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengles.GLES;
import org.lwjgl.opengles.GLES20;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSystem.class)
public class MixinRenderSystem {

    @Inject(method = "initRenderer", at = @At("HEAD"))
    private static void onInitRenderer(long windowHandle, int debugVerbosity, boolean sync, ShaderSourceGetter shaderSourceGetter, boolean renderDebugLabels, CallbackInfo ci) {
        System.out.println("[ESCraft] 正在初始化 RenderSystem...");
        System.out.println("[ESCraft] 窗口句柄: " + windowHandle);

        if (windowHandle == 0) {
            throw new RuntimeException("[ESCraft] 致命错误：窗口创建失败 (句柄为0)。请检查 ANGLE DLL 或 GLES 版本设置。");
        }


        try {
            GLES.create();
        } catch (Exception e) {

            System.out.println("[ESCraft] GLES 库状态检查: " + e.getMessage());
        }


        System.out.println("[ESCraft] 正在执行 glfwMakeContextCurrent...");
        GLFW.glfwMakeContextCurrent(windowHandle);


        long error = GLFW.glfwGetError(null);
        if (error != 0) {
            System.err.println("[ESCraft] 警告：glfwMakeContextCurrent 可能失败！错误码: " + error);
        } else {
            System.out.println("[ESCraft] 上下文绑定指令已发送。");
        }
        System.out.println("[ESCraft] 正在创建 GLES Capabilities...");
        try {
            long glfwContext = GLFW.glfwGetCurrentContext();

            System.out.println("GLFW Context Handle: " + glfwContext);

            GLES.createCapabilities();
            String renderer = GLES20.glGetString(GLES20.GL_RENDERER);
            String vendor = GLES20.glGetString(GLES20.GL_VENDOR);
            String version = GLES20.glGetString(GLES20.GL_VERSION);
            System.out.println("[ESCraft] Current Renderer: " + renderer);
            System.out.println("[ESCraft] Current Vendor: " + vendor);
            System.out.println("[ESCraft] Current Version: " + version);
            System.out.println("[ESCraft] GLES 初始化成功！渲染管线已就绪。");
        } catch (Throwable t) {
            System.err.println("[ESCraft] GLES 初始化崩溃！");
            System.err.println("  -> 请检查是否放入了 libEGL.dll 和 libGLESv2.dll");
            throw new RuntimeException("GLES Init Failed", t);
        }
    }
}*/
package dev.onyxcat.escraft.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderSourceGetter;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.tracy.TracyFrameCapturer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengles.*;
import org.lwjgl.system.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.lwjgl.opengles.GLES20;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Locale;

@Mixin(RenderSystem.class)
public class MixinRenderSystem {
    @Inject(method = "initRenderer", at = @At("HEAD"))
    private static void onInitRenderer(long windowHandle, int debugVerbosity, boolean sync, ShaderSourceGetter shaderSourceGetter, boolean renderDebugLabels, CallbackInfo ci) {
        System.out.println("[ESCraft] 正在初始化 RenderSystem (Mixin v4)...");
        System.out.println("[ESCraft] 窗口句柄: " + windowHandle);

        if (windowHandle == 0) {
            throw new RuntimeException("[ESCraft] 致命错误：窗口句柄为0。");
        }
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String osArch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        boolean isPC = osName.contains("windows") || (osName.contains("linux") && (osArch.contains("amd64") || osArch.contains("x86_64")));

        if (isPC) {
            System.out.println("[ESCraft] 检测到 PC 环境，执行默认预加载...");

            try {
                GLES.create();
            } catch (Exception e) {
                System.out.println("[ESCraft] GLES 库状态检查: " + e.getMessage());
            }

            System.out.println("[ESCraft] 执行 glfwMakeContextCurrent...");
            GLFW.glfwMakeContextCurrent(windowHandle);

            System.out.println("[ESCraft] 正在创建 GLES Capabilities...");
            try {
                long glfwContext = GLFW.glfwGetCurrentContext();
                System.out.println("GLFW Context Handle: " + glfwContext);

                GLES.createCapabilities();

                String renderer = GLES20.glGetString(GLES20.GL_RENDERER);
                System.out.println("[ESCraft] Current Renderer: " + renderer);
            } catch (Throwable t) {
                System.err.println("[ESCraft] PC GLES 初始化异常 (可能由原版接管): " + t.getMessage());
            }
        } else {
            System.out.println("[ESCraft] 检测到 Android 环境，执行直连注入...");
            System.setProperty("org.lwjgl.opengl.libname", "libGLESv2.so");
            System.setProperty("org.lwjgl.egl.libname", "libEGL.so");
            GLFW.glfwMakeContextCurrent(windowHandle);
            resetGLESState();
            FunctionProvider provider = new FunctionProvider() {
                private final SharedLibrary glesLib;
                {
                    try {
                        glesLib = Library.loadNative(GLES.class, "org.lwjgl.opengles", "libGLESv2.so");
                    } catch (Throwable t) {
                        throw new RuntimeException("无法加载 libGLESv2.so", t);
                    }
                }
                @Override
                public long getFunctionAddress(CharSequence functionName) {
                    try (MemoryStack stack = MemoryStack.stackPush()) {
                        return getFunctionAddress(stack.ASCII(functionName));
                    }
                }

                @Override
                public long getFunctionAddress(ByteBuffer functionName) {
                    return glesLib.getFunctionAddress(functionName);
                }
            };
            try {
                System.out.println("[ESCraft] 正在注入自定义 Provider...");
                GLES.create(provider);
                GLES.createCapabilities();

                System.out.println("[ESCraft] Android GLES 注入成功: " + GLES20.glGetString(GLES20.GL_RENDERER));
            } catch (Throwable t) {
                throw new RuntimeException("[ESCraft] Android GLES 初始化失败", t);
            }
        }
    }

    private static void resetGLESState() {
        try {
            Field providerField = GLES.class.getDeclaredField("functionProvider");
            providerField.setAccessible(true);
            providerField.set(null, null);
            try {
                Field capsField = GLES.class.getDeclaredField("caps");
                capsField.setAccessible(true);
                capsField.set(null, null);
            } catch (Exception ignored) {}
        } catch (Exception e) {
        }
    }
    @Inject(method = "flipFrame",at = @At("HEAD"))
    private static void onFlipFrame(Window window, TracyFrameCapturer capturer, CallbackInfo ci){
        int err = GLES32.glGetError();
        if(err != 0){
            System.out.println("Error:"+ err);
        }
    }
}