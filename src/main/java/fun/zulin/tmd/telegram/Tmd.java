package fun.zulin.tmd.telegram;


import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.setting.dialect.Props;
import fun.zulin.tmd.telegram.handler.AuthorizationStateWaitOtherDeviceConfirmationHandler;
import fun.zulin.tmd.telegram.handler.UpdateFileHandler;
import fun.zulin.tmd.telegram.handler.UpdateNewMessageHandler;
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

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.TimeUnit;


@Component
public class Tmd {

    public static SimpleTelegramClient client;

    public static TdApi.User me;

    public static TdApi.Chat savedMessagesChat;

    private final static SimpleTelegramClientFactory clientFactory = new SimpleTelegramClientFactory();


    @EventListener(ApplicationReadyEvent.class)
    public void init() throws Exception {

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
                    System.out.println("成功从项目根目录加载.env文件");
                }
            }
        } catch (Exception e) {
            System.err.println("加载.env文件时出现错误: " + e.getMessage());
        }
        
        if (!envFileLoaded) {
            System.err.println(".env文件未找到，将使用环境变量");
        }
        
        var appId = StrUtil.blankToDefault(props.getProperty("APP_ID"), System.getenv("APP_ID"));
        var apiHash = StrUtil.blankToDefault(props.getProperty("API_HASH"), System.getenv("API_HASH"));
        var testStr = StrUtil.blankToDefault(props.getProperty("Test"), System.getenv("Test"));
        var test = Boolean.valueOf(StrUtil.emptyToDefault(testStr, "false"));

        // 验证必要配置是否存在
        if (StrUtil.isBlank(appId)) {
            throw new IllegalStateException("APP_ID 未配置，请检查.env文件或环境变量");
        }
        if (StrUtil.isBlank(apiHash)) {
            throw new IllegalStateException("API_HASH 未配置，请检查.env文件或环境变量");
        }
        
        // 安全转换APP_ID为Integer
        Integer appIdInt = Convert.toInt(appId);
        if (appIdInt == null) {
            throw new IllegalStateException("APP_ID 格式错误，必须是数字: " + appId);
        }

        APIToken apiToken = new APIToken(appIdInt, apiHash);
        TDLibSettings settings = TDLibSettings.create(apiToken);

        var dataPath = Paths.get("data");
        Files.createDirectories(dataPath);
        var downloadsPath = Paths.get("downloads");
        Files.createDirectories(downloadsPath);
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

    }

    private void onUpdateAuthorizationState(TdApi.UpdateAuthorizationState update) {
        TdApi.AuthorizationState authorizationState = update.authorizationState;
        if (authorizationState instanceof TdApi.AuthorizationStateReady) {
            System.out.println("Logged in");

            var simpMessagingTemplate = SpringContext.getBean(SimpMessagingTemplate.class);
            simpMessagingTemplate.convertAndSend("/topic/auth", "ok");

        } else if (authorizationState instanceof TdApi.AuthorizationStateClosing) {
            System.out.println("Closing...");
        } else if (authorizationState instanceof TdApi.AuthorizationStateClosed) {
            System.out.println("Closed");
        } else if (authorizationState instanceof TdApi.AuthorizationStateLoggingOut) {
            System.out.println("Logging out...");
        }
    }


    public SimpleTelegramClientFactory getClientFactory() {
        return clientFactory;
    }


}
