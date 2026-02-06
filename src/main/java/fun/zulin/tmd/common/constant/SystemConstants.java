package fun.zulin.tmd.common.constant;

/**
 * 系统常量定义
 */
public class SystemConstants {
    
    /**
     * 数据库相关常量
     */
    public static class Database {
        /** SQLite数据库路径 */
        public static final String DB_PATH = "data/tmd.db";
        /** 数据库日期格式 */
        public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
        /** 时区设置 */
        public static final String TIME_ZONE = "Asia/Shanghai";
    }
    
    /**
     * 文件相关常量
     */
    public static class File {
        /** 下载目录 */
        public static final String DOWNLOAD_DIR = "downloads";
        /** 数据目录 */
        public static final String DATA_DIR = "data";
        /** 最大文件名长度 */
        public static final int MAX_FILENAME_LENGTH = 255;
    }
    
    /**
     * 下载相关常量
     */
    public static class Download {
        /** 默认下载优先级 */
        public static final int DEFAULT_PRIORITY = 16;
        /** 下载超时时间(分钟) */
        public static final int DOWNLOAD_TIMEOUT_MINUTES = 20;
        /** 进度更新频率 */
        public static final int PROGRESS_UPDATE_COUNT = 5;
        /** 速度计算时间窗口(秒) */
        public static final int SPEED_WINDOW_SECONDS = 2;
    }
    
    /**
     * WebSocket相关常量
     */
    public static class WebSocket {
        /** 认证主题 */
        public static final String AUTH_TOPIC = "/topic/auth";
        /** 已下载主题 */
        public static final String DOWNLOADED_TOPIC = "/topic/downloaded";
        /** 下载中主题 */
        public static final String DOWNLOADING_TOPIC = "/topic/downloading";
        /** 心跳间隔(毫秒) */
        public static final long HEARTBEAT_INTERVAL = 30000L;
    }
    
    /**
     * 调度相关常量
     */
    public static class Schedule {
        /** 已下载推送间隔(毫秒) */
        public static final long DOWNLOADED_PUSH_INTERVAL = 2000L;
        /** 下载中推送间隔(毫秒) */
        public static final long DOWNLOADING_PUSH_INTERVAL = 500L;
    }
    
    /**
     * 缓存相关常量
     */
    public static class Cache {
        /** 用户信息缓存名称 */
        public static final String USER_CACHE = "userCache";
        /** 下载项缓存名称 */
        public static final String DOWNLOAD_ITEM_CACHE = "downloadItemCache";
        /** 缓存过期时间(分钟) */
        public static final int CACHE_EXPIRE_MINUTES = 10;
    }
    
    /**
     * 日志相关常量
     */
    public static class Logging {
        /** 系统日志文件名 */
        public static final String LOG_FILE_NAME = "logs/tmd.log";
        /** 日志最大大小 */
        public static final String LOG_MAX_SIZE = "10MB";
        /** 日志保留天数 */
        public static final int LOG_MAX_HISTORY = 30;
    }
    
    /**
     * 反应下载功能开关
     * 系统属性: REACTION_DOWNLOAD_ENABLED
     * 默认值: true (默认启用，无需特别设置)
     * 设置为 false 可禁用反应下载功能
     */
    String REACTION_DOWNLOAD_ENABLED = "REACTION_DOWNLOAD_ENABLED";

}