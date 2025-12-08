package com.toyota.tsc.notificationhub.commons;

import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.sql.SQLTransientException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLNonTransientException;

/**
 * SQL例外抽出ユーティリティクラス
 */
public class ExtractSqlExceptionUtil {

    private ExtractSqlExceptionUtil() {
    }

    /**
     * ThrowableからSQLExceptionを抽出します。
     * 
     * @param t 対象Throwable
     * @return 抽出したSQLException（存在しない場合はnull）
     */
    public static SQLException findSqlException(Throwable t) {
        while (t != null) {
            if (t instanceof SQLException sqlException) {
                return sqlException;
            }
            t = t.getCause();
        }
        return null;
    }

    /**
     * SQLExceptionが接続系エラーか判定します。
     * 
     * @param e 判定対象SQLException
     * @return 接続系エラーの場合true
     */
    public static boolean isSqlConnectionError(SQLException e) {
        return e != null
                && (isConnectionType(e)
                        || hasSqlStateConnectionIssue(e.getSQLState())
                        || hasChainedConnectionIssue(e));
    }

    /**
     * SQLExceptionが操作系エラーか判定します。
     * 
     * @param e 判定対象SQLException
     * @return 操作系エラーの場合true
     */

    public static boolean isSqlOperationError(SQLException e) {
        return e != null
                && (isOperationType(e)
                        || hasSqlStateOperationIssue(e.getSQLState())
                        || hasChainedOperationIssue(e));
    }

    // #region 接続エラー判定部品
    /**
     * SQLExceptionの型から接続系エラーか判定します。
     */
    private static boolean isConnectionType(SQLException e) {
        return (e instanceof SQLTransientConnectionException)
                || (e instanceof SQLNonTransientConnectionException);
    }

    /**
     * SQLExceptionのSQLStateから接続系エラーか判定します。
     */
    private static boolean hasSqlStateConnectionIssue(String sqlState) {
        if (sqlState == null || sqlState.isEmpty()) {
            return false;
        }
        String state = sqlState.toUpperCase();
        String cls2 = state.substring(0, Math.min(2, state.length()));

        return
        // ODBC/一部プロキシ経由（稀）：通信リンク障害
        "08S01".equals(state)
                // 接続拒否（認可拒否）
                || state.startsWith("08004")
                // ---- クラス判定（PostgreSQL / SQL標準の接続例外クラス） ----
                // 08001/08003/08006/08P01 等。08004（接続拒否）も含むが、上で優先評価済み。
                || "08".equals(cls2)
                // ---- 認証関連（接続フェーズの致命）：28000/28P01 等 ----
                || state.startsWith("28")
                // ---- DBが存在しない（接続設定不備） ----
                || "3D000".equals(state)
                // ---- 運用系（代表的に「接続が確立できない/維持できない」） ----

                || "53300".equals(state)// 53300: 受入不可（リソース枯渇）
                || "57P01".equals(state)// 57P01: サーバ停止中
                || "57P03".equals(state);// 57P03: サーバ起動待ち
    }

    /**
     * SQLExceptionのチェインから接続系エラーか判定します。
     */
    private static boolean hasChainedConnectionIssue(SQLException e) {
        // nextException の判定
        SQLException next = e.getNextException();
        // cause が SQLException のときの判定
        Throwable cause = e.getCause();
        return (next != null && next != e && isSqlConnectionError(next))
                || (cause instanceof SQLException sqlException && cause != e && isSqlConnectionError(sqlException));
    }
    // #endregion

    // #region 操作エラー判定部品

    /**
     * SQLExceptionの型から操作系エラーか判定します。
     * ※ SQLTransientException / SQLNonTransientException を操作系として扱います。
     * （接続系例外のサブタイプも含まれる点に注意）
     */
    private static boolean isOperationType(SQLException e) {
        return (e instanceof SQLTransientException)
                || (e instanceof SQLNonTransientException);
    }

    /**
     * SQLExceptionのSQLStateから操作系エラーか判定します。
     */
    private static boolean hasSqlStateOperationIssue(String sqlState) {
        if (sqlState == null || sqlState.isEmpty()) {
            return false;
        }
        String state = sqlState.toUpperCase();

        return
        // --- トランザクション／並行制御 ---
        "40001".equals(state) // Serialization failure
                || "40P01".equals(state) // Deadlock detected
                // --- ロック／キャンセル／タイムアウト ---
                || "55P03".equals(state) // Lock not available
                || "57014".equals(state) // Query canceled
                // --- 整合性制約違反 ---
                || "23502".equals(state) // NOT NULL violation
                || "23503".equals(state) // Foreign key violation
                || "23505".equals(state) // Unique violation
                || "23514".equals(state) // Check violation
                // --- データ型／値の不整合 ---
                || "22001".equals(state) // String data right truncation
                || "22003".equals(state) // Numeric value out of range
                || "22P02".equals(state) // Invalid text representation
                // --- 構文／オブジェクト未定義 ---
                || "42601".equals(state) // Syntax error
                || "42703".equals(state) // Undefined column
                || "42P01".equals(state) // Undefined table
                || "42883".equals(state) // Undefined function
                // --- 機能未サポート ---
                || "0A000".equals(state); // Feature not supported
    }

    /**
     * SQLExceptionのチェインから操作系エラーか判定します。
     * （next / cause を再帰的に辿る）
     */
    private static boolean hasChainedOperationIssue(SQLException e) {
        // nextException の判定
        SQLException next = e.getNextException();
        // cause が SQLException のときの判定
        Throwable cause = e.getCause();

        return (next != null && next != e && isSqlOperationError(next))
                || (cause instanceof SQLException sqlException && cause != e && isSqlOperationError(sqlException));
    }
    // #endregion
}