package com.toyota.tsc.notificationhub.commons;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import com.toyota.tsc.notificationhub.exceptions.CustomSqlException;
import com.toyota.tsc.notificationhub.exceptions.TscApplicationException;

/**
 * グローバル例外ハンドラー
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomSqlException.class)
    /**
     * CustomSqlException発生時のハンドリング
     * 
     * @param ex CustomSqlException例外
     * @return 400 Bad Requestレスポンス
     */
    public ResponseEntity<String> handleCustomSqlException(CustomSqlException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(CommonUtil.getResultCode("EXCEPTION"));
    }

    @ExceptionHandler(TscApplicationException.class)
    /**
     * TscApplicationException発生時のハンドリング
     * 
     * @param ex TscApplicationException例外
     * @return 400 Bad Requestレスポンス
     */
    public ResponseEntity<String> handleTscApplicationException(TscApplicationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(CommonUtil.getResultCode("EXCEPTION"));
    }

    @ExceptionHandler(RuntimeException.class)
    /**
     * RuntimeException発生時のハンドリング
     * 
     * @param ex RuntimeException例外
     * @return 500 Internal Server Errorレスポンス
     */
    public ResponseEntity<String> handleRuntimeException(RuntimeException ex) {
        // 共通のランタイム例外は500 Internal Server Errorで返却
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(CommonUtil.getResultCode("EXCEPTION"));
    }

    @ExceptionHandler(Exception.class)
    /**
     * その他Exception発生時のハンドリング
     * 
     * @param ex Exception例外
     * @return 500 Internal Server Errorレスポンス
     */
    public ResponseEntity<Object> handleException(Exception ex) {
        // その他例外は500 Internal Server Errorで返却
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CommonUtil.getResultCode("EXCEPTION"));
    }
}
