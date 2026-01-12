# ESCraft

**Native OpenGL ES Support for Minecraft | 为 Minecraft 带来原生 GLES 支持**

> ⚠️ **Requirement / 硬性要求**
>
> This mod **strictly requires OpenGL ES 3.2** or higher.
> 本模组**严格要求 OpenGL ES 3.2** 或更高版本，否则无法启动。

---

## 🇬🇧 English Description

### 🚀 What is ESCraft?
ESCraft is a critical compatibility mod designed to enable **Native OpenGL ES** support for Minecraft on platforms where standard desktop OpenGL is unavailable or unstable.

Whether you are running Minecraft on **Android**, **Linux**, or custom embedded environments, this mod solves the rendering bottlenecks and context creation crashes often caused by translation layers or incomplete LWJGL implementations.

It forces Minecraft to communicate directly with your system's **native OpenGL ES drivers (`libGLESv2.so`)**, bypassing broken or missing GLFW functions.

### ✨ Key Features
* **Native GLES Integration:** Bypasses legacy translation layers to use your GPU's native drivers directly.
* **Crash Fixes:** Solves the critical `java.lang.IllegalStateException: There is no OpenGL ES context current` error prevents the game from starting.
* **GLFW Bypass:** Fixes the `NoSuchMethodError: glfwGetProcAddress` issue found in stripped-down or custom LWJGL builds.
* **Smart Detection:** Built-in environment detection ensures the mod only activates on supported GLES platforms.

### 📥 Installation Guide

#### 📱 On Android (Required Setup)
1.  **Check Requirements:** Ensure your device and GPU drivers support **OpenGL ES 3.2**.
2.  **Custom Launcher:** You must use the specific launcher build from **ZalithLauncher_ESCraft**. Download the artifact from the [GitHub Actions here](https://github.com/ItzGreenCat/ZalithLauncher_ESCraft/actions).
3.  **JVM Arguments:** Add the following line to your launcher's JVM Arguments (Runtime arguments):
    ```
    -Dorg.lwjgl.opengles.libname=libGLESv2.so
    ```
4.  **Install Mod:** Install Fabric Loader and place `ESCraft.jar` into your `mods` folder.
5.  **Launch:** Start the game.

#### 💻 On Windows / Linux x64
1.  **Prerequisites:** Ensure your system has a valid **EGL and OpenGL ES 3.2+ environment** (e.g., via drivers or ANGLE).
2.  **Install Mod:** Install Fabric Loader and place `ESCraft.jar` into your `mods` folder.
3.  **Launch:** Start the game.

---

## 🇨🇳 中文介绍

### 🚀 ESCraft 是什么？
ESCraft 是一个关键的兼容性模组，旨在为 Minecraft 提供**原生 OpenGL ES** 支持。

无论你是在 **Android**、**Linux** 还是其他定制的嵌入式环境中运行 Minecraft，本模组都能解决因转换层或 LWJGL 实现不完整而导致的渲染瓶颈和上下文创建崩溃问题。

它不再依赖中间层，而是强制 Minecraft 直接与系统的**原生 OpenGL ES 驱动 (`libGLESv2.so`)** 通信，绕过损坏或缺失的 GLFW 功能。

### ✨ 主要功能
* **原生 GLES 集成：** 绕过旧的转换层，直接调用 GPU 的原生驱动，提升启动成功率。
* **修复启动崩溃：** 彻底解决导致游戏无法启动的 `java.lang.IllegalStateException: There is no OpenGL ES context current` 错误。
* **修复 LWJGL 缺失方法：** 修复了部分精简版或定制版 LWJGL 中 `NoSuchMethodError: glfwGetProcAddress` 的问题。
* **智能环境检测：** 模组内置智能检测逻辑，仅在检测到 GLES 环境时激活修复。

### 📥 安装指南

#### 📱 安卓设备 (必须按步骤操作)
1.  **检查要求：** 确保你的设备硬件及驱动支持 **OpenGL ES 3.2** 或更高版本。
2.  **获取启动器：** 你必须使用 **ZalithLauncher_ESCraft** 的定制版本。请前往 [GitHub Actions](https://github.com/ItzGreenCat/ZalithLauncher_ESCraft/actions) 下载构建好的安装包。
3.  **添加启动参数：** 在启动器的 JVM 参数（运行参数）设置中，必须添加以下代码：
    ```
    -Dorg.lwjgl.opengles.libname=libGLESv2.so
    ```
4.  **安装模组：** 安装 Fabric Loader 并将 `ESCraft.jar` 放入 `mods` 文件夹。
5.  **启动游戏。**

#### 💻 Windows / Linux x64 设备
1.  **环境检查：** 确保你的系统已具备 **EGL 和 OpenGL ES 3.2+ 运行环境**（例如通过显卡驱动支持或 ANGLE）。
2.  **安装模组：** 安装 Fabric Loader 并将 `ESCraft.jar` 放入 `mods` 文件夹。
3.  **启动游戏。**

---

### 🐛 Issues & Source
[GitHub Repository](https://github.com/ItzGreenCat/ESCraft) | [Report Issues](https://github.com/ItzGreenCat/ESCraft/issues)
