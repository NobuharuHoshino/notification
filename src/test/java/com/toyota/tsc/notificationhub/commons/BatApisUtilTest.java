// TODO IT1用Mock
// package com.toyota.tsc.notificationhub.commons;

// import com.toyota.tsc.notificationhub.exceptions.CustomException;
// import com.toyota.tsc.notificationhub.models.RegisterNotificationRequestDto;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.Mock;
// import org.mockito.MockedConstruction;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.springframework.http.HttpEntity;
// import org.springframework.http.HttpMethod;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.client.RestTemplate;

// import java.util.List;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.*;
// import static org.mockito.Mockito.*;

// /**
// * BatApisUtil のテストクラス
// */
// @SuppressWarnings("all")
// @ExtendWith(MockitoExtension.class)
// class BatApisUtilTest {

// @Mock
// private PropertiesUtil propertiesUtil;

// /** クラス：BatApisUtil executeRegisterNotification
// 正常にAPIが呼ばれレスポンスが返ることを確認するテストケース */
// @Test
// void executeRegisterNotification_001() {
// // Arrange
// BatApisUtil sut = new BatApisUtil(propertiesUtil);
// when(propertiesUtil.getRegisterNotificationApiKey()).thenReturn("test-api-key");
// when(propertiesUtil.getRegisterNotificationUrl()).thenReturn("http://localhost/test");

// RegisterNotificationRequestDto request = new RegisterNotificationRequestDto(
// "123", "ME", "user001", List.of(), "1");

// ResponseEntity<String> expected = new
// ResponseEntity<>("{\"returnCode\":\"000000\"}", HttpStatus.OK);

// try (MockedConstruction<RestTemplate> mockedConstruction =
// mockConstruction(RestTemplate.class, (mock, context) -> {
// when(mock.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
// eq(String.class)))
// .thenReturn(expected);
// })) {
// // Act
// ResponseEntity<String> actual = sut.executeRegisterNotification(request);

// // Assert
// assertEquals(HttpStatus.OK, actual.getStatusCode());
// }
// }

// /** クラス：BatApisUtil executeRegisterNotification
// 例外が発生した場合CustomExceptionが投げられることを確認するテストケース */
// @Test
// void executeRegisterNotification_002() {
// // Arrange
// BatApisUtil sut = new BatApisUtil(propertiesUtil);
// when(propertiesUtil.getRegisterNotificationApiKey()).thenReturn("test-api-key");
// when(propertiesUtil.getRegisterNotificationUrl()).thenReturn("http://localhost/test");

// RegisterNotificationRequestDto request = new RegisterNotificationRequestDto(
// "123", "ME", "user001", List.of(), "1");

// try (MockedConstruction<RestTemplate> mockedConstruction =
// mockConstruction(RestTemplate.class, (mock, context) -> {
// when(mock.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
// eq(String.class)))
// .thenThrow(new RuntimeException("connection error"));
// })) {
// // Act & Assert
// assertThrows(CustomException.class, () ->
// sut.executeRegisterNotification(request));
// }
// }
// }
