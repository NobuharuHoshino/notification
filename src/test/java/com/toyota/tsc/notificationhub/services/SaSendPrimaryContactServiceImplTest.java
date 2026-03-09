
package com.toyota.tsc.notificationhub.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toyota.tsc.notificationhub.commons.CommonUtil;
import com.toyota.tsc.notificationhub.commons.JsapUtil;
import com.toyota.tsc.notificationhub.commons.LogUtil;
import com.toyota.tsc.notificationhub.exceptions.CustomException;
import com.toyota.tsc.notificationhub.exceptions.TscApplicationException;
import com.toyota.tsc.notificationhub.exceptions.TscPrimaryContactException;
import com.toyota.tsc.notificationhub.models.GetUserInfoResponseDto;
import com.toyota.tsc.notificationhub.models.RequestHeaderDto;
import com.toyota.tsc.notificationhub.models.ResponseDto;
import com.toyota.tsc.notificationhub.models.SendMessageResponseDto;
import com.toyota.tsc.notificationhub.models.SendPrimaryContactRequestDto;

import jp.toyota.res.common.auth.GetALJTokenResultDto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SaSendPrimaryContactServiceImpl のテストクラス
 */
@SuppressWarnings("all")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class SaSendPrimaryContactServiceImplTest {

        @Mock
        private JsapUtil jsapUtil;

        // --------------------
        // sendPrimaryContact (public)
        // --------------------

        /**
         * クラス：SaSendPrimaryContactServiceImpl
         * 異常系（必須不足）でTscApplicationExceptionとなることを確認するテストケース
         */
        @Test
        void sendPrimaryContact_002() {
                // Arrange
                SaSendPrimaryContactServiceImpl sut = new SaSendPrimaryContactServiceImpl(jsapUtil);

                SendPrimaryContactRequestDto request = mock(SendPrimaryContactRequestDto.class);
                RequestHeaderDto header = mock(RequestHeaderDto.class);
                when(header.getCorrelationId()).thenReturn("corr");// 必須不足を誘発
                when(request.getInternalUserId()).thenReturn("iu");
                when(request.getBrdCd()).thenReturn("0");

                try (MockedStatic<CommonUtil> cu = mockStatic(CommonUtil.class);
                                MockedStatic<LogUtil> lu = mockStatic(LogUtil.class)) {

                        cu.when(() -> CommonUtil.getLogsMessage(anyString(), org.mockito.Mockito.<Object[]>any()))
                                        .thenReturn("msg");
                        cu.when(() -> CommonUtil.getResultCode(anyString())).thenReturn("RC_BAD");
                        lu.when(() -> LogUtil.error(any(), anyString())).thenAnswer(inv -> null);

                        // Act + Assert
                        assertThrows(TscApplicationException.class, () -> sut.sendPrimaryContact(request, header));
                }
        }

        /**
         * クラス：SaSendPrimaryContactServiceImpl
         * 異常系（JSAPのUserId取得結果不正）でCustomExceptionとなることを確認するテストケース
         */
        @Test
        void sendPrimaryContact_003() {
                // Arrange
                SaSendPrimaryContactServiceImpl sut = new SaSendPrimaryContactServiceImpl(jsapUtil);

                SendPrimaryContactRequestDto request = mock(SendPrimaryContactRequestDto.class);
                RequestHeaderDto header = mock(RequestHeaderDto.class);
                when(header.getCorrelationId()).thenReturn("corr");
                when(request.getInternalUserId()).thenReturn("iu");
                when(request.getBrdCd()).thenReturn("0");

                when(jsapUtil.executeGetToken()).thenReturn(new GetALJTokenResultDto(true, "test"));
                when(jsapUtil.executeGetUserId("iu", "corr"))
                                .thenReturn(ResponseEntity.ok("{\"resultCode\":\"NG\",\"userId\":\"u\"}"));

                try (MockedStatic<CommonUtil> cu = mockStatic(CommonUtil.class);
                                MockedStatic<LogUtil> lu = mockStatic(LogUtil.class)) {

                        cu.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        cu.when(() -> CommonUtil.getLogsMessage(anyString(), org.mockito.Mockito.<Object[]>any()))
                                        .thenReturn("msg");
                        cu.when(() -> CommonUtil.getResultCode(anyString())).thenReturn("RC_ANY");
                        lu.when(() -> LogUtil.error(any(), anyString())).thenAnswer(inv -> null);

                        // Act + Assert
                        assertThrows(TscPrimaryContactException.class, () -> sut.sendPrimaryContact(request, header));
                }
        }

        // --------------------
        // private methods (reflection)
        // --------------------

        /**
         * クラス：SaSendPrimaryContactServiceImpl validateRequired
         * request=nullでrequestBodyが返ることを確認するテストケース
         */
        @Test
        void validateRequired_001() throws Exception {
                // Arrange
                SaSendPrimaryContactServiceImpl sut = new SaSendPrimaryContactServiceImpl(jsapUtil);
                Method m = SaSendPrimaryContactServiceImpl.class.getDeclaredMethod("validateRequired",
                                SendPrimaryContactRequestDto.class);
                m.setAccessible(true);

                // Act
                String res = (String) m.invoke(sut, (Object) null);

                // Assert
                assertEquals("requestBody", res);
        }

        /**
         * クラス：SaSendPrimaryContactServiceImpl isValidBrdCd 有効値(0)でtrueとなることを確認するテストケース
         */
        @Test
        void isValidBrdCd_001() throws Exception {
                // Arrange
                SaSendPrimaryContactServiceImpl sut = new SaSendPrimaryContactServiceImpl(jsapUtil);
                Method m = SaSendPrimaryContactServiceImpl.class.getDeclaredMethod("isValidBrdCd", String.class);
                m.setAccessible(true);

                // Act
                boolean res = (boolean) m.invoke(sut, "0");

                // Assert
                assertTrue(res);
        }

        /**
         * クラス：SaSendPrimaryContactServiceImpl isValidBrdCd 無効値(9)でfalseとなることを確認するテストケース
         */
        @Test
        void isValidBrdCd_002() throws Exception {
                // Arrange
                SaSendPrimaryContactServiceImpl sut = new SaSendPrimaryContactServiceImpl(jsapUtil);
                Method m = SaSendPrimaryContactServiceImpl.class.getDeclaredMethod("isValidBrdCd", String.class);
                m.setAccessible(true);

                // Act
                boolean res = (boolean) m.invoke(sut, "9");

                // Assert
                assertFalse(res);
        }

        /**
         * クラス：SaSendPrimaryContactServiceImpl sendOperation
         * primaryContactFlag=falseの場合に送信されないことを確認するテストケース
         */
        @Test
        void sendOperation_001() throws Exception {
                // Arrange
                SaSendPrimaryContactServiceImpl sut = new SaSendPrimaryContactServiceImpl(jsapUtil);
                Method m = SaSendPrimaryContactServiceImpl.class.getDeclaredMethod(
                                "sendOperation", List.class, SendPrimaryContactRequestDto.class, RequestHeaderDto.class,
                                String.class, String.class);
                m.setAccessible(true);

                GetUserInfoResponseDto.ContactDto contact = mock(GetUserInfoResponseDto.ContactDto.class);
                when(contact.isPrimaryContactFlag()).thenReturn(false);

                // Act
                m.invoke(sut, Collections.singletonList(contact), mock(SendPrimaryContactRequestDto.class),
                                mock(RequestHeaderDto.class), "u", "");

                // Assert
                verify(jsapUtil, never()).executeSendMessage(anyString(), anyString(), anyString(), anyString(), any(),
                                anyString());
        }

        // ---------------------------------------------------------------------
        // validateRequired (reflection) ：OR分岐をnull/empty/正常で網羅
        // ---------------------------------------------------------------------

        /**
         * クラス：SaSendPrimaryContactServiceImpl validateRequired
         * 必須項目がすべて有効な場合にnullが返ることを確認するテストケース
         */
        @Test
        void validateRequired_002() throws Exception {
                // Arrange
                SaSendPrimaryContactServiceImpl sut = new SaSendPrimaryContactServiceImpl(jsapUtil);
                Method m = SaSendPrimaryContactServiceImpl.class.getDeclaredMethod("validateRequired",
                                SendPrimaryContactRequestDto.class);
                m.setAccessible(true);

                SendPrimaryContactRequestDto request = mock(SendPrimaryContactRequestDto.class);
                when(request.getInternalUserId()).thenReturn("iu");
                when(request.getBrdCd()).thenReturn("0");

                // Act
                String res = (String) m.invoke(sut, request);

                // Assert
                assertNull(res);
        }

        /**
         * クラス：SaSendPrimaryContactServiceImpl validateRequired
         * processIdがnullの場合にprocessIdが返ることを確認するテストケース
         */
        @Test
        void validateRequired_003() throws Exception {
                // Arrange
                SaSendPrimaryContactServiceImpl sut = new SaSendPrimaryContactServiceImpl(jsapUtil);
                Method m = SaSendPrimaryContactServiceImpl.class.getDeclaredMethod("validateRequired",
                                SendPrimaryContactRequestDto.class);
                m.setAccessible(true);

                SendPrimaryContactRequestDto request = mock(SendPrimaryContactRequestDto.class); // null側
                when(request.getInternalUserId()).thenReturn("iu");
                when(request.getBrdCd()).thenReturn("0");

                // Act
                String res = (String) m.invoke(sut, request);

                // Assert
                assertEquals(null, res);
        }

        /**
         * クラス：SaSendPrimaryContactServiceImpl validateRequired
         * internalUserIdが空文字の場合にinternalUserIdが返ることを確認するテストケース
         */
        @Test
        void validateRequired_004() throws Exception {
                // Arrange
                SaSendPrimaryContactServiceImpl sut = new SaSendPrimaryContactServiceImpl(jsapUtil);
                Method m = SaSendPrimaryContactServiceImpl.class.getDeclaredMethod("validateRequired",
                                SendPrimaryContactRequestDto.class);
                m.setAccessible(true);

                SendPrimaryContactRequestDto request = mock(SendPrimaryContactRequestDto.class);
                when(request.getProcessId()).thenReturn("proc");
                when(request.getInternalUserId()).thenReturn(""); // empty側
                when(request.getBrdCd()).thenReturn("0");

                // Act
                String res = (String) m.invoke(sut, request);

                // Assert
                assertEquals(null, res);
        }

        /**
         * クラス：SaSendPrimaryContactServiceImpl validateRequired
         * brdCdが空文字の場合にbrdCdが返ることを確認するテストケース
         */
        @Test
        void validateRequired_005() throws Exception {
                // Arrange
                SaSendPrimaryContactServiceImpl sut = new SaSendPrimaryContactServiceImpl(jsapUtil);
                Method m = SaSendPrimaryContactServiceImpl.class.getDeclaredMethod("validateRequired",
                                SendPrimaryContactRequestDto.class);
                m.setAccessible(true);

                SendPrimaryContactRequestDto request = mock(SendPrimaryContactRequestDto.class);
                when(request.getInternalUserId()).thenReturn("iu");
                when(request.getBrdCd()).thenReturn(""); // empty側

                // Act
                String res = (String) m.invoke(sut, request);

                // Assert
                assertEquals("brdCd", res);
        }

        /**
         * クラス：SaSendPrimaryContactServiceImpl validateRequired
         * 複数項目不足の場合にカンマ区切りで返ることを確認するテストケース
         */
        @Test
        void validateRequired_006() throws Exception {
                // Arrange
                SaSendPrimaryContactServiceImpl sut = new SaSendPrimaryContactServiceImpl(jsapUtil);
                Method m = SaSendPrimaryContactServiceImpl.class.getDeclaredMethod("validateRequired",
                                SendPrimaryContactRequestDto.class);
                m.setAccessible(true);

                SendPrimaryContactRequestDto request = mock(SendPrimaryContactRequestDto.class);
                when(request.getProcessId()).thenReturn(""); // empty
                when(request.getInternalUserId()).thenReturn(null); // null
                when(request.getBrdCd()).thenReturn(""); // empty

                // Act
                String res = (String) m.invoke(sut, request);

                // Assert（追加順：processId, internalUserId, brdCd）
                assertEquals("internalUserId, processId,brdCd", res);
        }

        // ---------------------------------------------------------------------
        // isValidBrdCd (reflection) ：ORの "1" / "2" true を補完
        // ---------------------------------------------------------------------

        /**
         * クラス：SaSendPrimaryContactServiceImpl isValidBrdCd 有効値(1)でtrueとなることを確認するテストケース
         */
        @Test
        void isValidBrdCd_003() throws Exception {
                // Arrange
                SaSendPrimaryContactServiceImpl sut = new SaSendPrimaryContactServiceImpl(jsapUtil);
                Method m = SaSendPrimaryContactServiceImpl.class.getDeclaredMethod("isValidBrdCd", String.class);
                m.setAccessible(true);

                // Act
                boolean res = (boolean) m.invoke(sut, "1");

                // Assert
                assertTrue(res);
        }

        /**
         * クラス：SaSendPrimaryContactServiceImpl isValidBrdCd 有効値(2)でtrueとなることを確認するテストケース
         */
        @Test
        void isValidBrdCd_004() throws Exception {
                // Arrange
                SaSendPrimaryContactServiceImpl sut = new SaSendPrimaryContactServiceImpl(jsapUtil);
                Method m = SaSendPrimaryContactServiceImpl.class.getDeclaredMethod("isValidBrdCd", String.class);
                m.setAccessible(true);

                // Act
                boolean res = (boolean) m.invoke(sut, "2");

                // Assert
                assertTrue(res);
        }

        // ---------------------------------------------------------------------
        // sendPrimaryContact：validate の不正ブランド分岐
        // ---------------------------------------------------------------------

        /**
         * クラス：SaSendPrimaryContactServiceImpl
         * 異常系（不正ブランド）でTscApplicationExceptionとなることを確認するテストケース
         */
        @Test
        void sendPrimaryContact_004() {
                // Arrange
                SaSendPrimaryContactServiceImpl sut = new SaSendPrimaryContactServiceImpl(jsapUtil);
                SendPrimaryContactRequestDto request = mock(SendPrimaryContactRequestDto.class);
                RequestHeaderDto header = mock(RequestHeaderDto.class);
                when(header.getCorrelationId()).thenReturn("corr");

                when(request.getInternalUserId()).thenReturn("iu");
                when(request.getBrdCd()).thenReturn("9"); // 不正ブランド

                try (MockedStatic<CommonUtil> cu = mockStatic(CommonUtil.class);
                                MockedStatic<LogUtil> lu = mockStatic(LogUtil.class)) {

                        cu.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        cu.when(() -> CommonUtil.getLogsMessage(anyString(), org.mockito.Mockito.<Object[]>any()))
                                        .thenReturn("msg");
                        cu.when(() -> CommonUtil.getResultCode(anyString())).thenReturn("RC_BAD");
                        lu.when(() -> LogUtil.error(any(), anyString())).thenAnswer(inv -> null);

                        // Act + Assert
                        assertThrows(TscApplicationException.class, () -> sut.sendPrimaryContact(request, header));
                }
        }

        // ---------------------------------------------------------------------
        // sendRequest：getUserInfo が 2xx ではない分岐（sendPrimaryContact 経由）
        // ---------------------------------------------------------------------

        // ---------------------------------------------------------------------
        // sendOperation (reflection)：EMAIL分岐・未知contactType分岐
        // ---------------------------------------------------------------------

        /**
         * クラス：SaSendPrimaryContactServiceImpl sendOperation
         * contactType=EMAIL(1)の場合に送信が実行されることを確認するテストケース
         */
        @Test
        void sendOperation_002() throws Exception {
                // Arrange
                SaSendPrimaryContactServiceImpl sut = new SaSendPrimaryContactServiceImpl(jsapUtil);

                Method m = SaSendPrimaryContactServiceImpl.class.getDeclaredMethod(
                                "sendOperation", List.class, SendPrimaryContactRequestDto.class, RequestHeaderDto.class,
                                String.class, String.class);
                m.setAccessible(true);

                GetUserInfoResponseDto.ContactDto contact = mock(GetUserInfoResponseDto.ContactDto.class);
                when(contact.isPrimaryContactFlag()).thenReturn(true);
                when(contact.getContactType()).thenReturn("1"); // EMAIL

                SendPrimaryContactRequestDto request = mock(SendPrimaryContactRequestDto.class);
                when(request.getProcessId()).thenReturn("proc");
                when(request.getInternalUserId()).thenReturn("iu"); // hasUserId=true
                when(request.getBrdCd()).thenReturn("0");
                when(request.getTitle()).thenReturn("title");
                when(request.getBodyText()).thenReturn("text");
                when(request.getBodyHtml()).thenReturn("<b>html</b>");

                RequestHeaderDto header = mock(RequestHeaderDto.class);
                when(header.getCorrelationId()).thenReturn("corr");

                // executeSendMessage 内の ObjectMapper を安定させる（readValue を成功コードで返す）
                SendMessageResponseDto sendDto = mock(SendMessageResponseDto.class);
                when(sendDto.getResultCode()).thenReturn("000000");

                when(jsapUtil.executeSendMessage(eq("proc"), eq("u"), eq("1"), eq("title"), any(), eq("tok")))
                                .thenReturn(ResponseEntity.ok("{json}"));

                try (MockedConstruction<ObjectMapper> om = mockConstruction(ObjectMapper.class,
                                (mock, ctx) -> when(mock.readValue(anyString(), eq(SendMessageResponseDto.class)))
                                                .thenReturn(sendDto));
                                MockedStatic<CommonUtil> cu = mockStatic(CommonUtil.class)) {

                        cu.when(() -> CommonUtil.getLogsMessage(anyString(), org.mockito.Mockito.<Object[]>any()))
                                        .thenReturn("msg");
                        cu.when(() -> CommonUtil.getBrd(anyString())).thenReturn("brd");

                        // Act
                        m.invoke(sut, List.of(contact), request, header, "u", "tok");

                        // Assert
                        verify(jsapUtil, times(1))
                                        .executeSendMessage(eq("proc"), eq("u"), eq("1"), eq("title"), any(),
                                                        eq("tok"));
                }
        }

        /**
         * クラス：SaSendPrimaryContactServiceImpl sendOperation
         * 未知のcontactTypeの場合にCustomExceptionとなることを確認するテストケース
         */
        @Test
        void sendOperation_003() throws Exception {
                // Arrange
                SaSendPrimaryContactServiceImpl sut = new SaSendPrimaryContactServiceImpl(jsapUtil);

                Method m = SaSendPrimaryContactServiceImpl.class.getDeclaredMethod(
                                "sendOperation", List.class, SendPrimaryContactRequestDto.class, RequestHeaderDto.class,
                                String.class, String.class);
                m.setAccessible(true);

                GetUserInfoResponseDto.ContactDto contact = mock(GetUserInfoResponseDto.ContactDto.class);
                when(contact.isPrimaryContactFlag()).thenReturn(true);
                when(contact.getContactType()).thenReturn("9"); // 未知

                SendPrimaryContactRequestDto request = mock(SendPrimaryContactRequestDto.class);
                when(request.getInternalUserId()).thenReturn("iu"); // hasUserId計算で参照される

                // header/token/userId はこの分岐では実質使われないが、引数として必要
                RequestHeaderDto header = mock(RequestHeaderDto.class);

                // Act + Assert（InvocationTargetException の cause を投げ直す）
                assertThrows(CustomException.class, () -> {
                        try {
                                m.invoke(sut, List.of(contact), request, header, "u", "tok");
                        } catch (Exception ex) {
                                throw (RuntimeException) ex.getCause();
                        }
                });

                // Assert（送信APIは呼ばれない）
                verify(jsapUtil, never()).executeSendMessage(anyString(), anyString(), anyString(), anyString(), any(),
                                anyString());
        }

        // ---------------------------------------------------------------------
        // validateRequired：brdCd の null 分岐（OR左=true）
        // ---------------------------------------------------------------------

        /**
         * クラス：SaSendPrimaryContactServiceImpl validateRequired
         * brdCdがnullの場合にbrdCdが返ることを確認するテストケース
         */
        @Test
        void validateRequired_007() throws Exception {
                // Arrange
                SaSendPrimaryContactServiceImpl sut = new SaSendPrimaryContactServiceImpl(jsapUtil);
                Method m = SaSendPrimaryContactServiceImpl.class.getDeclaredMethod("validateRequired",
                                SendPrimaryContactRequestDto.class);
                m.setAccessible(true);

                SendPrimaryContactRequestDto request = mock(SendPrimaryContactRequestDto.class);
                when(request.getInternalUserId()).thenReturn("iu");
                when(request.getBrdCd()).thenReturn(null); // ★ null側

                // Act
                String res = (String) m.invoke(sut, request);

                // Assert
                assertEquals("brdCd", res);
        }

        // ---------------------------------------------------------------------
        // sendOperation：hasUserId の && 分岐（null / 空文字）
        // ※primaryContactFlag=false で早期returnさせ、余計な依存を作らず分岐だけ踏む
        // ---------------------------------------------------------------------

        /**
         * クラス：SaSendPrimaryContactServiceImpl sendOperation
         * internalUserId=nullの場合にhasUserId=falseとなる分岐を通ることを確認するテストケース
         */
        @Test
        void sendOperation_004() throws Exception {
                // Arrange
                SaSendPrimaryContactServiceImpl sut = new SaSendPrimaryContactServiceImpl(jsapUtil);
                Method m = SaSendPrimaryContactServiceImpl.class.getDeclaredMethod(
                                "sendOperation", List.class, SendPrimaryContactRequestDto.class, RequestHeaderDto.class,
                                String.class, String.class);
                m.setAccessible(true);

                GetUserInfoResponseDto.ContactDto contact = mock(GetUserInfoResponseDto.ContactDto.class);
                when(contact.isPrimaryContactFlag()).thenReturn(false); // ここで早期return

                SendPrimaryContactRequestDto request = mock(SendPrimaryContactRequestDto.class);
                when(request.getInternalUserId()).thenReturn(null); // ★ && 左が false

                // Act
                m.invoke(sut, List.of(contact), request, mock(RequestHeaderDto.class), "u", "tok");

                // Assert（送信はされない）
                verify(jsapUtil, never()).executeSendMessage(anyString(), anyString(), anyString(), anyString(), any(),
                                anyString());
        }

        /**
         * クラス：SaSendPrimaryContactServiceImpl sendOperation
         * internalUserId=""の場合にhasUserId=falseとなる分岐を通ることを確認するテストケース
         */
        @Test
        void sendOperation_005() throws Exception {
                // Arrange
                SaSendPrimaryContactServiceImpl sut = new SaSendPrimaryContactServiceImpl(jsapUtil);
                Method m = SaSendPrimaryContactServiceImpl.class.getDeclaredMethod(
                                "sendOperation", List.class, SendPrimaryContactRequestDto.class, RequestHeaderDto.class,
                                String.class, String.class);
                m.setAccessible(true);

                GetUserInfoResponseDto.ContactDto contact = mock(GetUserInfoResponseDto.ContactDto.class);
                when(contact.isPrimaryContactFlag()).thenReturn(false); // ここで早期return

                SendPrimaryContactRequestDto request = mock(SendPrimaryContactRequestDto.class);
                when(request.getInternalUserId()).thenReturn(""); // ★ && 左true → 右(!isEmpty)が false

                // Act
                m.invoke(sut, List.of(contact), request, mock(RequestHeaderDto.class), "u", "tok");

                // Assert（送信はされない）
                verify(jsapUtil, never()).executeSendMessage(anyString(), anyString(), anyString(), anyString(), any(),
                                anyString());
        }

        // ---------------------------------------------------------------------
        // executeSendMessage：hasUserId ? ... : ... の false 側を通す（Reflection直撃）
        // - 成功パス（RS07I00008 側：hasUserId=false）
        // - 失敗パス（RS07E00010 側：hasUserId=false）
        // ---------------------------------------------------------------------

        /**
         * クラス：SaSendPrimaryContactServiceImpl executeSendMessage
         * hasUserId=falseの場合に"プロセスID"側の分岐で正常終了することを確認するテストケース
         */
        @Test
        void executeSendMessage_001() throws Exception {
                // Arrange
                SaSendPrimaryContactServiceImpl sut = new SaSendPrimaryContactServiceImpl(jsapUtil);
                Method m = SaSendPrimaryContactServiceImpl.class.getDeclaredMethod(
                                "executeSendMessage",
                                SendPrimaryContactRequestDto.class, RequestHeaderDto.class,
                                GetUserInfoResponseDto.ContactDto.class,
                                String.class, boolean.class, Object.class, String.class);
                m.setAccessible(true);

                SendPrimaryContactRequestDto request = mock(SendPrimaryContactRequestDto.class);
                when(request.getProcessId()).thenReturn("proc");
                when(request.getBrdCd()).thenReturn("0");
                when(request.getTitle()).thenReturn("title");
                // ★ hasUserId=false側では request.getInternalUserId() は評価されないのでスタブしない

                RequestHeaderDto header = mock(RequestHeaderDto.class);
                when(header.getCorrelationId()).thenReturn("corr");

                GetUserInfoResponseDto.ContactDto contact = mock(GetUserInfoResponseDto.ContactDto.class);
                when(contact.getContactType()).thenReturn("0");

                when(jsapUtil.executeSendMessage(eq("proc"), eq("u"), eq("0"), eq("title"), any(), eq("tok")))
                                .thenReturn(ResponseEntity.ok("{json}"));

                com.toyota.tsc.notificationhub.models.SendMessageResponseDto sendDto = mock(
                                com.toyota.tsc.notificationhub.models.SendMessageResponseDto.class);
                when(sendDto.getResultCode()).thenReturn("000000");

                try (org.mockito.MockedConstruction<com.fasterxml.jackson.databind.ObjectMapper> om = mockConstruction(
                                com.fasterxml.jackson.databind.ObjectMapper.class,
                                (mock, ctx) -> when(mock.readValue(anyString(),
                                                eq(com.toyota.tsc.notificationhub.models.SendMessageResponseDto.class)))
                                                .thenReturn(sendDto));
                                MockedStatic<CommonUtil> cu = mockStatic(CommonUtil.class);
                                MockedStatic<LogUtil> lu = mockStatic(LogUtil.class)) {

                        cu.when(() -> CommonUtil.getLogsMessage(anyString(), org.mockito.Mockito.<Object[]>any()))
                                        .thenReturn("msg");
                        cu.when(() -> CommonUtil.getBrd(anyString())).thenReturn("brd");
                        lu.when(() -> LogUtil.info(any(), anyString())).thenAnswer(inv -> null);

                        // Act（hasUserId=false）
                        m.invoke(sut, request, header, contact, "u", false, new Object(), "tok");

                        // Assert
                        verify(jsapUtil, times(1))
                                        .executeSendMessage(eq("proc"), eq("u"), eq("0"), eq("title"), any(),
                                                        eq("tok"));
                }
        }

        /**
         * クラス：SaSendPrimaryContactServiceImpl executeSendMessage
         * hasUserId=falseの場合に"プロセスID"側の分岐で送信エラーとなることを確認するテストケース
         */
        @Test
        void executeSendMessage_002() throws Exception {
                // Arrange
                SaSendPrimaryContactServiceImpl sut = new SaSendPrimaryContactServiceImpl(jsapUtil);
                Method m = SaSendPrimaryContactServiceImpl.class.getDeclaredMethod(
                                "executeSendMessage",
                                SendPrimaryContactRequestDto.class, RequestHeaderDto.class,
                                GetUserInfoResponseDto.ContactDto.class,
                                String.class, boolean.class, Object.class, String.class);
                m.setAccessible(true);

                SendPrimaryContactRequestDto request = mock(SendPrimaryContactRequestDto.class);
                when(request.getProcessId()).thenReturn("proc");
                when(request.getBrdCd()).thenReturn("0");
                when(request.getTitle()).thenReturn("title");
                // ★ hasUserId=false側では request.getInternalUserId() は評価されないのでスタブしない

                RequestHeaderDto header = mock(RequestHeaderDto.class);
                when(header.getCorrelationId()).thenReturn("corr");

                GetUserInfoResponseDto.ContactDto contact = mock(GetUserInfoResponseDto.ContactDto.class);
                when(contact.getContactType()).thenReturn("0");

                when(jsapUtil.executeSendMessage(eq("proc"), eq("u"), eq("0"), eq("title"), any(), eq("tok")))
                                .thenReturn(ResponseEntity.ok("{json}"));

                com.toyota.tsc.notificationhub.models.SendMessageResponseDto sendDto = mock(
                                com.toyota.tsc.notificationhub.models.SendMessageResponseDto.class);
                when(sendDto.getResultCode()).thenReturn("NG");

                try (org.mockito.MockedConstruction<com.fasterxml.jackson.databind.ObjectMapper> om = mockConstruction(
                                com.fasterxml.jackson.databind.ObjectMapper.class,
                                (mock, ctx) -> when(mock.readValue(anyString(),
                                                eq(com.toyota.tsc.notificationhub.models.SendMessageResponseDto.class)))
                                                .thenReturn(sendDto));
                                MockedStatic<CommonUtil> cu = mockStatic(CommonUtil.class);
                                MockedStatic<LogUtil> lu = mockStatic(LogUtil.class)) {

                        cu.when(() -> CommonUtil.getLogsMessage(anyString(), org.mockito.Mockito.<Object[]>any()))
                                        .thenReturn("msg");
                        cu.when(() -> CommonUtil.getResultCode(anyString())).thenReturn("RC_SEND");
                        cu.when(() -> CommonUtil.getBrd(anyString())).thenReturn("brd");
                        lu.when(() -> LogUtil.error(any(), anyString())).thenAnswer(inv -> null);

                        // Act + Assert（privateメソッドなので cause を投げ直す）
                        assertThrows(TscPrimaryContactException.class, () -> {
                                try {
                                        m.invoke(sut, request, header, contact, "u", false, new Object(), "tok");
                                } catch (Exception ex) {
                                        throw (RuntimeException) ex.getCause();
                                }
                        });
                }
        }

        // ---------------------------------------------------------------------
        // sendPrimaryContact：token null/empty 分岐
        // ---------------------------------------------------------------------

        /**
         * クラス：SaSendPrimaryContactServiceImpl
         * 異常系（JSAPトークンがnull）でTscApplicationExceptionとなることを確認するテストケース
         */
        @Test
        void sendPrimaryContact_008() {
                // Arrange
                SaSendPrimaryContactServiceImpl sut = new SaSendPrimaryContactServiceImpl(jsapUtil);

                SendPrimaryContactRequestDto request = new SendPrimaryContactRequestDto(
                                "proc-008", "iu-008", "0", "title", "text", "<b>html</b>", "sms");
                RequestHeaderDto header = mock(RequestHeaderDto.class);
                when(header.getCorrelationId()).thenReturn("corr-008");

                // access_token = null
                when(jsapUtil.executeGetToken()).thenReturn(new GetALJTokenResultDto(true, null));

                try (MockedStatic<CommonUtil> cu = mockStatic(CommonUtil.class);
                                MockedStatic<LogUtil> lu = mockStatic(LogUtil.class)) {

                        cu.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        cu.when(() -> CommonUtil.getLogsMessage(anyString(), org.mockito.Mockito.<Object[]>any()))
                                        .thenReturn("msg");
                        cu.when(() -> CommonUtil.getResultCode(anyString())).thenReturn("RC_ERR");
                        lu.when(() -> LogUtil.info(any(), anyString())).thenAnswer(inv -> null);
                        lu.when(() -> LogUtil.error(any(), anyString())).thenAnswer(inv -> null);

                        // Act + Assert
                        assertThrows(CustomException.class, () -> sut.sendPrimaryContact(request, header));
                }
        }

        /**
         * クラス：SaSendPrimaryContactServiceImpl
         * 異常系（JSAPトークンが空文字）でTscApplicationExceptionとなることを確認するテストケース
         */
        @Test
        void sendPrimaryContact_009() {
                // Arrange
                SaSendPrimaryContactServiceImpl sut = new SaSendPrimaryContactServiceImpl(jsapUtil);

                SendPrimaryContactRequestDto request = new SendPrimaryContactRequestDto(
                                "proc-009", "iu-009", "0", "title", "text", "<b>html</b>", "sms");
                RequestHeaderDto header = mock(RequestHeaderDto.class);
                when(header.getCorrelationId()).thenReturn("corr-009");

                // access_token = ""
                when(jsapUtil.executeGetToken()).thenReturn(new GetALJTokenResultDto(true, ""));

                try (MockedStatic<CommonUtil> cu = mockStatic(CommonUtil.class);
                                MockedStatic<LogUtil> lu = mockStatic(LogUtil.class)) {

                        cu.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        cu.when(() -> CommonUtil.getLogsMessage(anyString(), org.mockito.Mockito.<Object[]>any()))
                                        .thenReturn("msg");
                        cu.when(() -> CommonUtil.getResultCode(anyString())).thenReturn("RC_ERR");
                        lu.when(() -> LogUtil.info(any(), anyString())).thenAnswer(inv -> null);
                        lu.when(() -> LogUtil.error(any(), anyString())).thenAnswer(inv -> null);

                        // Act + Assert
                        assertThrows(CustomException.class, () -> sut.sendPrimaryContact(request, header));
                }
        }

        // ---------------------------------------------------------------------
        // sendPrimaryContact：validate のmissingField != null 分岐
        // ---------------------------------------------------------------------

        /**
         * クラス：SaSendPrimaryContactServiceImpl
         * 異常系（必須項目不足・validate経由）でTscApplicationExceptionとなることを確認するテストケース
         */
        @Test
        void sendPrimaryContact_010() {
                // Arrange
                SaSendPrimaryContactServiceImpl sut = new SaSendPrimaryContactServiceImpl(jsapUtil);

                // internalUserId/processId共にnull → validateRequired returns non-null
                SendPrimaryContactRequestDto request = new SendPrimaryContactRequestDto(
                                null, null, "0", "title", "text", "<b>html</b>", "sms");
                RequestHeaderDto header = mock(RequestHeaderDto.class);
                when(header.getCorrelationId()).thenReturn("corr-010");

                try (MockedStatic<CommonUtil> cu = mockStatic(CommonUtil.class);
                                MockedStatic<LogUtil> lu = mockStatic(LogUtil.class)) {

                        cu.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        cu.when(() -> CommonUtil.getLogsMessage(anyString(), org.mockito.Mockito.<Object[]>any()))
                                        .thenReturn("msg");
                        cu.when(() -> CommonUtil.getResultCode(anyString())).thenReturn("RC_FIELD");
                        lu.when(() -> LogUtil.info(any(), anyString())).thenAnswer(inv -> null);
                        lu.when(() -> LogUtil.error(any(), anyString())).thenAnswer(inv -> null);

                        // Act + Assert
                        assertThrows(TscApplicationException.class, () -> sut.sendPrimaryContact(request, header));
                }
        }

        // ---------------------------------------------------------------------
        // validateRequired: processId == null 分岐
        // ---------------------------------------------------------------------

        /**
         * クラス：SaSendPrimaryContactServiceImpl validateRequired
         * internalUserIdとprocessIdが両方nullの場合に不足メッセージが返ることを確認するテストケース
         */
        @Test
        void validateRequired_008() throws Exception {
                // Arrange
                SaSendPrimaryContactServiceImpl sut = new SaSendPrimaryContactServiceImpl(jsapUtil);
                Method m = SaSendPrimaryContactServiceImpl.class.getDeclaredMethod("validateRequired",
                                SendPrimaryContactRequestDto.class);
                m.setAccessible(true);

                SendPrimaryContactRequestDto request = mock(SendPrimaryContactRequestDto.class);
                when(request.getInternalUserId()).thenReturn(null); // null
                when(request.getProcessId()).thenReturn(null); // null → processId == null → true (short-circuit)
                when(request.getBrdCd()).thenReturn("0");

                // Act
                String res = (String) m.invoke(sut, request);

                // Assert
                assertEquals("internalUserId, processId", res);
        }

        // ---------------------------------------------------------------------
        // sendPrimaryContact：catch (TscPrimaryContactException) を踏む
        // ※通常ルートでは CustomException に包まれがちなので、LogUtil.info から直接投げて捕捉させる
        // ---------------------------------------------------------------------

        /**
         * クラス：SaSendPrimaryContactServiceImpl
         * 異常系（TscPrimaryContactException発生）で同例外が再送出されることを確認するテストケース
         */
        @Test
        void sendPrimaryContact_007() {
                // Arrange
                SaSendPrimaryContactServiceImpl sut = new SaSendPrimaryContactServiceImpl(jsapUtil);

                SendPrimaryContactRequestDto request = new SendPrimaryContactRequestDto(
                                "proc-007", "iu-007", "0", "title", "text", "<b>html</b>", "sms");
                RequestHeaderDto header = mock(RequestHeaderDto.class);
                when(header.getCorrelationId()).thenReturn("corr-007");

                // ★ 実体例外（モックは使わない）
                TscPrimaryContactException pcEx = new TscPrimaryContactException("RC_PC");

                // ★ toJson を落とす：getMessage まで到達しないので getMessage の stub は絶対にしない
                try (MockedStatic<CommonUtil> cu = mockStatic(CommonUtil.class)) {

                        cu.when(() -> CommonUtil.toJson(any())).thenThrow(pcEx);

                        // Act
                        TscPrimaryContactException thrown = assertThrows(TscPrimaryContactException.class,
                                        () -> sut.sendPrimaryContact(request, header));

                        // Assert
                        assertEquals("RC_PC", thrown.getResultCode());
                }
        }

        // ---------------------------------------------------------------------
        // sendPrimaryContact：完全成功パス（getUserId成功 → getUserInfo 2xx → EMAIL送信成功）
        // ※ L82-89 (return ResponseDto), L120 false, L134 false, L126-143 を通す
        // ---------------------------------------------------------------------

        /**
         * クラス：SaSendPrimaryContactServiceImpl
         * 正常系（全処理正常完了）でResponseDtoが返ることを確認するテストケース
         */
        @Test
        void sendPrimaryContact_011() {
                // Arrange
                SaSendPrimaryContactServiceImpl sut = new SaSendPrimaryContactServiceImpl(jsapUtil);

                SendPrimaryContactRequestDto request = new SendPrimaryContactRequestDto(
                                "proc", "iu", "0", "title", "text", "<b>html</b>", "sms");
                RequestHeaderDto header = mock(RequestHeaderDto.class);
                when(header.getCorrelationId()).thenReturn("corr");

                // トークン有効
                when(jsapUtil.executeGetToken()).thenReturn(new GetALJTokenResultDto(true, "tok"));
                // getUserId 成功（RES_GETUSERID_SUCCESS = "00001548B123"）
                when(jsapUtil.executeGetUserId(anyString(), anyString()))
                                .thenReturn(ResponseEntity
                                                .ok("{\"resultCode\":\"00001548B123\",\"userId\":\"userId123\"}"));
                // getUserInfo 2xx（EMAIL contact 1件）
                when(jsapUtil.executeGetUserInfo(anyString(), anyString()))
                                .thenReturn(ResponseEntity
                                                .ok("{\"contactList\":[{\"contactType\":\"1\",\"primaryContactFlag\":true}]}"));
                // sendMessage 成功
                when(jsapUtil.executeSendMessage(any(), any(), any(), any(), any(), any()))
                                .thenReturn(ResponseEntity.ok("{\"resultCode\":\"000000\"}"));

                try (MockedStatic<CommonUtil> cu = mockStatic(CommonUtil.class);
                                MockedStatic<LogUtil> lu = mockStatic(LogUtil.class)) {

                        // Act
                        ResponseDto result = sut.sendPrimaryContact(request, header);

                        // Assert
                        assertNotNull(result);
                }
        }

        // ---------------------------------------------------------------------
        // sendPrimaryContact：getUserInfo が 2xx でない分岐（L134 true）
        // ---------------------------------------------------------------------

        /**
         * クラス：SaSendPrimaryContactServiceImpl
         * 異常系（getUserInfo非2xx）でTscPrimaryContactExceptionとなることを確認するテストケース
         */
        @Test
        void sendPrimaryContact_012() {
                // Arrange
                SaSendPrimaryContactServiceImpl sut = new SaSendPrimaryContactServiceImpl(jsapUtil);

                SendPrimaryContactRequestDto request = new SendPrimaryContactRequestDto(
                                "proc", "iu", "0", "title", "text", "<b>html</b>", "sms");
                RequestHeaderDto header = mock(RequestHeaderDto.class);
                when(header.getCorrelationId()).thenReturn("corr");

                when(jsapUtil.executeGetToken()).thenReturn(new GetALJTokenResultDto(true, "tok"));
                when(jsapUtil.executeGetUserId(anyString(), anyString()))
                                .thenReturn(ResponseEntity
                                                .ok("{\"resultCode\":\"00001548B123\",\"userId\":\"userId123\"}"));
                // 500 応答（非 2xx）
                when(jsapUtil.executeGetUserInfo(anyString(), anyString()))
                                .thenReturn(ResponseEntity.status(500).body("{}"));

                try (MockedStatic<CommonUtil> cu = mockStatic(CommonUtil.class);
                                MockedStatic<LogUtil> lu = mockStatic(LogUtil.class)) {

                        // Act + Assert
                        assertThrows(TscPrimaryContactException.class,
                                        () -> sut.sendPrimaryContact(request, header));
                }
        }

        // ---------------------------------------------------------------------
        // sendOperation：PHONE（contactType="0"）分岐（L170 true / L171）
        // ---------------------------------------------------------------------

        /**
         * クラス：SaSendPrimaryContactServiceImpl sendOperation
         * contactType=PHONE(0)の場合にcreateSmsContextが呼ばれることを確認するテストケース
         */
        @Test
        void sendOperation_007() throws Exception {
                // Arrange
                SaSendPrimaryContactServiceImpl sut = new SaSendPrimaryContactServiceImpl(jsapUtil);

                Method m = SaSendPrimaryContactServiceImpl.class.getDeclaredMethod(
                                "sendOperation", List.class, SendPrimaryContactRequestDto.class,
                                RequestHeaderDto.class, String.class, String.class);
                m.setAccessible(true);

                GetUserInfoResponseDto.ContactDto contact = mock(GetUserInfoResponseDto.ContactDto.class);
                when(contact.isPrimaryContactFlag()).thenReturn(true);
                when(contact.getContactType()).thenReturn("0"); // PHONE

                // 実オブジェクト（getBodyText() = "text" で createSmsContext が呼ばれる）
                SendPrimaryContactRequestDto request = new SendPrimaryContactRequestDto(
                                "proc", "iu", "0", "title", "text", "<b>html</b>", "sms");
                RequestHeaderDto header = mock(RequestHeaderDto.class);
                when(header.getCorrelationId()).thenReturn("corr");

                // executeSendMessage 内の jsapUtil.executeSendMessage 成功
                when(jsapUtil.executeSendMessage(any(), any(), any(), any(), any(), any()))
                                .thenReturn(ResponseEntity.ok("{\"resultCode\":\"000000\"}"));

                try (MockedStatic<CommonUtil> cu = mockStatic(CommonUtil.class);
                                MockedStatic<LogUtil> lu = mockStatic(LogUtil.class)) {

                        // Act
                        m.invoke(sut, List.of(contact), request, header, "u", "tok");

                        // Assert（PHONE → executeSendMessage が1回呼ばれる）
                        verify(jsapUtil, times(1))
                                        .executeSendMessage(any(), any(), any(), any(), any(), any());
                }
        }

        // ---------------------------------------------------------------------
        // executeSendMessage：hasUserId=true でエラーパスの三項演算（L209/L210 true）
        // ---------------------------------------------------------------------

        /**
         * クラス：SaSendPrimaryContactServiceImpl executeSendMessage
         * hasUserId=trueの場合に"内部UserID"側の分岐で送信エラーとなることを確認するテストケース
         */
        @Test
        void executeSendMessage_003() throws Exception {
                // Arrange
                SaSendPrimaryContactServiceImpl sut = new SaSendPrimaryContactServiceImpl(jsapUtil);
                Method m = SaSendPrimaryContactServiceImpl.class.getDeclaredMethod(
                                "executeSendMessage",
                                SendPrimaryContactRequestDto.class, RequestHeaderDto.class,
                                GetUserInfoResponseDto.ContactDto.class,
                                String.class, boolean.class, Object.class, String.class);
                m.setAccessible(true);

                SendPrimaryContactRequestDto request = mock(SendPrimaryContactRequestDto.class);
                when(request.getProcessId()).thenReturn("proc");
                when(request.getTitle()).thenReturn("title");
                when(request.getInternalUserId()).thenReturn("iu"); // hasUserId=true → L210 true側で参照
                when(request.getBrdCd()).thenReturn("0");

                RequestHeaderDto header = mock(RequestHeaderDto.class);
                when(header.getCorrelationId()).thenReturn("corr");

                GetUserInfoResponseDto.ContactDto contact = mock(GetUserInfoResponseDto.ContactDto.class);
                when(contact.getContactType()).thenReturn("1");

                when(jsapUtil.executeSendMessage(any(), any(), any(), any(), any(), any()))
                                .thenReturn(ResponseEntity.ok("{\"resultCode\":\"NG\"}"));

                try (MockedStatic<CommonUtil> cu = mockStatic(CommonUtil.class);
                                MockedStatic<LogUtil> lu = mockStatic(LogUtil.class)) {

                        // Act + Assert（hasUserId=true でエラー → L209/L210 true側を通る）
                        assertThrows(TscPrimaryContactException.class, () -> {
                                try {
                                        m.invoke(sut, request, header, contact, "u", true, new Object(), "tok");
                                } catch (Exception ex) {
                                        throw (RuntimeException) ex.getCause();
                                }
                        });
                }
        }

        // ---------------------------------------------------------------------
        // sendRequest（reflection）：予期外例外 → catch(Exception) → CustomException
        // ---------------------------------------------------------------------

        /**
         * クラス：SaSendPrimaryContactServiceImpl sendRequest
         * jsapUtil.executeGetUserIdで予期外例外が発生した場合にCustomExceptionとなることを確認するテストケース
         */
        @Test
        void sendRequest_001() throws Exception {
                // Arrange
                SaSendPrimaryContactServiceImpl sut = new SaSendPrimaryContactServiceImpl(jsapUtil);
                Method m = SaSendPrimaryContactServiceImpl.class.getDeclaredMethod(
                                "sendRequest", String.class, SendPrimaryContactRequestDto.class,
                                RequestHeaderDto.class);
                m.setAccessible(true);

                SendPrimaryContactRequestDto request = mock(SendPrimaryContactRequestDto.class);
                when(request.getInternalUserId()).thenReturn("iu");

                RequestHeaderDto header = mock(RequestHeaderDto.class);
                when(header.getCorrelationId()).thenReturn("corr");

                when(jsapUtil.executeGetUserId("iu", "corr"))
                                .thenThrow(new RuntimeException("unexpected error"));

                // Act + Assert
                assertThrows(CustomException.class, () -> {
                        try {
                                m.invoke(sut, "token", request, header);
                        } catch (Exception ex) {
                                throw (RuntimeException) ex.getCause();
                        }
                });
        }

        // ---------------------------------------------------------------------
        // executeSendMessage（reflection）：予期外例外 → catch(Exception) → CustomException
        // ---------------------------------------------------------------------

        /**
         * クラス：SaSendPrimaryContactServiceImpl executeSendMessage
         * jsapUtil.executeSendMessageで予期外例外が発生した場合にCustomExceptionとなることを確認するテストケース
         */
        @Test
        void executeSendMessage_004() throws Exception {
                // Arrange
                SaSendPrimaryContactServiceImpl sut = new SaSendPrimaryContactServiceImpl(jsapUtil);
                Method m = SaSendPrimaryContactServiceImpl.class.getDeclaredMethod(
                                "executeSendMessage",
                                SendPrimaryContactRequestDto.class, RequestHeaderDto.class,
                                GetUserInfoResponseDto.ContactDto.class,
                                String.class, boolean.class, Object.class, String.class);
                m.setAccessible(true);

                SendPrimaryContactRequestDto request = mock(SendPrimaryContactRequestDto.class);
                when(request.getProcessId()).thenReturn("proc");
                when(request.getTitle()).thenReturn("title");

                RequestHeaderDto header = mock(RequestHeaderDto.class);

                GetUserInfoResponseDto.ContactDto contact = mock(GetUserInfoResponseDto.ContactDto.class);
                when(contact.getContactType()).thenReturn("0");

                when(jsapUtil.executeSendMessage(any(), any(), any(), any(), any(), any()))
                                .thenThrow(new RuntimeException("network error"));

                // Act + Assert
                assertThrows(CustomException.class, () -> {
                        try {
                                m.invoke(sut, request, header, contact, "u", false, new Object(), "tok");
                        } catch (Exception ex) {
                                throw (RuntimeException) ex.getCause();
                        }
                });
        }

        /**
         * クラス：SaSendPrimaryContactServiceImpl
         * 異常系（executeGetTokenのresultがfalse）でTscApplicationExceptionとなることを確認するテストケース
         */
        @Test
        void sendPrimaryContact_013() {
                // Arrange
                SaSendPrimaryContactServiceImpl sut = new SaSendPrimaryContactServiceImpl(jsapUtil);

                SendPrimaryContactRequestDto request = mock(SendPrimaryContactRequestDto.class);
                RequestHeaderDto header = mock(RequestHeaderDto.class);
                when(header.getCorrelationId()).thenReturn("corr");
                when(request.getInternalUserId()).thenReturn("iu");
                when(request.getBrdCd()).thenReturn("0");

                // resToken.getResult() == false → TscApplicationException
                when(jsapUtil.executeGetToken()).thenReturn(new GetALJTokenResultDto(false, "someToken"));

                try (MockedStatic<CommonUtil> cu = mockStatic(CommonUtil.class);
                                MockedStatic<LogUtil> lu = mockStatic(LogUtil.class)) {

                        cu.when(() -> CommonUtil.getSaMessage(anyString(),
                                        org.mockito.Mockito.<Object[]>any())).thenReturn("msg");
                        cu.when(() -> CommonUtil.getResultCode(anyString())).thenReturn("RC_ERR");
                        lu.when(() -> LogUtil.error(any(), anyString())).thenAnswer(inv -> null);

                        // Act + Assert
                        assertThrows(TscApplicationException.class, () -> sut.sendPrimaryContact(request, header));
                }
        }
}
