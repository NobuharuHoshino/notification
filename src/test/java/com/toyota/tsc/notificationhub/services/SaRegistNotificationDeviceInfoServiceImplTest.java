
// ファイルパス: src/test/java/com/toyota/tsc/notificationhub/services/SaRegistNotificationDeviceInfoServiceImplTest.java
package com.toyota.tsc.notificationhub.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toyota.tsc.notificationhub.commons.CommonUtil;
import com.toyota.tsc.notificationhub.commons.JsapUtil;
import com.toyota.tsc.notificationhub.exceptions.CustomException;
import com.toyota.tsc.notificationhub.exceptions.CustomSqlException;
import com.toyota.tsc.notificationhub.exceptions.TscApplicationException;
import com.toyota.tsc.notificationhub.models.DvcLinkageResponseDto;
import com.toyota.tsc.notificationhub.models.GetUserIdResponseDto;
import com.toyota.tsc.notificationhub.models.RegistNotificationDeviceInfoRequestDto;
import com.toyota.tsc.notificationhub.models.RequestHeaderDto;
import com.toyota.tsc.notificationhub.models.ResponseDto;
import com.toyota.tsc.notificationhub.repositories.SaNtfInfoRepositoryIF;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SaRegistNotificationDeviceInfoServiceImpl のテストクラス
 */
@SuppressWarnings("all")
@ExtendWith(MockitoExtension.class)
class SaRegistNotificationDeviceInfoServiceImplTest {

        @Mock
        private SaNtfInfoRepositoryIF saNtfInfoRepository;

        @Mock
        private JsapUtil jsapUtil;

        private static RegistNotificationDeviceInfoRequestDto req(
                        String internalUserId, String platform, String deviceToken, String dvcId, String brdCd) {
                RegistNotificationDeviceInfoRequestDto r = mock(RegistNotificationDeviceInfoRequestDto.class);
                when(r.getInternalUserId()).thenReturn(internalUserId);
                when(r.getPlatform()).thenReturn(platform);
                when(r.getDeviceToken()).thenReturn(deviceToken);
                when(r.getDvcId()).thenReturn(dvcId);
                when(r.getBrdCd()).thenReturn(brdCd);
                return r;
        }

        private static RequestHeaderDto header(String correlationId) {
                RequestHeaderDto h = mock(RequestHeaderDto.class);
                when(h.getCorrelationId()).thenReturn(correlationId);
                return h;
        }

        /**
         * クラス：SaRegistNotificationDeviceInfoServiceImpl registDeviceInfo
         * 正常に成功コードが返ることを確認するテストケース
         */

        @Test
        void registDeviceInfo_001() throws Exception {
                // Arrange
                SaRegistNotificationDeviceInfoServiceImpl sut = new SaRegistNotificationDeviceInfoServiceImpl(
                                saNtfInfoRepository, jsapUtil);

                RegistNotificationDeviceInfoRequestDto request = req("u", "1", "tok", "dvc", "1");
                RequestHeaderDto header = header("cid");

                when(saNtfInfoRepository.upsert(any())).thenReturn(1);
                when(jsapUtil.executeGetUserId(eq("u"), eq("cid")))
                                .thenReturn(ResponseEntity.ok("{json}"));
                when(jsapUtil.executeDvcLink(eq("userId"), eq("tok"), eq("1"), anyString()))
                                .thenReturn(ResponseEntity.ok("{json2}"));

                GetUserIdResponseDto getUserIdDto = mock(GetUserIdResponseDto.class);
                // 実装：GetUserId 成功は "00001548B123"
                // [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/SaRegistNotificationDeviceInfoServiceImpl.java)
                when(getUserIdDto.getResultCode()).thenReturn("00001548B123");
                when(getUserIdDto.getUserId()).thenReturn("userId");

                DvcLinkageResponseDto dvcDto = mock(DvcLinkageResponseDto.class);
                // 実装：DvcLink 成功は "000000"
                // [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/SaRegistNotificationDeviceInfoServiceImpl.java)
                when(dvcDto.getResultCode()).thenReturn("000000");

                try (MockedConstruction<ObjectMapper> om = mockConstruction(ObjectMapper.class, (mock, ctx) -> {
                        when(mock.readValue(anyString(), eq(GetUserIdResponseDto.class))).thenReturn(getUserIdDto);
                        when(mock.readValue(anyString(), eq(DvcLinkageResponseDto.class))).thenReturn(dvcDto);
                });
                                MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {

                        common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class)))
                                        .thenReturn("msg");
                        common.when(() -> CommonUtil.getResultCode(anyString()))
                                        .thenAnswer(inv -> "RC_" + inv.getArgument(0));

                        // Act
                        ResponseDto resp = sut.registDeviceInfo(request, header);

                        // Assert
                        assertNotNull(resp);
                        verify(saNtfInfoRepository, times(1)).upsert(any());
                        verify(jsapUtil, times(1)).executeGetUserId(eq("u"), eq("cid"));
                        verify(jsapUtil, times(1)).executeDvcLink(eq("userId"), eq("tok"), eq("1"), anyString());
                }
        }

        /**
         * クラス：SaRegistNotificationDeviceInfoServiceImpl registDeviceInfo
         * 必須項目不足の場合にTscApplicationExceptionが送出されることを確認するテストケース
         */
        @Test
        void registDeviceInfo_002() {
                // Arrange
                SaRegistNotificationDeviceInfoServiceImpl sut = new SaRegistNotificationDeviceInfoServiceImpl(
                                saNtfInfoRepository, jsapUtil);
                RegistNotificationDeviceInfoRequestDto request = req(null, "1", "tok", "dvc", "1");
                RequestHeaderDto header = header("cid");

                try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
                        common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class)))
                                        .thenReturn("msg");
                        common.when(() -> CommonUtil.getResultCode(anyString()))
                                        .thenAnswer(inv -> "RC_" + inv.getArgument(0));

                        // Act
                        assertThrows(TscApplicationException.class, () -> sut.registDeviceInfo(request, header));

                        // Assert
                        verify(saNtfInfoRepository, never()).upsert(any());
                }
        }

        /**
         * クラス：SaRegistNotificationDeviceInfoServiceImpl registDeviceInfo DB
         * upsertが0の場合にCustomExceptionが送出されることを確認するテストケース
         */
        @Test
        void registDeviceInfo_003() throws Exception {
                // Arrange
                SaRegistNotificationDeviceInfoServiceImpl sut = new SaRegistNotificationDeviceInfoServiceImpl(
                                saNtfInfoRepository, jsapUtil);
                RegistNotificationDeviceInfoRequestDto request = req("u", "1", "tok", "dvc", "1");
                RequestHeaderDto header = header("cid");

                when(saNtfInfoRepository.upsert(any())).thenReturn(0);

                try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
                        common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class)))
                                        .thenReturn("msg");

                        // Act
                        assertThrows(CustomException.class, () -> sut.registDeviceInfo(request, header));

                        // Assert
                        verify(saNtfInfoRepository, times(1)).upsert(any());
                }
        }

        /**
         * クラス：SaRegistNotificationDeviceInfoServiceImpl registDeviceInfo
         * getUserIdの結果コードがSUCCESSの場合にTscApplicationExceptionが送出されることを確認するテストケース
         */

        @Test
        void registDeviceInfo_004() throws Exception {
                // Arrange
                SaRegistNotificationDeviceInfoServiceImpl sut = new SaRegistNotificationDeviceInfoServiceImpl(
                                saNtfInfoRepository, jsapUtil);

                RegistNotificationDeviceInfoRequestDto request = req("u", "1", "tok", "dvc", "1");
                RequestHeaderDto header = header("cid");

                when(saNtfInfoRepository.upsert(any())).thenReturn(1);
                when(jsapUtil.executeGetUserId(eq("u"), eq("cid")))
                                .thenReturn(ResponseEntity.ok("{json}"));

                GetUserIdResponseDto getUserIdDto = mock(GetUserIdResponseDto.class);
                // 実装：成功コード以外なら GetUserId エラーで TscApplicationException
                // [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/SaRegistNotificationDeviceInfoServiceImpl.java)
                when(getUserIdDto.getResultCode()).thenReturn("NOT_SUCCESS");

                try (MockedConstruction<ObjectMapper> om = mockConstruction(ObjectMapper.class, (mock, ctx) -> {
                        when(mock.readValue(anyString(), eq(GetUserIdResponseDto.class))).thenReturn(getUserIdDto);
                });
                                MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {

                        common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class)))
                                        .thenReturn("msg");
                        common.when(() -> CommonUtil.getResultCode(anyString()))
                                        .thenAnswer(inv -> "RC_" + inv.getArgument(0));

                        // Act & Assert
                        assertThrows(TscApplicationException.class, () -> sut.registDeviceInfo(request, header));

                        // Assert（GetUserId 失敗時は DvcLink
                        // まで到達しない）[1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/SaRegistNotificationDeviceInfoServiceImpl.java)
                        verify(saNtfInfoRepository, times(1)).upsert(any());
                        verify(jsapUtil, times(1)).executeGetUserId(eq("u"), eq("cid"));
                        verify(jsapUtil, never()).executeDvcLink(anyString(), anyString(), anyString(), anyString());
                }
        }

        /**
         * クラス：SaRegistNotificationDeviceInfoServiceImpl registDeviceInfo
         * dvcLinkage結果が非成功の場合にTscApplicationExceptionが送出されることを確認するテストケース
         */

        @Test
        void registDeviceInfo_005() throws Exception {
                // Arrange
                SaRegistNotificationDeviceInfoServiceImpl sut = new SaRegistNotificationDeviceInfoServiceImpl(
                                saNtfInfoRepository, jsapUtil);

                RegistNotificationDeviceInfoRequestDto request = req("u", "1", "tok", "dvc", "1");
                RequestHeaderDto header = header("cid");

                when(saNtfInfoRepository.upsert(any())).thenReturn(1);
                when(jsapUtil.executeGetUserId(eq("u"), eq("cid")))
                                .thenReturn(ResponseEntity.ok("{json}"));
                when(jsapUtil.executeDvcLink(eq("userId"), eq("tok"), eq("1"), anyString()))
                                .thenReturn(ResponseEntity.ok("{json2}"));

                GetUserIdResponseDto getUserIdDto = mock(GetUserIdResponseDto.class);
                // DvcLink の失敗を狙うので、GetUserId は成功させる
                // [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/SaRegistNotificationDeviceInfoServiceImpl.java)
                when(getUserIdDto.getResultCode()).thenReturn("00001548B123");
                when(getUserIdDto.getUserId()).thenReturn("userId");

                DvcLinkageResponseDto dvcDto = mock(DvcLinkageResponseDto.class);
                // 実装：成功は "000000"、それ以外は DvcLink エラーで TscApplicationException
                // [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/SaRegistNotificationDeviceInfoServiceImpl.java)
                when(dvcDto.getResultCode()).thenReturn("999999");

                try (MockedConstruction<ObjectMapper> om = mockConstruction(ObjectMapper.class, (mock, ctx) -> {
                        when(mock.readValue(anyString(), eq(GetUserIdResponseDto.class))).thenReturn(getUserIdDto);
                        when(mock.readValue(anyString(), eq(DvcLinkageResponseDto.class))).thenReturn(dvcDto);
                });
                                MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {

                        common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class)))
                                        .thenReturn("msg");
                        common.when(() -> CommonUtil.getResultCode(anyString()))
                                        .thenAnswer(inv -> "RC_" + inv.getArgument(0));

                        // Act & Assert
                        assertThrows(TscApplicationException.class, () -> sut.registDeviceInfo(request, header));

                        // Assert（DvcLink
                        // まで到達することを確認）[1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/SaRegistNotificationDeviceInfoServiceImpl.java)
                        verify(saNtfInfoRepository, times(1)).upsert(any());
                        verify(jsapUtil, times(1)).executeGetUserId(eq("u"), eq("cid"));
                        verify(jsapUtil, times(1)).executeDvcLink(eq("userId"), eq("tok"), eq("1"), anyString());
                }
        }

        /**
         * クラス：SaRegistNotificationDeviceInfoServiceImpl registDeviceInfo
         * JSON処理例外時にCustomExceptionが送出されることを確認するテストケース
         */
        @Test
        void registDeviceInfo_006() throws Exception {
                // Arrange
                SaRegistNotificationDeviceInfoServiceImpl sut = new SaRegistNotificationDeviceInfoServiceImpl(
                                saNtfInfoRepository, jsapUtil);
                RegistNotificationDeviceInfoRequestDto request = req("u", "1", "tok", "dvc", "1");
                RequestHeaderDto header = header("cid");

                when(saNtfInfoRepository.upsert(any())).thenReturn(1);
                when(jsapUtil.executeGetUserId(eq("u"), eq("cid"))).thenReturn(ResponseEntity.ok("{json}"));

                try (MockedConstruction<ObjectMapper> om = mockConstruction(ObjectMapper.class,
                                (mock, ctx) -> when(mock.readValue(anyString(), eq(GetUserIdResponseDto.class)))
                                                .thenThrow(new JsonProcessingException("boom") {
                                                }));
                                MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {

                        common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class)))
                                        .thenReturn("msg");

                        // Act
                        assertThrows(CustomException.class, () -> sut.registDeviceInfo(request, header));

                        // Assert
                        verify(jsapUtil, times(1)).executeGetUserId(eq("u"), eq("cid"));
                }
        }

        /**
         * クラス：SaRegistNotificationDeviceInfoServiceImpl registDeviceInfo
         * SQL接続系例外の場合にCustomExceptionが送出されることを確認するテストケース
         */
        @Test
        void registDeviceInfo_007() {
                // Arrange
                SaRegistNotificationDeviceInfoServiceImpl sut = new SaRegistNotificationDeviceInfoServiceImpl(
                                saNtfInfoRepository, jsapUtil);
                RegistNotificationDeviceInfoRequestDto request = req("u", "1", "tok", "dvc", "1");
                RequestHeaderDto header = header("cid");

                when(saNtfInfoRepository.upsert(any()))
                                .thenThrow(new RuntimeException(new SQLException("x", "08S01")));

                try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
                        common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class)))
                                        .thenReturn("msg");

                        // Act
                        assertThrows(CustomException.class, () -> sut.registDeviceInfo(request, header));

                        // Assert
                        verify(saNtfInfoRepository, times(1)).upsert(any());
                }
        }

        /**
         * クラス：SaRegistNotificationDeviceInfoServiceImpl registDeviceInfo
         * SQL操作系例外の場合にCustomSqlExceptionが送出されることを確認するテストケース
         */
        @Test
        void registDeviceInfo_008() {
                // Arrange
                SaRegistNotificationDeviceInfoServiceImpl sut = new SaRegistNotificationDeviceInfoServiceImpl(
                                saNtfInfoRepository, jsapUtil);
                RegistNotificationDeviceInfoRequestDto request = req("u", "1", "tok", "dvc", "1");
                RequestHeaderDto header = header("cid");

                when(saNtfInfoRepository.upsert(any()))
                                .thenThrow(new RuntimeException(new SQLException("x", "23505")));

                try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
                        common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class)))
                                        .thenReturn("msg");

                        // Act
                        assertThrows(CustomSqlException.class, () -> sut.registDeviceInfo(request, header));

                        // Assert
                        verify(saNtfInfoRepository, times(1)).upsert(any());
                }
        }

        /*
         * *
         * クラス：SaRegistNotificationDeviceInfoServiceImpl registDeviceInfo
         * リクエストがnullの場合にTscApplicationExceptionが送出されることを確認するテストケース
         */
        @Test
        void registDeviceInfo_009() {
                // Arrange
                SaRegistNotificationDeviceInfoServiceImpl sut = new SaRegistNotificationDeviceInfoServiceImpl(
                                saNtfInfoRepository, jsapUtil);
                RegistNotificationDeviceInfoRequestDto request = null;
                RequestHeaderDto header = header("cid");

                try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
                        common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class)))
                                        .thenReturn("msg");
                        common.when(() -> CommonUtil.getResultCode(anyString()))
                                        .thenAnswer(inv -> "RC_" + inv.getArgument(0));

                        // Act
                        assertThrows(TscApplicationException.class, () -> sut.registDeviceInfo(request, header));

                        // Assert
                        verify(saNtfInfoRepository, never()).upsert(any());
                }
        }

        /*
         * *
         * クラス：SaRegistNotificationDeviceInfoServiceImpl registDeviceInfo
         * 不正ブランドの場合にTscApplicationExceptionが送出されることを確認するテストケース
         */
        @Test
        void registDeviceInfo_010() {
                // Arrange
                SaRegistNotificationDeviceInfoServiceImpl sut = new SaRegistNotificationDeviceInfoServiceImpl(
                                saNtfInfoRepository, jsapUtil);
                RegistNotificationDeviceInfoRequestDto request = req("u", "1", "tok", "dvc", "9");
                RequestHeaderDto header = header("cid");

                try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
                        common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class)))
                                        .thenReturn("msg");
                        common.when(() -> CommonUtil.getResultCode(anyString()))
                                        .thenAnswer(inv -> "RC_" + inv.getArgument(0));

                        // Act
                        assertThrows(TscApplicationException.class, () -> sut.registDeviceInfo(request, header));

                        // Assert
                        verify(saNtfInfoRepository, never()).upsert(any());
                }
        }

        /*
         * *
         * クラス：SaRegistNotificationDeviceInfoServiceImpl registDeviceInfo
         * 不正プラットフォームの場合にTscApplicationExceptionが送出されることを確認するテストケース
         */
        @Test
        void registDeviceInfo_011() {
                // Arrange
                SaRegistNotificationDeviceInfoServiceImpl sut = new SaRegistNotificationDeviceInfoServiceImpl(
                                saNtfInfoRepository, jsapUtil);
                RegistNotificationDeviceInfoRequestDto request = req("u", "9", "tok", "dvc", "1");
                RequestHeaderDto header = header("cid");

                try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
                        common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class)))
                                        .thenReturn("msg");
                        common.when(() -> CommonUtil.getResultCode(anyString()))
                                        .thenAnswer(inv -> "RC_" + inv.getArgument(0));

                        // Act
                        assertThrows(TscApplicationException.class, () -> sut.registDeviceInfo(request, header));

                        // Assert
                        verify(saNtfInfoRepository, never()).upsert(any());
                }
        }

        /*
         * *
         * クラス：SaRegistNotificationDeviceInfoServiceImpl registDeviceInfo
         * ブランド=2/プラットフォーム=2の有効値で正常に成功コードが返ることを確認するテストケース
         */

        @Test
        void registDeviceInfo_012() throws Exception {
                // Arrange
                SaRegistNotificationDeviceInfoServiceImpl sut = new SaRegistNotificationDeviceInfoServiceImpl(
                                saNtfInfoRepository, jsapUtil);

                RegistNotificationDeviceInfoRequestDto request = req("u", "2", "tok", "dvc", "2");
                RequestHeaderDto header = header("cid");

                when(saNtfInfoRepository.upsert(any())).thenReturn(1);
                when(jsapUtil.executeGetUserId(eq("u"), eq("cid")))
                                .thenReturn(ResponseEntity.ok("{json}"));
                when(jsapUtil.executeDvcLink(eq("userId"), eq("tok"), eq("2"), anyString()))
                                .thenReturn(ResponseEntity.ok("{json2}"));

                GetUserIdResponseDto getUserIdDto = mock(GetUserIdResponseDto.class);
                // 実装：GetUserId 成功は "00001548B123"
                // [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/SaRegistNotificationDeviceInfoServiceImpl.java)
                when(getUserIdDto.getResultCode()).thenReturn("00001548B123");
                when(getUserIdDto.getUserId()).thenReturn("userId");

                DvcLinkageResponseDto dvcDto = mock(DvcLinkageResponseDto.class);
                // 実装：DvcLink 成功は "000000"
                // [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/SaRegistNotificationDeviceInfoServiceImpl.java)
                when(dvcDto.getResultCode()).thenReturn("000000");

                try (MockedConstruction<ObjectMapper> om = mockConstruction(ObjectMapper.class, (mock, ctx) -> {
                        when(mock.readValue(anyString(), eq(GetUserIdResponseDto.class))).thenReturn(getUserIdDto);
                        when(mock.readValue(anyString(), eq(DvcLinkageResponseDto.class))).thenReturn(dvcDto);
                });
                                MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {

                        common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class)))
                                        .thenReturn("msg");
                        common.when(() -> CommonUtil.getResultCode(anyString()))
                                        .thenAnswer(inv -> "RC_" + inv.getArgument(0));

                        // Act
                        ResponseDto resp = sut.registDeviceInfo(request, header);

                        // Assert
                        assertNotNull(resp);
                        verify(saNtfInfoRepository, times(1)).upsert(any());
                        verify(jsapUtil, times(1)).executeGetUserId(eq("u"), eq("cid"));
                        verify(jsapUtil, times(1)).executeDvcLink(eq("userId"), eq("tok"), eq("2"), anyString());
                }
        }

        /*
         * *
         * クラス：SaRegistNotificationDeviceInfoServiceImpl registDeviceInfo
         * SQLExceptionが存在するが接続/操作どちらでもない場合にCustomExceptionが送出されることを確認するテストケース
         */
        @Test
        void registDeviceInfo_013() {
                // Arrange
                SaRegistNotificationDeviceInfoServiceImpl sut = new SaRegistNotificationDeviceInfoServiceImpl(
                                saNtfInfoRepository, jsapUtil);
                RegistNotificationDeviceInfoRequestDto request = req("u", "1", "tok", "dvc", "1");
                RequestHeaderDto header = header("cid");

                when(saNtfInfoRepository.upsert(any()))
                                .thenThrow(new RuntimeException(new SQLException("x", "99999"))); // connection/operation
                                                                                                  // に該当しにくい

                try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
                        common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class)))
                                        .thenReturn("msg");
                        // ★ getResultCode はこの経路では呼ばれないため stub しない（UnnecessaryStubbing回避）

                        // Act
                        assertThrows(CustomException.class, () -> sut.registDeviceInfo(request, header));

                        // Assert
                        verify(saNtfInfoRepository, times(1)).upsert(any());
                }
        }

        /*
         * *
         * クラス：SaRegistNotificationDeviceInfoServiceImpl registDeviceInfo
         * SQLExceptionが存在しない場合にCustomExceptionが送出されることを確認するテストケース
         */
        @Test
        void registDeviceInfo_014() {
                // Arrange
                SaRegistNotificationDeviceInfoServiceImpl sut = new SaRegistNotificationDeviceInfoServiceImpl(
                                saNtfInfoRepository, jsapUtil);
                RegistNotificationDeviceInfoRequestDto request = req("u", "1", "tok", "dvc", "1");
                RequestHeaderDto header = header("cid");

                when(saNtfInfoRepository.upsert(any())).thenThrow(new RuntimeException("boom"));

                try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
                        common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class)))
                                        .thenReturn("msg");
                        // ★ getResultCode はこの経路では呼ばれないため stub しない（UnnecessaryStubbing回避）

                        // Act
                        assertThrows(CustomException.class, () -> sut.registDeviceInfo(request, header));

                        // Assert
                        verify(saNtfInfoRepository, times(1)).upsert(any());
                }
        }

        // ----- validateRequired の分岐網羅（Reflectionで直接テスト） -----

        /*
         * *
         * クラス：SaRegistNotificationDeviceInfoServiceImpl validateRequired
         * リクエストがnullの場合に"requestBody"が返ることを確認するテストケース
         */
        @Test
        void validateRequired_001() throws Exception {
                // Arrange
                SaRegistNotificationDeviceInfoServiceImpl sut = new SaRegistNotificationDeviceInfoServiceImpl(
                                saNtfInfoRepository, jsapUtil);
                java.lang.reflect.Method m = SaRegistNotificationDeviceInfoServiceImpl.class
                                .getDeclaredMethod("validateRequired", RegistNotificationDeviceInfoRequestDto.class);
                m.setAccessible(true);

                // Act
                String result = (String) m.invoke(sut, (RegistNotificationDeviceInfoRequestDto) null);

                // Assert
                assertEquals("requestBody", result);
        }

        /*
         * *
         * クラス：SaRegistNotificationDeviceInfoServiceImpl validateRequired
         * 必須項目がすべて有効な場合にnullが返ることを確認するテストケース
         */
        @Test
        void validateRequired_002() throws Exception {
                // Arrange
                SaRegistNotificationDeviceInfoServiceImpl sut = new SaRegistNotificationDeviceInfoServiceImpl(
                                saNtfInfoRepository, jsapUtil);
                java.lang.reflect.Method m = SaRegistNotificationDeviceInfoServiceImpl.class
                                .getDeclaredMethod("validateRequired", RegistNotificationDeviceInfoRequestDto.class);
                m.setAccessible(true);

                RegistNotificationDeviceInfoRequestDto request = req("u", "1", "tok", "dvc", "1");

                // Act
                String result = (String) m.invoke(sut, request);

                // Assert
                assertNull(result);
        }

        /*
         * *
         * クラス：SaRegistNotificationDeviceInfoServiceImpl validateRequired
         * internalUserIdがnullの場合に不足項目としてinternalUserIdが返ることを確認するテストケース
         */
        @Test
        void validateRequired_003() throws Exception {
                // Arrange
                SaRegistNotificationDeviceInfoServiceImpl sut = new SaRegistNotificationDeviceInfoServiceImpl(
                                saNtfInfoRepository, jsapUtil);
                java.lang.reflect.Method m = SaRegistNotificationDeviceInfoServiceImpl.class
                                .getDeclaredMethod("validateRequired", RegistNotificationDeviceInfoRequestDto.class);
                m.setAccessible(true);

                RegistNotificationDeviceInfoRequestDto request = req(null, "1", "tok", "dvc", "1");

                // Act
                String result = (String) m.invoke(sut, request);

                // Assert
                assertEquals("internalUserId", result);
        }

        /*
         * *
         * クラス：SaRegistNotificationDeviceInfoServiceImpl validateRequired
         * internalUserIdが空文字の場合に不足項目としてinternalUserIdが返ることを確認するテストケース
         */
        @Test
        void validateRequired_004() throws Exception {
                // Arrange
                SaRegistNotificationDeviceInfoServiceImpl sut = new SaRegistNotificationDeviceInfoServiceImpl(
                                saNtfInfoRepository, jsapUtil);
                java.lang.reflect.Method m = SaRegistNotificationDeviceInfoServiceImpl.class
                                .getDeclaredMethod("validateRequired", RegistNotificationDeviceInfoRequestDto.class);
                m.setAccessible(true);

                RegistNotificationDeviceInfoRequestDto request = req("", "1", "tok", "dvc", "1");

                // Act
                String result = (String) m.invoke(sut, request);

                // Assert
                assertEquals("internalUserId", result);
        }

        /*
         * *
         * クラス：SaRegistNotificationDeviceInfoServiceImpl validateRequired
         * platformがnullの場合に不足項目としてplatformが返ることを確認するテストケース
         */
        @Test
        void validateRequired_005() throws Exception {
                // Arrange
                SaRegistNotificationDeviceInfoServiceImpl sut = new SaRegistNotificationDeviceInfoServiceImpl(
                                saNtfInfoRepository, jsapUtil);
                java.lang.reflect.Method m = SaRegistNotificationDeviceInfoServiceImpl.class
                                .getDeclaredMethod("validateRequired", RegistNotificationDeviceInfoRequestDto.class);
                m.setAccessible(true);

                RegistNotificationDeviceInfoRequestDto request = req("u", null, "tok", "dvc", "1");

                // Act
                String result = (String) m.invoke(sut, request);

                // Assert
                assertEquals("platform", result);
        }

        /*
         * *
         * クラス：SaRegistNotificationDeviceInfoServiceImpl validateRequired
         * platformが空文字の場合に不足項目としてplatformが返ることを確認するテストケース
         */
        @Test
        void validateRequired_006() throws Exception {
                // Arrange
                SaRegistNotificationDeviceInfoServiceImpl sut = new SaRegistNotificationDeviceInfoServiceImpl(
                                saNtfInfoRepository, jsapUtil);
                java.lang.reflect.Method m = SaRegistNotificationDeviceInfoServiceImpl.class
                                .getDeclaredMethod("validateRequired", RegistNotificationDeviceInfoRequestDto.class);
                m.setAccessible(true);

                RegistNotificationDeviceInfoRequestDto request = req("u", "", "tok", "dvc", "1");

                // Act
                String result = (String) m.invoke(sut, request);

                // Assert
                assertEquals("platform", result);
        }

        /*
         * *
         * クラス：SaRegistNotificationDeviceInfoServiceImpl validateRequired
         * deviceTokenがnullの場合に不足項目としてdeviceTokenが返ることを確認するテストケース
         */
        @Test
        void validateRequired_007() throws Exception {
                // Arrange
                SaRegistNotificationDeviceInfoServiceImpl sut = new SaRegistNotificationDeviceInfoServiceImpl(
                                saNtfInfoRepository, jsapUtil);
                java.lang.reflect.Method m = SaRegistNotificationDeviceInfoServiceImpl.class
                                .getDeclaredMethod("validateRequired", RegistNotificationDeviceInfoRequestDto.class);
                m.setAccessible(true);

                RegistNotificationDeviceInfoRequestDto request = req("u", "1", null, "dvc", "1");

                // Act
                String result = (String) m.invoke(sut, request);

                // Assert
                assertEquals("deviceToken", result);
        }

        /*
         * *
         * クラス：SaRegistNotificationDeviceInfoServiceImpl validateRequired
         * deviceTokenが空文字の場合に不足項目としてdeviceTokenが返ることを確認するテストケース
         */
        @Test
        void validateRequired_008() throws Exception {
                // Arrange
                SaRegistNotificationDeviceInfoServiceImpl sut = new SaRegistNotificationDeviceInfoServiceImpl(
                                saNtfInfoRepository, jsapUtil);
                java.lang.reflect.Method m = SaRegistNotificationDeviceInfoServiceImpl.class
                                .getDeclaredMethod("validateRequired", RegistNotificationDeviceInfoRequestDto.class);
                m.setAccessible(true);

                RegistNotificationDeviceInfoRequestDto request = req("u", "1", "", "dvc", "1");

                // Act
                String result = (String) m.invoke(sut, request);

                // Assert
                assertEquals("deviceToken", result);
        }

        /*
         * *
         * クラス：SaRegistNotificationDeviceInfoServiceImpl validateRequired
         * dvcIdがnullの場合に不足項目としてdvcIdが返ることを確認するテストケース
         */
        @Test
        void validateRequired_009() throws Exception {
                // Arrange
                SaRegistNotificationDeviceInfoServiceImpl sut = new SaRegistNotificationDeviceInfoServiceImpl(
                                saNtfInfoRepository, jsapUtil);
                java.lang.reflect.Method m = SaRegistNotificationDeviceInfoServiceImpl.class
                                .getDeclaredMethod("validateRequired", RegistNotificationDeviceInfoRequestDto.class);
                m.setAccessible(true);

                RegistNotificationDeviceInfoRequestDto request = req("u", "1", "tok", null, "1");

                // Act
                String result = (String) m.invoke(sut, request);

                // Assert
                assertEquals("dvcId", result);
        }

        /*
         * *
         * クラス：SaRegistNotificationDeviceInfoServiceImpl validateRequired
         * dvcIdが空文字の場合に不足項目としてdvcIdが返ることを確認するテストケース
         */
        @Test
        void validateRequired_010() throws Exception {
                // Arrange
                SaRegistNotificationDeviceInfoServiceImpl sut = new SaRegistNotificationDeviceInfoServiceImpl(
                                saNtfInfoRepository, jsapUtil);
                java.lang.reflect.Method m = SaRegistNotificationDeviceInfoServiceImpl.class
                                .getDeclaredMethod("validateRequired", RegistNotificationDeviceInfoRequestDto.class);
                m.setAccessible(true);

                RegistNotificationDeviceInfoRequestDto request = req("u", "1", "tok", "", "1");

                // Act
                String result = (String) m.invoke(sut, request);

                // Assert
                assertEquals("dvcId", result);
        }

        /*
         * *
         * クラス：SaRegistNotificationDeviceInfoServiceImpl validateRequired
         * brdCdがnullの場合に不足項目としてbrdCdが返ることを確認するテストケース
         */
        @Test
        void validateRequired_011() throws Exception {
                // Arrange
                SaRegistNotificationDeviceInfoServiceImpl sut = new SaRegistNotificationDeviceInfoServiceImpl(
                                saNtfInfoRepository, jsapUtil);
                java.lang.reflect.Method m = SaRegistNotificationDeviceInfoServiceImpl.class
                                .getDeclaredMethod("validateRequired", RegistNotificationDeviceInfoRequestDto.class);
                m.setAccessible(true);

                RegistNotificationDeviceInfoRequestDto request = req("u", "1", "tok", "dvc", null);

                // Act
                String result = (String) m.invoke(sut, request);

                // Assert
                assertEquals("brdCd", result);
        }

        /*
         * *
         * クラス：SaRegistNotificationDeviceInfoServiceImpl validateRequired
         * brdCdが空文字の場合に不足項目としてbrdCdが返ることを確認するテストケース
         */
        @Test
        void validateRequired_012() throws Exception {
                // Arrange
                SaRegistNotificationDeviceInfoServiceImpl sut = new SaRegistNotificationDeviceInfoServiceImpl(
                                saNtfInfoRepository, jsapUtil);
                java.lang.reflect.Method m = SaRegistNotificationDeviceInfoServiceImpl.class
                                .getDeclaredMethod("validateRequired", RegistNotificationDeviceInfoRequestDto.class);
                m.setAccessible(true);

                RegistNotificationDeviceInfoRequestDto request = req("u", "1", "tok", "dvc", "");

                // Act
                String result = (String) m.invoke(sut, request);

                // Assert
                assertEquals("brdCd", result);
        }

        /*
         * *
         * クラス：SaRegistNotificationDeviceInfoServiceImpl validateRequired
         * 必須項目が複数不足している場合にカンマ区切りで返ることを確認するテストケース
         */
        @Test
        void validateRequired_013() throws Exception {
                // Arrange
                SaRegistNotificationDeviceInfoServiceImpl sut = new SaRegistNotificationDeviceInfoServiceImpl(
                                saNtfInfoRepository, jsapUtil);
                java.lang.reflect.Method m = SaRegistNotificationDeviceInfoServiceImpl.class
                                .getDeclaredMethod("validateRequired", RegistNotificationDeviceInfoRequestDto.class);
                m.setAccessible(true);

                // internalUserId と platform を不足させる（順序は実装のadd順）
                RegistNotificationDeviceInfoRequestDto request = req(null, "", "tok", "dvc", "1");

                // Act
                String result = (String) m.invoke(sut, request);

                // Assert
                assertEquals("internalUserId,platform", result);
        }

}
