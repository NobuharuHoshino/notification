package com.toyota.tsc.notificationhub.exceptions;

/**
 * DB操作系エラーを表すカスタム例外クラス（RuntimeException継承）
 */
public class CustomSqlException extends RuntimeException {
    private final String table;

    public CustomSqlException() {
        super();
        this.table = null;
    }

    public CustomSqlException(String table) {
        super(table);
        this.table = table;
    }

    public CustomSqlException(String table, Throwable cause) {
        super(table, cause);
        this.table = table;
    }

    public String getTable() {
        return table;
    }
}
