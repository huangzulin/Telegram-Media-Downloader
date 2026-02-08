package fun.zulin.tmd.telegram.handler;

import fun.zulin.tmd.data.item.DownloadItem;
import fun.zulin.tmd.data.item.DownloadItemServiceImpl;
import fun.zulin.tmd.data.item.DownloadState;
import fun.zulin.tmd.telegram.DownloadManage;
import fun.zulin.tmd.telegram.Tmd;
import fun.zulin.tmd.utils.SpringContext;
import it.tdlight.jni.TdApi;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

@Slf4j
public class UpdateNewMessageHandler {

    public static void accept(TdApi.UpdateNewMessage update) {

        var messageContent = update.message.content;
        final long messageId = update.message.id;
        final long chatId = update.message.chatId; // 保存chatId用于后续使用
        TdApi.Chat savedMessagesChat = Tmd.savedMessagesChat;

        if (chatId != savedMessagesChat.id) {
            return;
        }

        if (messageContent instanceof TdApi.MessageText messageText) {
            // Get the text of the text message
            final String text = messageText.text.text;
            if (Strings.isNotBlank(text) && text.toLowerCase().startsWith("https://t.me")) {
                Tmd.client.send(new TdApi.GetMessageLinkInfo(text), res -> {
                    if (res != null && !res.isError()) {
                        TdApi.MessageLinkInfo linkInfo = res.get();
                        if (linkInfo != null && linkInfo.message != null && 
                            linkInfo.message.content instanceof TdApi.MessageVideo video) {
                            log.info("在saved messages中检测到视频链接: {}", text);
                            processVideoMessage(messageId, video, chatId);
                        } else {
                            log.debug("链接指向的消息不是视频类型或无法解析");
                        }
                    } else {
                        log.warn("无法解析链接消息: {}", text);
                    }
                });
            }
        } else {
            if (messageContent instanceof TdApi.MessageVideo video) {
                processVideoMessage(messageId, video, chatId);
            }
        }

    }

    public static void processVideoMessage(long messageId, TdApi.MessageVideo video, long chatId) {
        var service = SpringContext.getBean(DownloadItemServiceImpl.class);
        var uniqueId = video.video.video.remote.uniqueId;

        var item = service.getByUniqueId(uniqueId);
        if (item != null) {
            return;
        }
        // 获取原始文件名作为描述
        String originalFilename = video.video.fileName;
        String captionText = video.caption.text;
        
        // 构造完整的描述信息
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
        
        String description = descriptionBuilder.toString();
        if (description.isEmpty()) {
            description = "Unnamed Video";
        }
        
        // 不再处理Telegram缩略图，直接使用本地生成
        
        // 先保存获取数据库ID
        item = DownloadItem.builder()
                .description(description)  // 原始描述，包含特殊字符
                .filename("temp_placeholder") // 临时占位符
                .caption(video.caption.text)
                .createTime(LocalDateTime.now(ZoneId.of("Asia/Shanghai")))
                .downloadedSize(0)
                .fileId(video.video.video.id)
                .fileSize(video.video.video.size)
                .massageId(messageId)
                .chatId(chatId)  // 保存chatId用于恢复下载
                .uniqueId(uniqueId)
                .state(DownloadState.Created.name())
                .build();
        service.save(item);
        
        // 使用数据库ID作为文件名
        String idBasedFilename = generateIdBasedFilename(item, originalFilename);
        item.setFilename(idBasedFilename);
        service.updateById(item);
        //
        DownloadManage.addDownloadingItems(item);
        // 下载视频
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

}
