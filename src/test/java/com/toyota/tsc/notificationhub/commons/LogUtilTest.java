package com.toyota.tsc.notificationhub.commons;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * テストクラス：LogUtilTest
 */
class LogUtilTest {

    /** クラス：LogUtil info（SLF4Jへ委譲）を確認するテストケース */
    @Test
    void info_01() {
        // 準備
        Logger logger = mock(Logger.class);
        try (MockedStatic<LoggerFactory> mocked = mockStatic(LoggerFactory.class)) {
            mocked.when(() -> LoggerFactory.getLogger(eq(LogUtilTest.class))).thenReturn(logger);

            // 実行
            assertDoesNotThrow(() -> LogUtil.info(LogUtilTest.class, "hello-info"));

            // 確認
            verify(logger, times(1)).info(eq("hello-info"));
        }
    }

    /** クラス：LogUtil warn（SLF4Jへ委譲）を確認するテストケース */
    @Test
    void warn_01() {
        // 準備
        Logger logger = mock(Logger.class);
        try (MockedStatic<LoggerFactory> mocked = mockStatic(LoggerFactory.class)) {
            mocked.when(() -> LoggerFactory.getLogger(eq(LogUtilTest.class))).thenReturn(logger);

            // 実行
            LogUtil.warn(LogUtilTest.class, "hello-warn");

            // 確認
            verify(logger, times(1)).warn(eq("hello-warn"));
        }
    }

    /** クラス：LogUtil error（SLF4Jへ委譲）を確認するテストケース */
    @Test
    void error_01() {
        // 準備
        Logger logger = mock(Logger.class);
        try (MockedStatic<LoggerFactory> mocked = mockStatic(LoggerFactory.class)) {
            mocked.when(() -> LoggerFactory.getLogger(eq(LogUtilTest.class))).thenReturn(logger);

            // 実行
            LogUtil.error(LogUtilTest.class, "hello-error");

            // 確認
            verify(logger, times(1)).error(eq("hello-error"));
        }
    }

    /** クラス：LogUtil debug（SLF4Jへ委譲）を確認するテストケース */
    @Test
    void debug_01() {
        // 準備
        Logger logger = mock(Logger.class);
        try (MockedStatic<LoggerFactory> mocked = mockStatic(LoggerFactory.class)) {
            mocked.when(() -> LoggerFactory.getLogger(eq(LogUtilTest.class))).thenReturn(logger);

            // 実行
            LogUtil.debug(LogUtilTest.class, "hello-debug");

            // 確認
            verify(logger, times(1)).debug(eq("hello-debug"));
        }
    }
}
