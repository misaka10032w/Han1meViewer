# mpv 按需重编译方案（Han1meViewer 定制版）

> **✅ 已实施完成（2026-11-15）**，最终成果：
> - 基线升级：ffmpeg **n7.0.2-dev → n7.1.1**、mpv **→ v0.42.0**、NDK r28b
> - arm64 release APK：**20 → 13.88 MB**（含此前 packaging 优化共 **39 → 13.88 MB，-64%**）
> - 四架构分发包：arm64 13.88 / v7a 13.50 / x86_64 14.19 / x86 14.02 MB
> - strip 后原生库（arm64 未压缩）：av 全家 6.3 MB + libmpv 5.9 MB + libc++ 1.25 MB
> - 已真机验证：PJZ110 (Android 16) debug/release 双变体启动、libmpv 加载、
>   MPVLib init 正常（logcat 确认 libavcodec 61.19.101 = n7.1.1）
> - AAR 位于 `app/libs/mpv-android-lite-0.1.7.aar`；MpvKernel.kt 有两处小 API 适配
>   （tag 版 `getPropertyBoolean` 返回可空、EventObserver 新增 MPVNode 重载）
> - WSL 构建环境保留在 `~/mpv-lite`，重编入口 `bash /mnt/d/fftrim/build_all.sh native|aar`
> - 注意：AAR 阶段必须先 strip prefix 与 libc++_shared（原 CI 环节，本地路径默认缺失）


> 目标：把 `com.github.abdallahmehiz:mpv-android:v0.1.7` 里的 ffmpeg 全家桶
> 从"全量编译"换成按本应用实际播放场景的精简编译，只换 ffmpeg 的 8 个 `.so`，
> **libmpv / libplayer / libc++_shared 原样复用**，Java/Kotlin API 零改动。
> 支持后续扩展 x86 / x86_64 / armeabi-v7a 分架构包。

## 一、实测收益（arm64，ffmpeg 7.0.2 + NDK r21 本地交叉编译）

| | 未压缩 | APK 内 deflate 后 |
|---|---|---|
| 当前 AAR av 全家 8 个 .so | 19.55 MB | 9.76 MB |
| **精简版（推荐配置）** | **3.83 MB** | **~2.0 MB**（含 mbedtls 修正 +0.3） |
| 精简版不开 `--enable-small` | 5.48 MB | 2.37 MB |

- libavcodec 11.43 → 2.01 MB，libavformat 2.55 → 0.58 MB，libavfilter 4.02 → 0.15 MB
- **每架构 release APK 净省约 7.5~8 MB**（当前 arm64 release 20 MB → 约 12 MB）
- 音频解码器与 hevc 软解全保留只多花 ~0.3 MB（压缩后），不值得裁
- 工具链差异（新 NDK、-O2）预计 ±10~15%

多架构预估（按同比例外推，v7a/x86 为估算值）：

| ABI | 当前 av 全家(未压缩) | 精简后预估(未压缩) | 精简后 APK 内 |
|---|---|---|---|
| arm64-v8a | 19.55 MB | **3.83 MB（实测）** | ~2.0 MB |
| armeabi-v7a | 17.71 MB | ~3.8 MB | ~2.0 MB |
| x86_64 | 25.17 MB | ~5.0 MB | ~2.6 MB |
| x86 | 21.88 MB | ~4.3 MB | ~2.3 MB |

## 二、剪裁明细（对照当前 AAR 的全量构建）

> **⚠️ 已知限制（2026-11-16 决策：暂不补救）**：应用有 `video/*` 的 ACTION_VIEW
> intent-filter（外部文件可直接打开，格式不可控）。当前裁剪集下：
> - ✅ h264/hevc 系文件基本全通（MP4/MKV/TS/MOV + aac/mp3/ac3/flac/ogg 音频）
> - ❌ WebM(VP9/VP8/AV1)、AVI/RMVB/WMV/FLV/VOB 容器、DTS/PCM 音轨（有像无声）、
>   WAV（无声）、MKV 内嵌 PGS/VobSub 字幕
>
> 若需补救，在 ffmpeg.sh 的组件清单加回对应项后重编（压缩后增量进 APK）：
> - **A 档 +0.9MB**：vp9 vp8 + 解析器、dts、pcm_s16le pcm_s24le pcm_s32le pcm_u8
>   pcm_f32le、avi/mpegps demuxer、alac、theora、av1_mediacodec、pgssub、dvdsub
> - **B 档再加 +1.3MB**：dav1d（AV1 软解，依赖 buildall 里恢复 dav1d 组件）
> - **C 档再加 +0.7MB**：rv40/cook/sipr + rm demuxer、vc1 + asf demuxer（rmvb/wmv）

### 保留（应用实际用到）

| 类别 | 组件 | 为什么 |
|---|---|---|
| 视频软解 | h264, hevc, mpeg2video, mpeg4 | SW 档兜底；h264/hevc 为主，mpeg2/4 给老 MP4 兜底 |
| 硬解 wrapper | h264_mediacodec, hevc_mediacodec | 设置里 Auto/HW/HW+ 三档硬解 |
| 音频软解 | aac, aac_latm, mp3, flac, opus, vorbis, ac3, eac3 | 全部加起来压缩后 <0.2 MB，全留 |
| 解复用 | hls, mpegts, mov(mp4系), matroska, aac, flac, mp3, ogg, wav | HLS 段(TS/fMP4)、MP4 直链、本地文件 |
| 协议 | file, http, https, tcp, tls, crypto, pipe, **fd** | 网络流 + 本地下载经 `fd://` 播放（MpvKernel.kt:356） |
| 码流过滤 | aac_adtstoasc, h264_mp4toannexb, hevc_mp4toannexb, **extract_extradata** | extract_extradata 是 MediaCodec 初始化必需 |
| 滤镜 | aformat/aresample/format/scale/null/anull/buffer 系 | libavfilter 是 mpv 硬依赖，留最小集 |
| 功能开关 | jni, mediacodec, mbedtls, gpl, version3, --enable-small | TLS/证书校验依赖 mbedtls；small 省 32% |

### 移除（当前 AAR 里有、应用用不到）

| 类别 | 移除内容 | 省多少（未压缩） |
|---|---|---|
| AV1 软解 | **dav1d**（整个 AV1 解码器） | ~2-3 MB |
| 其他视频解码器 | vp8/vp9/av1/theora/wmv/real/mjpeg/png/... 共 200+ 个 | ~4 MB |
| 其他音频解码器 | pcm 全家/ape/tta/amr/... | ~0.5 MB |
| 其他解复用器 | avi/flv/rtsp/dash/... 共 200+ 个 | ~1 MB |
| 其他协议 | rtmp/ftp/smb/rtsp/... | ~0.3 MB |
| 全部 encoder | 当前版留了 mjpeg/png（截图用，本应用无截图） | ~0.3 MB |
| 全部 muxer | 当前版留了 mov/matroska/mpegts（dump-cache 用，本应用未用） | ~0.3 MB |
| libavfilter 滤镜 | 400+ 个滤镜 | ~3.8 MB |

### 不动的部分
- `libmpv.so`（5.47 MB）：渲染器(gpu/gpu-next)、libass、Anime4K 的 GLSL 管线全在里面
- `libplayer.so`、`libc++_shared.so`
- `subfont.ttf` 已在 app 侧用 `ignoreAssetsPattern` 剔除，与本次无关

## 三、施工步骤

### 0. Fork 并建分支

Fork `abdallahmehiz/mpv-android`，**基于 `v0.1.7` tag** 建 `lite` 分支。
不要用 `master`/`library` 分支——已重构成 `MPVSession` API，与
`MpvKernel.kt` 用的 `MPVLib.EventObserver` 不兼容。

### 1. 替换 ffmpeg.sh

用本目录的 [ffmpeg-trimmed.sh](ffmpeg-trimmed.sh) 覆盖
`buildscripts/scripts/ffmpeg.sh`。

> **关键坑（实测踩过）**：ffmpeg 7.0.2 的 configure 对
> `--enable-protocol=a,b,c` 逗号列表在 protocol/demuxer/filter/bsf 类别
> 会静默忽略（configure 正常退出、组件没编、不报错）。必须用重复单标志。
> 脚本里已内置"hls demuxer 未启用即中止"的防御校验。
> 顺手删掉 `buildscripts/scripts/dav1d.sh` 在 `buildall.sh` 里的引用。

### 2. 多架构原生构建（WSL 或 Linux）

```bash
cd buildscripts
./download.sh                     # 装 SDK/NDK/源码（可符号链接现有 SDK）
./buildall.sh --arch armv7l mpv   # 默认就是 v7a；每次换架构先跑这个
./buildall.sh --arch arm64 mpv
./buildall.sh --arch x86 mpv
./buildall.sh --arch x86_64 mpv
# 每架构约 20~40 分钟；产物在 buildscripts/prefix/（按架构分后缀）
```

构建后自检（每架构都要过）：
```bash
llvm-nm prefix*/lib/libavformat.so.* | grep -c "ff_hls_demuxer"   # ≥1
llvm-nm prefix*/lib/libavformat.so.* | grep -c "ff_fd_protocol"   # ≥1
llvm-nm prefix*/lib/libavcodec.so.* | grep -c "mediacodec"        # ≥1
llvm-nm prefix*/lib/libavcodec.so.* | grep -c "dav1d"             # = 0
```

### 3. 组装 AAR

v0.1.7 的 `app/src/main/jniLibs` 是个指向构建产物的**软链接**（5 字节），
`app/build.gradle` 可能有下载预编译 deps 的任务（JitPack 能构建出带 .so 的
AAR，说明构建链路里有来源）。处理方式：

1. 删掉 `jniLibs` 软链接，建真实目录 `app/src/main/jniLibs/<abi>/`
2. 每架构从 `buildscripts/prefix*/lib/` 复制 8 个 `libav*.so` / `libsw*.so` /
   `libpostproc.so`（去掉 `.so.NN` 版本号后缀，对齐 AAR 现有命名），
   再放回**原版未动**的 `libmpv.so` / `libc++_shared.so`
3. `./gradlew :app:assembleRelease` 出 AAR（`libplayer.so` 由 ndk-build
   从 `app/src/main/jni/` 现编，自动链接你的精简 deps）
4. 版本号建议 tag `v0.1.7-lite.1`

### 4. 分发二选一

- **本地 AAR（推荐，个人项目最省事）**：AAR 拷到 Han1meViewer `app/libs/`，
  `implementation(files("libs/mpv-android-v0.1.7-lite.aar"))` 替换原依赖
- **JitPack**：fork 打 tag 推上去，`libs.versions.toml` 里
  `mpv = "v0.1.7-lite.1"`，仓库 `com.github.<你的GitHub名>:mpv-android`

### 5. app 侧多架构分包

后期要发 x86/x86_64/v7a 时，改 [app/build.gradle.kts](../../app/build.gradle.kts)：

```kotlin
splits {
    abi {
        isEnable = (gradle.startParameter.taskRequests.toString().contains("Release"))
        reset()
        include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
        isUniversalApk = false
    }
}
```

输出 4 个 APK，命名在 `androidComponents.onVariants` 里按
`output.filters` 取 ABI 后缀（当前写死了 `Han1meViewer-v${version}.apk`，
多架构时记得加上 `_${abi}`，否则 4 个包同名互相覆盖）。

## 四、回归测试清单（真机，每架构）

- [ ] MPV 内核：HLS 播放，各清晰度切换（协议 hls/mpegts/mov + http/https）
- [ ] MPV 内核：getchu 源 MP4 直链
- [ ] 本地下载视频播放（`fd://` 路径，含断点续播）
- [ ] 硬解三档：Auto / HW(mediacodec-copy) / HW+(mediacodec)
- [ ] **SW 纯软解档**（软解只剩 h264/hevc + 常见音频，本站内容应全过）
- [ ] Anime4K 超分各档（gpu 渲染器 GLSL，与本次改动无关但顺手验）
- [ ] `tls-verify` 开/关各播一次（mbedtls 路径）
- [ ] 断网重连、倍速、循环播放
- [ ] x86/x86_64：模拟器安装 + 播放（注意 x86 软解无 SIMD，模拟器靠宿主 CPU + MediaCodec）

## 五、故障对照表（出问题往回加）

| 症状 | 加回的 configure 项 |
|---|---|
| 某链接打不开/黑屏无日志 | 对应 demuxer（如 dash）；先 `adb logcat -s mpv` 看 lavf 报错 |
| 有画面无声音 | 对应音频 decoder/parser |
| MediaCodec 初始化失败 | 确认 `--enable-jni --enable-mediacodec` 和 extract_extradata bsf 在 |
| https 播放失败 | 确认 mbedtls 构建成功、tls/https 协议在（本机实验没编 TLS，正式构建必须带） |
| `dlopen failed: library "libavcodec.so.61" not found` | soname 不匹配——没用 fork 锁定的 ffmpeg 版本，重对 depinfo.sh |
| 本地文件打不开 | fd/file/pipe 协议缺失 |

## 六、长期维护

- **上游 fork 更新**：`buildscripts/include/depinfo.sh` 锁版本。升 ffmpeg
  大版本 = soname 变（libavcodec.so.61 → .62）= libmpv 必须一起重编，
  那就要动 `mpv.sh`（顺带可 `-Dlua=disabled` 再省 ~0.4 MB）
- **升级 Han1meViewer 播放需求**（如将来加字幕/更多格式）：往第二节的
  保留清单里加组件，重新 buildall 即可，脚本骨架不用动
- 本机实验产物：`D:\fftrim`（源码 + 3 个配置构建树，~1.5 GB，可删）

## 附：实验方法备忘

本次数据来自 ffmpeg 7.0.2 官方源码 + 本机 NDK r21 clang 交叉编译
（Windows 上用 Git Bash configure + busybox-w32 当 make 的 SHELL +
NDK 自带 make，`LN_S` 覆盖为 `cp -f` 绕过 Windows 符号链接权限）。
压缩率按 APK 内实测 deflate（当前 AAR arm64 全套 50% 保留率）核算。
若在 WSL 里复现，直接用标准工具链即可，无需这些绕路。
