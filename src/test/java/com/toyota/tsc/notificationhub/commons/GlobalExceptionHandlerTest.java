package com.toyota.tsc.notificationhub.commons;

import com.toyota.tsc.notificationhub.exceptions.CustomSqlException;
import com.toyota.tsc.notificationhub.exceptions.TscApplicationException;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * クラス：GlobalExceptionHandler すべてのハンドラ分岐を確認するテストケース
 */
class GlobalExceptionHandlerTest {

    /** クラス：GlobalExceptionHandler handleCustomSqlException 400 が返ることを確認するテストケース */
    @Test
    void handleCustomSqlException_01() {
        // 準備
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        CustomSqlException ex = new CustomSqlException("sql");

        try (MockedStatic<CommonUtil> cm = Mockito.mockStatic(CommonUtil.class)) {
            cm.when(() -> CommonUtil.getResultCode(anyString())).thenReturn("RC");

            // 実行
            ResponseEntity<String> r = handler.handleCustomSqlException(ex);

            // 確認
            assertEquals(400, r.getStatusCode().value());
            assertEquals("RC", r.getBody());
        }
    }

    /**
     * クラス：GlobalExceptionHandler handleTscApplicationException 400 が返ることを確認するテストケース
     */
    @Test
    void handleTscApplicationException_01() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        TscApplicationException ex = new TscApplicationException();

        try (MockedStatic<CommonUtil> cm = Mockito.mockStatic(CommonUtil.class)) {
            cm.when(() -> CommonUtil.getResultCode(anyString())).thenReturn("RC");
            ResponseEntity<String> r = handler.handleTscApplicationException(ex);
            assertEquals(400, r.getStatusCode().value());
            assertEquals("RC", r.getBody());
        }
    }

    /** クラス：GlobalExceptionHandler handleRuntimeException 500 が返ることを確認するテストケース */
    @Test
    void handleRuntimeException_01() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        RuntimeException ex = new RuntimeException("boom");

        try (MockedStatic<CommonUtil> cm = Mockito.mockStatic(CommonUtil.class)) {
            cm.when(() -> CommonUtil.getResultCode(anyString())).thenReturn("RC");
            ResponseEntity<String> r = handler.handleRuntimeException(ex);
            assertEquals(500, r.getStatusCode().value());
            assertEquals("RC", r.getBody());
        }
    }

    /** クラス：GlobalExceptionHandler handleException 500 が返ることを確認するテストケース */
    @Test
    void handleException_01() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        Exception ex = new Exception("other");

        try (MockedStatic<CommonUtil> cm = Mockito.mockStatic(CommonUtil.class)) {
            cm.when(() -> CommonUtil.getResultCode(anyString())).thenReturn("RC");
            ResponseEntity<Object> r = handler.handleException(ex);
            assertEquals(500, r.getStatusCode().value());
            assertEquals("RC", r.getBody());
        }
    }
}