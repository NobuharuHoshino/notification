package com.toyota.tsc.notificationhub.commons;

import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * クラス：LogUtil すべての分岐を確認するテストケース
 */
class LogUtilTest {

    private ByteArrayOutputStream out;

    @BeforeEach
    void setup() {
        out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
    }

    @AfterEach
    void teardown() {
        System.setOut(System.out);
    }

    /** クラス：LogUtil info ログが標準出力へ出ることを確認するテストケース */
    @Test
    void info_01() {
        try (MockedStatic<LoggerFactory> lf = Mockito.mockStatic(LoggerFactory.class)) {
            Logger mockLogger = mock(Logger.class);
            doAnswer(inv -> {
                System.out.println("INFO:" + inv.getArgument(0));
                return null;
            })
                    .when(mockLogger).info(any(String.class));
            lf.when(() -> LoggerFactory.getLogger(any(Class.class))).thenReturn(mockLogger);

            // 実行
            LogUtil.info(LogUtilTest.class, "hello");

            // 確認
            String s = out.toString();
            assertTrue(s.contains("INFO:hello"));
        }
    }

    /** クラス：LogUtil warn ログが標準出力へ出ることを確認するテストケース */
    @Test
    void warn_01() {
        try (MockedStatic<LoggerFactory> lf = Mockito.mockStatic(LoggerFactory.class)) {
            Logger mockLogger = mock(Logger.class);
            doAnswer(inv -> {
                System.out.println("WARN:" + inv.getArgument(0));
                return null;
            })
                    .when(mockLogger).warn(any(String.class));
            lf.when(() -> LoggerFactory.getLogger(any(Class.class))).thenReturn(mockLogger);

            LogUtil.warn(LogUtilTest.class, "warn");
            assertTrue(out.toString().contains("WARN:warn"));
        }
    }

    /** クラス：LogUtil error ログが標準出力へ出ることを確認するテストケース */
    @Test
    void error_01() {
        try (MockedStatic<LoggerFactory> lf = Mockito.mockStatic(LoggerFactory.class)) {
            Logger mockLogger = mock(Logger.class);
            doAnswer(inv -> {
                System.out.println("ERROR:" + inv.getArgument(0));
                return null;
            })
                    .when(mockLogger).error(any(String.class));
            lf.when(() -> LoggerFactory.getLogger(any(Class.class))).thenReturn(mockLogger);

            LogUtil.error(LogUtilTest.class, "error!");
            assertTrue(out.toString().contains("ERROR:error!"));
        }
    }

    /** クラス：LogUtil debug ログが標準出力へ出ることを確認するテストケース */
    @Test
    void debug_01() {
        try (MockedStatic<LoggerFactory> lf = Mockito.mockStatic(LoggerFactory.class)) {
            Logger mockLogger = mock(Logger.class);
            doAnswer(inv -> {
                System.out.println("DEBUG:" + inv.getArgument(0));
                return null;
            })
                    .when(mockLogger).debug(any(String.class));
            lf.when(() -> LoggerFactory.getLogger(any(Class.class))).thenReturn(mockLogger);

            LogUtil.debug(LogUtilTest.class, "dbg");
            assertTrue(out.toString().contains("DEBUG:dbg"));
        }
    }

    /** クラス：LogUtil getLogger LoggerFactoryが呼ばれることを確認するテストケース */
    @Test
    void getLogger_01() {
        try (MockedStatic<LoggerFactory> lf = Mockito.mockStatic(LoggerFactory.class)) {
            Logger mockLogger = mock(Logger.class);
            lf.when(() -> LoggerFactory.getLogger(any(Class.class))).thenReturn(mockLogger);

            Logger logger = LogUtil.getLogger(LogUtilTest.class);
            assertNotNull(logger);
            lf.verify(() -> LoggerFactory.getLogger(LogUtilTest.class), times(1));
        }
    }

    /** クラス：LogUtil private コンストラクタがインスタンス化できることを確認するテストケース */
    @Test
    void constructor_01() throws Exception {
        Constructor<LogUtil> c = LogUtil.class.getDeclaredConstructor();
        c.setAccessible(true);
        LogUtil inst = c.newInstance();
        assertNotNull(inst);
    }
}