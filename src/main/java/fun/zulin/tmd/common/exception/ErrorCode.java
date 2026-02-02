package fun.zulin.tmd.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {
    
    // 系统相关错误
    SYSTEM_ERROR(1000, "系统内部错误"),
    PARAM_ERROR(1001, "参数错误"),
    NOT_FOUND(1002, "资源不存在"),
    
    // Telegram相关错误
    TELEGRAM_CLIENT_NOT_READY(2001, "Telegram客户端未就绪"),
    TELEGRAM_AUTH_FAILED(2002, "Telegram认证失败"),
    TELEGRAM_API_ERROR(2003, "Telegram API调用失败"),
    
    // 下载相关错误
    DOWNLOAD_TASK_EXISTS(3001, "下载任务已存在"),
    DOWNLOAD_TASK_NOT_FOUND(3002, "下载任务不存在"),
    DOWNLOAD_FILE_ERROR(3003, "文件下载失败"),
    DOWNLOAD_PATH_ERROR(3004, "下载路径错误"),
    
    // 数据库相关错误
    DATABASE_ERROR(4001, "数据库操作失败"),
    DATABASE_CONNECTION_ERROR(4002, "数据库连接失败"),
    
    // 权限相关错误
    UNAUTHORIZED(5001, "未授权访问"),
    FORBIDDEN(5002, "权限不足");
    
    private final int code;
    private final String message;
}