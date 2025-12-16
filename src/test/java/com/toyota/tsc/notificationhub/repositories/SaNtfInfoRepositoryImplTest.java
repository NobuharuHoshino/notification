
// ファイルパス: src/test/java/com/toyota/tsc/notificationhub/repositories/SaNtfInfoRepositoryImplTest.java
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
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * SaNtfInfoRepositoryImpl のテストクラス
 */
@SuppressWarnings("all")
@ExtendWith(MockitoExtension.class)
class SaNtfInfoRepositoryImplTest {

        @Mock
        private SaNtfInfoMapper saNtfInfoMapper;

        @Mock
        private PropertiesUtil propertiesUtil;

        /** クラス：SaNtfInfoRepositoryImpl insert mapperの戻り値が返ることを確認するテストケース */
        @Test
        void insert_001() {
                // Arrange
                SaNtfInfoRepositoryImpl sut = new SaNtfInfoRepositoryImpl(saNtfInfoMapper, propertiesUtil);
                SaNtfInfoEntity entity = mock(SaNtfInfoEntity.class);
                when(saNtfInfoMapper.insert(entity)).thenReturn(1);

                // Act
                int actual = sut.insert(entity);

                // Assert
                assertEquals(1, actual);
                verify(saNtfInfoMapper, times(1)).insert(entity);
        }

        /** クラス：SaNtfInfoRepositoryImpl update mapperの戻り値が返ることを確認するテストケース */
        @Test
        void update_001() {
                // Arrange
                SaNtfInfoRepositoryImpl sut = new SaNtfInfoRepositoryImpl(saNtfInfoMapper, propertiesUtil);
                SaNtfInfoEntity entity = mock(SaNtfInfoEntity.class);
                when(saNtfInfoMapper.update(entity)).thenReturn(2);

                // Act
                int actual = sut.update(entity);

                // Assert
                assertEquals(2, actual);
                verify(saNtfInfoMapper, times(1)).update(entity);
        }

        /** クラス：SaNtfInfoRepositoryImpl select mapperの戻り値が返ることを確認するテストケース */
        @Test
        void select_001() {
                // Arrange
                SaNtfInfoRepositoryImpl sut = new SaNtfInfoRepositoryImpl(saNtfInfoMapper, propertiesUtil);
                SaNtfInfoEntity entity = mock(SaNtfInfoEntity.class);
                when(saNtfInfoMapper.select("u")).thenReturn(entity);

                // Act
                SaNtfInfoEntity actual = sut.select("u");

                // Assert
                assertSame(entity, actual);
                verify(saNtfInfoMapper, times(1)).select("u");
        }

        /**
         * クラス：SaNtfInfoRepositoryImpl delete
         * Transientが1回失敗後に成功する場合にリトライされることを確認するテストケース
         */
        @Test
        void delete_001() {
                // Arrange
                when(propertiesUtil.getRetryCount()).thenReturn(3);
                when(propertiesUtil.getNtfinfoUpsertRetryBaseInterval()).thenReturn(0);
                when(propertiesUtil.getNtfinfoUpsertRetryMaxInterval()).thenReturn(0);

                when(saNtfInfoMapper.delete("u"))
                                .thenThrow(new RuntimeException(new SQLTransientException("transient")))
                                .thenReturn(1);

                SaNtfInfoRepositoryImpl sut = new SaNtfInfoRepositoryImpl(saNtfInfoMapper, propertiesUtil);

                // Act
                int actual = sut.delete("u");

                // Assert
                assertEquals(1, actual);
                verify(saNtfInfoMapper, times(2)).delete("u");
        }

        /**
         * クラス：SaNtfInfoRepositoryImpl delete
         * 非SQL例外の場合にCustomExceptionが送出されることを確認するテストケース
         */
        @Test
        void delete_002() {
                // Arrange
                when(propertiesUtil.getRetryCount()).thenReturn(3);
                when(saNtfInfoMapper.delete("u")).thenThrow(new RuntimeException("boom"));

                SaNtfInfoRepositoryImpl sut = new SaNtfInfoRepositoryImpl(saNtfInfoMapper, propertiesUtil);

                // Act
                CustomException ex = assertThrows(CustomException.class, () -> sut.delete("u"));

                // Assert
                assertNotNull(ex.getCause());
        }

        /**
         * クラス：SaNtfInfoRepositoryImpl delete
         * 非TransientなSQL例外の場合にCustomExceptionが送出されることを確認するテストケース
         */
        @Test
        void delete_003() {
                // Arrange
                when(propertiesUtil.getRetryCount()).thenReturn(3);
                when(saNtfInfoMapper.delete("u"))
                                .thenThrow(new RuntimeException(new SQLNonTransientException("non-transient")));

                SaNtfInfoRepositoryImpl sut = new SaNtfInfoRepositoryImpl(saNtfInfoMapper, propertiesUtil);

                // Act
                CustomException ex = assertThrows(CustomException.class, () -> sut.delete("u"));

                // Assert
                assertNotNull(ex.getCause());
        }

        /**
         * クラス：SaNtfInfoRepositoryImpl delete
         * Transientがリトライ回数超過した場合にCustomExceptionが送出されることを確認するテストケース
         */
        @Test
        void delete_004() {
                // Arrange
                when(propertiesUtil.getRetryCount()).thenReturn(1);
                when(propertiesUtil.getNtfinfoUpsertRetryBaseInterval()).thenReturn(0);
                when(propertiesUtil.getNtfinfoUpsertRetryMaxInterval()).thenReturn(0);

                when(saNtfInfoMapper.delete("u"))
                                .thenThrow(new RuntimeException(new SQLTransientException("transient")));

                SaNtfInfoRepositoryImpl sut = new SaNtfInfoRepositoryImpl(saNtfInfoMapper, propertiesUtil);

                // Act
                CustomException ex = assertThrows(CustomException.class, () -> sut.delete("u"));

                // Assert
                assertNotNull(ex.getCause());
        }

        /**
         * クラス：SaNtfInfoRepositoryImpl upsert
         * Transientが1回失敗後に成功する場合にリトライされることを確認するテストケース
         */
        @Test
        void upsert_001() {
                // Arrange
                when(propertiesUtil.getRetryCount()).thenReturn(3);
                when(propertiesUtil.getNtfinfoUpsertRetryBaseInterval()).thenReturn(0);
                when(propertiesUtil.getNtfinfoUpsertRetryMaxInterval()).thenReturn(0);

                SaNtfInfoEntity entity = mock(SaNtfInfoEntity.class);
                when(saNtfInfoMapper.upsert(entity))
                                .thenThrow(new RuntimeException(new SQLTransientException("transient")))
                                .thenReturn(1);

                SaNtfInfoRepositoryImpl sut = new SaNtfInfoRepositoryImpl(saNtfInfoMapper, propertiesUtil);

                // Act
                int actual = sut.upsert(entity);

                // Assert
                assertEquals(1, actual);
                verify(saNtfInfoMapper, times(2)).upsert(entity);
        }

        /**
         * クラス：SaNtfInfoRepositoryImpl upsert
         * 非TransientなSQL例外の場合にCustomExceptionが送出されることを確認するテストケース
         */
        @Test
        void upsert_002() {
                // Arrange
                when(propertiesUtil.getRetryCount()).thenReturn(3);
                SaNtfInfoEntity entity = mock(SaNtfInfoEntity.class);
                when(saNtfInfoMapper.upsert(entity))
                                .thenThrow(new RuntimeException(new SQLNonTransientException("non-transient")));

                SaNtfInfoRepositoryImpl sut = new SaNtfInfoRepositoryImpl(saNtfInfoMapper, propertiesUtil);

                // Act
                CustomException ex = assertThrows(CustomException.class, () -> sut.upsert(entity));

                // Assert
                assertNotNull(ex.getCause());
        }

        // -------- private method tests (Reflection) --------

        /**
         * クラス：SaNtfInfoRepositoryImpl shouldThrow SQL例外が見つからない場合にtrueとなることを確認するテストケース
         */
        @Test
        void shouldThrow_001() throws Exception {
                // Arrange
                SaNtfInfoRepositoryImpl sut = new SaNtfInfoRepositoryImpl(saNtfInfoMapper, propertiesUtil);
                Method m = SaNtfInfoRepositoryImpl.class.getDeclaredMethod("shouldThrow", Exception.class, int.class,
                                int.class);
                m.setAccessible(true);

                // Act
                boolean actual = (boolean) m.invoke(sut, new Exception("x"), 0, 3);

                // Assert
                assertTrue(actual);
        }

        /**
         * クラス：SaNtfInfoRepositoryImpl shouldThrow
         * 非TransientなSQL例外の場合にtrueとなることを確認するテストケース
         */
        @Test
        void shouldThrow_002() throws Exception {
                // Arrange
                SaNtfInfoRepositoryImpl sut = new SaNtfInfoRepositoryImpl(saNtfInfoMapper, propertiesUtil);
                Method m = SaNtfInfoRepositoryImpl.class.getDeclaredMethod("shouldThrow", Exception.class, int.class,
                                int.class);
                m.setAccessible(true);

                Exception e = new Exception(new SQLNonTransientException("x"));

                // Act
                boolean actual = (boolean) m.invoke(sut, e, 0, 3);

                // Assert
                assertTrue(actual);
        }

        /**
         * クラス：SaNtfInfoRepositoryImpl shouldThrow
         * Transientで回数未満の場合にfalseとなることを確認するテストケース
         */
        @Test
        void shouldThrow_003() throws Exception {
                // Arrange
                SaNtfInfoRepositoryImpl sut = new SaNtfInfoRepositoryImpl(saNtfInfoMapper, propertiesUtil);
                Method m = SaNtfInfoRepositoryImpl.class.getDeclaredMethod("shouldThrow", Exception.class, int.class,
                                int.class);
                m.setAccessible(true);

                Exception e = new Exception(new SQLTransientException("x"));

                // Act
                boolean actual = (boolean) m.invoke(sut, e, 0, 3);

                // Assert
                assertFalse(actual);
        }

        /**
         * クラス：SaNtfInfoRepositoryImpl shouldThrow
         * Transientで回数超過の場合にtrueとなることを確認するテストケース
         */
        @Test
        void shouldThrow_004() throws Exception {
                // Arrange
                SaNtfInfoRepositoryImpl sut = new SaNtfInfoRepositoryImpl(saNtfInfoMapper, propertiesUtil);
                Method m = SaNtfInfoRepositoryImpl.class.getDeclaredMethod("shouldThrow", Exception.class, int.class,
                                int.class);
                m.setAccessible(true);

                Exception e = new Exception(new SQLTransientException("x"));

                // Act
                boolean actual = (boolean) m.invoke(sut, e, 1, 1);

                // Assert
                assertTrue(actual);
        }

        /**
         * クラス：SaNtfInfoRepositoryImpl toCustom
         * causeにSQLExceptionがある場合にSQLExceptionが原因として採用されることを確認するテストケース
         */
        @Test
        void toCustom_001() throws Exception {
                // Arrange
                SaNtfInfoRepositoryImpl sut = new SaNtfInfoRepositoryImpl(saNtfInfoMapper, propertiesUtil);
                Method m = SaNtfInfoRepositoryImpl.class.getDeclaredMethod("toCustom", Exception.class);
                m.setAccessible(true);

                SQLException sql = new SQLException("sql");
                Exception e = new Exception(sql);

                // Act
                CustomException ex = (CustomException) m.invoke(sut, e);

                // Assert
                assertSame(sql, ex.getCause());
        }

        /**
         * クラス：SaNtfInfoRepositoryImpl toCustom SQL例外がない場合に元例外が原因として採用されることを確認するテストケース
         */
        @Test
        void toCustom_002() throws Exception {
                // Arrange
                SaNtfInfoRepositoryImpl sut = new SaNtfInfoRepositoryImpl(saNtfInfoMapper, propertiesUtil);
                Method m = SaNtfInfoRepositoryImpl.class.getDeclaredMethod("toCustom", Exception.class);
                m.setAccessible(true);

                Exception e = new Exception("x");

                // Act
                CustomException ex = (CustomException) m.invoke(sut, e);

                // Assert
                assertSame(e, ex.getCause());
        }

        /**
         * クラス：SaNtfInfoRepositoryImpl backoffMillis
         * 計算結果がmaxIntervalで上限クリップされることを確認するテストケース
         */
        @Test
        void backoffMillis_001() throws Exception {
                // Arrange
                when(propertiesUtil.getNtfinfoUpsertRetryBaseInterval()).thenReturn(10);
                when(propertiesUtil.getNtfinfoUpsertRetryMaxInterval()).thenReturn(15);

                SaNtfInfoRepositoryImpl sut = new SaNtfInfoRepositoryImpl(saNtfInfoMapper, propertiesUtil);
                Method m = SaNtfInfoRepositoryImpl.class.getDeclaredMethod("backoffMillis", int.class);
                m.setAccessible(true);

                // Act
                long actual = (long) m.invoke(sut, 2);

                // Assert
                assertEquals(15L, actual);
        }

        /**
         * クラス：SaNtfInfoRepositoryImpl backoffMillis
         * maxInterval未満の場合に計算値が返ることを確認するテストケース
         */
        @Test
        void backoffMillis_002() throws Exception {
                // Arrange
                when(propertiesUtil.getNtfinfoUpsertRetryBaseInterval()).thenReturn(10);
                when(propertiesUtil.getNtfinfoUpsertRetryMaxInterval()).thenReturn(1000);

                SaNtfInfoRepositoryImpl sut = new SaNtfInfoRepositoryImpl(saNtfInfoMapper, propertiesUtil);
                Method m = SaNtfInfoRepositoryImpl.class.getDeclaredMethod("backoffMillis", int.class);
                m.setAccessible(true);

                // Act
                long actual = (long) m.invoke(sut, 3);

                // Assert
                assertEquals(40L, actual);
        }

        /**
         * クラス：SaNtfInfoRepositoryImpl sleep 割り込み時にinterruptフラグが立つことを確認し、最後にクリアするテストケース
         */
        @Test
        void sleep_001() throws Exception {
                // Arrange
                SaNtfInfoRepositoryImpl sut = new SaNtfInfoRepositoryImpl(saNtfInfoMapper, propertiesUtil);
                Method m = SaNtfInfoRepositoryImpl.class.getDeclaredMethod("sleep", long.class);
                m.setAccessible(true);

                Thread.currentThread().interrupt();

                // Act
                m.invoke(sut, 1L);

                // Assert
                assertTrue(Thread.currentThread().isInterrupted());

                // 副作用除去（次テストへ割り込みを残さない）
                Thread.interrupted();
        }

        /**
         * クラス：SaNtfInfoRepositoryImpl executeWithRetry
         * actionが即成功する場合に結果が返ることを確認するテストケース
         */
        @Test
        void executeWithRetry_001() throws Exception {
                // Arrange
                SaNtfInfoRepositoryImpl sut = new SaNtfInfoRepositoryImpl(saNtfInfoMapper, propertiesUtil);
                Method m = SaNtfInfoRepositoryImpl.class.getDeclaredMethod("executeWithRetry", Callable.class,
                                int.class);
                m.setAccessible(true);

                Callable<Integer> action = () -> 7;

                // Act
                int actual = (int) m.invoke(sut, action, 3);

                // Assert
                assertEquals(7, actual);
        }
}
