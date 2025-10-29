package com.toyota.tsc.notificationhub.commons;

import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.sql.SQLNonTransientConnectionException;

public class ExtractSqlExceptionUtil {

    public static boolean isSqlConnectionError(SQLException e) {
        // 型による判定
        if (e instanceof SQLTransientConnectionException || e instanceof SQLNonTransientConnectionException) {
            return true;
        }

        // SQLStateによる判定
        String state = e.getSQLState();
        if (state != null && !state.isEmpty()) {
            state = state.toUpperCase();
            String cls2 = state.substring(0, Math.min(2, state.length()));

            // 接続例外クラス（PostgreSQL / SQL標準）
            if ("08".equals(cls2)) {
                // 08001/08003/08006/08P01 等。08004（接続拒否）も含む
                return true;
            }
            // ODBC/一部プロキシ経由で出る可能性（稀）
            if ("08S01".equals(state)) {
                return true; // 通信リンク障害
            }
            // 接続拒否（認可拒否）
            if (state.startsWith("08004")) {
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
            // 代表的に「接続が確立できない／維持できない」運用系
            if ("53300".equals(state) || "57P01".equals(state) || "57P03".equals(state)) {
                return true; // too many connections / admin shutdown / cannot_connect_now
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
}