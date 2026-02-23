package com.toyota.tsc.notificationhub.repositories;

import com.toyota.tsc.notificationhub.commons.PropertiesUtil;
import com.toyota.tsc.notificationhub.exceptions.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLNonTransientException;
import java.sql.SQLTransientException;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * NtfBatchExecHistoryRepositoryImpl のテストクラス
 */
@SuppressWarnings("all")
@ExtendWith(MockitoExtension.class)
class NtfBatchExecHistoryRepositoryImplTest {

    @Mock
    private NtfBatchExecHistoryMapper ntfBatchExecHistoryMapper;

    @Mock
    private PropertiesUtil propertiesUtil;

    /** クラス：NtfBatchExecHistoryRepositoryImpl mapperのinsertが呼ばれ戻り値が返ることを確認するテストケース */
    @Test
    void insert_001() {
        // Arrange
        NtfBatchExecHistoryRepositoryImpl sut = new NtfBatchExecHistoryRepositoryImpl(ntfBatchExecHistoryMapper, propertiesUtil);
        NtfBatchExecHistoryEntity entity = mock(NtfBatchExecHistoryEntity.class);
        when(ntfBatchExecHistoryMapper.insert(entity)).thenReturn(1);

        // Act
        int actual = sut.insert(entity);

        // Assert
        assertEquals(1, actual);
        verify(ntfBatchExecHistoryMapper, times(1)).insert(entity);
    }

    /** クラス：NtfBatchExecHistoryRepositoryImpl 正常にupdateStatusが実行されることを確認するテストケース */
    @Test
    void updateStatus_001() {
        // Arrange
        NtfBatchExecHistoryRepositoryImpl sut = new NtfBatchExecHistoryRepositoryImpl(ntfBatchExecHistoryMapper, propertiesUtil);
        when(propertiesUtil.getNtfinfoUpsertRetryCount()).thenReturn(3);
        when(ntfBatchExecHistoryMapper.updateStatus(1, 2, "1")).thenReturn(1);

        // Act
        int actual = sut.updateStatus(1, 2, "1");

        // Assert
        assertEquals(1, actual);
        verify(ntfBatchExecHistoryMapper, times(1)).updateStatus(1, 2, "1");
    }

    /** クラス：NtfBatchExecHistoryRepositoryImpl SQLTransientException発生時にリトライして成功することを確認するテストケース */
    @Test
    void updateStatus_002() {
        // Arrange
        NtfBatchExecHistoryRepositoryImpl sut = new NtfBatchExecHistoryRepositoryImpl(ntfBatchExecHistoryMapper, propertiesUtil);
        when(propertiesUtil.getNtfinfoUpsertRetryCount()).thenReturn(3);
        when(propertiesUtil.getNtfinfoUpsertRetryBaseInterval()).thenReturn(1);
        when(propertiesUtil.getNtfinfoUpsertRetryMaxInterval()).thenReturn(100);
        when(ntfBatchExecHistoryMapper.updateStatus(1, 2, "1"))
                .thenThrow(new RuntimeException(new SQLTransientException("transient")))
                .thenReturn(1);

        // Act
        int actual = sut.updateStatus(1, 2, "1");

        // Assert
        assertEquals(1, actual);
        verify(ntfBatchExecHistoryMapper, times(2)).updateStatus(1, 2, "1");
    }

    /** クラス：NtfBatchExecHistoryRepositoryImpl SQLTransientExceptionでリトライ上限を超えCustomExceptionが投げられることを確認するテストケース */
    @Test
    void updateStatus_003() {
        // Arrange
        NtfBatchExecHistoryRepositoryImpl sut = new NtfBatchExecHistoryRepositoryImpl(ntfBatchExecHistoryMapper, propertiesUtil);
        when(propertiesUtil.getNtfinfoUpsertRetryCount()).thenReturn(0);
        when(ntfBatchExecHistoryMapper.updateStatus(1, 2, "1")).thenThrow(new RuntimeException(new SQLTransientException("transient")));

        // Act & Assert
        assertThrows(CustomException.class, () -> sut.updateStatus(1, 2, "1"));
    }

    /** クラス：NtfBatchExecHistoryRepositoryImpl 非SQL例外発生時にCustomExceptionが投げられることを確認するテストケース */
    @Test
    void updateStatus_004() {
        // Arrange
        NtfBatchExecHistoryRepositoryImpl sut = new NtfBatchExecHistoryRepositoryImpl(ntfBatchExecHistoryMapper, propertiesUtil);
        when(propertiesUtil.getNtfinfoUpsertRetryCount()).thenReturn(3);
        when(ntfBatchExecHistoryMapper.updateStatus(1, 2, "1")).thenThrow(new RuntimeException("runtime"));

        // Act & Assert
        assertThrows(CustomException.class, () -> sut.updateStatus(1, 2, "1"));
    }

    /** クラス：NtfBatchExecHistoryRepositoryImpl SQLNonTransientException発生時にCustomExceptionが投げられることを確認するテストケース */
    @Test
    void updateStatus_005() {
        // Arrange
        NtfBatchExecHistoryRepositoryImpl sut = new NtfBatchExecHistoryRepositoryImpl(ntfBatchExecHistoryMapper, propertiesUtil);
        when(propertiesUtil.getNtfinfoUpsertRetryCount()).thenReturn(3);
        when(ntfBatchExecHistoryMapper.updateStatus(1, 2, "1")).thenThrow(new RuntimeException(new SQLNonTransientException("non-transient")));

        // Act & Assert
        assertThrows(CustomException.class, () -> sut.updateStatus(1, 2, "1"));
    }

    /** クラス：NtfBatchExecHistoryRepositoryImpl backoffMillis baseがmaxInterval以下の場合baseが返ることを確認するテストケース */
    @Test
    void backoffMillis_001() throws Exception {
        // Arrange
        NtfBatchExecHistoryRepositoryImpl sut = new NtfBatchExecHistoryRepositoryImpl(ntfBatchExecHistoryMapper, propertiesUtil);
        when(propertiesUtil.getNtfinfoUpsertRetryBaseInterval()).thenReturn(1);
        when(propertiesUtil.getNtfinfoUpsertRetryMaxInterval()).thenReturn(10000);
        Method method = NtfBatchExecHistoryRepositoryImpl.class.getDeclaredMethod("backoffMillis", int.class);
        method.setAccessible(true);

        // Act
        long actual = (long) method.invoke(sut, 1);

        // Assert
        assertEquals(1L, actual);
    }

    /** クラス：NtfBatchExecHistoryRepositoryImpl backoffMillis baseがmaxIntervalを超える場合maxIntervalが返ることを確認するテストケース */
    @Test
    void backoffMillis_002() throws Exception {
        // Arrange
        NtfBatchExecHistoryRepositoryImpl sut = new NtfBatchExecHistoryRepositoryImpl(ntfBatchExecHistoryMapper, propertiesUtil);
        when(propertiesUtil.getNtfinfoUpsertRetryBaseInterval()).thenReturn(1000);
        when(propertiesUtil.getNtfinfoUpsertRetryMaxInterval()).thenReturn(100);
        Method method = NtfBatchExecHistoryRepositoryImpl.class.getDeclaredMethod("backoffMillis", int.class);
        method.setAccessible(true);

        // Act
        long actual = (long) method.invoke(sut, 10);

        // Assert
        assertEquals(100L, actual);
    }

    /** クラス：NtfBatchExecHistoryRepositoryImpl sleep InterruptedExceptionが発生した場合スレッドに割り込みフラグが立てられることを確認するテストケース */
    @Test
    void sleep_001() throws Exception {
        // Arrange
        NtfBatchExecHistoryRepositoryImpl sut = new NtfBatchExecHistoryRepositoryImpl(ntfBatchExecHistoryMapper, propertiesUtil);
        Method method = NtfBatchExecHistoryRepositoryImpl.class.getDeclaredMethod("sleep", long.class);
        method.setAccessible(true);
        Thread.currentThread().interrupt();

        // Act
        method.invoke(sut, 0L);

        // Assert
        assertTrue(Thread.currentThread().isInterrupted());
        Thread.interrupted(); // clear interrupt flag
    }

    /** クラス：NtfBatchExecHistoryRepositoryImpl sleep 正常にスリープが実行されることを確認するテストケース */
    @Test
    void sleep_002() throws Exception {
        // Arrange
        NtfBatchExecHistoryRepositoryImpl sut = new NtfBatchExecHistoryRepositoryImpl(ntfBatchExecHistoryMapper, propertiesUtil);
        Method method = NtfBatchExecHistoryRepositoryImpl.class.getDeclaredMethod("sleep", long.class);
        method.setAccessible(true);

        // Act & Assert
        assertDoesNotThrow(() -> method.invoke(sut, 1L));
    }

    /** クラス：NtfBatchExecHistoryRepositoryImpl shouldThrow sqlExがnullの場合trueを返すことを確認するテストケース */
    @Test
    void shouldThrow_001() throws Exception {
        // Arrange
        NtfBatchExecHistoryRepositoryImpl sut = new NtfBatchExecHistoryRepositoryImpl(ntfBatchExecHistoryMapper, propertiesUtil);
        Method method = NtfBatchExecHistoryRepositoryImpl.class.getDeclaredMethod("shouldThrow", Exception.class, int.class, int.class);
        method.setAccessible(true);
        RuntimeException nonSqlEx = new RuntimeException("not sql");

        // Act
        boolean actual = (boolean) method.invoke(sut, nonSqlEx, 0, 3);

        // Assert
        assertTrue(actual);
    }

    /** クラス：NtfBatchExecHistoryRepositoryImpl shouldThrow sqlExがSQLNonTransientの場合trueを返すことを確認するテストケース */
    @Test
    void shouldThrow_002() throws Exception {
        // Arrange
        NtfBatchExecHistoryRepositoryImpl sut = new NtfBatchExecHistoryRepositoryImpl(ntfBatchExecHistoryMapper, propertiesUtil);
        Method method = NtfBatchExecHistoryRepositoryImpl.class.getDeclaredMethod("shouldThrow", Exception.class, int.class, int.class);
        method.setAccessible(true);
        SQLNonTransientException nonTransientEx = new SQLNonTransientException("non-transient");

        // Act
        boolean actual = (boolean) method.invoke(sut, nonTransientEx, 0, 3);

        // Assert
        assertTrue(actual);
    }

    /** クラス：NtfBatchExecHistoryRepositoryImpl shouldThrow SQLTransientかつretry>=retryCountの場合trueを返すことを確認するテストケース */
    @Test
    void shouldThrow_003() throws Exception {
        // Arrange
        NtfBatchExecHistoryRepositoryImpl sut = new NtfBatchExecHistoryRepositoryImpl(ntfBatchExecHistoryMapper, propertiesUtil);
        Method method = NtfBatchExecHistoryRepositoryImpl.class.getDeclaredMethod("shouldThrow", Exception.class, int.class, int.class);
        method.setAccessible(true);
        SQLTransientException transientEx = new SQLTransientException("transient");

        // Act
        boolean actual = (boolean) method.invoke(sut, transientEx, 3, 3);

        // Assert
        assertTrue(actual);
    }

    /** クラス：NtfBatchExecHistoryRepositoryImpl shouldThrow SQLTransientかつretry<retryCountの場合falseを返すことを確認するテストケース */
    @Test
    void shouldThrow_004() throws Exception {
        // Arrange
        NtfBatchExecHistoryRepositoryImpl sut = new NtfBatchExecHistoryRepositoryImpl(ntfBatchExecHistoryMapper, propertiesUtil);
        Method method = NtfBatchExecHistoryRepositoryImpl.class.getDeclaredMethod("shouldThrow", Exception.class, int.class, int.class);
        method.setAccessible(true);
        SQLTransientException transientEx = new SQLTransientException("transient");

        // Act
        boolean actual = (boolean) method.invoke(sut, transientEx, 0, 3);

        // Assert
        assertFalse(actual);
    }

    /** クラス：NtfBatchExecHistoryRepositoryImpl toCustom sqlExがnullでない場合sqlExをラップしたCustomExceptionが返ることを確認するテストケース */
    @Test
    void toCustom_001() throws Exception {
        // Arrange
        NtfBatchExecHistoryRepositoryImpl sut = new NtfBatchExecHistoryRepositoryImpl(ntfBatchExecHistoryMapper, propertiesUtil);
        Method method = NtfBatchExecHistoryRepositoryImpl.class.getDeclaredMethod("toCustom", Exception.class);
        method.setAccessible(true);
        SQLTransientException sqlEx = new SQLTransientException("sql");

        // Act
        CustomException actual = (CustomException) method.invoke(sut, sqlEx);

        // Assert
        assertNotNull(actual);
    }

    /** クラス：NtfBatchExecHistoryRepositoryImpl toCustom sqlExがnullの場合元の例外をラップしたCustomExceptionが返ることを確認するテストケース */
    @Test
    void toCustom_002() throws Exception {
        // Arrange
        NtfBatchExecHistoryRepositoryImpl sut = new NtfBatchExecHistoryRepositoryImpl(ntfBatchExecHistoryMapper, propertiesUtil);
        Method method = NtfBatchExecHistoryRepositoryImpl.class.getDeclaredMethod("toCustom", Exception.class);
        method.setAccessible(true);
        RuntimeException ex = new RuntimeException("runtime");

        // Act
        CustomException actual = (CustomException) method.invoke(sut, ex);

        // Assert
        assertNotNull(actual);
    }
}
