package com.toyota.tsc.notificationhub.commons;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLTransientConnectionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * テストクラス：ExtractSqlExceptionUtilTest
 */
class ExtractSqlExceptionUtilTest {

    /**
     * クラス：ExtractSqlExceptionUtil
     * 接続エラー判定（型：SQLTransientConnectionException）を確認するテストケース
     */
    @Test
    void isSqlConnectionError_01() {
        // 準備
        SQLException ex = new SQLTransientConnectionException("transient", "08001", 0);

        // 実行
        boolean result = ExtractSqlExceptionUtil.isSqlConnectionError(ex);

        // 確認
        assertTrue(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil
     * 接続エラー判定（型：SQLNonTransientConnectionException）を確認するテストケース
     */
    @Test
    void isSqlConnectionError_02() {
        // 準備
        SQLException ex = new SQLNonTransientConnectionException("non-transient", "08006", 0);

        // 実行
        boolean result = ExtractSqlExceptionUtil.isSqlConnectionError(ex);

        // 確認
        assertTrue(result);
    }

    /** クラス：ExtractSqlExceptionUtil 接続エラー判定（SQLState: 08001）を確認するテストケース */
    @Test
    void isSqlConnectionError_03() {
        // 準備
        SQLException ex = new SQLException("state", "08001");

        // 実行
        boolean result = ExtractSqlExceptionUtil.isSqlConnectionError(ex);

        // 確認
        assertTrue(result);
    }

    /** クラス：ExtractSqlExceptionUtil 接続エラー判定（SQLState: 08S01 ODBC通信障害）を確認するテストケース */
    @Test
    void isSqlConnectionError_04() {
        // 準備
        SQLException ex = new SQLException("state", "08S01");

        // 実行
        boolean result = ExtractSqlExceptionUtil.isSqlConnectionError(ex);

        // 確認
        assertTrue(result);
    }

    /** クラス：ExtractSqlExceptionUtil 接続エラー判定（SQLState: 28xxx 認証系）を確認するテストケース */
    @Test
    void isSqlConnectionError_05() {
        // 準備
        SQLException ex = new SQLException("state", "28000");

        // 実行
        boolean result = ExtractSqlExceptionUtil.isSqlConnectionError(ex);

        // 確認
        assertTrue(result);
    }

    /** クラス：ExtractSqlExceptionUtil 接続エラー判定（SQLState: 3D000 DB存在なし）を確認するテストケース */
    @Test
    void isSqlConnectionError_06() {
        // 準備
        SQLException ex = new SQLException("state", "3D000");

        // 実行
        boolean result = ExtractSqlExceptionUtil.isSqlConnectionError(ex);

        // 確認
        assertTrue(result);
    }

    /**
     * クラス：ExtractSqlExceptionUtil 接続エラー判定（SQLState: 53300 too many
     * connections）を確認するテストケース
     */
    @Test
    void isSqlConnectionError_07() {
        // 準備
        SQLException ex = new SQLException("state", "53300");

        // 実行
        boolean result = ExtractSqlExceptionUtil.isSqlConnectionError(ex);

        // 確認
        assertTrue(result);
    }

    /** クラス：ExtractSqlExceptionUtil 接続エラー判定（nextExceptionに接続系あり）を確認するテストケース */
    @Test
    void isSqlConnectionError_08() {
        // 準備
        SQLException root = new SQLException("root", "23505"); // 非接続
        SQLException next = new SQLException("next", "08006"); // 接続失敗
        root.setNextException(next);

        // 実行
        boolean result = ExtractSqlExceptionUtil.isSqlConnectionError(root);

        // 確認
        assertTrue(result);
    }

    /** クラス：ExtractSqlExceptionUtil 接続エラー判定（causeに接続系あり）を確認するテストケース */
    @Test
    void isSqlConnectionError_09() {
        // 準備
        SQLException root = new SQLException("root", "23505"); // 非接続
        root.initCause(new SQLTransientConnectionException("cause", "08001", 0));

        // 実行
        boolean result = ExtractSqlExceptionUtil.isSqlConnectionError(root);

        // 確認
        assertTrue(result);
    }

    /** クラス：ExtractSqlExceptionUtil 接続エラー判定（非接続系）を確認するテストケース */
    @Test
    void isSqlConnectionError_10() {
        // 準備
        SQLException ex = new SQLException("root", "23505");

        // 実行
        boolean result = ExtractSqlExceptionUtil.isSqlConnectionError(ex);

        // 確認
        assertFalse(result);
    }
}