
// // ファイルパス:
// src/test/java/com/toyota/tsc/notificationhub/commons/SendGridUtilTest.java
// package com.toyota.tsc.notificationhub.commons;

// import com.sendgrid.Request;
// import com.sendgrid.Response;
// import com.sendgrid.SendGrid;
// import com.sendgrid.helpers.mail.Mail;
// import com.toyota.tsc.notificationhub.exceptions.CustomException;
// import com.toyota.tsc.notificationhub.exceptions.TscEMailException;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.Mock;
// import org.mockito.MockedConstruction;
// import org.mockito.junit.jupiter.MockitoExtension;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.Mockito.*;

// /**
// * SendGridUtil のテストクラス
// */
// @SuppressWarnings("all")
// @ExtendWith(MockitoExtension.class)
// class SendGridUtilTest {

// @Mock
// private PropertiesUtil propertiesUtil;

// /** クラス：SendGridUtil generateEmail TOYOTAブランド(1)でMailが生成されることを確認するテストケース */
// @Test
// void generateEmail_001() {
// // Arrange
// when(propertiesUtil.getFromAddressToyota()).thenReturn("from@toyota.example");
// when(propertiesUtil.getFromNameToyota()).thenReturn("Toyota");

// SendGridUtil sut = new SendGridUtil(propertiesUtil);

// // Act
// Mail mail = sut.generateEmail("to@example", "title", "body", null, "1");

// // Assert
// assertNotNull(mail);
// assertEquals("title", mail.getSubject());
// }

// /**
// * クラス：SendGridUtil generateEmail 無効ブランド(0)でもTOYOTA扱いでMailが生成されることを確認するテストケース
// */
// @Test
// void generateEmail_002() {
// // Arrange
// when(propertiesUtil.getFromAddressToyota()).thenReturn("from@toyota.example");
// when(propertiesUtil.getFromNameToyota()).thenReturn("Toyota");

// SendGridUtil sut = new SendGridUtil(propertiesUtil);

// // Act
// Mail mail = sut.generateEmail("to@example", "title", "body", null, "0");

// // Assert
// assertNotNull(mail);
// }

// /** クラス：SendGridUtil generateEmail LEXUSブランド(2)でMailが生成されることを確認するテストケース */
// @Test
// void generateEmail_003() {
// // Arrange
// when(propertiesUtil.getFromAddressLexus()).thenReturn("from@lexus.example");
// when(propertiesUtil.getFromNameLexus()).thenReturn("Lexus");

// SendGridUtil sut = new SendGridUtil(propertiesUtil);

// // Act
// Mail mail = sut.generateEmail("to@example", "title", "body", null, "2");

// // Assert
// assertNotNull(mail);
// assertEquals("title", mail.getSubject());
// }

// /** クラス：SendGridUtil generateEmail 不正ブランドの場合にnullが返ることを確認するテストケース */
// @Test
// void generateEmail_004() {
// // Arrange
// SendGridUtil sut = new SendGridUtil(propertiesUtil);

// // Act
// Mail mail = sut.generateEmail("to@example", "title", "body", null, "9");

// // Assert
// assertNull(mail);
// }

// /** クラス：SendGridUtil generateEmail bodyTextがnullの場合にMailが生成されることを確認するテストケース
// */
// @Test
// void generateEmail_005() {
// // Arrange
// when(propertiesUtil.getFromAddressToyota()).thenReturn("from@toyota.example");
// when(propertiesUtil.getFromNameToyota()).thenReturn("Toyota");

// SendGridUtil sut = new SendGridUtil(propertiesUtil);

// // Act
// Mail mail = sut.generateEmail("to@example", "title", null, null, "1");

// // Assert
// assertNotNull(mail);
// }

// /** クラス：SendGridUtil generateEmail bodyTextが空の場合にMailが生成されることを確認するテストケース */
// @Test
// void generateEmail_006() {
// // Arrange
// when(propertiesUtil.getFromAddressToyota()).thenReturn("from@toyota.example");
// when(propertiesUtil.getFromNameToyota()).thenReturn("Toyota");

// SendGridUtil sut = new SendGridUtil(propertiesUtil);

// // Act
// Mail mail = sut.generateEmail("to@example", "title", "", null, "1");

// // Assert
// assertNotNull(mail);
// }

// /** クラス：SendGridUtil generateEmail bodyHtmlが指定される場合にMailが生成されることを確認するテストケース
// */
// @Test
// void generateEmail_007() {
// // Arrange
// when(propertiesUtil.getFromAddressToyota()).thenReturn("from@toyota.example");
// when(propertiesUtil.getFromNameToyota()).thenReturn("Toyota");

// SendGridUtil sut = new SendGridUtil(propertiesUtil);

// // Act
// Mail mail = sut.generateEmail("to@example", "title", "body", "<b>html</b>",
// "1");

// // Assert
// assertNotNull(mail);
// }

// /** クラス：SendGridUtil generateEmail bodyHtmlが空の場合にMailが生成されることを確認するテストケース */
// @Test
// void generateEmail_008() {
// // Arrange
// when(propertiesUtil.getFromAddressToyota()).thenReturn("from@toyota.example");
// when(propertiesUtil.getFromNameToyota()).thenReturn("Toyota");

// SendGridUtil sut = new SendGridUtil(propertiesUtil);

// // Act
// Mail mail = sut.generateEmail("to@example", "title", "body", "", "1");

// // Assert
// assertNotNull(mail);
// }

// /** クラス：SendGridUtil executeSendEmail 2xxレスポンスの場合に例外が発生しないことを確認するテストケース */
// @Test
// void executeSendEmail_001() throws Exception {
// // Arrange
// when(propertiesUtil.getSendGridApiKey()).thenReturn("SG-KEY");

// SendGridUtil sut = new SendGridUtil(propertiesUtil);
// Mail mail = mock(Mail.class);
// when(mail.build()).thenReturn("{}");

// Response resp = mock(Response.class);
// when(resp.getStatusCode()).thenReturn(202);

// try (MockedConstruction<SendGrid> mocked = mockConstruction(SendGrid.class,
// (mock, ctx) -> when(mock.api(any(Request.class))).thenReturn(resp))) {

// // Act
// assertDoesNotThrow(() -> sut.executeSendEmail(mail));

// // Assert
// SendGrid sg = mocked.constructed().get(0);
// verify(sg, times(1)).api(any(Request.class));
// }
// }

// /**
// * クラス：SendGridUtil executeSendEmail
// * 2xx以外の場合にTscEMailExceptionが送出されることを確認するテストケース
// */
// @Test
// void executeSendEmail_002() throws Exception {
// // Arrange
// when(propertiesUtil.getSendGridApiKey()).thenReturn("SG-KEY");

// SendGridUtil sut = new SendGridUtil(propertiesUtil);
// Mail mail = mock(Mail.class);
// when(mail.build()).thenReturn("{}");
// when(mail.getSubject()).thenReturn("title");

// com.sendgrid.helpers.mail.objects.Email to = new
// com.sendgrid.helpers.mail.objects.Email("to@example");
// com.sendgrid.helpers.mail.objects.Personalization p = new
// com.sendgrid.helpers.mail.objects.Personalization();
// p.addTo(to);
// when(mail.getPersonalization()).thenReturn(java.util.List.of(p));

// Response resp = mock(Response.class);
// when(resp.getStatusCode()).thenReturn(400);

// try (MockedConstruction<SendGrid> mocked = mockConstruction(SendGrid.class,
// (mock, ctx) -> when(mock.api(any(Request.class))).thenReturn(resp))) {

// // Act
// TscEMailException ex = assertThrows(TscEMailException.class, () ->
// sut.executeSendEmail(mail));

// // Assert
// assertEquals(400, ex.getStatusCode());
// assertEquals("to@example", ex.getAddress());
// assertEquals("title", ex.getTitle());
// }
// }

// /**
// * クラス：SendGridUtil executeSendEmail
// *
// SendGridAPI呼び出しでTscEMailExceptionが発生した場合にTscEMailExceptionが送出されることを確認するテストケース
// */
// @Test
// void executeSendEmail_003() throws Exception {
// // Arrange
// when(propertiesUtil.getSendGridApiKey()).thenReturn("SG-KEY");

// SendGridUtil sut = new SendGridUtil(propertiesUtil);
// Mail mail = mock(Mail.class);
// when(mail.build()).thenReturn("{}");

// TscEMailException cause = new TscEMailException(500, "to@example", "title");

// try (MockedConstruction<SendGrid> mocked = mockConstruction(SendGrid.class,
// (mock, ctx) -> when(mock.api(any(Request.class))).thenThrow(cause))) {

// // Act
// TscEMailException ex = assertThrows(TscEMailException.class, () ->
// sut.executeSendEmail(mail));

// // Assert
// assertEquals(500, ex.getStatusCode());
// assertEquals("to@example", ex.getAddress());
// assertEquals("title", ex.getTitle());
// }
// }

// /**
// * クラス：SendGridUtil executeSendEmail
// * SendGridAPI呼び出しで例外が発生した場合にCustomExceptionが送出されることを確認するテストケース
// */
// @Test
// void executeSendEmail_004() throws Exception {
// // Arrange
// when(propertiesUtil.getSendGridApiKey()).thenReturn("SG-KEY");

// SendGridUtil sut = new SendGridUtil(propertiesUtil);
// Mail mail = mock(Mail.class);
// when(mail.build()).thenReturn("{}");

// try (MockedConstruction<SendGrid> mocked = mockConstruction(SendGrid.class,
// (mock, ctx) -> when(mock.api(any(Request.class))).thenThrow(new
// RuntimeException("boom")))) {

// // Act
// CustomException ex = assertThrows(CustomException.class, () ->
// sut.executeSendEmail(mail));

// // Assert
// assertNotNull(ex.getCause());
// }
// }

// /**
// * クラス：SendGridUtil executeSendEmail
// * mail.buildが例外の場合にCustomExceptionが送出されることを確認するテストケース
// */
// @Test
// void executeSendEmail_005() throws Exception {
// // Arrange
// when(propertiesUtil.getSendGridApiKey()).thenReturn("SG-KEY");

// SendGridUtil sut = new SendGridUtil(propertiesUtil);
// Mail mail = mock(Mail.class);
// when(mail.build()).thenThrow(new RuntimeException("build-fail"));

// try (MockedConstruction<SendGrid> mocked = mockConstruction(SendGrid.class))
// {

// // Act
// CustomException ex = assertThrows(CustomException.class, () ->
// sut.executeSendEmail(mail));

// // Assert
// assertNotNull(ex.getCause());
// }
// }

// /**
// * クラス：SendGridUtil executeSendEmail
// * 非2xxかつ宛先情報が取得できない場合にCustomExceptionが送出されることを確認するテストケース
// */
// @Test
// void executeSendEmail_006() throws Exception {
// // Arrange
// when(propertiesUtil.getSendGridApiKey()).thenReturn("SG-KEY");

// SendGridUtil sut = new SendGridUtil(propertiesUtil);

// Mail mail = mock(Mail.class);
// when(mail.build()).thenReturn("{}");
// when(mail.getPersonalization()).thenReturn(java.util.List.of()); // get(0)で例外

// Response resp = mock(Response.class);
// when(resp.getStatusCode()).thenReturn(400);

// try (MockedConstruction<SendGrid> mocked = mockConstruction(SendGrid.class,
// (mock, ctx) -> when(mock.api(any(Request.class))).thenReturn(resp))) {

// // Act
// CustomException ex = assertThrows(CustomException.class, () ->
// sut.executeSendEmail(mail));

// // Assert
// assertNotNull(ex.getCause());
// }
// }

// /**
// * クラス：SendGridUtil executeSendEmail
// * ステータスコードが200未満(例:199)の場合にTscEMailExceptionが送出されることを確認するテストケース
// */
// @Test
// void executeSendEmail_007() throws Exception {
// // Arrange
// when(propertiesUtil.getSendGridApiKey()).thenReturn("SG-KEY");
// SendGridUtil sut = new SendGridUtil(propertiesUtil);

// Mail mail = mock(Mail.class);
// when(mail.build()).thenReturn("{}");
// when(mail.getSubject()).thenReturn("title");

// com.sendgrid.helpers.mail.objects.Email to = new
// com.sendgrid.helpers.mail.objects.Email("to@example");
// com.sendgrid.helpers.mail.objects.Personalization p = new
// com.sendgrid.helpers.mail.objects.Personalization();
// p.addTo(to);
// when(mail.getPersonalization()).thenReturn(java.util.List.of(p));

// Response resp = mock(Response.class);
// when(resp.getStatusCode()).thenReturn(199);

// try (MockedConstruction<SendGrid> mocked = mockConstruction(SendGrid.class,
// (mock, ctx) -> when(mock.api(any(Request.class))).thenReturn(resp))) {

// // Act
// TscEMailException ex = assertThrows(TscEMailException.class, () ->
// sut.executeSendEmail(mail));

// // Assert
// assertEquals(199, ex.getStatusCode());
// assertEquals("to@example", ex.getAddress());
// assertEquals("title", ex.getTitle());

// SendGrid sg = mocked.constructed().get(0);
// verify(sg, times(1)).api(any(Request.class));
// }

// }
// }
