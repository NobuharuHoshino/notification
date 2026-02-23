package com.toyota.tsc.notificationhub.commons;

import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.toyota.tsc.notificationhub.exceptions.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SendGridUtil のテストクラス
 */
@SuppressWarnings("all")
@ExtendWith(MockitoExtension.class)
class SendGridUtilTest {

    @Mock
    private PropertiesUtil propertiesUtil;

    /** クラス：SendGridUtil generateEmail TOYOTAブランド(1)でMailが生成されることを確認するテストケース */
    @Test
    void generateEmail_001() {
        // Arrange
        when(propertiesUtil.getFromAddressToyota()).thenReturn("from@toyota.example");
        when(propertiesUtil.getFromNameToyota()).thenReturn("Toyota");
        SendGridUtil sut = new SendGridUtil(propertiesUtil);

        // Act
        Mail mail = sut.generateEmail("to@example", "title", "body", null, "1");

        // Assert
        assertNotNull(mail);
        assertEquals("title", mail.getSubject());
    }

    /** クラス：SendGridUtil generateEmail 無効ブランド(0)でもTOYOTA扱いでMailが生成されることを確認するテストケース */
    @Test
    void generateEmail_002() {
        // Arrange
        when(propertiesUtil.getFromAddressToyota()).thenReturn("from@toyota.example");
        when(propertiesUtil.getFromNameToyota()).thenReturn("Toyota");
        SendGridUtil sut = new SendGridUtil(propertiesUtil);

        // Act
        Mail mail = sut.generateEmail("to@example", "title", "body", null, "0");

        // Assert
        assertNotNull(mail);
    }

    /** クラス：SendGridUtil generateEmail LEXUSブランド(2)でMailが生成されることを確認するテストケース */
    @Test
    void generateEmail_003() {
        // Arrange
        when(propertiesUtil.getFromAddressLexus()).thenReturn("from@lexus.example");
        when(propertiesUtil.getFromNameLexus()).thenReturn("Lexus");
        SendGridUtil sut = new SendGridUtil(propertiesUtil);

        // Act
        Mail mail = sut.generateEmail("to@example", "title", "body", null, "2");

        // Assert
        assertNotNull(mail);
        assertEquals("title", mail.getSubject());
    }

    /** クラス：SendGridUtil generateEmail 不正ブランドの場合にnullが返ることを確認するテストケース */
    @Test
    void generateEmail_004() {
        // Arrange
        SendGridUtil sut = new SendGridUtil(propertiesUtil);

        // Act
        Mail mail = sut.generateEmail("to@example", "title", "body", null, "9");

        // Assert
        assertNull(mail);
    }

    /** クラス：SendGridUtil generateEmail bodyTextがnullの場合にMailが生成されることを確認するテストケース */
    @Test
    void generateEmail_005() {
        // Arrange
        when(propertiesUtil.getFromAddressToyota()).thenReturn("from@toyota.example");
        when(propertiesUtil.getFromNameToyota()).thenReturn("Toyota");
        SendGridUtil sut = new SendGridUtil(propertiesUtil);

        // Act
        Mail mail = sut.generateEmail("to@example", "title", null, null, "1");

        // Assert
        assertNotNull(mail);
    }

    /** クラス：SendGridUtil generateEmail bodyTextが空の場合にMailが生成されることを確認するテストケース */
    @Test
    void generateEmail_006() {
        // Arrange
        when(propertiesUtil.getFromAddressToyota()).thenReturn("from@toyota.example");
        when(propertiesUtil.getFromNameToyota()).thenReturn("Toyota");
        SendGridUtil sut = new SendGridUtil(propertiesUtil);

        // Act
        Mail mail = sut.generateEmail("to@example", "title", "", null, "1");

        // Assert
        assertNotNull(mail);
    }

    /** クラス：SendGridUtil generateEmail bodyHtmlが指定される場合にMailが生成されることを確認するテストケース */
    @Test
    void generateEmail_007() {
        // Arrange
        when(propertiesUtil.getFromAddressToyota()).thenReturn("from@toyota.example");
        when(propertiesUtil.getFromNameToyota()).thenReturn("Toyota");
        SendGridUtil sut = new SendGridUtil(propertiesUtil);

        // Act
        Mail mail = sut.generateEmail("to@example", "title", "body", "<b>html</b>", "1");

        // Assert
        assertNotNull(mail);
    }

    /** クラス：SendGridUtil generateEmail bodyHtmlが空の場合にMailが生成されることを確認するテストケース */
    @Test
    void generateEmail_008() {
        // Arrange
        when(propertiesUtil.getFromAddressToyota()).thenReturn("from@toyota.example");
        when(propertiesUtil.getFromNameToyota()).thenReturn("Toyota");
        SendGridUtil sut = new SendGridUtil(propertiesUtil);

        // Act
        Mail mail = sut.generateEmail("to@example", "title", "body", "", "1");

        // Assert
        assertNotNull(mail);
    }

    /** クラス：SendGridUtil executeSendEmail 2xxレスポンスの場合に正常終了することを確認するテストケース */
    @Test
    void executeSendEmail_001() throws Exception {
        // Arrange
        when(propertiesUtil.getSendGridApiKey()).thenReturn("SG-KEY");
        SendGridUtil sut = new SendGridUtil(propertiesUtil);
        Mail mail = mock(Mail.class);
        when(mail.build()).thenReturn("{}");

        Response resp = mock(Response.class);

        try (MockedConstruction<SendGrid> mocked = mockConstruction(SendGrid.class,
                (mock, ctx) -> when(mock.api(any(Request.class))).thenReturn(resp))) {

            // Act
            assertDoesNotThrow(() -> sut.executeSendEmail(mail));

            // Assert
            SendGrid sg = mocked.constructed().get(0);
            verify(sg, times(1)).api(any(Request.class));
        }
    }

    /** クラス：SendGridUtil executeSendEmail 非2xxレスポンスの場合にResponseが返ることを確認するテストケース */
    @Test
    void executeSendEmail_002() throws Exception {
        // Arrange
        when(propertiesUtil.getSendGridApiKey()).thenReturn("SG-KEY");
        SendGridUtil sut = new SendGridUtil(propertiesUtil);
        Mail mail = mock(Mail.class);
        when(mail.build()).thenReturn("{}");

        Response resp = mock(Response.class);
        when(resp.getStatusCode()).thenReturn(400);

        try (MockedConstruction<SendGrid> mocked = mockConstruction(SendGrid.class,
                (mock, ctx) -> when(mock.api(any(Request.class))).thenReturn(resp))) {

            // Act
            Response result = sut.executeSendEmail(mail);

            // Assert
            assertNotNull(result);
            assertEquals(400, result.getStatusCode());
        }
    }

    /** クラス：SendGridUtil executeSendEmail SendGridAPI呼び出しでRuntimeExceptionが発生した場合にCustomExceptionが送出されることを確認するテストケース */
    @Test
    void executeSendEmail_003() throws Exception {
        // Arrange
        when(propertiesUtil.getSendGridApiKey()).thenReturn("SG-KEY");
        SendGridUtil sut = new SendGridUtil(propertiesUtil);
        Mail mail = mock(Mail.class);
        when(mail.build()).thenReturn("{}");

        try (MockedConstruction<SendGrid> mocked = mockConstruction(SendGrid.class,
                (mock, ctx) -> when(mock.api(any(Request.class))).thenThrow(new RuntimeException("boom")))) {

            // Act
            CustomException ex = assertThrows(CustomException.class, () -> sut.executeSendEmail(mail));

            // Assert
            assertNotNull(ex.getCause());
        }
    }

    /** クラス：SendGridUtil executeSendEmail mail.buildが例外の場合にCustomExceptionが送出されることを確認するテストケース */
    @Test
    void executeSendEmail_004() throws Exception {
        // Arrange
        when(propertiesUtil.getSendGridApiKey()).thenReturn("SG-KEY");
        SendGridUtil sut = new SendGridUtil(propertiesUtil);
        Mail mail = mock(Mail.class);
        when(mail.build()).thenThrow(new RuntimeException("build-fail"));

        try (MockedConstruction<SendGrid> mocked = mockConstruction(SendGrid.class)) {

            // Act
            CustomException ex = assertThrows(CustomException.class, () -> sut.executeSendEmail(mail));

            // Assert
            assertNotNull(ex.getCause());
        }
    }
}
