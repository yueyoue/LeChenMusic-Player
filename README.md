# LeChenMusic Player

LeChenMusic 私有音乐流媒体服务端的配套 Android APP 播放器。

## 功能

- 🎵 音乐播放（支持后台播放、锁屏控制）
- 📀 专辑浏览与播放
- 🎤 艺人浏览
- 📝 歌单管理
- ❤️ 收藏功能
- 📻 电台播放
- 🔍 搜索功能
- 🌙 深色/浅色主题
- 📱 歌词显示
- 🔄 自动更新检查
- ⏰ 睡眠定时器

## 配套服务端

- [LeChenMusic-Server](https://github.com/yueyoue/LeChenMusic-Server)

## 构建

```bash
./gradlew assembleRelease
```

APK 输出路径: `app/build/outputs/apk/release/`

## 签名

使用 `app/release.keystore.p12` 签名，确保所有版本签名一致。
