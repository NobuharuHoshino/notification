
// ファイルパス: src/test/java/com/toyota/tsc/notificationhub/commons/GlobalExceptionHandlerTest.java
package com.toyota.tsc.notificationhub.commons;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toyota.tsc.notificationhub.exceptions.CustomException;
import com.toyota.tsc.notificationhub.exceptions.TscApplicationException;
import com.toyota.tsc.notificationhub.exceptions.TscNotificationHubsException;
import com.toyota.tsc.notificationhub.exceptions.TscPrimaryContactException;
import com.toyota.tsc.notificationhub.models.ResponseDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * GlobalExceptionHandler のテストクラス
 */
@SuppressWarnings("all")
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * クラス：GlobalExceptionHandler handleTscApplicationException
     * 400で例外の結果コードが返ることを確認するテストケース
     */
    @Test
    void handleTscApplicationException_001() throws Exception {
        // Arrange
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        TscApplicationException ex = mock(TscApplicationException.class);
        when(ex.getResultCode()).thenReturn("RC-APP");

        // Act
        ResponseEntity<ResponseDto> response = handler.handleTscApplicationException(ex);
        String json = mapper.writeValueAsString(response.getBody());

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(json.contains("RC-APP"));
    }

    /**
     * クラス：GlobalExceptionHandler handleTscApplicationException
     * getResultCodeが例外を投げた場合に例外が伝播することを確認するテストケース
     */
    @Test
    void handleTscApplicationException_002() {
        // Arrange
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        TscApplicationException ex = mock(TscApplicationException.class);
        when(ex.getResultCode()).thenThrow(new RuntimeException("boom"));

        // Act
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> handler.handleTscApplicationException(ex));

        // Assert
        assertEquals("boom", thrown.getMessage());
    }

    /**
     * クラス：GlobalExceptionHandler handleTscNotificationHubsException
     * 500で例外の結果コードが返ることを確認するテストケース
     */
    @Test
    void handleTscNotificationHubsException_001() throws Exception {
        // Arrange
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        TscNotificationHubsException ex = mock(TscNotificationHubsException.class);
        when(ex.getResultCode()).thenReturn("RC-HUB");

        // Act
        ResponseEntity<ResponseDto> response = handler.handleTscNotificationHubsException(ex);
        String json = mapper.writeValueAsString(response.getBody());

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(json.contains("RC-HUB"));
    }

    /**
     * クラス：GlobalExceptionHandler handleTscPrimaryContactException
     * 400で例外の結果コードが返ることを確認するテストケース
     */
    @Test
    void handleTscPrimaryContactException_001() throws Exception {
        // Arrange
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        TscPrimaryContactException ex = mock(TscPrimaryContactException.class);
        when(ex.getResultCode()).thenReturn("RC-PC");

        // Act
        ResponseEntity<ResponseDto> response = handler.handleTscPrimaryContactException(ex);
        String json = mapper.writeValueAsString(response.getBody());

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(json.contains("RC-PC"));
    }

    /**
     * クラス：GlobalExceptionHandler handleCustomException
     * 500でEXCEPTIONの結果コードが返ることを確認するテストケース
     */
    @Test
    void handleCustomException_001() throws Exception {
        // Arrange
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        CustomException ex = mock(CustomException.class);

        // Act
        ResponseEntity<ResponseDto> response = handler.handleCustomException(ex);
        String json = mapper.writeValueAsString(response.getBody());

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    /**
     * クラス：GlobalExceptionHandler handleRuntimeException
     * 500でEXCEPTIONの結果コードが返ることを確認するテストケース
     */
    @Test
    void handleRuntimeException_001() throws Exception {
        // Arrange
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        RuntimeException ex = new RuntimeException("x");

        // Act
        ResponseEntity<ResponseDto> response = handler.handleRuntimeException(ex);
        String json = mapper.writeValueAsString(response.getBody());

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    /**
     * クラス：GlobalExceptionHandler handleException
     * 500でEXCEPTIONの結果コードが返ることを確認するテストケース
     */
    @Test
    void handleException_001() throws Exception {
        // Arrange
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        Exception ex = new Exception("x");

        // Act
        ResponseEntity<ResponseDto> response = handler.handleException(ex);
        String json = mapper.writeValueAsString(response.getBody());

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}
