package fun.zulin.tmd.common.exception;

import lombok.Data;

/**
 * 统一响应结果封装
 */
@Data
public class ApiResponse<T> {
    
    private int code;
    private String message;
    private T data;
    private long timestamp;
    
    public ApiResponse() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public ApiResponse(int code, String message, T data) {
        this();
        this.code = code;
        this.message = message;
        this.data = data;
    }
    
    /**
     * 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data);
    }
    
    /**
     * 成功响应(无数据)
     */
    public static <T> ApiResponse<T> success() {
        return success(null);
    }
    
    /**
     * 失败响应
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
    
    /**
     * 失败响应(使用错误码)
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return error(errorCode.getCode(), errorCode.getMessage());
    }
    
    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return code == 200;
    }
}