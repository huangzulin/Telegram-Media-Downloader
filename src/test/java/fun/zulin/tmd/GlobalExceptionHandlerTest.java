package fun.zulin.tmd.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    @Test
    void testBusinessExceptionCreation() {
        BusinessException exception = new BusinessException("测试异常");
        
        assertEquals(500, exception.getCode());
        assertEquals("测试异常", exception.getMessage());
    }

    @Test
    void testBusinessExceptionWithErrorCode() {
        BusinessException exception = new BusinessException(ErrorCode.PARAM_ERROR);
        
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());
        assertEquals(ErrorCode.PARAM_ERROR.getMessage(), exception.getMessage());
    }

    @Test
    void testApiResponseSuccess() {
        ApiResponse<String> response = ApiResponse.success("测试数据");
        
        assertTrue(response.isSuccess());
        assertEquals(200, response.getCode());
        assertEquals("success", response.getMessage());
        assertEquals("测试数据", response.getData());
    }

    @Test
    void testApiResponseError() {
        ApiResponse<String> response = ApiResponse.error(400, "错误信息");
        
        assertFalse(response.isSuccess());
        assertEquals(400, response.getCode());
        assertEquals("错误信息", response.getMessage());
        assertNull(response.getData());
    }

    @Test
    void testApiResponseWithErrorEnum() {
        ApiResponse<String> response = ApiResponse.error(ErrorCode.NOT_FOUND);
        
        assertFalse(response.isSuccess());
        assertEquals(ErrorCode.NOT_FOUND.getCode(), response.getCode());
        assertEquals(ErrorCode.NOT_FOUND.getMessage(), response.getMessage());
    }
}