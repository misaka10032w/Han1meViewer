# Han1meViewer
🔞R18警告！
Han1meViewer 是一个基于 Kotlin 开发的 Android 应用，用于播放和浏览 hanime 视频资源，支持双栏与单栏布局切换、ExoPlayer 自定义播放内核、系列视频导航、播放列表和推荐内容展示等功能。

## 📜 项目来源与许可声明

此项目最初 Fork 自 [YenalyLiew/Han1meViewer](https://github.com/YenalyLiew/Han1meViewer)，感谢原作者的贡献。原项目采用 [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)。

根据 Apache 2.0 许可证要求：
- 保留原版权声明
- 包含许可证文件副本
- 说明修改内容（如有）

**原始仓库**: [YenalyLiew/Han1meViewer](https://github.com/YenalyLiew/Han1meViewer)  
**许可证文件**: [本项目 LICENSE 文件](LICENSE)

---

## ✨ 功能特色

* 🎞️ 视频播放：基于 ExoPlayer 实现，封装为 `ExoMediaKernel`，用于自定义的 `JZMediaInterface` 播放核心。
* 📄 视频详情页：采用 `Fragment` 形式展示，包含标题、画质选择、播放列表、相关视频等模块。
* 📚 双栏/单栏切换：用户可点击按钮在「详情页 + 播放页」双栏布局 和 单页面布局之间切换。
* 🎨 动态布局：使用 `LinearLayout` 实现灵活的左右布局比例（2:3），可动态交换主内容和导航栏位置。
* 🧭 Navigation 支持：支持 `NavigationView` 进行功能切换，集成 Jetpack Navigation 架构组件。
* 🔄 数据绑定与状态管理：使用 `StateFlow` + `ViewModel` 管理视频加载状态（Loading、Success、Error 等）。
* 📦 播放列表与推荐视频：支持水平/网格列表显示，使用 `ConcatAdapter` 拼接多种类型的 RecyclerView Adapter。

## 📷 截图预览

> ![img.png](img.png) ![img_1.png](img_1.png)
> ![img_2.png](img_2.png)
> ![img_3.png](img_3.png)

## 🛠️ 技术栈

* Kotlin
* Jetpack Navigation
* ViewModel + StateFlow
* ExoPlayer
* JZPlayer 自定义接口
* Fragment + ConcatAdapter 多类型布局
* 动态布局比例切换与视图位置交换
* ......

## 📂 项目结构概览

```
app/
├── activity/            # 主界面、播放器界面
├── fragment/            # 视频详情页 Fragment（VideoIntroductionFragment）
├── exoplayer/           # 自定义 ExoMediaKernel 播放内核
├── adapter/             # 多种 RecyclerView Adapter（系列视频、推荐视频等）
├── model/               # HanimeVideo 数据模型
├── viewmodel/           # VideoViewModel 控制状态流
└── res/
    ├── layout/          # 各类 XML 布局文件
    ├── menu/            # Toolbar 与 NavigationView 菜单
    └── drawable/        # 图标与背景资源
```

## 🧪 使用说明

### 运行环境

* Android Studio 可靠编译版本：Android Studio Meerkat Feature Drop | 2024.3.2 Patch 1 
Build AI-243.26053.27.2432.13536105, built on May 22, 2025
* 最低支持 Android 7.0 (API 24 Nougat)
* 目标版本 Android 15 (API 35)
* androidGradle 8.9.0
* kotlin 2.0.21
* serializationPlugin 2.0.21
* ksp 2.0.21-1.0.27

### 启动流程

1. 克隆项目：

   ```bash
   git clone https://github.com/misaka10032w/Han1meViewer.git
   ```

2. 使用 Android Studio 打开并同步 Gradle。

3. 编译测试。

### 视频数据说明

* 视频数据通过 `VideoViewModel` 提供，`StateFlow` 形式推送状态；
* `HanimeVideo` 数据结构包含系列、推荐、订阅等字段。


## 🧩 TODO

* [✅] 或许有吧


## 📄 许可证

本项目继承原始项目的 [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)，主要条款包括：
- 允许商用、修改、分发
- 要求保留版权声明和许可证文件
- 提供修改说明（如有）
- 不提供质量担保
- 不承担用户使用风险

完整条款请参阅项目根目录下的 [LICENSE](LICENSE) 文件。

