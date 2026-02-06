# 点赞消息下载功能说明

## 功能概述

本功能允许用户通过对Telegram消息添加心形表情（点赞）来触发视频下载，无需手动发送链接。

## 支持的反应类型

以下表情都会触发下载：
- ❤️ 红心
- ♥️ 黑桃心  
- 💖 闪亮心
- 💘 箭穿心
- 💝 礼物心
- 💓 跳动心
- 💗 成长心
- 💕 两心
- 💞 旋转心
- 💟 装饰心
- 😍 爱心眼
- 🥰 微笑脸
- 😻 猫爱心
- 👍 大拇指
- 🔥 火焰
- 💯 百分百

## 配置选项

### 环境变量配置

```bash
# 启用反应下载功能（默认已启用）
export REACTION_DOWNLOAD_ENABLED=true

# 禁用反应下载功能
export REACTION_DOWNLOAD_ENABLED=false
```

### 系统属性配置

```bash
# 启用反应下载功能（默认已启用）
-DREACTION_DOWNLOAD_ENABLED=true

# 禁用反应下载功能
-DREACTION_DOWNLOAD_ENABLED=false
```

### application.yml 配置

```yaml
tmd:
  reaction:
    download:
      # 是否启用反应下载功能（默认启用）
      enabled: ${REACTION_DOWNLOAD_ENABLED:true}
```

> ⚠️ **注意**: 反应下载功能现在默认启用，除非明确设置 `REACTION_DOWNLOAD_ENABLED=false`，否则无需额外配置。

## 使用方法

1. 在Telegram中找到想要下载的视频消息
2. 对该消息添加任意支持的心形表情
3. 系统会自动检测到反应并开始下载视频
4. 下载的视频会在视频库中显示，并带有"[已点赞]"标记

## 技术实现

### 主要组件

1. **UpdateMessageReactionHandler** - 消息反应处理器
2. **ReactionConstants** - 反应相关常量配置
3. **配置注入** - 在Tmd.java中注册处理器

### 工作流程

```
用户添加反应 → TDLib通知 → UpdateMessageReactionHandler接收 → 
检查配置和消息类型 → 解析视频内容 → 创建下载任务 → 开始下载
```

## 注意事项

- 仅支持Saved Messages聊天中的消息
- 重复的反应不会触发重复下载
- 已经下载过的视频会被跳过
- 支持直接视频消息和链接形式的视频消息

## 故障排除

如果功能不工作，请检查：

1. 确认功能已启用（查看日志中的"反应下载功能已禁用"信息）
2. 检查是否在正确的聊天中添加反应（必须是Saved Messages）
3. 查看应用日志确认是否有相关错误信息
4. 验证网络连接和Telegram认证状态