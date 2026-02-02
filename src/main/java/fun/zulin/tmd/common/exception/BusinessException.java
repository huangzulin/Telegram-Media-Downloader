package fun.zulin.tmd.common.exception;

import lombok.Getter;

/**
 * 业务异常类
 */
@Getter
public class BusinessException extends RuntimeException {
    
    private final int code;
    private final String message;
    
    public BusinessException(String message) {
        super(message);
        this.code = 500;
        this.message = message;
    }
    
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
    
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }
}