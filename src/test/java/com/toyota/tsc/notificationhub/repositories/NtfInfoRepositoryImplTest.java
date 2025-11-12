package com.toyota.tsc.notificationhub.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.sql.SQLNonTransientException;
import java.sql.SQLTransientException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockedStatic;

import com.toyota.tsc.notificationhub.commons.ExtractSqlExceptionUtil;

/**
 * クラス：NtfInfoRepositoryImpl NtfInfoRepositoryImplの各メソッドを網羅的に確認するテストケース
 */
@ExtendWith(MockitoExtension.class)
public class NtfInfoRepositoryImplTest {

    @Mock
    private NtfInfoMapper mapper;

    private NtfInfoRepositoryImpl sut;

    @BeforeEach
    void setUp() {
        sut = new NtfInfoRepositoryImpl(mapper);
        // デフォルトのリトライ設定（必要に応じて各テストで上書き）
        setField(sut, "upsertRetryCount", 3);
        setField(sut, "upsertRetryBaseInterval", 0L);
        setField(sut, "upsertRetryMaxInterval", 0L);
    }

    @AfterEach
    void tearDown() {
        // 割り込みフラグのクリア（他テストへ影響させない）
        Thread.interrupted();
    }

    // ---------------------------
    // insert
    // ---------------------------

    /** クラス：NtfInfoRepositoryImpl insertの正常系を確認するテストケース */
    @Test
    void insert_01() {
        // 準備
        NtfInfoEntity entity = sampleEntity();
        when(mapper.insert(entity)).thenReturn(1);

        // 実行
        int result = sut.insert(entity);

        // 確認
        assertThat(result).isEqualTo(1);
        verify(mapper).insert(entity);
    }

    /** クラス：NtfInfoRepositoryImpl insertで例外が伝播されることを確認するテストケース */
    @Test
    void insert_02() {
        // 準備
        NtfInfoEntity entity = sampleEntity();
        RuntimeException ex = new RuntimeException("insert failed");
        when(mapper.insert(entity)).thenThrow(ex);

        // 実行 & 確認
        assertThrows(RuntimeException.class, () -> sut.insert(entity));
        verify(mapper).insert(entity);
    }

    // ---------------------------
    // update
    // ---------------------------

    /** クラス：NtfInfoRepositoryImpl updateの正常系を確認するテストケース */
    @Test
    void update_01() {
        // 準備
        NtfInfoEntity entity = sampleEntity();
        when(mapper.update(entity)).thenReturn(1);

        // 実行
        int result = sut.update(entity);

        // 確認
        assertThat(result).isEqualTo(1);
        verify(mapper).update(entity);
    }

    /** クラス：NtfInfoRepositoryImpl updateで例外が伝播されることを確認するテストケース */
    @Test
    void update_02() {
        // 準備
        NtfInfoEntity entity = sampleEntity();
        RuntimeException ex = new RuntimeException("update failed");
        when(mapper.update(entity)).thenThrow(ex);

        // 実行 & 確認
        assertThrows(RuntimeException.class, () -> sut.update(entity));
        verify(mapper).update(entity);
    }

    // ---------------------------
    // select
    // ---------------------------

    /** クラス：NtfInfoRepositoryImpl selectの正常系を確認するテストケース */
    @Test
    void select_01() {
        // 準備
        String internalUserId = "u1";
        String installationId = "inst1";
        NtfInfoEntity entity = sampleEntity();
        when(mapper.select(internalUserId, installationId)).thenReturn(entity);

        // 実行
        NtfInfoEntity result = sut.select(internalUserId, installationId);

        // 確認
        assertThat(result).isNotNull();
        assertThat(result.getInternalUserId()).isEqualTo(entity.getInternalUserId());
        verify(mapper).select(internalUserId, installationId);
    }

    /** クラス：NtfInfoRepositoryImpl selectで例外が伝播されることを確認するテストケース */
    @Test
    void select_02() {
        // 準備
        String internalUserId = "u1";
        String installationId = "inst1";
        RuntimeException ex = new RuntimeException("select failed");
        when(mapper.select(internalUserId, installationId)).thenThrow(ex);

        // 実行 & 確認
        assertThrows(RuntimeException.class, () -> sut.select(internalUserId, installationId));
        verify(mapper).select(internalUserId, installationId);
    }

    // ---------------------------
    // selectAllByInternalUserId
    // ---------------------------

    /** クラス：NtfInfoRepositoryImpl selectAllByInternalUserIdの正常系を確認するテストケース */
    @Test
    void selectAllByInternalUserId_01() {
        // 準備
        String internalUserId = "u1";
        List<NtfInfoEntity> list = Arrays.asList(sampleEntity(), sampleEntity());
        when(mapper.selectAllByInternalUserId(internalUserId)).thenReturn(list);

        // 実行
        List<NtfInfoEntity> result = sut.selectAllByInternalUserId(internalUserId);

        // 確認
        assertThat(result).hasSize(2);
        verify(mapper).selectAllByInternalUserId(internalUserId);
    }

    /** クラス：NtfInfoRepositoryImpl selectAllByInternalUserIdで例外が伝播されることを確認するテストケース */
    @Test
    void selectAllByInternalUserId_02() {
        // 準備
        String internalUserId = "u1";
        RuntimeException ex = new RuntimeException("selectAll failed");
        when(mapper.selectAllByInternalUserId(internalUserId)).thenThrow(ex);

        // 実行 & 確認
        assertThrows(RuntimeException.class, () -> sut.selectAllByInternalUserId(internalUserId));
        verify(mapper).selectAllByInternalUserId(internalUserId);
    }

    // ---------------------------
    // delete（executeWithRetry 経由）
    // ---------------------------

    /** クラス：NtfInfoRepositoryImpl deleteの正常系（リトライなし）を確認するテストケース */
    @Test
    void delete_01() {
        // 準備
        when(mapper.delete("u1", "inst1")).thenReturn(1);

        // 実行
        int result = sut.delete("u1", "inst1");

        // 確認
        assertThat(result).isEqualTo(1);
        verify(mapper).delete("u1", "inst1");
    }

    /**
     * クラス：NtfInfoRepositoryImpl deleteでSQLTransientException後にリトライ成功することを確認するテストケース
     */
    @Test
    void delete_02() {
        // 準備
        setField(sut, "upsertRetryCount", 3);
        setField(sut, "upsertRetryBaseInterval", 0L);
        setField(sut, "upsertRetryMaxInterval", 0L);

        SQLTransientException tex = new SQLTransientException("temporary");
        RuntimeException wrapped = new RuntimeException("wrapped transient", tex);

        try (MockedStatic<ExtractSqlExceptionUtil> mocked = mockStatic(ExtractSqlExceptionUtil.class)) {
            // ラップ例外を渡すと SQLTransientException が見つかる設定
            mocked.when(() -> ExtractSqlExceptionUtil.findSqlException(wrapped)).thenReturn(tex);

            when(mapper.delete("u1", "inst1"))
                    .thenThrow(wrapped) // 1回目：ラップ例外（catch → transient と判定 → リトライ）
                    .thenReturn(1); // 2回目：成功

            // 実行
            int result = sut.delete("u1", "inst1");

            // 確認
            assertThat(result).isEqualTo(1);
            verify(mapper, times(2)).delete("u1", "inst1");
        }
    }

    /**
     * クラス：NtfInfoRepositoryImpl
     * deleteでSQLTransientExceptionがリトライ上限を超えた場合にRuntimeExceptionが発生することを確認するテストケース
     */
    @Test
    void delete_03() {
        // 準備：上限0 → 1回目で越える
        setField(sut, "upsertRetryCount", 0);
        setField(sut, "upsertRetryBaseInterval", 0L);
        setField(sut, "upsertRetryMaxInterval", 0L);

        SQLTransientException tex = new SQLTransientException("temporary");
        RuntimeException wrapped = new RuntimeException("wrapped transient", tex);

        try (MockedStatic<ExtractSqlExceptionUtil> mocked = mockStatic(ExtractSqlExceptionUtil.class)) {
            mocked.when(() -> ExtractSqlExceptionUtil.findSqlException(wrapped)).thenReturn(tex);
            when(mapper.delete("u1", "inst1")).thenThrow(wrapped);

            // 実行 & 確認
            assertThrows(RuntimeException.class, () -> sut.delete("u1", "inst1"));
            verify(mapper, times(1)).delete("u1", "inst1");
        }
    }

    /**
     * クラス：NtfInfoRepositoryImpl
     * deleteでSQLNonTransientExceptionが発生した場合にRuntimeExceptionが発生することを確認するテストケース
     */
    @Test
    void delete_04() {
        // 準備
        SQLNonTransientException ntex = new SQLNonTransientException("non-transient");
        RuntimeException wrapped = new RuntimeException("wrapped non-transient", ntex);

        try (MockedStatic<ExtractSqlExceptionUtil> mocked = mockStatic(ExtractSqlExceptionUtil.class)) {
            mocked.when(() -> ExtractSqlExceptionUtil.findSqlException(wrapped)).thenReturn(ntex);
            when(mapper.delete("u1", "inst1")).thenThrow(wrapped);

            // 実行 & 確認
            assertThrows(RuntimeException.class, () -> sut.delete("u1", "inst1"));
            verify(mapper, times(1)).delete("u1", "inst1");
        }
    }

    /**
     * クラス：NtfInfoRepositoryImpl
     * deleteでSQLException（Transient以外）が発生した場合にRuntimeExceptionが発生することを確認するテストケース
     */
    @Test
    void delete_05() {
        // 準備
        SQLException sqlex = new SQLException("sql");
        RuntimeException wrapped = new RuntimeException("wrapped sql", sqlex);

        try (MockedStatic<ExtractSqlExceptionUtil> mocked = mockStatic(ExtractSqlExceptionUtil.class)) {
            mocked.when(() -> ExtractSqlExceptionUtil.findSqlException(wrapped)).thenReturn(sqlex);
            when(mapper.delete("u1", "inst1")).thenThrow(wrapped);

            // 実行 & 確認
            assertThrows(RuntimeException.class, () -> sut.delete("u1", "inst1"));
            verify(mapper, times(1)).delete("u1", "inst1");
        }
    }

    /**
     * クラス：NtfInfoRepositoryImpl
     * deleteでSQLExceptionに該当しない例外が発生した場合にRuntimeExceptionが発生することを確認するテストケース
     */
    @Test
    void delete_06() {
        // 準備
        RuntimeException ex = new RuntimeException("other");
        try (MockedStatic<ExtractSqlExceptionUtil> mocked = mockStatic(ExtractSqlExceptionUtil.class)) {
            mocked.when(() -> ExtractSqlExceptionUtil.findSqlException(ex)).thenReturn(null);
            when(mapper.delete("u1", "inst1")).thenThrow(ex);

            // 実行 & 確認
            assertThrows(RuntimeException.class, () -> sut.delete("u1", "inst1"));
        }
    }

    /**
     * クラス：NtfInfoRepositoryImpl
     * deleteでスリープ中に割り込みが発生しても処理が継続されること（割り込みフラグ再設定）を確認するテストケース
     */
    @Test
    void delete_07() {
        // 準備
        setField(sut, "upsertRetryCount", 1); // 1回目はリトライする
        setField(sut, "upsertRetryBaseInterval", 0L);
        setField(sut, "upsertRetryMaxInterval", 0L);
        Thread.currentThread().interrupt(); // 事前に割り込み

        SQLTransientException tex = new SQLTransientException("temporary");
        RuntimeException wrapped = new RuntimeException("wrapped transient", tex);

        try (MockedStatic<ExtractSqlExceptionUtil> mocked = mockStatic(ExtractSqlExceptionUtil.class)) {
            mocked.when(() -> ExtractSqlExceptionUtil.findSqlException(wrapped)).thenReturn(tex);

            when(mapper.delete("u1", "inst1"))
                    .thenThrow(wrapped) // 1回目：ラップ例外 → transient 判定 → sleep(0)で割り込み
                    .thenReturn(1); // 2回目：成功

            // 実行
            int result = sut.delete("u1", "inst1");

            // 確認
            assertThat(result).isEqualTo(1);
            assertThat(Thread.currentThread().isInterrupted()).isTrue(); // 割り込みフラグ再設定を確認
            verify(mapper, times(2)).delete("u1", "inst1");
        }
    }

    // ---------------------------
    // upsert（executeWithRetry 経由）
    // ---------------------------

    /** クラス：NtfInfoRepositoryImpl upsertの正常系（リトライなし）を確認するテストケース */
    @Test
    void upsert_01() {
        // 準備
        NtfInfoEntity entity = sampleEntity();
        when(mapper.upsert(entity)).thenReturn(1);

        // 実行
        int result = sut.upsert(entity);

        // 確認
        assertThat(result).isEqualTo(1);
        verify(mapper).upsert(entity);
    }

    /**
     * クラス：NtfInfoRepositoryImpl upsertでSQLTransientException後にリトライ成功することを確認するテストケース
     */
    @Test
    void upsert_02() {
        // 準備
        setField(sut, "upsertRetryCount", 3);
        setField(sut, "upsertRetryBaseInterval", 0L);
        setField(sut, "upsertRetryMaxInterval", 0L);

        NtfInfoEntity entity = sampleEntity();
        SQLTransientException tex = new SQLTransientException("temporary");
        RuntimeException wrapped = new RuntimeException("wrapped transient", tex);

        try (MockedStatic<ExtractSqlExceptionUtil> mocked = mockStatic(ExtractSqlExceptionUtil.class)) {
            mocked.when(() -> ExtractSqlExceptionUtil.findSqlException(wrapped)).thenReturn(tex);

            when(mapper.upsert(entity))
                    .thenThrow(wrapped) // 1回目：ラップ例外 → transient 判定 → リトライ
                    .thenReturn(1); // 2回目：成功

            // 実行
            int result = sut.upsert(entity);

            // 確認
            assertThat(result).isEqualTo(1);
            verify(mapper, times(2)).upsert(entity);
        }
    }

    /**
     * クラス：NtfInfoRepositoryImpl
     * upsertでSQLTransientExceptionがリトライ上限を超えた場合にRuntimeExceptionが発生することを確認するテストケース
     */
    @Test
    void upsert_03() {
        // 準備：上限0 → 1回目で越える
        setField(sut, "upsertRetryCount", 0);
        setField(sut, "upsertRetryBaseInterval", 0L);
        setField(sut, "upsertRetryMaxInterval", 0L);

        NtfInfoEntity entity = sampleEntity();
        SQLTransientException tex = new SQLTransientException("temporary");
        RuntimeException wrapped = new RuntimeException("wrapped transient", tex);

        try (MockedStatic<ExtractSqlExceptionUtil> mocked = mockStatic(ExtractSqlExceptionUtil.class)) {
            mocked.when(() -> ExtractSqlExceptionUtil.findSqlException(wrapped)).thenReturn(tex);
            when(mapper.upsert(entity)).thenThrow(wrapped);

            // 実行 & 確認
            assertThrows(RuntimeException.class, () -> sut.upsert(entity));
            verify(mapper, times(1)).upsert(entity);
        }
    }

    /**
     * クラス：NtfInfoRepositoryImpl
     * upsertでSQLNonTransientExceptionが発生した場合にRuntimeExceptionが発生することを確認するテストケース
     */
    @Test
    void upsert_04() {
        // 準備
        NtfInfoEntity entity = sampleEntity();
        SQLNonTransientException ntex = new SQLNonTransientException("non-transient");
        RuntimeException wrapped = new RuntimeException("wrapped non-transient", ntex);

        try (MockedStatic<ExtractSqlExceptionUtil> mocked = mockStatic(ExtractSqlExceptionUtil.class)) {
            mocked.when(() -> ExtractSqlExceptionUtil.findSqlException(wrapped)).thenReturn(ntex);
            when(mapper.upsert(entity)).thenThrow(wrapped);

            // 実行 & 確認
            assertThrows(RuntimeException.class, () -> sut.upsert(entity));
            verify(mapper, times(1)).upsert(entity);
        }
    }

    /**
     * クラス：NtfInfoRepositoryImpl
     * upsertでSQLException（Transient以外）が発生した場合にRuntimeExceptionが発生することを確認するテストケース
     */
    @Test
    void upsert_05() {
        // 準備
        NtfInfoEntity entity = sampleEntity();
        SQLException sqlex = new SQLException("sql");
        RuntimeException wrapped = new RuntimeException("wrapped sql", sqlex);

        try (MockedStatic<ExtractSqlExceptionUtil> mocked = mockStatic(ExtractSqlExceptionUtil.class)) {
            mocked.when(() -> ExtractSqlExceptionUtil.findSqlException(wrapped)).thenReturn(sqlex);
            when(mapper.upsert(entity)).thenThrow(wrapped);

            // 実行 & 確認
            assertThrows(RuntimeException.class, () -> sut.upsert(entity));
            verify(mapper, times(1)).upsert(entity);
        }
    }

    /**
     * クラス：NtfInfoRepositoryImpl
     * upsertでSQLExceptionに該当しない例外が発生した場合にRuntimeExceptionが発生することを確認するテストケース
     */
    @Test
    void upsert_06() {
        // 準備
        NtfInfoEntity entity = sampleEntity();
        RuntimeException ex = new RuntimeException("other");
        try (MockedStatic<ExtractSqlExceptionUtil> mocked = mockStatic(ExtractSqlExceptionUtil.class)) {
            mocked.when(() -> ExtractSqlExceptionUtil.findSqlException(ex)).thenReturn(null);
            when(mapper.upsert(entity)).thenThrow(ex);

            // 実行 & 確認
            assertThrows(RuntimeException.class, () -> sut.upsert(entity));
        }
    }

    /**
     * クラス：NtfInfoRepositoryImpl
     * upsertでスリープ中に割り込みが発生しても処理が継続されること（割り込みフラグ再設定）を確認するテストケース
     */
    @Test
    void upsert_07() {
        // 準備
        setField(sut, "upsertRetryCount", 1); // 1回目はリトライする
        setField(sut, "upsertRetryBaseInterval", 0L);
        setField(sut, "upsertRetryMaxInterval", 0L);
        Thread.currentThread().interrupt(); // 事前に割り込み

        NtfInfoEntity entity = sampleEntity();
        SQLTransientException tex = new SQLTransientException("temporary");
        RuntimeException wrapped = new RuntimeException("wrapped transient", tex);

        try (MockedStatic<ExtractSqlExceptionUtil> mocked = mockStatic(ExtractSqlExceptionUtil.class)) {
            mocked.when(() -> ExtractSqlExceptionUtil.findSqlException(wrapped)).thenReturn(tex);

            when(mapper.upsert(entity))
                    .thenThrow(wrapped) // 1回目：ラップ例外 → transient 判定 → sleep(0)で割り込み
                    .thenReturn(1); // 2回目：成功

            // 実行
            int result = sut.upsert(entity);

            // 確認
            assertThat(result).isEqualTo(1);
            assertThat(Thread.currentThread().isInterrupted()).isTrue(); // 割り込みフラグ再設定
            verify(mapper, times(2)).upsert(entity);
        }
    }

    // -----------------------------------
    // ユーティリティ（テストデータ/反射）
    // -----------------------------------

    private NtfInfoEntity sampleEntity() {
        return new NtfInfoEntity(
                "u1",
                "inst1",
                "token1",
                "device1",
                "1",
                "1",
                LocalDateTime.now(),
                LocalDateTime.now());
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
