
package com.toyota.tsc.notificationhub.services;

import com.sendgrid.helpers.mail.Mail;
import com.toyota.tsc.notificationhub.commons.CommonUtil;
import com.toyota.tsc.notificationhub.commons.PersonalInfoUtil;
import com.toyota.tsc.notificationhub.commons.SendGridUtil;
import com.toyota.tsc.notificationhub.commons.SmsCountryUtil;
import com.toyota.tsc.notificationhub.exceptions.CustomException;
import com.toyota.tsc.notificationhub.exceptions.TscApplicationException;
import com.toyota.tsc.notificationhub.exceptions.TscEMailException;
import com.toyota.tsc.notificationhub.exceptions.TscSMSException;
import com.toyota.tsc.notificationhub.models.PersonalInfoResponseDto;
import com.toyota.tsc.notificationhub.models.RequestHeaderDto;
import com.toyota.tsc.notificationhub.models.ResponseDto;
import com.toyota.tsc.notificationhub.models.SendPrimaryContactRequestDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpEntity;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SendPrimaryContactServiceImpl のテストクラス
 */
@SuppressWarnings("all")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class SendPrimaryContactServiceImplTest {

        @Mock
        private SmsCountryUtil smsCountryUtil;

        @Mock
        private SendGridUtil sendGridUtil;

        @Mock
        private PersonalInfoUtil personalInfoUtil;

        // ----------------------------
        // public: sendPrimaryContact
        // ----------------------------

        /**
         * クラス：SendPrimaryContactServiceImpl sendPrimaryContact_001
         * 正常系（primary=SMS）で成功コードが返ることを確認するテストケース
         */
        @Test
        void sendPrimaryContact_001() {
                // Arrange
                SendPrimaryContactServiceImpl sut = new SendPrimaryContactServiceImpl(smsCountryUtil, sendGridUtil,
                                personalInfoUtil);

                SendPrimaryContactRequestDto request = mock(SendPrimaryContactRequestDto.class);
                when(request.getProcessId()).thenReturn("proc");
                when(request.getInternalUserId()).thenReturn("iu");
                when(request.getBrdCd()).thenReturn("0");
                when(request.getTitle()).thenReturn("title");
                when(request.getBodySms()).thenReturn("sms-body");

                RequestHeaderDto header = mock(RequestHeaderDto.class);
                when(header.getCorrelationId()).thenReturn("corr");

                PersonalInfoResponseDto response = mock(PersonalInfoResponseDto.class);
                PersonalInfoResponseDto.ContactDto contact = mock(PersonalInfoResponseDto.ContactDto.class);
                when(contact.isPrimaryContactFlag()).thenReturn(true);
                when(contact.getContactType()).thenReturn("1"); // phone
                when(contact.getContact()).thenReturn("090-1234-5678");
                when(response.getContactList()).thenReturn(List.of(contact));
                when(personalInfoUtil.getPersonalInfoApiResponse("iu", "corr")).thenReturn(response);

                HttpEntity<String> entity = mock(HttpEntity.class);
                when(smsCountryUtil.createRequest(anyString(), eq("sms-body"), eq("0"))).thenReturn(entity);

                try (MockedStatic<CommonUtil> cu = mockStatic(CommonUtil.class)) {
                        cu.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        cu.when(() -> CommonUtil.getMessage(anyString(), org.mockito.Mockito.<Object[]>any()))
                                        .thenReturn("msg");
                        cu.when(() -> CommonUtil.getResultCode(anyString())).thenReturn("PC_SUCCESS");
                        cu.when(() -> CommonUtil.normalizePhoneNumber(anyString())).thenReturn("09012345678");

                        // Act
                        ResponseDto res = sut.sendPrimaryContact(request, header);

                        // Assert
                        assertNotNull(res);
                        assertEquals("PC_SUCCESS", res.getResultCode());
                        verify(smsCountryUtil, times(1)).sendSmsCountry(eq(entity), eq("090-1234-5678"));
                        verify(sendGridUtil, never()).executeSendEmail(any());
                }
        }

        /**
         * クラス：SendPrimaryContactServiceImpl sendPrimaryContact_002
         * 正常系（primary=EMAIL）で成功コードが返ることを確認するテストケース
         */
        @Test
        void sendPrimaryContact_002() {
                // Arrange
                SendPrimaryContactServiceImpl sut = new SendPrimaryContactServiceImpl(smsCountryUtil, sendGridUtil,
                                personalInfoUtil);

                SendPrimaryContactRequestDto request = mock(SendPrimaryContactRequestDto.class);
                when(request.getProcessId()).thenReturn("proc");
                when(request.getInternalUserId()).thenReturn("iu");
                when(request.getBrdCd()).thenReturn("1");
                when(request.getTitle()).thenReturn("title");
                when(request.getBodyText()).thenReturn("text");
                when(request.getBodyHtml()).thenReturn("<b>html</b>");

                RequestHeaderDto header = mock(RequestHeaderDto.class);
                when(header.getCorrelationId()).thenReturn("corr");

                PersonalInfoResponseDto response = mock(PersonalInfoResponseDto.class);
                PersonalInfoResponseDto.ContactDto contact = mock(PersonalInfoResponseDto.ContactDto.class);
                when(contact.isPrimaryContactFlag()).thenReturn(true);
                when(contact.getContactType()).thenReturn("2"); // email
                when(contact.getContact()).thenReturn("a@example.com");
                when(response.getContactList()).thenReturn(List.of(contact));
                when(personalInfoUtil.getPersonalInfoApiResponse("iu", "corr")).thenReturn(response);

                Mail mail = mock(Mail.class);
                when(sendGridUtil.generateEmail(eq("a@example.com"), eq("title"), eq("text"), eq("<b>html</b>"),
                                eq("1"))).thenReturn(mail);

                try (MockedStatic<CommonUtil> cu = mockStatic(CommonUtil.class)) {
                        cu.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        cu.when(() -> CommonUtil.getMessage(anyString(), org.mockito.Mockito.<Object[]>any()))
                                        .thenReturn("msg");
                        cu.when(() -> CommonUtil.getResultCode(anyString())).thenReturn("PC_SUCCESS");

                        // Act
                        ResponseDto res = sut.sendPrimaryContact(request, header);

                        // Assert
                        assertNotNull(res);
                        assertEquals("PC_SUCCESS", res.getResultCode());
                        verify(sendGridUtil, times(1)).executeSendEmail(eq(mail));
                        verify(smsCountryUtil, never()).sendSmsCountry(any(), anyString());
                }
        }

        /**
         * クラス：SendPrimaryContactServiceImpl sendPrimaryContact_003
         * 異常系（personalInfoレスポンスnull）でTscApplicationExceptionとなることを確認するテストケース
         */
        @Test
        void sendPrimaryContact_003() {
                // Arrange
                SendPrimaryContactServiceImpl sut = new SendPrimaryContactServiceImpl(smsCountryUtil, sendGridUtil,
                                personalInfoUtil);

                SendPrimaryContactRequestDto request = mock(SendPrimaryContactRequestDto.class);
                when(request.getProcessId()).thenReturn("proc");
                when(request.getInternalUserId()).thenReturn("iu");
                when(request.getBrdCd()).thenReturn("0");

                RequestHeaderDto header = mock(RequestHeaderDto.class);
                when(header.getCorrelationId()).thenReturn("corr");

                when(personalInfoUtil.getPersonalInfoApiResponse("iu", "corr")).thenReturn(null);

                try (MockedStatic<CommonUtil> cu = mockStatic(CommonUtil.class)) {
                        cu.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        cu.when(() -> CommonUtil.getMessage(anyString(), org.mockito.Mockito.<Object[]>any()))
                                        .thenReturn("msg");
                        cu.when(() -> CommonUtil.getResultCode(anyString())).thenReturn("PC_GET_PERSONALINFO_EMPTY");

                        // Act + Assert
                        assertThrows(TscApplicationException.class, () -> sut.sendPrimaryContact(request, header));
                }
        }

        /**
         * クラス：SendPrimaryContactServiceImpl sendPrimaryContact_004
         * 異常系（必須不足：request=null）でTscApplicationExceptionとなることを確認するテストケース
         */
        @Test
        void sendPrimaryContact_004() {
                // Arrange
                SendPrimaryContactServiceImpl sut = new SendPrimaryContactServiceImpl(smsCountryUtil, sendGridUtil,
                                personalInfoUtil);

                SendPrimaryContactRequestDto request = null;

                RequestHeaderDto header = mock(RequestHeaderDto.class);
                when(header.getCorrelationId()).thenReturn("corr");

                try (MockedStatic<CommonUtil> cu = mockStatic(CommonUtil.class)) {
                        cu.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        cu.when(() -> CommonUtil.getMessage(anyString(), org.mockito.Mockito.<Object[]>any()))
                                        .thenReturn("msg");
                        cu.when(() -> CommonUtil.getResultCode(anyString())).thenReturn("PC_FIELD_MISSING");

                        // Act + Assert
                        assertThrows(TscApplicationException.class, () -> sut.sendPrimaryContact(request, header));
                }
        }

        /**
         * クラス：SendPrimaryContactServiceImpl sendPrimaryContact_005
         * 異常系（ブランド不正）でTscApplicationExceptionとなることを確認するテストケース
         */
        @Test
        void sendPrimaryContact_005() {
                // Arrange
                SendPrimaryContactServiceImpl sut = new SendPrimaryContactServiceImpl(smsCountryUtil, sendGridUtil,
                                personalInfoUtil);

                SendPrimaryContactRequestDto request = mock(SendPrimaryContactRequestDto.class);
                when(request.getProcessId()).thenReturn("proc");
                when(request.getInternalUserId()).thenReturn("iu");
                when(request.getBrdCd()).thenReturn("9"); // invalid

                RequestHeaderDto header = mock(RequestHeaderDto.class);
                when(header.getCorrelationId()).thenReturn("corr");

                try (MockedStatic<CommonUtil> cu = mockStatic(CommonUtil.class)) {
                        cu.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        cu.when(() -> CommonUtil.getMessage(anyString(), org.mockito.Mockito.<Object[]>any()))
                                        .thenReturn("msg");
                        cu.when(() -> CommonUtil.getResultCode(anyString())).thenReturn("PC_INVALID_BRAND");

                        // Act + Assert
                        assertThrows(TscApplicationException.class, () -> sut.sendPrimaryContact(request, header));
                }
        }

        /**
         * クラス：SendPrimaryContactServiceImpl sendPrimaryContact_006
         * 異常系（メール送信でTscEMailException）でCustomExceptionとなることを確認するテストケース
         */
        @Test
        void sendPrimaryContact_006() {
                // Arrange
                SendPrimaryContactServiceImpl sut = new SendPrimaryContactServiceImpl(smsCountryUtil, sendGridUtil,
                                personalInfoUtil);

                SendPrimaryContactRequestDto request = mock(SendPrimaryContactRequestDto.class);
                when(request.getProcessId()).thenReturn("proc");
                when(request.getInternalUserId()).thenReturn("iu");
                when(request.getBrdCd()).thenReturn("1");
                when(request.getTitle()).thenReturn("title");
                when(request.getBodyText()).thenReturn("text");
                when(request.getBodyHtml()).thenReturn("html");

                RequestHeaderDto header = mock(RequestHeaderDto.class);
                when(header.getCorrelationId()).thenReturn("corr");

                PersonalInfoResponseDto response = mock(PersonalInfoResponseDto.class);
                PersonalInfoResponseDto.ContactDto contact = mock(PersonalInfoResponseDto.ContactDto.class);
                when(contact.isPrimaryContactFlag()).thenReturn(true);
                when(contact.getContactType()).thenReturn("2");
                when(contact.getContact()).thenReturn("a@example.com");
                when(response.getContactList()).thenReturn(List.of(contact));
                when(personalInfoUtil.getPersonalInfoApiResponse("iu", "corr")).thenReturn(response);

                Mail mail = mock(Mail.class);
                when(sendGridUtil.generateEmail(eq("a@example.com"), eq("title"), eq("text"), eq("html"), eq("1")))
                                .thenReturn(mail);
                doThrow(new TscEMailException(400, "a@example.com", "title")).when(sendGridUtil)
                                .executeSendEmail(eq(mail));

                try (MockedStatic<CommonUtil> cu = mockStatic(CommonUtil.class)) {
                        cu.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        cu.when(() -> CommonUtil.getMessage(anyString(), org.mockito.Mockito.<Object[]>any()))
                                        .thenReturn("msg");
                        cu.when(() -> CommonUtil.maskText(anyString())).thenReturn("***");
                        cu.when(() -> CommonUtil.getResultCode(anyString())).thenReturn("X");

                        // Act + Assert
                        assertThrows(CustomException.class, () -> sut.sendPrimaryContact(request, header));
                }
        }

        /**
         * クラス：SendPrimaryContactServiceImpl sendPrimaryContact_007
         * 異常系（SMS送信でTscSMSException）でCustomExceptionとなることを確認するテストケース
         */
        @Test
        void sendPrimaryContact_007() {
                // Arrange
                SendPrimaryContactServiceImpl sut = new SendPrimaryContactServiceImpl(smsCountryUtil, sendGridUtil,
                                personalInfoUtil);

                SendPrimaryContactRequestDto request = mock(SendPrimaryContactRequestDto.class);
                when(request.getProcessId()).thenReturn("proc");
                when(request.getInternalUserId()).thenReturn("iu");
                when(request.getBrdCd()).thenReturn("0");
                when(request.getTitle()).thenReturn("title");
                when(request.getBodySms()).thenReturn("sms-body");

                RequestHeaderDto header = mock(RequestHeaderDto.class);
                when(header.getCorrelationId()).thenReturn("corr");

                PersonalInfoResponseDto response = mock(PersonalInfoResponseDto.class);
                PersonalInfoResponseDto.ContactDto contact = mock(PersonalInfoResponseDto.ContactDto.class);
                when(contact.isPrimaryContactFlag()).thenReturn(true);
                when(contact.getContactType()).thenReturn("1");
                when(contact.getContact()).thenReturn("090");
                when(response.getContactList()).thenReturn(List.of(contact));
                when(personalInfoUtil.getPersonalInfoApiResponse("iu", "corr")).thenReturn(response);

                HttpEntity<String> entity = mock(HttpEntity.class);
                when(smsCountryUtil.createRequest(anyString(), eq("sms-body"), eq("0"))).thenReturn(entity);
                doThrow(new TscSMSException(400, "NG", "090")).when(smsCountryUtil).sendSmsCountry(eq(entity),
                                eq("090"));

                try (MockedStatic<CommonUtil> cu = mockStatic(CommonUtil.class)) {
                        cu.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        cu.when(() -> CommonUtil.getMessage(anyString(), org.mockito.Mockito.<Object[]>any()))
                                        .thenReturn("msg");
                        cu.when(() -> CommonUtil.normalizePhoneNumber(anyString())).thenReturn("090");
                        cu.when(() -> CommonUtil.maskPhoneNumber(anyString())).thenReturn("***");
                        cu.when(() -> CommonUtil.getResultCode(anyString())).thenReturn("X");

                        // Act + Assert
                        assertThrows(CustomException.class, () -> sut.sendPrimaryContact(request, header));
                }
        }

        /**
         * クラス：SendPrimaryContactServiceImpl sendPrimaryContact_008
         * 異常系（想定外例外）でCustomExceptionとなることを確認するテストケース
         */
        @Test
        void sendPrimaryContact_008() {
                // Arrange
                SendPrimaryContactServiceImpl sut = new SendPrimaryContactServiceImpl(smsCountryUtil, sendGridUtil,
                                personalInfoUtil);

                SendPrimaryContactRequestDto request = mock(SendPrimaryContactRequestDto.class);
                when(request.getProcessId()).thenReturn("proc");
                when(request.getInternalUserId()).thenReturn("iu");
                when(request.getBrdCd()).thenReturn("0");

                RequestHeaderDto header = mock(RequestHeaderDto.class);
                when(header.getCorrelationId()).thenReturn("corr");

                when(personalInfoUtil.getPersonalInfoApiResponse("iu", "corr")).thenThrow(new RuntimeException("boom"));

                try (MockedStatic<CommonUtil> cu = mockStatic(CommonUtil.class)) {
                        cu.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        cu.when(() -> CommonUtil.getMessage(anyString(), org.mockito.Mockito.<Object[]>any()))
                                        .thenReturn("msg");

                        // Act + Assert
                        assertThrows(CustomException.class, () -> sut.sendPrimaryContact(request, header));
                }
        }

        // ----------------------------
        // private methods (reflection)
        // ----------------------------

        /**
         * クラス：SendPrimaryContactServiceImpl validateRequired_001
         * request=nullでrequestBodyが返ることを確認するテストケース
         */
        @Test
        void validateRequired_001() throws Exception {
                // Arrange
                SendPrimaryContactServiceImpl sut = new SendPrimaryContactServiceImpl(smsCountryUtil, sendGridUtil,
                                personalInfoUtil);
                Method m = SendPrimaryContactServiceImpl.class.getDeclaredMethod("validateRequired",
                                SendPrimaryContactRequestDto.class);
                m.setAccessible(true);

                // Act
                String res = (String) m.invoke(sut, (Object) null);

                // Assert
                assertEquals("requestBody", res);
        }

        /**
         * クラス：SendPrimaryContactServiceImpl validateRequired_002
         * processId不足でprocessIdが返ることを確認するテストケース
         */
        @Test
        void validateRequired_002() throws Exception {
                // Arrange
                SendPrimaryContactServiceImpl sut = new SendPrimaryContactServiceImpl(smsCountryUtil, sendGridUtil,
                                personalInfoUtil);
                Method m = SendPrimaryContactServiceImpl.class.getDeclaredMethod("validateRequired",
                                SendPrimaryContactRequestDto.class);
                m.setAccessible(true);

                SendPrimaryContactRequestDto request = mock(SendPrimaryContactRequestDto.class);
                when(request.getProcessId()).thenReturn("");
                when(request.getInternalUserId()).thenReturn("iu");
                when(request.getBrdCd()).thenReturn("0");

                // Act
                String res = (String) m.invoke(sut, request);

                // Assert
                assertEquals("processId", res);
        }

        /**
         * クラス：SendPrimaryContactServiceImpl validateRequired_003
         * 複数不足でカンマ連結が返ることを確認するテストケース
         */
        @Test
        void validateRequired_003() throws Exception {
                // Arrange
                SendPrimaryContactServiceImpl sut = new SendPrimaryContactServiceImpl(smsCountryUtil, sendGridUtil,
                                personalInfoUtil);
                Method m = SendPrimaryContactServiceImpl.class.getDeclaredMethod("validateRequired",
                                SendPrimaryContactRequestDto.class);
                m.setAccessible(true);

                SendPrimaryContactRequestDto request = mock(SendPrimaryContactRequestDto.class);
                when(request.getProcessId()).thenReturn("");
                when(request.getInternalUserId()).thenReturn("");
                when(request.getBrdCd()).thenReturn("");

                // Act
                String res = (String) m.invoke(sut, request);

                // Assert
                assertEquals("processId,internalUserId,brdCd", res);
        }

        /**
         * クラス：SendPrimaryContactServiceImpl isValidBrdCd_001
         * 有効値（0）でtrueとなることを確認するテストケース
         */
        @Test
        void isValidBrdCd_001() throws Exception {
                // Arrange
                SendPrimaryContactServiceImpl sut = new SendPrimaryContactServiceImpl(smsCountryUtil, sendGridUtil,
                                personalInfoUtil);
                Method m = SendPrimaryContactServiceImpl.class.getDeclaredMethod("isValidBrdCd", String.class);
                m.setAccessible(true);

                // Act
                boolean res = (boolean) m.invoke(sut, "0");

                // Assert
                assertTrue(res);
        }

        /**
         * クラス：SendPrimaryContactServiceImpl isValidBrdCd_002
         * 無効値（9）でfalseとなることを確認するテストケース
         */
        @Test
        void isValidBrdCd_002() throws Exception {
                // Arrange
                SendPrimaryContactServiceImpl sut = new SendPrimaryContactServiceImpl(smsCountryUtil, sendGridUtil,
                                personalInfoUtil);
                Method m = SendPrimaryContactServiceImpl.class.getDeclaredMethod("isValidBrdCd", String.class);
                m.setAccessible(true);

                // Act
                boolean res = (boolean) m.invoke(sut, "9");

                // Assert
                assertFalse(res);
        }

        /**
         * クラス：SendPrimaryContactServiceImpl sendRequest_001
         * primaryContactFlag=falseで外部送信されないことを確認するテストケース
         */
        @Test
        void sendRequest_001() throws Exception {
                // Arrange
                SendPrimaryContactServiceImpl sut = new SendPrimaryContactServiceImpl(smsCountryUtil, sendGridUtil,
                                personalInfoUtil);
                Method m = SendPrimaryContactServiceImpl.class.getDeclaredMethod("sendRequest", List.class,
                                SendPrimaryContactRequestDto.class, RequestHeaderDto.class);
                m.setAccessible(true);

                PersonalInfoResponseDto.ContactDto contact = mock(PersonalInfoResponseDto.ContactDto.class);
                when(contact.isPrimaryContactFlag()).thenReturn(false);

                SendPrimaryContactRequestDto request = mock(SendPrimaryContactRequestDto.class);
                RequestHeaderDto header = mock(RequestHeaderDto.class);

                try (MockedStatic<CommonUtil> cu = mockStatic(CommonUtil.class)) {
                        cu.when(() -> CommonUtil.getMessage(anyString(), org.mockito.Mockito.<Object[]>any()))
                                        .thenReturn("msg");

                        // Act
                        m.invoke(sut, List.of(contact), request, header);

                        // Assert
                        verifyNoInteractions(smsCountryUtil);
                        verifyNoInteractions(sendGridUtil);
                }
        }

        /**
         * クラス：SendPrimaryContactServiceImpl sendRequest_002
         * primaryContact=PHONEでSMS送信が呼ばれることを確認するテストケース
         */
        @Test
        void sendRequest_002() throws Exception {
                // Arrange
                SendPrimaryContactServiceImpl sut = new SendPrimaryContactServiceImpl(smsCountryUtil, sendGridUtil,
                                personalInfoUtil);
                Method m = SendPrimaryContactServiceImpl.class.getDeclaredMethod("sendRequest", List.class,
                                SendPrimaryContactRequestDto.class, RequestHeaderDto.class);
                m.setAccessible(true);

                PersonalInfoResponseDto.ContactDto contact = mock(PersonalInfoResponseDto.ContactDto.class);
                when(contact.isPrimaryContactFlag()).thenReturn(true);
                when(contact.getContactType()).thenReturn("1");
                when(contact.getContact()).thenReturn("090");

                SendPrimaryContactRequestDto request = mock(SendPrimaryContactRequestDto.class);
                when(request.getInternalUserId()).thenReturn("iu");
                when(request.getBrdCd()).thenReturn("0");
                when(request.getTitle()).thenReturn("title");
                when(request.getBodySms()).thenReturn("sms-body");

                RequestHeaderDto header = mock(RequestHeaderDto.class);
                when(header.getCorrelationId()).thenReturn("corr");

                HttpEntity<String> entity = mock(HttpEntity.class);
                when(smsCountryUtil.createRequest(anyString(), eq("sms-body"), eq("0"))).thenReturn(entity);

                try (MockedStatic<CommonUtil> cu = mockStatic(CommonUtil.class)) {
                        cu.when(() -> CommonUtil.getMessage(anyString(), org.mockito.Mockito.<Object[]>any()))
                                        .thenReturn("msg");
                        cu.when(() -> CommonUtil.normalizePhoneNumber(anyString())).thenReturn("090");

                        // Act
                        m.invoke(sut, List.of(contact), request, header);

                        // Assert
                        verify(smsCountryUtil, times(1)).sendSmsCountry(eq(entity), eq("090"));
                        verifyNoInteractions(sendGridUtil);
                }
        }

        /**
         * クラス：SendPrimaryContactServiceImpl sendRequest_003
         * primaryContact=EMAILでメール送信が呼ばれることを確認するテストケース
         */
        @Test
        void sendRequest_003() throws Exception {
                // Arrange
                SendPrimaryContactServiceImpl sut = new SendPrimaryContactServiceImpl(smsCountryUtil, sendGridUtil,
                                personalInfoUtil);
                Method m = SendPrimaryContactServiceImpl.class.getDeclaredMethod("sendRequest", List.class,
                                SendPrimaryContactRequestDto.class, RequestHeaderDto.class);
                m.setAccessible(true);

                PersonalInfoResponseDto.ContactDto contact = mock(PersonalInfoResponseDto.ContactDto.class);
                when(contact.isPrimaryContactFlag()).thenReturn(true);
                when(contact.getContactType()).thenReturn("2");
                when(contact.getContact()).thenReturn("a@example.com");

                SendPrimaryContactRequestDto request = mock(SendPrimaryContactRequestDto.class);
                when(request.getInternalUserId()).thenReturn("iu");
                when(request.getBrdCd()).thenReturn("1");
                when(request.getTitle()).thenReturn("title");
                when(request.getBodyText()).thenReturn("text");
                when(request.getBodyHtml()).thenReturn("html");

                RequestHeaderDto header = mock(RequestHeaderDto.class);
                when(header.getCorrelationId()).thenReturn("corr");

                Mail mail = mock(Mail.class);
                when(sendGridUtil.generateEmail(eq("a@example.com"), eq("title"), eq("text"), eq("html"), eq("1")))
                                .thenReturn(mail);

                try (MockedStatic<CommonUtil> cu = mockStatic(CommonUtil.class)) {
                        cu.when(() -> CommonUtil.getMessage(anyString(), org.mockito.Mockito.<Object[]>any()))
                                        .thenReturn("msg");

                        // Act
                        m.invoke(sut, List.of(contact), request, header);

                        // Assert
                        verify(sendGridUtil, times(1)).executeSendEmail(eq(mail));
                        verifyNoInteractions(smsCountryUtil);
                }
        }

        /*
         * =========================================================
         * validateRequired (reflection) : null側/不足なし(return null) を補完
         * =========================================================
         */

        /**
         * クラス：SendPrimaryContactServiceImpl validateRequired_004
         * 全項目が有効な場合にnullが返ることを確認するテストケース
         */
        @Test
        void validateRequired_004() throws Exception {
                // Arrange
                SendPrimaryContactServiceImpl sut = new SendPrimaryContactServiceImpl(smsCountryUtil, sendGridUtil,
                                personalInfoUtil);
                Method m = SendPrimaryContactServiceImpl.class.getDeclaredMethod("validateRequired",
                                SendPrimaryContactRequestDto.class);
                m.setAccessible(true);

                SendPrimaryContactRequestDto request = mock(SendPrimaryContactRequestDto.class);
                when(request.getProcessId()).thenReturn("proc");
                when(request.getInternalUserId()).thenReturn("iu");
                when(request.getBrdCd()).thenReturn("0");

                // Act
                String res = (String) m.invoke(sut, request);

                // Assert
                assertNull(res);
        }

        /**
         * クラス：SendPrimaryContactServiceImpl validateRequired_005
         * processIdがnullの場合にprocessIdが返ることを確認するテストケース
         */
        @Test
        void validateRequired_005() throws Exception {
                // Arrange
                SendPrimaryContactServiceImpl sut = new SendPrimaryContactServiceImpl(smsCountryUtil, sendGridUtil,
                                personalInfoUtil);
                Method m = SendPrimaryContactServiceImpl.class.getDeclaredMethod("validateRequired",
                                SendPrimaryContactRequestDto.class);
                m.setAccessible(true);

                SendPrimaryContactRequestDto request = mock(SendPrimaryContactRequestDto.class);
                when(request.getProcessId()).thenReturn(null); // ★ null側
                when(request.getInternalUserId()).thenReturn("iu");
                when(request.getBrdCd()).thenReturn("0");

                // Act
                String res = (String) m.invoke(sut, request);

                // Assert
                assertEquals("processId", res);
        }

        /**
         * クラス：SendPrimaryContactServiceImpl validateRequired_006
         * internalUserIdがnullの場合にinternalUserIdが返ることを確認するテストケース
         */
        @Test
        void validateRequired_006() throws Exception {
                // Arrange
                SendPrimaryContactServiceImpl sut = new SendPrimaryContactServiceImpl(smsCountryUtil, sendGridUtil,
                                personalInfoUtil);
                Method m = SendPrimaryContactServiceImpl.class.getDeclaredMethod("validateRequired",
                                SendPrimaryContactRequestDto.class);
                m.setAccessible(true);

                SendPrimaryContactRequestDto request = mock(SendPrimaryContactRequestDto.class);
                when(request.getProcessId()).thenReturn("proc");
                when(request.getInternalUserId()).thenReturn(null); // ★ null側
                when(request.getBrdCd()).thenReturn("0");

                // Act
                String res = (String) m.invoke(sut, request);

                // Assert
                assertEquals("internalUserId", res);
        }

        /**
         * クラス：SendPrimaryContactServiceImpl validateRequired_007
         * brdCdがnullの場合にbrdCdが返ることを確認するテストケース
         */
        @Test
        void validateRequired_007() throws Exception {
                // Arrange
                SendPrimaryContactServiceImpl sut = new SendPrimaryContactServiceImpl(smsCountryUtil, sendGridUtil,
                                personalInfoUtil);
                Method m = SendPrimaryContactServiceImpl.class.getDeclaredMethod("validateRequired",
                                SendPrimaryContactRequestDto.class);
                m.setAccessible(true);

                SendPrimaryContactRequestDto request = mock(SendPrimaryContactRequestDto.class);
                when(request.getProcessId()).thenReturn("proc");
                when(request.getInternalUserId()).thenReturn("iu");
                when(request.getBrdCd()).thenReturn(null); // ★ null側

                // Act
                String res = (String) m.invoke(sut, request);

                // Assert
                assertEquals("brdCd", res);
        }

        /*
         * =========================================================
         * isValidBrdCd (reflection) : "1" / "2" true を補完
         * =========================================================
         */

        /**
         * クラス：SendPrimaryContactServiceImpl isValidBrdCd_003
         * 有効値（1）でtrueとなることを確認するテストケース
         */
        @Test
        void isValidBrdCd_003() throws Exception {
                // Arrange
                SendPrimaryContactServiceImpl sut = new SendPrimaryContactServiceImpl(smsCountryUtil, sendGridUtil,
                                personalInfoUtil);
                Method m = SendPrimaryContactServiceImpl.class.getDeclaredMethod("isValidBrdCd", String.class);
                m.setAccessible(true);

                // Act
                boolean res = (boolean) m.invoke(sut, "1");

                // Assert
                assertTrue(res);
        }

        /**
         * クラス：SendPrimaryContactServiceImpl isValidBrdCd_004
         * 有効値（2）でtrueとなることを確認するテストケース
         */
        @Test
        void isValidBrdCd_004() throws Exception {
                // Arrange
                SendPrimaryContactServiceImpl sut = new SendPrimaryContactServiceImpl(smsCountryUtil, sendGridUtil,
                                personalInfoUtil);
                Method m = SendPrimaryContactServiceImpl.class.getDeclaredMethod("isValidBrdCd", String.class);
                m.setAccessible(true);

                // Act
                boolean res = (boolean) m.invoke(sut, "2");

                // Assert
                assertTrue(res);
        }

        /*
         * =========================================================
         * sendRequest (reflection) : contactTypeが想定外のreturn分岐を補完
         * =========================================================
         */

        /**
         * クラス：SendPrimaryContactServiceImpl sendRequest_004
         * primaryContactFlag=trueかつcontactTypeが想定外の場合に送信されないことを確認するテストケース
         */
        @Test
        void sendRequest_004() throws Exception {
                // Arrange
                SendPrimaryContactServiceImpl sut = new SendPrimaryContactServiceImpl(smsCountryUtil, sendGridUtil,
                                personalInfoUtil);
                Method m = SendPrimaryContactServiceImpl.class.getDeclaredMethod("sendRequest",
                                List.class, SendPrimaryContactRequestDto.class, RequestHeaderDto.class);
                m.setAccessible(true);

                PersonalInfoResponseDto.ContactDto contact = mock(PersonalInfoResponseDto.ContactDto.class);
                when(contact.isPrimaryContactFlag()).thenReturn(true);
                when(contact.getContactType()).thenReturn("9"); // ★ 想定外

                // ※この分岐では request/header の getter は参照されないためスタブしない（Strict stubs対策）
                SendPrimaryContactRequestDto request = mock(SendPrimaryContactRequestDto.class);
                RequestHeaderDto header = mock(RequestHeaderDto.class);

                // Act
                m.invoke(sut, List.of(contact), request, header);

                // Assert
                verifyNoInteractions(smsCountryUtil);
                verifyNoInteractions(sendGridUtil);
        }

}
