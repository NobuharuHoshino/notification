
// ファイルパス: src/test/java/com/toyota/tsc/notificationhub/commons/ExtractSqlExceptionUtilTest.java
package com.toyota.tsc.notificationhub.commons;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedConstruction;

import com.windowsazure.messaging.AppleInstallation;
import com.windowsazure.messaging.AppleNotification;
import com.windowsazure.messaging.FcmV1Installation;
import com.windowsazure.messaging.FcmV1Notification;
import com.windowsazure.messaging.Notification;
import com.windowsazure.messaging.NotificationHub;
import com.windowsazure.messaging.NotificationOutcome;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLNonTransientException;
import java.sql.SQLTransientConnectionException;
import java.sql.SQLTransientException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ExtractSqlExceptionUtil のテストクラス
 */
@SuppressWarnings("all")
class ExtractSqlExceptionUtilTest {

    @Mock
    private PropertiesUtil propertiesUtil;

    /** クラス：ExtractSqlExceptionUtil コンストラクタがprivateであることを確認するテストケース */
    @Test
    void ExtractSqlExceptionUtil_001() throws Exception {
        // Arrange
        Constructor<ExtractSqlExceptionUtil> ctor = ExtractSqlExceptionUtil.class.getDeclaredConstructor();

        // Act
        int mod = ctor.getModifiers();

        // Assert
        assertTrue(Modifier.isPrivate(mod));
    }

    /**
     * クラス：ExtractSqlExceptionUtil findSqlException 引数がnullの場合にnullが返ることを確認するテストケース
     */
    @Test
    void findSqlException_001() {
        // Arrange
        Throwable t = null;

        // Act
        SQLException result = ExtractSqlExceptionUtil.findSqlException(t);

        // Assert
        assertNull(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil findSqlException
     * 引数がSQLExceptionの場合に同一インスタンスが返ることを確認するテストケース
     */
    @Test
    void findSqlException_002() {
        // Arrange
        SQLException sql = new SQLException("x");

        // Act
        SQLException result = ExtractSqlExceptionUtil.findSqlException(sql);

        // Assert
        assertSame(sql, result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil findSqlException
     * causeチェーンにSQLExceptionがある場合に抽出できることを確認するテストケース
     */
    @Test
    void findSqlException_003() {
        // Arrange
        SQLException sql = new SQLException("sql");
        RuntimeException wrap = new RuntimeException(new IllegalStateException(sql));

        // Act
        SQLException result = ExtractSqlExceptionUtil.findSqlException(wrap);

        // Assert
        assertSame(sql, result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil findSqlException
     * causeチェーンにSQLExceptionがない場合にnullが返ることを確認するテストケース
     */
    @Test
    void findSqlException_004() {
        // Arrange
        RuntimeException wrap = new RuntimeException(new IllegalStateException(new RuntimeException("x")));

        // Act
        SQLException result = ExtractSqlExceptionUtil.findSqlException(wrap);

        // Assert
        assertNull(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil isSqlConnectionError
     * 引数がnullの場合にfalseが返ることを確認するテストケース
     */
    @Test
    void isSqlConnectionError_001() {
        // Arrange
        SQLException e = null;

        // Act
        boolean result = ExtractSqlExceptionUtil.isSqlConnectionError(e);

        // Assert
        assertFalse(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil isSqlConnectionError
     * SQLTransientConnectionExceptionの場合にtrueが返ることを確認するテストケース
     */
    @Test
    void isSqlConnectionError_002() {
        // Arrange
        SQLException e = new SQLTransientConnectionException("x");

        // Act
        boolean result = ExtractSqlExceptionUtil.isSqlConnectionError(e);

        // Assert
        assertTrue(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil isSqlConnectionError
     * SQLNonTransientConnectionExceptionの場合にtrueが返ることを確認するテストケース
     */
    @Test
    void isSqlConnectionError_003() {
        // Arrange
        SQLException e = new SQLNonTransientConnectionException("x");

        // Act
        boolean result = ExtractSqlExceptionUtil.isSqlConnectionError(e);

        // Assert
        assertTrue(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil isSqlConnectionError
     * SQLStateが08S01の場合にtrueが返ることを確認するテストケース
     */
    @Test
    void isSqlConnectionError_004() {
        // Arrange
        SQLException e = new SQLException("x", "08S01");

        // Act
        boolean result = ExtractSqlExceptionUtil.isSqlConnectionError(e);

        // Assert
        assertTrue(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil isSqlConnectionError
     * SQLStateが08004で始まる場合にtrueが返ることを確認するテストケース
     */
    @Test
    void isSqlConnectionError_005() {
        // Arrange
        SQLException e = new SQLException("x", "08004");

        // Act
        boolean result = ExtractSqlExceptionUtil.isSqlConnectionError(e);

        // Assert
        assertTrue(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil isSqlConnectionError
     * SQLStateクラスが08の場合にtrueが返ることを確認するテストケース
     */
    @Test
    void isSqlConnectionError_006() {
        // Arrange
        SQLException e = new SQLException("x", "08006");

        // Act
        boolean result = ExtractSqlExceptionUtil.isSqlConnectionError(e);

        // Assert
        assertTrue(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil isSqlConnectionError
     * SQLStateが28で始まる場合にtrueが返ることを確認するテストケース
     */
    @Test
    void isSqlConnectionError_007() {
        // Arrange
        SQLException e = new SQLException("x", "28P01");

        // Act
        boolean result = ExtractSqlExceptionUtil.isSqlConnectionError(e);

        // Assert
        assertTrue(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil isSqlConnectionError
     * SQLStateが3D000の場合にtrueが返ることを確認するテストケース
     */
    @Test
    void isSqlConnectionError_008() {
        // Arrange
        SQLException e = new SQLException("x", "3D000");

        // Act
        boolean result = ExtractSqlExceptionUtil.isSqlConnectionError(e);

        // Assert
        assertTrue(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil isSqlConnectionError
     * SQLStateが53300の場合にtrueが返ることを確認するテストケース
     */
    @Test
    void isSqlConnectionError_009() {
        // Arrange
        SQLException e = new SQLException("x", "53300");

        // Act
        boolean result = ExtractSqlExceptionUtil.isSqlConnectionError(e);

        // Assert
        assertTrue(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil isSqlConnectionError
     * SQLStateが57P03の場合にtrueが返ることを確認するテストケース
     */
    @Test
    void isSqlConnectionError_010() {
        // Arrange
        SQLException e = new SQLException("x", "57p03");

        // Act
        boolean result = ExtractSqlExceptionUtil.isSqlConnectionError(e);

        // Assert
        assertTrue(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil isSqlConnectionError
     * nextExceptionが接続系エラーの場合にtrueが返ることを確認するテストケース
     */
    @Test
    void isSqlConnectionError_011() {
        // Arrange
        SQLException root = new SQLException("x", "00000");
        SQLException next = new SQLException("y", "08S01");
        root.setNextException(next);

        // Act
        boolean result = ExtractSqlExceptionUtil.isSqlConnectionError(root);

        // Assert
        assertTrue(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil isSqlConnectionError
     * causeが接続系SQLExceptionの場合にtrueが返ることを確認するテストケース
     */
    @Test
    void isSqlConnectionError_012() {
        // Arrange
        SQLException cause = new SQLException("y", "08S01");
        SQLException root = new SQLException("x", "00000");
        root.initCause(cause);

        // Act
        boolean result = ExtractSqlExceptionUtil.isSqlConnectionError(root);

        // Assert
        assertTrue(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil isSqlConnectionError
     * 型/SQLState/チェーンいずれにも該当しない場合にfalseが返ることを確認するテストケース
     */
    @Test
    void isSqlConnectionError_013() {
        // Arrange
        SQLException e = new SQLException("x", "22001");

        // Act
        boolean result = ExtractSqlExceptionUtil.isSqlConnectionError(e);

        // Assert
        assertFalse(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil isSqlOperationError
     * 引数がnullの場合にfalseが返ることを確認するテストケース
     */
    @Test
    void isSqlOperationError_001() {
        // Arrange
        SQLException e = null;

        // Act
        boolean result = ExtractSqlExceptionUtil.isSqlOperationError(e);

        // Assert
        assertFalse(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil isSqlOperationError
     * SQLTransientExceptionの場合にtrueが返ることを確認するテストケース
     */
    @Test
    void isSqlOperationError_002() {
        // Arrange
        SQLException e = new SQLTransientException("x");

        // Act
        boolean result = ExtractSqlExceptionUtil.isSqlOperationError(e);

        // Assert
        assertTrue(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil isSqlOperationError
     * SQLNonTransientExceptionの場合にtrueが返ることを確認するテストケース
     */
    @Test
    void isSqlOperationError_003() {
        // Arrange
        SQLException e = new SQLNonTransientException("x");

        // Act
        boolean result = ExtractSqlExceptionUtil.isSqlOperationError(e);

        // Assert
        assertTrue(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil isSqlOperationError
     * SQLStateが40001の場合にtrueが返ることを確認するテストケース
     */
    @Test
    void isSqlOperationError_004() {
        // Arrange
        SQLException e = new SQLException("x", "40001");

        // Act
        boolean result = ExtractSqlExceptionUtil.isSqlOperationError(e);

        // Assert
        assertTrue(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil isSqlOperationError
     * SQLStateが23505の場合にtrueが返ることを確認するテストケース
     */
    @Test
    void isSqlOperationError_005() {
        // Arrange
        SQLException e = new SQLException("x", "23505");

        // Act
        boolean result = ExtractSqlExceptionUtil.isSqlOperationError(e);

        // Assert
        assertTrue(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil isSqlOperationError
     * SQLStateが0A000の場合にtrueが返ることを確認するテストケース
     */
    @Test
    void isSqlOperationError_006() {
        // Arrange
        SQLException e = new SQLException("x", "0A000");

        // Act
        boolean result = ExtractSqlExceptionUtil.isSqlOperationError(e);

        // Assert
        assertTrue(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil isSqlOperationError
     * nextExceptionが操作系エラーの場合にtrueが返ることを確認するテストケース
     */
    @Test
    void isSqlOperationError_007() {
        // Arrange
        SQLException root = new SQLException("x", "00000");
        SQLException next = new SQLException("y", "40001");
        root.setNextException(next);

        // Act
        boolean result = ExtractSqlExceptionUtil.isSqlOperationError(root);

        // Assert
        assertTrue(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil isSqlOperationError
     * causeが操作系SQLExceptionの場合にtrueが返ることを確認するテストケース
     */
    @Test
    void isSqlOperationError_008() {
        // Arrange
        SQLException cause = new SQLException("y", "23505");
        SQLException root = new SQLException("x", "00000");
        root.initCause(cause);

        // Act
        boolean result = ExtractSqlExceptionUtil.isSqlOperationError(root);

        // Assert
        assertTrue(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil isSqlOperationError
     * 型/SQLState/チェーンいずれにも該当しない場合にfalseが返ることを確認するテストケース
     */
    @Test
    void isSqlOperationError_009() {
        // Arrange
        SQLException e = new SQLException("x", "08S01");

        // Act
        boolean result = ExtractSqlExceptionUtil.isSqlOperationError(e);

        // Assert
        assertFalse(result);
    }

    // ---- private static メソッドの直接テスト（Reflection）----

    /**
     * クラス：ExtractSqlExceptionUtil isConnectionType
     * SQLTransientConnectionExceptionでtrueとなることを確認するテストケース
     */
    @Test
    void isConnectionType_001() throws Exception {
        // Arrange
        Method m = ExtractSqlExceptionUtil.class.getDeclaredMethod("isConnectionType", SQLException.class);
        m.setAccessible(true);
        SQLException e = new SQLTransientConnectionException("x");

        // Act
        boolean result = (boolean) m.invoke(null, e);

        // Assert
        assertTrue(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil isConnectionType
     * 一般SQLExceptionでfalseとなることを確認するテストケース
     */
    @Test
    void isConnectionType_002() throws Exception {
        // Arrange
        Method m = ExtractSqlExceptionUtil.class.getDeclaredMethod("isConnectionType", SQLException.class);
        m.setAccessible(true);
        SQLException e = new SQLException("x");

        // Act
        boolean result = (boolean) m.invoke(null, e);

        // Assert
        assertFalse(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil hasSqlStateConnectionIssue
     * nullでfalseとなることを確認するテストケース
     */
    @Test
    void hasSqlStateConnectionIssue_001() throws Exception {
        // Arrange
        Method m = ExtractSqlExceptionUtil.class.getDeclaredMethod("hasSqlStateConnectionIssue", String.class);
        m.setAccessible(true);

        // Act
        boolean result = (boolean) m.invoke(null, (String) null);

        // Assert
        assertFalse(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil hasSqlStateConnectionIssue
     * 空文字でfalseとなることを確認するテストケース
     */
    @Test
    void hasSqlStateConnectionIssue_002() throws Exception {
        // Arrange
        Method m = ExtractSqlExceptionUtil.class.getDeclaredMethod("hasSqlStateConnectionIssue", String.class);
        m.setAccessible(true);

        // Act
        boolean result = (boolean) m.invoke(null, "");

        // Assert
        assertFalse(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil hasSqlStateConnectionIssue
     * クラス08判定でtrueとなることを確認するテストケース
     */
    @Test
    void hasSqlStateConnectionIssue_003() throws Exception {
        // Arrange
        Method m = ExtractSqlExceptionUtil.class.getDeclaredMethod("hasSqlStateConnectionIssue", String.class);
        m.setAccessible(true);

        // Act
        boolean result = (boolean) m.invoke(null, "08");

        // Assert
        assertTrue(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil hasSqlStateConnectionIssue
     * 操作系SQLStateではfalseとなることを確認するテストケース
     */
    @Test
    void hasSqlStateConnectionIssue_004() throws Exception {
        // Arrange
        Method m = ExtractSqlExceptionUtil.class.getDeclaredMethod("hasSqlStateConnectionIssue", String.class);
        m.setAccessible(true);

        // Act
        boolean result = (boolean) m.invoke(null, "55P03");

        // Assert
        assertFalse(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil hasChainedConnectionIssue
     * nextExceptionでtrueとなることを確認するテストケース
     */
    @Test
    void hasChainedConnectionIssue_001() throws Exception {
        // Arrange
        Method m = ExtractSqlExceptionUtil.class.getDeclaredMethod("hasChainedConnectionIssue", SQLException.class);
        m.setAccessible(true);
        SQLException root = new SQLException("x", "00000");
        SQLException next = new SQLException("y", "08S01");
        root.setNextException(next);

        // Act
        boolean result = (boolean) m.invoke(null, root);

        // Assert
        assertTrue(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil hasChainedConnectionIssue
     * causeでtrueとなることを確認するテストケース
     */
    @Test
    void hasChainedConnectionIssue_002() throws Exception {
        // Arrange
        Method m = ExtractSqlExceptionUtil.class.getDeclaredMethod("hasChainedConnectionIssue", SQLException.class);
        m.setAccessible(true);
        SQLException root = new SQLException("x", "00000");
        SQLException cause = new SQLException("y", "08S01");
        root.initCause(cause);

        // Act
        boolean result = (boolean) m.invoke(null, root);

        // Assert
        assertTrue(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil hasChainedConnectionIssue
     * チェーンがない場合にfalseとなることを確認するテストケース
     */
    @Test
    void hasChainedConnectionIssue_003() throws Exception {
        // Arrange
        Method m = ExtractSqlExceptionUtil.class.getDeclaredMethod("hasChainedConnectionIssue", SQLException.class);
        m.setAccessible(true);
        SQLException root = new SQLException("x", "00000");

        // Act
        boolean result = (boolean) m.invoke(null, root);

        // Assert
        assertFalse(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil isOperationType
     * SQLTransientExceptionでtrueとなることを確認するテストケース
     */
    @Test
    void isOperationType_001() throws Exception {
        // Arrange
        Method m = ExtractSqlExceptionUtil.class.getDeclaredMethod("isOperationType", SQLException.class);
        m.setAccessible(true);
        SQLException e = new SQLTransientException("x");

        // Act
        boolean result = (boolean) m.invoke(null, e);

        // Assert
        assertTrue(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil isOperationType
     * 一般SQLExceptionでfalseとなることを確認するテストケース
     */
    @Test
    void isOperationType_002() throws Exception {
        // Arrange
        Method m = ExtractSqlExceptionUtil.class.getDeclaredMethod("isOperationType", SQLException.class);
        m.setAccessible(true);
        SQLException e = new SQLException("x");

        // Act
        boolean result = (boolean) m.invoke(null, e);

        // Assert
        assertFalse(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil hasSqlStateOperationIssue
     * nullでfalseとなることを確認するテストケース
     */
    @Test
    void hasSqlStateOperationIssue_001() throws Exception {
        // Arrange
        Method m = ExtractSqlExceptionUtil.class.getDeclaredMethod("hasSqlStateOperationIssue", String.class);
        m.setAccessible(true);

        // Act
        boolean result = (boolean) m.invoke(null, (String) null);

        // Assert
        assertFalse(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil hasSqlStateOperationIssue
     * 40001でtrueとなることを確認するテストケース
     */
    @Test
    void hasSqlStateOperationIssue_002() throws Exception {
        // Arrange
        Method m = ExtractSqlExceptionUtil.class.getDeclaredMethod("hasSqlStateOperationIssue", String.class);
        m.setAccessible(true);

        // Act
        boolean result = (boolean) m.invoke(null, "40001");

        // Assert
        assertTrue(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil hasSqlStateOperationIssue
     * 接続系SQLStateではfalseとなることを確認するテストケース
     */
    @Test
    void hasSqlStateOperationIssue_003() throws Exception {
        // Arrange
        Method m = ExtractSqlExceptionUtil.class.getDeclaredMethod("hasSqlStateOperationIssue", String.class);
        m.setAccessible(true);

        // Act
        boolean result = (boolean) m.invoke(null, "08S01");

        // Assert
        assertFalse(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil hasChainedOperationIssue
     * nextExceptionでtrueとなることを確認するテストケース
     */
    @Test
    void hasChainedOperationIssue_001() throws Exception {
        // Arrange
        Method m = ExtractSqlExceptionUtil.class.getDeclaredMethod("hasChainedOperationIssue", SQLException.class);
        m.setAccessible(true);
        SQLException root = new SQLException("x", "00000");
        SQLException next = new SQLException("y", "40001");
        root.setNextException(next);

        // Act
        boolean result = (boolean) m.invoke(null, root);

        // Assert
        assertTrue(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil hasChainedOperationIssue
     * causeでtrueとなることを確認するテストケース
     */
    @Test
    void hasChainedOperationIssue_002() throws Exception {
        // Arrange
        Method m = ExtractSqlExceptionUtil.class.getDeclaredMethod("hasChainedOperationIssue", SQLException.class);
        m.setAccessible(true);
        SQLException root = new SQLException("x", "00000");
        SQLException cause = new SQLException("y", "23505");
        root.initCause(cause);

        // Act
        boolean result = (boolean) m.invoke(null, root);

        // Assert
        assertTrue(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil hasChainedOperationIssue
     * チェーンがない場合にfalseとなることを確認するテストケース
     */
    @Test
    void hasChainedOperationIssue_003() throws Exception {
        // Arrange
        Method m = ExtractSqlExceptionUtil.class.getDeclaredMethod("hasChainedOperationIssue", SQLException.class);
        m.setAccessible(true);
        SQLException root = new SQLException("x", "00000");

        // Act
        boolean result = (boolean) m.invoke(null, root);

        // Assert
        assertFalse(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil isSqlConnectionError
     * SQLStateが57P01(Server shutdown)の場合にtrueが返ることを確認するテストケース
     */
    @Test
    void isSqlConnectionError_014() {
        // Arrange
        SQLException e = new SQLException("x", "57P01");

        // Act
        boolean result = ExtractSqlExceptionUtil.isSqlConnectionError(e);

        // Assert
        assertTrue(result);
    }

    /* ===== Operation系：SQLState OR 連鎖（不足分） ===== */

    /** 40P01: Deadlock detected */
    @Test
    void isSqlOperationError_010() {
        // Arrange
        SQLException e = new SQLException("x", "40P01");

        // Act
        boolean result = ExtractSqlExceptionUtil.isSqlOperationError(e);

        // Assert
        assertTrue(result);
    }

    /** 55P03: Lock not available */
    @Test
    void isSqlOperationError_011() {
        // Arrange
        SQLException e = new SQLException("x", "55P03");

        // Act
        boolean result = ExtractSqlExceptionUtil.isSqlOperationError(e);

        // Assert
        assertTrue(result);
    }

    /** 57014: Query canceled */
    @Test
    void isSqlOperationError_012() {
        // Arrange
        SQLException e = new SQLException("x", "57014");

        // Act
        boolean result = ExtractSqlExceptionUtil.isSqlOperationError(e);

        // Assert
        assertTrue(result);
    }

    /** 23502: NOT NULL violation */
    @Test
    void isSqlOperationError_013() {
        // Arrange
        SQLException e = new SQLException("x", "23502");

        // Act
        boolean result = ExtractSqlExceptionUtil.isSqlOperationError(e);

        // Assert
        assertTrue(result);
    }

    /** 23503: Foreign key violation */
    @Test
    void isSqlOperationError_014() {
        // Arrange
        SQLException e = new SQLException("x", "23503");

        // Act
        boolean result = ExtractSqlExceptionUtil.isSqlOperationError(e);

        // Assert
        assertTrue(result);
    }

    /** 23514: Check violation */
    @Test
    void isSqlOperationError_015() {
        // Arrange
        SQLException e = new SQLException("x", "23514");

        // Act
        boolean result = ExtractSqlExceptionUtil.isSqlOperationError(e);

        // Assert
        assertTrue(result);
    }

    /** 22001: String data right truncation */
    @Test
    void isSqlOperationError_016() {
        // Arrange
        SQLException e = new SQLException("x", "22001");

        // Act
        boolean result = ExtractSqlExceptionUtil.isSqlOperationError(e);

        // Assert
        assertTrue(result);
    }

    /** 22003: Numeric value out of range */
    @Test
    void isSqlOperationError_017() {
        // Arrange
        SQLException e = new SQLException("x", "22003");

        // Act
        boolean result = ExtractSqlExceptionUtil.isSqlOperationError(e);

        // Assert
        assertTrue(result);
    }

    /** 22P02: Invalid text representation */
    @Test
    void isSqlOperationError_018() {
        // Arrange
        SQLException e = new SQLException("x", "22P02");

        // Act
        boolean result = ExtractSqlExceptionUtil.isSqlOperationError(e);

        // Assert
        assertTrue(result);
    }

    /** 42601: Syntax error */
    @Test
    void isSqlOperationError_019() {
        // Arrange
        SQLException e = new SQLException("x", "42601");

        // Act
        boolean result = ExtractSqlExceptionUtil.isSqlOperationError(e);

        // Assert
        assertTrue(result);
    }

    /** 42703: Undefined column */
    @Test
    void isSqlOperationError_020() {
        // Arrange
        SQLException e = new SQLException("x", "42703");

        // Act
        boolean result = ExtractSqlExceptionUtil.isSqlOperationError(e);

        // Assert
        assertTrue(result);
    }

    /** 42P01: Undefined table */
    @Test
    void isSqlOperationError_021() {
        // Arrange
        SQLException e = new SQLException("x", "42P01");

        // Act
        boolean result = ExtractSqlExceptionUtil.isSqlOperationError(e);

        // Assert
        assertTrue(result);
    }

    /** 42883: Undefined function */
    @Test
    void isSqlOperationError_022() {
        // Arrange
        SQLException e = new SQLException("x", "42883");

        // Act
        boolean result = ExtractSqlExceptionUtil.isSqlOperationError(e);

        // Assert
        assertTrue(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil isSqlOperationError
     * SQLStateが空文字の場合にfalseが返ることを確認するテストケース
     */
    @Test
    void isSqlOperationError_023() {
        // Arrange
        SQLException e = new SQLException("x", ""); // empty SQLState

        // Act
        boolean result = ExtractSqlExceptionUtil.isSqlOperationError(e);

        // Assert
        assertFalse(result);
    }

    /* ===== Chained系：AND途中で落ちる枝（Connection） ===== */

    /**
     * クラス：ExtractSqlExceptionUtil hasChainedConnectionIssue
     * nextExceptionは存在するが接続系エラーではない場合にfalseとなることを確認するテストケース
     */
    @Test
    void hasChainedConnectionIssue_004() throws Exception {
        // Arrange
        Method m = ExtractSqlExceptionUtil.class.getDeclaredMethod("hasChainedConnectionIssue", SQLException.class);
        m.setAccessible(true);

        SQLException root = new SQLException("x", "00000");
        SQLException next = new SQLException("y", "00000"); // connection issue ではない
        root.setNextException(next);

        // Act
        boolean result = (boolean) m.invoke(null, root);

        // Assert
        assertFalse(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil hasChainedConnectionIssue
     * nextExceptionが自分自身(next == e)の場合にfalseとなることを確認するテストケース
     */
    @Test
    void hasChainedConnectionIssue_005() throws Exception {
        // Arrange
        Method m = ExtractSqlExceptionUtil.class.getDeclaredMethod("hasChainedConnectionIssue", SQLException.class);
        m.setAccessible(true);

        SQLException root = new SQLException("x", "00000");
        root.setNextException(root); // next == e

        // Act
        boolean result = (boolean) m.invoke(null, root);

        // Assert
        assertFalse(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil hasChainedConnectionIssue
     * causeがSQLExceptionだが接続系エラーではない場合にfalseとなることを確認するテストケース
     */
    @Test
    void hasChainedConnectionIssue_006() throws Exception {
        // Arrange
        Method m = ExtractSqlExceptionUtil.class.getDeclaredMethod("hasChainedConnectionIssue", SQLException.class);
        m.setAccessible(true);

        SQLException root = new SQLException("x", "00000");
        SQLException cause = new SQLException("y", "00000"); // connection issue ではない
        root.initCause(cause);

        // Act
        boolean result = (boolean) m.invoke(null, root);

        // Assert
        assertFalse(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil hasChainedConnectionIssue
     * cause == self（cause != e がfalse）となる分岐を踏み、falseとなることを確認するテストケース
     */
    @Test
    void hasChainedConnectionIssue_007() throws Exception {
        // Arrange
        Method m = ExtractSqlExceptionUtil.class.getDeclaredMethod("hasChainedConnectionIssue", SQLException.class);
        m.setAccessible(true);

        class SelfCauseSQLException extends SQLException {
            SelfCauseSQLException(String reason, String sqlState) {
                super(reason, sqlState);
            }

            @Override
            public synchronized Throwable getCause() {
                return this;
            } // cause == e を作る
        }

        SQLException root = new SelfCauseSQLException("x", "00000");

        // Act
        boolean result = (boolean) m.invoke(null, root);

        // Assert
        assertFalse(result);
    }

    /* ===== Chained系：AND途中で落ちる枝（Operation） ===== */

    /**
     * クラス：ExtractSqlExceptionUtil hasChainedOperationIssue
     * nextExceptionは存在するが操作系エラーではない場合にfalseとなることを確認するテストケース
     */
    @Test
    void hasChainedOperationIssue_004() throws Exception {
        // Arrange
        Method m = ExtractSqlExceptionUtil.class.getDeclaredMethod("hasChainedOperationIssue", SQLException.class);
        m.setAccessible(true);

        SQLException root = new SQLException("x", "00000");
        SQLException next = new SQLException("y", "00000"); // operation issue ではない
        root.setNextException(next);

        // Act
        boolean result = (boolean) m.invoke(null, root);

        // Assert
        assertFalse(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil hasChainedOperationIssue
     * nextExceptionが自分自身(next == e)の場合にfalseとなることを確認するテストケース
     */
    @Test
    void hasChainedOperationIssue_005() throws Exception {
        // Arrange
        Method m = ExtractSqlExceptionUtil.class.getDeclaredMethod("hasChainedOperationIssue", SQLException.class);
        m.setAccessible(true);

        SQLException root = new SQLException("x", "00000");
        root.setNextException(root); // next == e

        // Act
        boolean result = (boolean) m.invoke(null, root);

        // Assert
        assertFalse(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil hasChainedOperationIssue
     * causeがSQLExceptionだが操作系エラーではない場合にfalseとなることを確認するテストケース
     */
    @Test
    void hasChainedOperationIssue_006() throws Exception {
        // Arrange
        Method m = ExtractSqlExceptionUtil.class.getDeclaredMethod("hasChainedOperationIssue", SQLException.class);
        m.setAccessible(true);

        SQLException root = new SQLException("x", "00000");
        SQLException cause = new SQLException("y", "00000"); // operation issue ではない
        root.initCause(cause);

        // Act
        boolean result = (boolean) m.invoke(null, root);

        // Assert
        assertFalse(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil hasChainedOperationIssue
     * cause == self（cause != e がfalse）となる分岐を踏み、falseとなることを確認するテストケース
     */
    @Test
    void hasChainedOperationIssue_007() throws Exception {
        // Arrange
        Method m = ExtractSqlExceptionUtil.class.getDeclaredMethod("hasChainedOperationIssue", SQLException.class);
        m.setAccessible(true);

        class SelfCauseSQLException extends SQLException {
            SelfCauseSQLException(String reason, String sqlState) {
                super(reason, sqlState);
            }

            @Override
            public synchronized Throwable getCause() {
                return this;
            }
        }

        SQLException root = new SelfCauseSQLException("x", "00000");

        // Act
        boolean result = (boolean) m.invoke(null, root);

        // Assert
        assertFalse(result);
    }

}
