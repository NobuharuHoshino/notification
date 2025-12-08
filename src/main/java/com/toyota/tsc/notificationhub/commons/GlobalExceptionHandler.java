package com.toyota.tsc.notificationhub.commons;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import com.toyota.tsc.notificationhub.exceptions.CustomSqlException;
import com.toyota.tsc.notificationhub.exceptions.TscApplicationException;
import com.toyota.tsc.notificationhub.models.ResponseDto;

/**
 * グローバル例外ハンドラー
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final String EXCEPTION = "EXCEPTION";

    @ExceptionHandler(CustomSqlException.class)
    /**
     * CustomSqlException発生時のハンドリング
     * 
     * @param ex CustomSqlException例外
     * @return 400 Bad Requestレスポンス
     */
    public ResponseEntity<ResponseDto> handleCustomSqlException(CustomSqlException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ResponseDto(CommonUtil.getResultCode(EXCEPTION)));
    }

    @ExceptionHandler(TscApplicationException.class)
    /**
     * TscApplicationException発生時のハンドリング
     * 
     * @param ex TscApplicationException例外
     * @return 400 Bad Requestレスポンス
     */
    public ResponseEntity<ResponseDto> handleTscApplicationException(TscApplicationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ResponseDto(CommonUtil.getResultCode(EXCEPTION)));
    }

    @ExceptionHandler(RuntimeException.class)
    /**
     * RuntimeException発生時のハンドリング
     * 
     * @param ex RuntimeException例外
     * @return 500 Internal Server Errorレスポンス
     */
    public ResponseEntity<ResponseDto> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ResponseDto(CommonUtil.getResultCode(EXCEPTION)));
    }

    @ExceptionHandler(Exception.class)
    /**
     * その他Exception発生時のハンドリング
     * 
     * @param ex Exception例外
     * @return 500 Internal Server Errorレスポンス
     */
    public ResponseEntity<ResponseDto> handleException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ResponseDto(CommonUtil.getResultCode(EXCEPTION)));
    }
}
