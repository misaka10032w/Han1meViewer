#!/bin/bash -e
# 精简版 ffmpeg 构建脚本 —— 替换 abdallahmehiz/mpv-android 的
# buildscripts/scripts/ffmpeg.sh（基于 v0.1.7 tag，即 Han1meViewer 当前依赖版本）
#
# !!! 重要教训（实测踩坑）: ffmpeg 7.0.2 的 configure 对
#     --enable-protocol=a,b,c 这类"逗号列表"在部分类别（protocol/demuxer/
#     filter/bsf）会【静默忽略】—— configure 正常完成、不报错，但组件全没编进去
#     （decoder/parser 恰好又有效）。必须用重复单标志形式：
#         --enable-protocol=file --enable-protocol=http ...
#     构建完务必检查 configure.log 的 "Enabled protocols/demuxers" 段非空！
#
# 实测体积 (arm64, ffmpeg 7.0.2, NDK r21, --enable-small, 2026-11):
#   当前 AAR av 全家 8 个 .so：19.55 MB (APK 内 deflate 后 9.76 MB)
#   本配置产物 8 个 .so：      3.83 MB (APK 内 deflate 后 1.71 MB)
#   => 每架构 APK 净省约 7.5~8 MB
#
# 注意：
#   1. 必须用 fork 锁定的 ffmpeg 版本（v0.1.7 → Lavc61.11.100，7.0 系），
#      soname 对不上 libmpv 会加载失败
#   2. mbedtls 保留（应用的 tls-verify / tls-ca-file / https 播放依赖它）
#   3. dav1d (AV1) 已移除；vulkan 维持禁用（与当前 AAR 行为一致，UI 的
#      Vulkan 硬解选项本来就无效）
#   4. 本脚本保留原版的多架构骨架：armv7 / aarch64 / x86 / x86_64 全适用

. ../../include/path.sh

if [ "$1" == "build" ]; then
	true
elif [ "$1" == "clean" ]; then
	rm -rf _build$ndk_suffix
	exit 0
else
	exit 255
fi

mkdir -p _build$ndk_suffix
cd _build$ndk_suffix

cpu=armv7-a
[[ "$ndk_triple" == "aarch64"* ]] && cpu=armv8-a
[[ "$ndk_triple" == "x86_64"* ]] && cpu=generic
[[ "$ndk_triple" == "i686"* ]] && cpu="i686 --disable-asm"

cpuflags=
[[ "$ndk_triple" == "arm"* ]] && cpuflags="$cpuflags -mfpu=neon -mcpu=cortex-a8"

# 组件清单（重复单标志，勿改成逗号列表）
PROTOCOLS="file http https tcp tls crypto pipe fd"
DEMUXERS="hls mpegts mov matroska aac flac mp3 ogg wav"
DECODERS="h264 hevc mpeg2video mpeg4 aac aac_latm mp3 flac opus vorbis ac3 eac3 h264_mediacodec hevc_mediacodec"
PARSERS="h264 hevc mpegvideo mpeg4video mpegaudio aac aac_latm ac3 flac opus vorbis"
BSFS="aac_adtstoasc h264_mp4toannexb hevc_mp4toannexb extract_extradata"
FILTERS="aformat aresample format scale null anull buffer abuffer buffersink abuffersink"

args=(
	--target-os=android --enable-cross-compile
	--cross-prefix=$ndk_triple- --cc=$CC --pkg-config=pkg-config --nm=llvm-nm
	--arch=${ndk_triple%%-*} --cpu=$cpu
	--extra-cflags="-I$prefix_dir/include $cpuflags" --extra-ldflags="-L$prefix_dir/lib"

	# 保留：JNI 桥接、MediaCodec 硬解、mbedtls (TLS)
	--enable-{jni,mediacodec,mbedtls}
	--disable-vulkan
	--disable-static --enable-shared --enable-{gpl,version3}

	--disable-{stripping,doc,programs,muxers,encoders,devices}

	# ===== 核心裁剪 =====
	# (--enable-small 可再省 32% 体积但软解慢 5~15%，按需打开)
	--disable-everything
)

for x in $PROTOCOLS; do args+=(--enable-protocol=$x); done
for x in $DEMUXERS;  do args+=(--enable-demuxer=$x);  done
for x in $DECODERS;  do args+=(--enable-decoder=$x);  done
for x in $PARSERS;   do args+=(--enable-parser=$x);   done
for x in $BSFS;      do args+=(--enable-bsf=$x);      done
for x in $FILTERS;   do args+=(--enable-filter=$x);   done

../configure "${args[@]}"

# 防御性校验：组件真的进去了（逗号列表事故的教训，见文件头注释）
grep -q "CONFIG_HLS_DEMUXER 1" config_components.h \
	|| { echo "FATAL: hls demuxer 未启用，中止"; exit 1; }
grep -q "CONFIG_FD_PROTOCOL 1" config_components.h \
	|| { echo "FATAL: fd 协议未启用，中止"; exit 1; }

make -j$cores
make DESTDIR="$prefix_dir" install
