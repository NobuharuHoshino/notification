
// ファイルパス: src/test/java/com/toyota/tsc/notificationhub/repositories/NtfInfoRepositoryImplTest.java
package com.toyota.tsc.notificationhub.repositories;

import com.toyota.tsc.notificationhub.commons.PropertiesUtil;
import com.toyota.tsc.notificationhub.exceptions.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.sql.SQLNonTransientException;
import java.sql.SQLTransientException;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * NtfInfoRepositoryImpl のテストクラス
 */
@SuppressWarnings("all")
@ExtendWith(MockitoExtension.class)
class NtfInfoRepositoryImplTest {

        @Mock
        private NtfInfoMapper ntfInfoMapper;

        @Mock
        private PropertiesUtil propertiesUtil;

        /** クラス：NtfInfoRepositoryImpl insert mapperの戻り値が返ることを確認するテストケース */
        @Test
        void insert_001() {
                // Arrange
                NtfInfoRepositoryImpl sut = new NtfInfoRepositoryImpl(ntfInfoMapper, propertiesUtil);
                NtfInfoEntity entity = mock(NtfInfoEntity.class);
                when(ntfInfoMapper.insert(entity)).thenReturn(1);

                // Act
                int actual = sut.insert(entity);

                // Assert
                assertEquals(1, actual);
                verify(ntfInfoMapper, times(1)).insert(entity);
        }

        /** クラス：NtfInfoRepositoryImpl update mapperの戻り値が返ることを確認するテストケース */
        @Test
        void update_001() {
                // Arrange
                NtfInfoRepositoryImpl sut = new NtfInfoRepositoryImpl(ntfInfoMapper, propertiesUtil);
                NtfInfoEntity entity = mock(NtfInfoEntity.class);
                when(ntfInfoMapper.update(entity)).thenReturn(2);

                // Act
                int actual = sut.update(entity);

                // Assert
                assertEquals(2, actual);
                verify(ntfInfoMapper, times(1)).update(entity);
        }

        /** クラス：NtfInfoRepositoryImpl select mapperの戻り値が返ることを確認するテストケース */
        @Test
        void select_001() {
                // Arrange
                NtfInfoRepositoryImpl sut = new NtfInfoRepositoryImpl(ntfInfoMapper, propertiesUtil);
                NtfInfoEntity entity = mock(NtfInfoEntity.class);
                when(ntfInfoMapper.select("u", "i")).thenReturn(entity);

                // Act
                NtfInfoEntity actual = sut.select("u", "i");

                // Assert
                assertSame(entity, actual);
                verify(ntfInfoMapper, times(1)).select("u", "i");
        }

        /**
         * クラス：NtfInfoRepositoryImpl delete Transientが1回失敗後に成功する場合にリトライされることを確認するテストケース
         */
        @Test
        void delete_001() {
                // Arrange
                when(propertiesUtil.getNtfinfoUpsertRetryCount()).thenReturn(3);
                when(propertiesUtil.getNtfinfoUpsertRetryBaseInterval()).thenReturn(0);
                when(propertiesUtil.getNtfinfoUpsertRetryMaxInterval()).thenReturn(0);

                when(ntfInfoMapper.delete("u", "i"))
                                .thenThrow(new RuntimeException(new java.sql.SQLTransientException("transient")))
                                .thenReturn(1);

                NtfInfoRepositoryImpl sut = new NtfInfoRepositoryImpl(ntfInfoMapper, propertiesUtil);

                // Act
                int actual = sut.delete("u", "i");

                // Assert
                assertEquals(1, actual);
                verify(ntfInfoMapper, times(2)).delete("u", "i");
        }

        /**
         * クラス：NtfInfoRepositoryImpl delete 非SQL例外の場合にCustomExceptionが送出されることを確認するテストケース
         */
        @Test
        void delete_002() {
                // Arrange
                when(propertiesUtil.getNtfinfoUpsertRetryCount()).thenReturn(3);

                when(ntfInfoMapper.delete("u", "i")).thenThrow(new RuntimeException("boom"));

                NtfInfoRepositoryImpl sut = new NtfInfoRepositoryImpl(ntfInfoMapper, propertiesUtil);

                // Act
                CustomException ex = assertThrows(CustomException.class, () -> sut.delete("u", "i"));

                // Assert
                assertNotNull(ex.getCause());
        }

        /**
         * クラス：NtfInfoRepositoryImpl delete
         * 非TransientなSQL例外の場合にCustomExceptionが送出されることを確認するテストケース
         */
        @Test
        void delete_003() {
                // Arrange
                when(propertiesUtil.getNtfinfoUpsertRetryCount()).thenReturn(3);

                when(ntfInfoMapper.delete("u", "i"))
                                .thenThrow(new RuntimeException(
                                                new java.sql.SQLNonTransientException("non-transient")));

                NtfInfoRepositoryImpl sut = new NtfInfoRepositoryImpl(ntfInfoMapper, propertiesUtil);

                // Act
                CustomException ex = assertThrows(CustomException.class, () -> sut.delete("u", "i"));

                // Assert
                assertNotNull(ex.getCause());
        }

        /**
         * クラス：NtfInfoRepositoryImpl delete
         * Transientがリトライ回数超過した場合にCustomExceptionが送出されることを確認するテストケース
         */
        @Test
        void delete_004() {
                // Arrange
                when(propertiesUtil.getNtfinfoUpsertRetryCount()).thenReturn(1);
                when(propertiesUtil.getNtfinfoUpsertRetryBaseInterval()).thenReturn(0);
                when(propertiesUtil.getNtfinfoUpsertRetryMaxInterval()).thenReturn(0);

                when(ntfInfoMapper.delete("u", "i"))
                                .thenThrow(new RuntimeException(new java.sql.SQLTransientException("transient")));

                NtfInfoRepositoryImpl sut = new NtfInfoRepositoryImpl(ntfInfoMapper, propertiesUtil);

                // Act
                CustomException ex = assertThrows(CustomException.class, () -> sut.delete("u", "i"));

                // Assert
                assertNotNull(ex.getCause());
        }

        /**
         * クラス：NtfInfoRepositoryImpl upsert Transientが1回失敗後に成功する場合にリトライされることを確認するテストケース
         */
        @Test
        void upsert_001() {
                // Arrange
                when(propertiesUtil.getNtfinfoUpsertRetryCount()).thenReturn(3);
                when(propertiesUtil.getNtfinfoUpsertRetryBaseInterval()).thenReturn(0);
                when(propertiesUtil.getNtfinfoUpsertRetryMaxInterval()).thenReturn(0);

                NtfInfoEntity entity = mock(NtfInfoEntity.class);

                when(ntfInfoMapper.upsert(entity))
                                .thenThrow(new RuntimeException(new java.sql.SQLTransientException("transient")))
                                .thenReturn(1);

                NtfInfoRepositoryImpl sut = new NtfInfoRepositoryImpl(ntfInfoMapper, propertiesUtil);

                // Act
                int actual = sut.upsert(entity);

                // Assert
                assertEquals(1, actual);
                verify(ntfInfoMapper, times(2)).upsert(entity);
        }

        /**
         * クラス：NtfInfoRepositoryImpl selectAllByInternalUserId
         * 正常にmapperの結果が返ることを確認するテストケース
         */
        @Test
        void selectAllByInternalUserId_001() {
                // Arrange
                NtfInfoRepositoryImpl sut = new NtfInfoRepositoryImpl(ntfInfoMapper, propertiesUtil);
                List<NtfInfoEntity> list = List.of(mock(NtfInfoEntity.class));
                when(ntfInfoMapper.selectAllByInternalUserId("u")).thenReturn(list);

                // Act
                List<NtfInfoEntity> actual = sut.selectAllByInternalUserId("u");

                // Assert
                assertSame(list, actual);
        }

        /**
         * クラス：NtfInfoRepositoryImpl selectAllByInternalUserId
         * mapper例外がそのまま再送出されることを確認するテストケース
         */
        @Test
        void selectAllByInternalUserId_002() {
                // Arrange
                NtfInfoRepositoryImpl sut = new NtfInfoRepositoryImpl(ntfInfoMapper, propertiesUtil);
                RuntimeException cause = new RuntimeException("boom");
                when(ntfInfoMapper.selectAllByInternalUserId("u")).thenThrow(cause);

                // Act
                RuntimeException ex = assertThrows(RuntimeException.class, () -> sut.selectAllByInternalUserId("u"));

                // Assert
                assertSame(cause, ex);
        }

        /** クラス：NtfInfoRepositoryImpl shouldThrow SQL例外が見つからない場合にtrueとなることを確認するテストケース */
        @Test
        void shouldThrow_001() throws Exception {
                // Arrange
                NtfInfoRepositoryImpl sut = new NtfInfoRepositoryImpl(ntfInfoMapper, propertiesUtil);
                Method m = NtfInfoRepositoryImpl.class.getDeclaredMethod("shouldThrow", Exception.class, int.class,
                                int.class);
                m.setAccessible(true);

                // Act
                boolean actual = (boolean) m.invoke(sut, new Exception("x"), 0, 3);

                // Assert
                assertTrue(actual);
        }

        /**
         * クラス：NtfInfoRepositoryImpl shouldThrow
         * 非TransientなSQL例外の場合にtrueとなることを確認するテストケース
         */
        @Test
        void shouldThrow_002() throws Exception {
                // Arrange
                NtfInfoRepositoryImpl sut = new NtfInfoRepositoryImpl(ntfInfoMapper, propertiesUtil);
                Method m = NtfInfoRepositoryImpl.class.getDeclaredMethod("shouldThrow", Exception.class, int.class,
                                int.class);
                m.setAccessible(true);

                Exception e = new Exception(new SQLNonTransientException("x"));

                // Act
                boolean actual = (boolean) m.invoke(sut, e, 0, 3);

                // Assert
                assertTrue(actual);
        }

        /**
         * クラス：NtfInfoRepositoryImpl shouldThrow Transientで回数未満の場合にfalseとなることを確認するテストケース
         */
        @Test
        void shouldThrow_003() throws Exception {
                // Arrange
                NtfInfoRepositoryImpl sut = new NtfInfoRepositoryImpl(ntfInfoMapper, propertiesUtil);
                Method m = NtfInfoRepositoryImpl.class.getDeclaredMethod("shouldThrow", Exception.class, int.class,
                                int.class);
                m.setAccessible(true);

                Exception e = new Exception(new SQLTransientException("x"));

                // Act
                boolean actual = (boolean) m.invoke(sut, e, 0, 3);

                // Assert
                assertFalse(actual);
        }

        /**
         * クラス：NtfInfoRepositoryImpl shouldThrow Transientで回数超過の場合にtrueとなることを確認するテストケース
         */
        @Test
        void shouldThrow_004() throws Exception {
                // Arrange
                NtfInfoRepositoryImpl sut = new NtfInfoRepositoryImpl(ntfInfoMapper, propertiesUtil);
                Method m = NtfInfoRepositoryImpl.class.getDeclaredMethod("shouldThrow", Exception.class, int.class,
                                int.class);
                m.setAccessible(true);

                Exception e = new Exception(new SQLTransientException("x"));

                // Act
                boolean actual = (boolean) m.invoke(sut, e, 1, 1);

                // Assert
                assertTrue(actual);
        }

        /**
         * クラス：NtfInfoRepositoryImpl toCustom
         * causeにSQLExceptionがある場合にSQLExceptionが原因として採用されることを確認するテストケース
         */
        @Test
        void toCustom_001() throws Exception {
                // Arrange
                NtfInfoRepositoryImpl sut = new NtfInfoRepositoryImpl(ntfInfoMapper, propertiesUtil);
                Method m = NtfInfoRepositoryImpl.class.getDeclaredMethod("toCustom", Exception.class);
                m.setAccessible(true);

                SQLException sql = new SQLException("sql");
                Exception e = new Exception(sql);

                // Act
                CustomException ex = (CustomException) m.invoke(sut, e);

                // Assert
                assertSame(sql, ex.getCause());
        }

        /** クラス：NtfInfoRepositoryImpl toCustom SQL例外がない場合に元例外が原因として採用されることを確認するテストケース */
        @Test
        void toCustom_002() throws Exception {
                // Arrange
                NtfInfoRepositoryImpl sut = new NtfInfoRepositoryImpl(ntfInfoMapper, propertiesUtil);
                Method m = NtfInfoRepositoryImpl.class.getDeclaredMethod("toCustom", Exception.class);
                m.setAccessible(true);

                Exception e = new Exception("x");

                // Act
                CustomException ex = (CustomException) m.invoke(sut, e);

                // Assert
                assertSame(e, ex.getCause());
        }

        /**
         * クラス：NtfInfoRepositoryImpl backoffMillis
         * 計算結果がmaxIntervalで上限クリップされることを確認するテストケース
         */
        @Test
        void backoffMillis_001() throws Exception {
                // Arrange
                when(propertiesUtil.getNtfinfoUpsertRetryBaseInterval()).thenReturn(10);
                when(propertiesUtil.getNtfinfoUpsertRetryMaxInterval()).thenReturn(15);

                NtfInfoRepositoryImpl sut = new NtfInfoRepositoryImpl(ntfInfoMapper, propertiesUtil);
                Method m = NtfInfoRepositoryImpl.class.getDeclaredMethod("backoffMillis", int.class);
                m.setAccessible(true);

                // Act
                long actual = (long) m.invoke(sut, 2);

                // Assert
                assertEquals(15L, actual);
        }

        /**
         * クラス：NtfInfoRepositoryImpl backoffMillis maxInterval未満の場合に計算値が返ることを確認するテストケース
         */
        @Test
        void backoffMillis_002() throws Exception {
                // Arrange
                when(propertiesUtil.getNtfinfoUpsertRetryBaseInterval()).thenReturn(10);
                when(propertiesUtil.getNtfinfoUpsertRetryMaxInterval()).thenReturn(1000);

                NtfInfoRepositoryImpl sut = new NtfInfoRepositoryImpl(ntfInfoMapper, propertiesUtil);
                Method m = NtfInfoRepositoryImpl.class.getDeclaredMethod("backoffMillis", int.class);
                m.setAccessible(true);

                // Act
                long actual = (long) m.invoke(sut, 3);

                // Assert
                assertEquals(40L, actual);
        }

        /** クラス：NtfInfoRepositoryImpl sleep 割り込み時にinterruptフラグが立つことを確認するテストケース */
        @Test
        void sleep_001() throws Exception {
                // Arrange
                NtfInfoRepositoryImpl sut = new NtfInfoRepositoryImpl(ntfInfoMapper, propertiesUtil);
                Method m = NtfInfoRepositoryImpl.class.getDeclaredMethod("sleep", long.class);
                m.setAccessible(true);

                Thread.currentThread().interrupt();

                // Act
                m.invoke(sut, 1L);

                // Assert
                assertTrue(Thread.currentThread().isInterrupted());

                // ★ 副作用除去：interruptフラグをクリア（以降のテストに影響を出さない）
                Thread.interrupted();
        }

        /**
         * クラス：NtfInfoRepositoryImpl executeWithRetry actionが即成功する場合に結果が返ることを確認するテストケース
         */
        @Test
        void executeWithRetry_001() throws Exception {
                // Arrange
                NtfInfoRepositoryImpl sut = new NtfInfoRepositoryImpl(ntfInfoMapper, propertiesUtil);
                Method m = NtfInfoRepositoryImpl.class.getDeclaredMethod("executeWithRetry", Callable.class, int.class);
                m.setAccessible(true);

                Callable<Integer> action = () -> 7;

                // Act
                int actual = (int) m.invoke(sut, action, 3);

                // Assert
                assertEquals(7, actual);
        }
}
