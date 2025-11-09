package com.toyota.tsc.notificationhub.commons;

import org.junit.jupiter.api.Test;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * クラス：ExtractSqlExceptionUtil すべての分岐・例外系を確認するテストケース
 */
class ExtractSqlExceptionUtilTest {

    // --- findSqlException ---

    /**
     * クラス：ExtractSqlExceptionUtil findSqlException 直近の Throwable が SQLException
     * の場合を確認するテストケース
     */
    @Test
    void findSqlException_01() {
        SQLException e = new SQLException("x");
        assertSame(e, ExtractSqlExceptionUtil.findSqlException(e));
    }

    /**
     * クラス：ExtractSqlExceptionUtil findSqlException cause チェーン中に SQLException
     * が存在する場合を確認するテストケース
     */
    @Test
    void findSqlException_02() {
        Throwable t = new RuntimeException("top", new SQLException("inner"));
        assertTrue(ExtractSqlExceptionUtil.findSqlException(t) instanceof SQLException);
    }

    /**
     * クラス：ExtractSqlExceptionUtil findSqlException SQLException が無い場合に null
     * を返すことを確認するテストケース
     */
    @Test
    void findSqlException_03() {
        assertNull(ExtractSqlExceptionUtil.findSqlException(new RuntimeException("x")));
    }

    // --- isSqlConnectionError（型判定）---

    /**
     * クラス：ExtractSqlExceptionUtil isSqlConnectionError
     * SQLTransientConnectionException を接続例外として判定することを確認するテストケース
     */
    @Test
    void isSqlConnectionError_01() {
        assertTrue(ExtractSqlExceptionUtil.isSqlConnectionError(new SQLTransientConnectionException("tce")));
    }

    /**
     * クラス：ExtractSqlExceptionUtil isSqlConnectionError
     * SQLNonTransientConnectionException を接続例外として判定することを確認するテストケース
     */
    @Test
    void isSqlConnectionError_02() {
        assertTrue(ExtractSqlExceptionUtil.isSqlConnectionError(new SQLNonTransientConnectionException("ntce")));
    }

    // --- isSqlConnectionError（SQLState 判定）---

    /**
     * クラス：ExtractSqlExceptionUtil isSqlConnectionError SQLState が "08"
     * クラスの場合を確認するテストケース
     */
    @Test
    void isSqlConnectionError_03() {
        SQLException e = new SQLException("x", "08006"); // 接続失敗
        assertTrue(ExtractSqlExceptionUtil.isSqlConnectionError(e));
    }

    /**
     * クラス：ExtractSqlExceptionUtil isSqlConnectionError SQLState が "08S01"
     * の場合を確認するテストケース
     */
    @Test
    void isSqlConnectionError_04() {
        SQLException e = new SQLException("x", "08S01");
        assertTrue(ExtractSqlExceptionUtil.isSqlConnectionError(e));
    }

    /**
     * クラス：ExtractSqlExceptionUtil isSqlConnectionError SQLState が "08004"
     * で始まる場合を確認するテストケース
     */
    @Test
    void isSqlConnectionError_05() {
        SQLException e = new SQLException("x", "08004");
        assertTrue(ExtractSqlExceptionUtil.isSqlConnectionError(e));
    }

    /**
     * クラス：ExtractSqlExceptionUtil isSqlConnectionError SQLState が "28"
     * クラスの場合を確認するテストケース
     */
    @Test
    void isSqlConnectionError_06() {
        SQLException e = new SQLException("x", "28000");
        assertTrue(ExtractSqlExceptionUtil.isSqlConnectionError(e));
    }

    /**
     * クラス：ExtractSqlExceptionUtil isSqlConnectionError SQLState が "3D000"
     * の場合を確認するテストケース
     */
    @Test
    void isSqlConnectionError_07() {
        SQLException e = new SQLException("x", "3D000");
        assertTrue(ExtractSqlExceptionUtil.isSqlConnectionError(e));
    }

    /**
     * クラス：ExtractSqlExceptionUtil isSqlConnectionError SQLState が "53300"
     * の場合を確認するテストケース
     */
    @Test
    void isSqlConnectionError_08() {
        SQLException e = new SQLException("x", "53300");
        assertTrue(ExtractSqlExceptionUtil.isSqlConnectionError(e));
    }

    /**
     * クラス：ExtractSqlExceptionUtil isSqlConnectionError SQLState が "57P01"
     * の場合を確認するテストケース
     */
    @Test
    void isSqlConnectionError_09() {
        SQLException e = new SQLException("x", "57P01");
        assertTrue(ExtractSqlExceptionUtil.isSqlConnectionError(e));
    }

    /**
     * クラス：ExtractSqlExceptionUtil isSqlConnectionError SQLState が "57P03"
     * の場合を確認するテストケース
     */
    @Test
    void isSqlConnectionError_10() {
        SQLException e = new SQLException("x", "57P03");
        assertTrue(ExtractSqlExceptionUtil.isSqlConnectionError(e));
    }

    // --- isSqlConnectionError（チェーン判定）---

    /**
     * クラス：ExtractSqlExceptionUtil isSqlConnectionError nextException
     * が接続例外の場合を確認するテストケース
     */
    @Test
    void isSqlConnectionError_11() {
        SQLException e = new SQLException("top", "00000");
        e.setNextException(new SQLException("next", "28000")); // 認証系
        assertTrue(ExtractSqlExceptionUtil.isSqlConnectionError(e));
    }

    /**
     * クラス：ExtractSqlExceptionUtil isSqlConnectionError cause が接続例外の場合を確認するテストケース
     */
    @Test
    void isSqlConnectionError_12() {
        SQLException cause = new SQLException("cause", "08006");
        SQLException e = new SQLException("top", "00000", cause);
        assertTrue(ExtractSqlExceptionUtil.isSqlConnectionError(e));
    }

    /**
     * クラス：ExtractSqlExceptionUtil isSqlConnectionError いずれにも該当しない場合 false
     * を返すことを確認するテストケース
     */
    @Test
    void isSqlConnectionError_13() {
        SQLException e = new SQLException("x", "00000");
        assertFalse(ExtractSqlExceptionUtil.isSqlConnectionError(e));
    }

    // --- 27行目：state != null && !state.isEmpty() の偽側（state=null）
    @Test
    void isSqlConnectionError_stateNull_false() {
        SQLException e = new SQLException("x", (String) null);
        assertFalse(ExtractSqlExceptionUtil.isSqlConnectionError(e));
    }

    // --- 27行目：state != null && !state.isEmpty() の偽側（state=""）
    @Test
    void isSqlConnectionError_stateEmpty_false() {
        SQLException e = new SQLException("x", "");
        assertFalse(ExtractSqlExceptionUtil.isSqlConnectionError(e));
    }

    // --- 27行目：真側に入る最小ケース（state長1）→ 08/28/3D000 等に該当せず false
    @Test
    void isSqlConnectionError_stateLen1_false() {
        SQLException e = new SQLException("x", "0");
        assertFalse(ExtractSqlExceptionUtil.isSqlConnectionError(e));
    }

    // --- 37行目："08S01".equals(state) の偽側評価（非08系）→ 全体 false
    @Test
    void isSqlConnectionError_non08S01_false() {
        SQLException e = new SQLException("x", "XX001");
        assertFalse(ExtractSqlExceptionUtil.isSqlConnectionError(e));
    }

    // --- 41行目：state.startsWith("08004") の偽側評価（非08004）→ 全体 false
    @Test
    void isSqlConnectionError_non08004_false() {
        SQLException e2 = new SQLException("x2", "ZZ004");
        assertFalse(ExtractSqlExceptionUtil.isSqlConnectionError(e2));
    }

    // --- 60行目：next != null && next != e && isSqlConnectionError(next)
    // の偽（next==self）
    @Test
    void isSqlConnectionError_nextIsSelf_false() {
        SQLException e = new SQLException("top", "00000");
        e.setNextException(e);
        assertFalse(ExtractSqlExceptionUtil.isSqlConnectionError(e));
    }

    // --- 60行目：next != null && next != e && isSqlConnectionError(next)
    // の偽（nextが非接続系）
    @Test
    void isSqlConnectionError_nextNonConn_false() {
        SQLException e = new SQLException("top", "00000");
        e.setNextException(new SQLException("next", "00000"));
        assertFalse(ExtractSqlExceptionUtil.isSqlConnectionError(e));
    }

    // --- 64行目：cause instanceof SQLException ... の偽（causeが非SQLException）
    @Test
    void isSqlConnectionError_causeNonSql_false() {
        SQLException e = new SQLException("top", "00000", new RuntimeException("non-sql"));
        assertFalse(ExtractSqlExceptionUtil.isSqlConnectionError(e));
    }

    // --- 64行目：cause instanceof SQLException ... の偽（causeはSQLExceptionだが非接続系）
    @Test
    void isSqlConnectionError_causeSqlNonConn_false() {
        SQLException cause = new SQLException("cause", "00000");
        SQLException e = new SQLException("top", "00000", cause);
        assertFalse(ExtractSqlExceptionUtil.isSqlConnectionError(e));
    }

    // cause == e を作るための特殊な SQLException
    static class SelfCauseSQLException extends SQLException {
        SelfCauseSQLException(String msg, String state) {
            super(msg, state);
        }

        @Override
        public Throwable getCause() {
            return this;
        }
    }

    // --- 64行目：全サブ条件が真（true）→ 全体 true
    @Test
    void causeBranch_true_allSubcondsTrue() {
        SQLException cause = new SQLException("cause", "08006"); // 接続失敗
        SQLException e = new SQLException("top", "00000", cause); // top は非接続系
        assertTrue(ExtractSqlExceptionUtil.isSqlConnectionError(e));
    }

    // --- 64行目：第1サブ条件が偽（cause が非 SQLException）→ 全体 false
    @Test
    void causeBranch_false_nonSqlCause() {
        SQLException e = new SQLException("top", "00000", new RuntimeException("x"));
        assertFalse(ExtractSqlExceptionUtil.isSqlConnectionError(e));
    }

    // --- 64行目：第2サブ条件が偽（cause == e）→ 全体 false
    @Test
    void causeBranch_false_causeEqualsE() {
        SelfCauseSQLException e = new SelfCauseSQLException("top", "00000");
        assertFalse(ExtractSqlExceptionUtil.isSqlConnectionError(e));
    }

    // --- 64行目：第3サブ条件が偽（cause は SQLException だが接続例外ではない）→ 全体 false
    @Test
    void causeBranch_false_sqlCauseNonConn() {
        SQLException cause = new SQLException("cause", "00000");
        SQLException e = new SQLException("top", "00000", cause);
        assertFalse(ExtractSqlExceptionUtil.isSqlConnectionError(e));
    }

}