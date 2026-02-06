package fun.zulin.tmd.telegram.handler;

import fun.zulin.tmd.common.constant.ReactionConstants;
import fun.zulin.tmd.data.item.DownloadItem;
import fun.zulin.tmd.data.item.DownloadItemServiceImpl;
import fun.zulin.tmd.data.item.DownloadState;
import fun.zulin.tmd.telegram.DownloadManage;
import fun.zulin.tmd.telegram.Tmd;
import fun.zulin.tmd.utils.SpringContext;
import it.tdlight.jni.TdApi;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

/**
 * 消息反应处理器
 * 处理用户对消息的反应（如点赞），当检测到特定反应时触发下载
 */
@Slf4j
public class UpdateMessageReactionHandler {

    // 从常量类获取支持的表情符号
    private static final String[] HEART_EMOJIS = ReactionConstants.SupportedReactions.HEART_EMOJIS;

    /**
     * 处理消息反应更新
     * @param update 消息反应更新事件
     */
    public static void accept(TdApi.UpdateMessageReactions update) {
        try {
            // 检查功能是否启用
            if (!isReactionDownloadEnabled()) {
                log.debug("反应下载功能已禁用");
                return;
            }
            
            // 检查是否为目标聊天
            if (update.chatId != Tmd.savedMessagesChat.id) {
                return;
            }

            log.debug("收到消息反应更新 - 聊天ID: {}, 消息ID: {}", update.chatId, update.messageId);

            // 获取完整消息内容
            Tmd.client.send(new TdApi.GetMessage(update.chatId, update.messageId), messageResult -> {
                if (messageResult.isError()) {
                    log.warn("获取消息失败 {}: {}", update.messageId, messageResult.getError().message);
                    return;
                }

                TdApi.Message message = messageResult.get();
                if (message == null) {
                    log.warn("消息为空: {}", update.messageId);
                    return;
                }

                // 检查消息内容类型
                if (message.content instanceof TdApi.MessageVideo video) {
                    // 检查是否有心形反应
                    if (hasHeartReaction(update.reactions)) {
                        log.info("检测到心形反应，准备下载视频 - 消息ID: {}", update.messageId);
                        processVideoMessageWithReaction(message.id, video);
                    }
                } else if (message.content instanceof TdApi.MessageText textMessage) {
                    // 处理文本消息中的链接
                    String text = textMessage.text.text;
                    if (text != null && text.toLowerCase().startsWith("https://t.me")) {
                        // 对于带反应的链接消息也进行处理
                        if (hasHeartReaction(update.reactions)) {
                            log.info("检测到心形反应的链接消息，准备解析 - 消息ID: {}", update.messageId);
                            processLinkMessageWithReaction(message.id, text);
                        }
                    }
                } else {
                    log.debug("消息类型不支持反应下载: {}", message.content.getClass().getSimpleName());
                }
            });

        } catch (Exception e) {
            log.error("处理消息反应时发生异常", e);
        }
    }

    /**
     * 检查反应中是否包含心形表情
     * @param reactions 消息反应数组
     * @return 是否包含心形反应
     */
    private static boolean hasHeartReaction(TdApi.MessageReaction[] reactions) {
        if (reactions == null || reactions.length == 0) {
            return false;
        }

        for (TdApi.MessageReaction reaction : reactions) {
            if (reaction.type instanceof TdApi.ReactionTypeEmoji emojiReaction) {
                String emoji = emojiReaction.emoji;
                for (String heartEmoji : HEART_EMOJIS) {
                    if (heartEmoji.equals(emoji)) {
                        log.debug("匹配到心形表情: {}", emoji);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 处理带有反应的视频消息
     * @param messageId 消息ID
     * @param video 视频消息内容
     */
    private static void processVideoMessageWithReaction(long messageId, TdApi.MessageVideo video) {
        var service = SpringContext.getBean(DownloadItemServiceImpl.class);
        var uniqueId = video.video.video.remote.uniqueId;

        // 检查是否已经下载过
        var existingItem = service.getByUniqueId(uniqueId);
        if (existingItem != null) {
            log.info("视频已存在，跳过下载: {}", existingItem.getFilename());
            return;
        }

        // 获取原始文件名和描述
        String originalFilename = video.video.fileName;
        String captionText = video.caption != null ? video.caption.text : "";

        // 构造描述信息（添加反应标记）
        StringBuilder descriptionBuilder = new StringBuilder();
        if (captionText != null && !captionText.trim().isEmpty()) {
            descriptionBuilder.append(captionText.trim());
        }
        if (originalFilename != null && !originalFilename.trim().isEmpty()) {
            if (descriptionBuilder.length() > 0) {
                descriptionBuilder.append(" - ");
            }
            descriptionBuilder.append(originalFilename.trim());
        }
        
        // 添加反应标记
        descriptionBuilder.append(" [已点赞]");
        
        String description = descriptionBuilder.toString();
        if (description.isEmpty()) {
            description = "Unnamed Video [已点赞]";
        }

        // 创建下载项
        DownloadItem item = DownloadItem.builder()
                .description(description)
                .filename("temp_placeholder")
                .caption(video.caption != null ? video.caption.text : "")
                .createTime(LocalDateTime.now(ZoneId.of("Asia/Shanghai")))
                .downloadedSize(0)
                .fileId(video.video.video.id)
                .fileSize(video.video.video.size)
                .massageId(messageId)
                .uniqueId(uniqueId)
                .state(DownloadState.Created.name())
                .build();

        // 保存到数据库获取ID
        service.save(item);

        // 使用数据库ID生成文件名
        String idBasedFilename = generateIdBasedFilename(item, originalFilename);
        item.setFilename(idBasedFilename);
        service.updateById(item);

        log.info("开始下载被点赞的视频: {} (ID: {})", description, item.getId());

        // 添加到下载队列并开始下载
        DownloadManage.addDownloadingItems(item);
        DownloadManage.download(item);
    }

    /**
     * 使用数据库ID生成文件名
     * @param item 下载项
     * @param originalFilename 原始文件名
     * @return ID-based文件名
     */
    private static String generateIdBasedFilename(DownloadItem item, String originalFilename) {
        Objects.requireNonNull(item.getId(), "数据库ID不能为空");

        // 获取文件扩展名
        String extension = ".mp4"; // 默认扩展名
        if (originalFilename != null && !originalFilename.trim().isEmpty()) {
            int lastDotIndex = originalFilename.lastIndexOf('.');
            if (lastDotIndex > 0) {
                extension = originalFilename.substring(lastDotIndex);
            }
        }

        // 使用数据库ID作为文件名
        return item.getId() + extension;
    }
    
    /**
     * 处理带有反应的链接消息
     * @param messageId 消息ID
     * @param link 链接内容
     */
    private static void processLinkMessageWithReaction(long messageId, String link) {
        Tmd.client.send(new TdApi.GetMessageLinkInfo(link), res -> {
            if (res.get() != null && res.get().message != null) {
                TdApi.Message linkedMessage = res.get().message;
                if (linkedMessage.content instanceof TdApi.MessageVideo video) {
                    log.info("解析链接成功，准备下载视频 - 消息ID: {}", messageId);
                    processVideoMessageWithReaction(messageId, video);
                } else {
                    log.debug("链接指向的消息不是视频类型: {}", linkedMessage.content.getClass().getSimpleName());
                }
            } else {
                log.warn("无法解析链接消息: {}", link);
            }
        });
    }
    
    /**
     * 检查反应下载功能是否启用
     * @return 是否启用
     */
    private static boolean isReactionDownloadEnabled() {
        try {
            String enabledStr = System.getProperty(ReactionConstants.Feature.ENABLE_REACTION_DOWNLOAD);
            if (enabledStr != null) {
                return Boolean.parseBoolean(enabledStr);
            }
            // 检查环境变量
            enabledStr = System.getenv("REACTION_DOWNLOAD_ENABLED");
            if (enabledStr != null) {
                return Boolean.parseBoolean(enabledStr);
            }
            // 返回默认值
            return ReactionConstants.Feature.DEFAULT_ENABLED;
        } catch (Exception e) {
            log.warn("读取反应下载配置时出错，使用默认值: {}", e.getMessage());
            return ReactionConstants.Feature.DEFAULT_ENABLED;
        }
    }
}