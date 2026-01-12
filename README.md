# ESCraft

**Native OpenGL ES Translation Layer for Minecraft**

![API](https://img.shields.io/badge/Source_API-OpenGL_3.3+-blue?style=for-the-badge)
![Target_API](https://img.shields.io/badge/Target_API-OpenGL_ES_3.2+-green?style=for-the-badge)
![Platform](https://img.shields.io/badge/Platform-Android_|_Windows_x64_|_Linux_x64-orange?style=for-the-badge)

ESCraft is a high-performance rendering compatibility mod that translates Minecraft's desktop OpenGL calls into native OpenGL ES commands. 

> [!IMPORTANT]
> **Supported Platforms:** Windows x64, Linux x64, and Android.  
> **支持平台：** 仅限 Windows x64、Linux x64 以及安卓设备。

---

## 🇬🇧 English Description

### 🛠️ How it Works
Unlike traditional wrappers, ESCraft operates at the engine level to redirect rendering calls:
* **Call Translation:** Intercepts desktop OpenGL invocations and translates them into their **OpenGL ES** equivalents.
* **Shader Transpilation:** Utilizes **ShaderC** and **SpirvC** to dynamically transpile Minecraft's GLSL shaders into GLES-compatible shader code (ESSL) on the fly.
* **SPIR-V Integration:** Leverages intermediate representation to ensure high-fidelity graphics reconstruction on GLES drivers.

### 🌟 Key Features
* **Performance-Driven:** Designed to minimize overhead by utilizing native GLES 3.2+ features directly.
* **Advanced Shader Support:** Handles complex core profile shaders by converting them through a robust SPIR-V pipeline.
* **Hardware Bridge:** Allows modern Minecraft (OpenGL 3.3+) to run on Android GPUs and GLES-centric environments without heavy translation layers like GL4ES.

---

## 🇨🇳 中文介绍

### 🛠️ 技术原理
ESCraft 是一个深度的渲染兼容层模组，通过在底层修改 Minecraft 的渲染行为来实现跨 API 支持：
* **调用转换：** 将 Minecraft 原生的桌面级 OpenGL 调用动态转换为 **OpenGL ES** 指令。
* **Shader 转译：** 集成了 **ShaderC** 与 **SpirvC**，将游戏的 GLSL Shader 实时转译为兼容 GLES 的着色器代码（ESSL）。
* **SPIR-V 流水线：** 通过引入 SPIR-V 中间层，确保即便在移动端或嵌入式驱动上也能精准重现游戏的视觉效果。

### 🌟 核心特性
* **原生级效率：** 针对 GLES 3.2+ 特性优化，直接调用驱动接口，减少性能损耗。
* **动态着色器处理：** 完美处理现代版本 Minecraft 的核心配置文件（Core Profile）着色器。
* **硬件桥接：** 让原本需要 OpenGL 3.3+ 的现代 Minecraft 能够在仅支持 GLES 的安卓或嵌入式设备上原生运行。

---

## 📥 Installation / 安装指南

### 📱 Android (Required / 必须按步骤操作)
1. **Check Requirements:** Ensure your device and GPU drivers support **OpenGL ES 3.2** or higher.
   **检查要求：** 确保你的设备硬件及驱动支持 **OpenGL ES 3.2** 或更高版本。
2. **Custom Launcher:** You **must** use the specific build of **ZalithLauncher_ESCraft**. Download it from [GitHub Actions](https://github.com/ItzGreenCat/ZalithLauncher_ESCraft/actions). **Do not** choose the "GL4ES" renderer.
   **获取启动器：** 你必须使用 **ZalithLauncher_ESCraft** 的定制版本。请前往 [GitHub Actions](https://github.com/ItzGreenCat/ZalithLauncher_ESCraft/actions) 下载。**注意：不要选择 GL4ES 渲染器。**
3. **JVM Arguments:** Add the following line to your launcher's JVM Arguments (Runtime arguments):
   **添加启动参数：** 在启动器的 JVM 参数（运行参数）设置中，必须添加以下代码：
   -Dorg.lwjgl.opengles.libname=libGLESv2.so
4. **Install Mod:** Install Fabric Loader and place the `ESCraft.jar` into your `mods` folder.
   **安装模组：** 安装 Fabric Loader 并将 `ESCraft.jar` 放入 `mods` 文件夹。

### 💻 Windows x64 / Linux x64
1. **Prerequisites:** Ensure your system has a valid **EGL and OpenGL ES 3.2+ environment** (e.g., via native drivers or ANGLE).
   **环境检查：** 确保你的系统已具备 **EGL 和 OpenGL ES 3.2+ 运行环境**。
2. **Install Mod:** Install Fabric Loader and place the `ESCraft.jar` into your `mods` folder.
   **安装模组：** 安装 Fabric Loader 并将 `ESCraft.jar` 放入 `mods` 文件夹。

---

### 🔗 Resources
* **Source Code:** [GitHub Repository](https://github.com/ItzGreenCat/ESCraft)
* **Bug Tracker:** [Report Issues](https://github.com/ItzGreenCat/ESCraft/issues)
