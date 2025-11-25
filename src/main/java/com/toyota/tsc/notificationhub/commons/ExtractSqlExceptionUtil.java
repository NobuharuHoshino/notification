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
            if (t instanceof SQLException) {
                return (SQLException) t;
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
        // 型による判定
        if (e instanceof SQLTransientConnectionException
                || e instanceof SQLNonTransientConnectionException) {
            return true;
        }

        // SQLStateによる判定（特定値を先に評価して到達不能分岐を解消）
        String state = e.getSQLState();
        if (state != null && !state.isEmpty()) {
            state = state.toUpperCase();
            String cls2 = state.substring(0, Math.min(2, state.length()));

            // ODBC/一部プロキシ経由（稀）
            if ("08S01".equals(state)) {
                return true; // 通信リンク障害
            }
            // 接続拒否（認可拒否）
            if (state.startsWith("08004")) {
                return true;
            }
            // 接続例外クラス（PostgreSQL / SQL標準）
            if ("08".equals(cls2)) {
                // 08001/08003/08006/08P01 等、08004（接続拒否）も含む
                return true;
            }
            // 認証関連（接続フェーズの致命）：28000/28P01 等
            if (state.startsWith("28")) {
                return true;
            }
            // DBが存在しない（接続設定不備）
            if ("3D000".equals(state)) {
                return true;
            }
            // 代表的に「接続が確立できない/維持できない」運用系
            if ("53300".equals(state)
                    || "57P01".equals(state)
                    || "57P03".equals(state)) {
                return true;
            }
        }

        // 例外チェインによる判定
        SQLException next = e.getNextException();
        if (next != null && next != e && isSqlConnectionError(next)) {
            return true;
        }
        Throwable cause = e.getCause();
        if (cause instanceof SQLException && cause != e && isSqlConnectionError((SQLException) cause)) {
            return true;
        }
        return false;
    }

    /**
     * SQLExceptionが操作系エラーか判定します。
     * 
     * @param e 判定対象SQLException
     * @return 操作系エラーの場合true
     */
    public static boolean isSqlOperationError(SQLException e) {
        if (e instanceof SQLTransientException || e instanceof SQLNonTransientException) {
            return true;
        }

        String state = e.getSQLState();
        if (state != null && !state.isEmpty()) {
            state = state.toUpperCase();

            // --- トランザクション／並行制御 ---
            if ("40001".equals(state)) { // Serialization failure
                return true;
            }
            if ("40P01".equals(state)) { // Deadlock detected
                return true;
            }
            // --- ロック／キャンセル／タイムアウト ---
            if ("55P03".equals(state)) { // Lock not available
                return true;
            }
            if ("57014".equals(state)) { // Query canceled
                return true;
            }
            // --- 整合性制約違反 ---
            if ("23502".equals(state) // NOT NULL violation
                    || "23503".equals(state) // Foreign key violation
                    || "23505".equals(state) // Unique violation
                    || "23514".equals(state) // Check violation
            ) {
                return true;
            }
            // --- データ型／値の不整合 ---
            if ("22001".equals(state) // String data right truncation
                    || "22003".equals(state) // Numeric value out of range
                    || "22P02".equals(state) // Invalid text representation
            ) {
                return true;
            }
            // --- 構文／オブジェクト未定義 ---
            if ("42601".equals(state) // Syntax error
                    || "42703".equals(state) // Undefined column
                    || "42P01".equals(state) // Undefined table
                    || "42883".equals(state) // Undefined function
            ) {
                return true;
            }
            // --- 機能未サポート ---
            if ("0A000".equals(state)) { // Feature not supported
                return true;
            }
        }
        // 例外チェインによる判定（Next / Cause）
        SQLException next = e.getNextException();
        if (next != null && next != e && isSqlOperationError(next)) {
            return true;
        }
        Throwable cause = e.getCause();
        if (cause instanceof SQLException && cause != e && isSqlOperationError((SQLException) cause)) {
            return true;
        }

        return false;
    }

}