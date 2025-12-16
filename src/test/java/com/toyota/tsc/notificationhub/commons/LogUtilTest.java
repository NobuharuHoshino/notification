
// ファイルパス: src/test/java/com/toyota/tsc/notificationhub/commons/LogUtilTest.java
package com.toyota.tsc.notificationhub.commons;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LogUtil のテストクラス
 */
class LogUtilTest {

    static class DummyClass {
    }

    /** クラス：LogUtil コンストラクタがprivateであることを確認するテストケース */
    @Test
    void LogUtil_001() throws Exception {
        // Arrange
        Constructor<LogUtil> ctor = LogUtil.class.getDeclaredConstructor();

        // Act
        int mod = ctor.getModifiers();

        // Assert
        assertTrue(Modifier.isPrivate(mod));
    }

    /** クラス：LogUtil getLogger 指定クラスのLoggerが取得できることを確認するテストケース */
    @Test
    void getLogger_001() {
        // Arrange
        Class<?> clazz = DummyClass.class;

        // Act
        org.slf4j.Logger logger = LogUtil.getLogger(clazz);

        // Assert
        assertNotNull(logger);
        assertTrue(logger.getName().contains(clazz.getName()));
    }

    /** クラス：LogUtil info infoレベルのログが出力されることを確認するテストケース */
    @Test
    void info_001() {
        // Arrange
        Logger logger = (Logger) LoggerFactory.getLogger(DummyClass.class);
        ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        // Act
        LogUtil.info(DummyClass.class, "INFO_MSG");

        // Assert
        assertEquals(1, appender.list.size());
        assertEquals(Level.INFO, appender.list.get(0).getLevel());
        assertEquals("INFO_MSG", appender.list.get(0).getFormattedMessage());

        logger.detachAppender(appender);
    }

    /** クラス：LogUtil warn warnレベルのログが出力されることを確認するテストケース */
    @Test
    void warn_001() {
        // Arrange
        Logger logger = (Logger) LoggerFactory.getLogger(DummyClass.class);
        ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        // Act
        LogUtil.warn(DummyClass.class, "WARN_MSG");

        // Assert
        assertEquals(1, appender.list.size());
        assertEquals(Level.WARN, appender.list.get(0).getLevel());
        assertEquals("WARN_MSG", appender.list.get(0).getFormattedMessage());

        logger.detachAppender(appender);
    }

    /** クラス：LogUtil error errorレベルのログが出力されることを確認するテストケース */
    @Test
    void error_001() {
        // Arrange
        Logger logger = (Logger) LoggerFactory.getLogger(DummyClass.class);
        ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        // Act
        LogUtil.error(DummyClass.class, "ERROR_MSG");

        // Assert
        assertEquals(1, appender.list.size());
        assertEquals(Level.ERROR, appender.list.get(0).getLevel());
        assertEquals("ERROR_MSG", appender.list.get(0).getFormattedMessage());

        logger.detachAppender(appender);
    }

    /** クラス：LogUtil debug debugレベルのログが出力されることを確認するテストケース */
    @Test
    void debug_001() {
        // Arrange
        Logger logger = (Logger) LoggerFactory.getLogger(DummyClass.class);
        logger.setLevel(Level.DEBUG);
        ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        // Act
        LogUtil.debug(DummyClass.class, "DEBUG_MSG");

        // Assert
        assertEquals(1, appender.list.size());
        assertEquals(Level.DEBUG, appender.list.get(0).getLevel());
        assertEquals("DEBUG_MSG", appender.list.get(0).getFormattedMessage());

        logger.detachAppender(appender);
    }
}
