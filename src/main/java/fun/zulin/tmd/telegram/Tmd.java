package fun.zulin.tmd.telegram;


import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import fun.zulin.tmd.telegram.handler.AuthorizationStateWaitOtherDeviceConfirmationHandler;
import fun.zulin.tmd.telegram.handler.UpdateFileHandler;
import fun.zulin.tmd.telegram.handler.UpdateNewMessageHandler;
import fun.zulin.tmd.utils.DownloadDirectoryManager;
import fun.zulin.tmd.utils.SpringContext;
import it.tdlight.Init;
import it.tdlight.Log;
import it.tdlight.Slf4JLogMessageHandler;
import it.tdlight.client.*;
import it.tdlight.jni.TdApi;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.TimeUnit;


@Slf4j
@Component
public class Tmd {

    public static SimpleTelegramClient client;

    public static TdApi.User me;

    public static TdApi.Chat savedMessagesChat;

    private final static SimpleTelegramClientFactory clientFactory = new SimpleTelegramClientFactory();
    
    private DownloadDirectoryManager directoryManager;


    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        try {

        Init.init();
        Log.setLogMessageHandler(1, new Slf4JLogMessageHandler());

        // 优先从.env文件读取配置，fallback到环境变量
        Properties props = new Properties();
        boolean envFileLoaded = false;
        
        try {
            // 尝试从项目根目录加载.env文件
            File envFile = new File(".env");
            if (envFile.exists()) {
                try (InputStream inputStream = Files.newInputStream(envFile.toPath())) {
                    props.load(inputStream);
                    envFileLoaded = true;
                    log.info("成功从项目根目录加载.env文件");
                }
            }
        } catch (Exception e) {
            log.warn("加载.env文件时出现错误: {}", e.getMessage());
        }
        
        if (!envFileLoaded) {
            log.warn(".env文件未找到，将使用环境变量");
        }
        
        var appId = StrUtil.blankToDefault(props.getProperty("APP_ID"), System.getenv("APP_ID"));
        var apiHash = StrUtil.blankToDefault(props.getProperty("API_HASH"), System.getenv("API_HASH"));
        var testStr = StrUtil.blankToDefault(props.getProperty("Test"), System.getenv("Test"));
        var test = Boolean.valueOf(StrUtil.emptyToDefault(testStr, "false"));

        // 在测试模式下，允许不配置APP_ID和API_HASH
        if (!test) {
            // 生产模式下必须配置
            if (StrUtil.isBlank(appId)) {
                throw new IllegalStateException("APP_ID 未配置，请检查.env文件或环境变量");
            }
            if (StrUtil.isBlank(apiHash)) {
                throw new IllegalStateException("API_HASH 未配置，请检查.env文件或环境变量");
            }
        } else {
            // 测试模式下使用默认值或跳过Telegram初始化
            log.info("运行在测试模式下，跳过Telegram客户端初始化");
            return; // 直接返回，不初始化Telegram客户端
        }
        
        // 安全转换APP_ID为Integer
        Integer appIdInt = Convert.toInt(appId);
        if (appIdInt == null) {
            throw new IllegalStateException("APP_ID 格式错误，必须是数字: " + appId);
        }

        APIToken apiToken = new APIToken(appIdInt, apiHash);
        TDLibSettings settings = TDLibSettings.create(apiToken);

        // 注入目录管理器
        directoryManager = SpringContext.getBean(DownloadDirectoryManager.class);
        
        var dataPath = Path.of("data");
        var downloadsPath = directoryManager.getDownloadPath();
        settings.setDatabaseDirectoryPath(dataPath);
        settings.setDownloadedFilesDirectoryPath(downloadsPath);
        settings.setUseTestDatacenter(test);

        SimpleTelegramClientBuilder clientBuilder = clientFactory.builder(settings);


        SimpleAuthenticationSupplier<?> authenticationData = AuthenticationSupplier.qrCode();

        clientBuilder.addUpdateHandler(TdApi.UpdateAuthorizationState.class, this::onUpdateAuthorizationState);
        clientBuilder.addUpdateHandler(TdApi.UpdateNewMessage.class, UpdateNewMessageHandler::accept);
        clientBuilder.addUpdateHandler(TdApi.UpdateFile.class, UpdateFileHandler::accept);
        

        var clientInteraction = new QrCodeClientInteraction();

        clientBuilder.addUpdateHandler(TdApi.UpdateAuthorizationState.class,
                new AuthorizationStateWaitOtherDeviceConfirmationHandler(clientInteraction)
        );

        clientBuilder.setClientInteraction(clientInteraction);
        client = clientBuilder.build(authenticationData);

        var meAsync = client.getMeAsync();
        me = meAsync.get();
        if (me != null) {
            savedMessagesChat = client.send(new TdApi.CreatePrivateChat(me.id, true)).get(1, TimeUnit.MINUTES);
            //开始下载未完成任务
            DownloadManage.startDownloading();
        }
        
        } catch (Exception e) {
            log.error("Telegram客户端初始化失败: {}", e.getMessage(), e);
            // 不抛出异常，让应用能够继续启动
            // 可以在这里添加降级处理逻辑
        }
    }

    private void onUpdateAuthorizationState(TdApi.UpdateAuthorizationState update) {
        TdApi.AuthorizationState authorizationState = update.authorizationState;
        if (authorizationState instanceof TdApi.AuthorizationStateReady) {
            log.info("[🐛 DEBUG] Telegram认证状态: 已就绪");
            log.info("[🐛 DEBUG] 准备发送 /topic/auth 消息");

            try {
                var simpMessagingTemplate = SpringContext.getBean(SimpMessagingTemplate.class);
                log.info("[🐛 DEBUG] 获取SimpMessagingTemplate成功");
                
                simpMessagingTemplate.convertAndSend("/topic/auth", "ok");
                log.info("[🐛 DEBUG] 已发送 /topic/auth 消息: ok");
                
            } catch (Exception e) {
                log.error("[🐛 DEBUG] 发送 /topic/auth 消息失败: {}", e.getMessage(), e);
            }

        } else if (authorizationState instanceof TdApi.AuthorizationStateClosing) {
            log.info("[🐛 DEBUG] Telegram认证状态: 关闭中");
        } else if (authorizationState instanceof TdApi.AuthorizationStateClosed) {
            log.info("[🐛 DEBUG] Telegram认证状态: 已关闭");
        } else if (authorizationState instanceof TdApi.AuthorizationStateLoggingOut) {
            log.info("[🐛 DEBUG] Telegram认证状态: 登出中");
        } else {
            log.info("[🐛 DEBUG] Telegram认证状态: {}", authorizationState.getClass().getSimpleName());
        }
    }


    public SimpleTelegramClientFactory getClientFactory() {
        return clientFactory;
    }


}
