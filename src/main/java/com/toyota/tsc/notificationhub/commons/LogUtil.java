package com.toyota.tsc.notificationhub.commons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ログ出力ユーティリティクラス
 */
public class LogUtil {

    private LogUtil() {
    }

    /**
     * 指定クラスのLoggerを取得します。
     * 
     * @param clazz ログ取得対象クラス
     * @return Loggerインスタンス
     */
    public static Logger getLogger(Class<?> clazz) {

        return LoggerFactory.getLogger(clazz);
    }

    /**
     * infoレベルのログを出力します。
     * 
     * @param clazz   ログ出力対象クラス
     * @param message ログメッセージ
     * @return なし
     */
    public static void info(Class<?> clazz, String message) {

        getLogger(clazz).info(message);
    }

    /**
     * warnレベルのログを出力します。
     * 
     * @param clazz   ログ出力対象クラス
     * @param message ログメッセージ
     * @return なし
     */
    public static void warn(Class<?> clazz, String message) {

        getLogger(clazz).warn(message);
    }

    /**
     * errorレベルのログを出力します。
     * 
     * @param clazz   ログ出力対象クラス
     * @param message ログメッセージ
     * @return なし
     */
    public static void error(Class<?> clazz, String message) {

        getLogger(clazz).error(message);
    }

    /**
     * debugレベルのログを出力します。
     * 
     * @param clazz   ログ出力対象クラス
     * @param message ログメッセージ
     * @return なし
     */
    public static void debug(Class<?> clazz, String message) {

        getLogger(clazz).debug(message);
    }
}
