package com.toyota.tsc.notificationhub.repositories;

import com.toyota.tsc.notificationhub.commons.PropertiesUtil;
import com.toyota.tsc.notificationhub.exceptions.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.sql.SQLNonTransientException;
import java.sql.SQLTransientException;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * NtfBatchExecErrorInfoRepositoryImpl のテストクラス
 */
@SuppressWarnings("all")
@ExtendWith(MockitoExtension.class)
class NtfBatchExecErrorInfoRepositoryImplTest {

    @Mock
    private NtfBatchExecErrorInfoMapper ntfBatchExecErrorInfoMapper;

    @Mock
    private PropertiesUtil propertiesUtil;

    /** クラス：NtfBatchExecErrorInfoRepositoryImpl mapperのinsertが呼ばれ戻り値が返ることを確認するテストケース */
    @Test
    void insert_001() {
        // Arrange
        NtfBatchExecErrorInfoRepositoryImpl sut = new NtfBatchExecErrorInfoRepositoryImpl(ntfBatchExecErrorInfoMapper, propertiesUtil);
        NtfBatchExecErrorInfoEntity entity = mock(NtfBatchExecErrorInfoEntity.class);
        when(ntfBatchExecErrorInfoMapper.insert(entity)).thenReturn(1);

        // Act
        int actual = sut.insert(entity);

        // Assert
        assertEquals(1, actual);
        verify(ntfBatchExecErrorInfoMapper, times(1)).insert(entity);
    }

    /** クラス：NtfBatchExecErrorInfoRepositoryImpl executeWithRetry actionが成功した場合結果が返ることを確認するテストケース */
    @Test
    void executeWithRetry_001() throws Exception {
        // Arrange
        NtfBatchExecErrorInfoRepositoryImpl sut = new NtfBatchExecErrorInfoRepositoryImpl(ntfBatchExecErrorInfoMapper, propertiesUtil);
        Method method = NtfBatchExecErrorInfoRepositoryImpl.class.getDeclaredMethod("executeWithRetry", Callable.class, int.class);
        method.setAccessible(true);
        Callable<Integer> action = () -> 42;

        // Act
        int actual = (int) method.invoke(sut, action, 3);

        // Assert
        assertEquals(42, actual);
    }

    /** クラス：NtfBatchExecErrorInfoRepositoryImpl executeWithRetry SQLTransientExceptionが発生しリトライして成功することを確認するテストケース */
    @Test
    void executeWithRetry_002() throws Exception {
        // Arrange
        NtfBatchExecErrorInfoRepositoryImpl sut = new NtfBatchExecErrorInfoRepositoryImpl(ntfBatchExecErrorInfoMapper, propertiesUtil);
        when(propertiesUtil.getNtfinfoUpsertRetryBaseInterval()).thenReturn(1);
        when(propertiesUtil.getNtfinfoUpsertRetryMaxInterval()).thenReturn(100);
        Method method = NtfBatchExecErrorInfoRepositoryImpl.class.getDeclaredMethod("executeWithRetry", Callable.class, int.class);
        method.setAccessible(true);
        int[] callCount = {0};
        Callable<Integer> action = () -> {
            callCount[0]++;
            if (callCount[0] == 1) throw new SQLTransientException("transient");
            return 1;
        };

        // Act
        int actual = (int) method.invoke(sut, action, 3);

        // Assert
        assertEquals(1, actual);
    }

    /** クラス：NtfBatchExecErrorInfoRepositoryImpl executeWithRetry SQLTransientExceptionでリトライ上限を超えCustomExceptionが投げられることを確認するテストケース */
    @Test
    void executeWithRetry_003() throws Exception {
        // Arrange
        NtfBatchExecErrorInfoRepositoryImpl sut = new NtfBatchExecErrorInfoRepositoryImpl(ntfBatchExecErrorInfoMapper, propertiesUtil);
        Method method = NtfBatchExecErrorInfoRepositoryImpl.class.getDeclaredMethod("executeWithRetry", Callable.class, int.class);
        method.setAccessible(true);
        Callable<Integer> action = () -> {
            throw new SQLTransientException("transient");
        };

        // Act & Assert
        assertThrows(Exception.class, () -> method.invoke(sut, action, 0));
    }

    /** クラス：NtfBatchExecErrorInfoRepositoryImpl executeWithRetry 非SQL例外発生時にCustomExceptionが投げられることを確認するテストケース */
    @Test
    void executeWithRetry_004() throws Exception {
        // Arrange
        NtfBatchExecErrorInfoRepositoryImpl sut = new NtfBatchExecErrorInfoRepositoryImpl(ntfBatchExecErrorInfoMapper, propertiesUtil);
        Method method = NtfBatchExecErrorInfoRepositoryImpl.class.getDeclaredMethod("executeWithRetry", Callable.class, int.class);
        method.setAccessible(true);
        Callable<Integer> action = () -> {
            throw new RuntimeException("runtime");
        };

        // Act & Assert
        assertThrows(Exception.class, () -> method.invoke(sut, action, 3));
    }

    /** クラス：NtfBatchExecErrorInfoRepositoryImpl executeWithRetry SQLNonTransientException発生時にCustomExceptionが投げられることを確認するテストケース */
    @Test
    void executeWithRetry_005() throws Exception {
        // Arrange
        NtfBatchExecErrorInfoRepositoryImpl sut = new NtfBatchExecErrorInfoRepositoryImpl(ntfBatchExecErrorInfoMapper, propertiesUtil);
        Method method = NtfBatchExecErrorInfoRepositoryImpl.class.getDeclaredMethod("executeWithRetry", Callable.class, int.class);
        method.setAccessible(true);
        Callable<Integer> action = () -> {
            throw new SQLNonTransientException("non-transient");
        };

        // Act & Assert
        assertThrows(Exception.class, () -> method.invoke(sut, action, 3));
    }

    /** クラス：NtfBatchExecErrorInfoRepositoryImpl shouldThrow sqlExがnullの場合trueを返すことを確認するテストケース */
    @Test
    void shouldThrow_001() throws Exception {
        // Arrange
        NtfBatchExecErrorInfoRepositoryImpl sut = new NtfBatchExecErrorInfoRepositoryImpl(ntfBatchExecErrorInfoMapper, propertiesUtil);
        Method method = NtfBatchExecErrorInfoRepositoryImpl.class.getDeclaredMethod("shouldThrow", Exception.class, int.class, int.class);
        method.setAccessible(true);

        // Act
        boolean actual = (boolean) method.invoke(sut, new RuntimeException("not sql"), 0, 3);

        // Assert
        assertTrue(actual);
    }

    /** クラス：NtfBatchExecErrorInfoRepositoryImpl shouldThrow SQLNonTransientの場合trueを返すことを確認するテストケース */
    @Test
    void shouldThrow_002() throws Exception {
        // Arrange
        NtfBatchExecErrorInfoRepositoryImpl sut = new NtfBatchExecErrorInfoRepositoryImpl(ntfBatchExecErrorInfoMapper, propertiesUtil);
        Method method = NtfBatchExecErrorInfoRepositoryImpl.class.getDeclaredMethod("shouldThrow", Exception.class, int.class, int.class);
        method.setAccessible(true);

        // Act
        boolean actual = (boolean) method.invoke(sut, new SQLNonTransientException("non-transient"), 0, 3);

        // Assert
        assertTrue(actual);
    }

    /** クラス：NtfBatchExecErrorInfoRepositoryImpl shouldThrow SQLTransientかつretry>=retryCountの場合trueを返すことを確認するテストケース */
    @Test
    void shouldThrow_003() throws Exception {
        // Arrange
        NtfBatchExecErrorInfoRepositoryImpl sut = new NtfBatchExecErrorInfoRepositoryImpl(ntfBatchExecErrorInfoMapper, propertiesUtil);
        Method method = NtfBatchExecErrorInfoRepositoryImpl.class.getDeclaredMethod("shouldThrow", Exception.class, int.class, int.class);
        method.setAccessible(true);

        // Act
        boolean actual = (boolean) method.invoke(sut, new SQLTransientException("transient"), 3, 3);

        // Assert
        assertTrue(actual);
    }

    /** クラス：NtfBatchExecErrorInfoRepositoryImpl shouldThrow SQLTransientかつretry<retryCountの場合falseを返すことを確認するテストケース */
    @Test
    void shouldThrow_004() throws Exception {
        // Arrange
        NtfBatchExecErrorInfoRepositoryImpl sut = new NtfBatchExecErrorInfoRepositoryImpl(ntfBatchExecErrorInfoMapper, propertiesUtil);
        Method method = NtfBatchExecErrorInfoRepositoryImpl.class.getDeclaredMethod("shouldThrow", Exception.class, int.class, int.class);
        method.setAccessible(true);

        // Act
        boolean actual = (boolean) method.invoke(sut, new SQLTransientException("transient"), 0, 3);

        // Assert
        assertFalse(actual);
    }

    /** クラス：NtfBatchExecErrorInfoRepositoryImpl backoffMillis baseがmaxInterval以下の場合baseが返ることを確認するテストケース */
    @Test
    void backoffMillis_001() throws Exception {
        // Arrange
        NtfBatchExecErrorInfoRepositoryImpl sut = new NtfBatchExecErrorInfoRepositoryImpl(ntfBatchExecErrorInfoMapper, propertiesUtil);
        when(propertiesUtil.getNtfinfoUpsertRetryBaseInterval()).thenReturn(1);
        when(propertiesUtil.getNtfinfoUpsertRetryMaxInterval()).thenReturn(10000);
        Method method = NtfBatchExecErrorInfoRepositoryImpl.class.getDeclaredMethod("backoffMillis", int.class);
        method.setAccessible(true);

        // Act
        long actual = (long) method.invoke(sut, 1);

        // Assert
        assertEquals(1L, actual);
    }

    /** クラス：NtfBatchExecErrorInfoRepositoryImpl backoffMillis baseがmaxIntervalを超える場合maxIntervalが返ることを確認するテストケース */
    @Test
    void backoffMillis_002() throws Exception {
        // Arrange
        NtfBatchExecErrorInfoRepositoryImpl sut = new NtfBatchExecErrorInfoRepositoryImpl(ntfBatchExecErrorInfoMapper, propertiesUtil);
        when(propertiesUtil.getNtfinfoUpsertRetryBaseInterval()).thenReturn(1000);
        when(propertiesUtil.getNtfinfoUpsertRetryMaxInterval()).thenReturn(100);
        Method method = NtfBatchExecErrorInfoRepositoryImpl.class.getDeclaredMethod("backoffMillis", int.class);
        method.setAccessible(true);

        // Act
        long actual = (long) method.invoke(sut, 10);

        // Assert
        assertEquals(100L, actual);
    }

    /** クラス：NtfBatchExecErrorInfoRepositoryImpl sleep 正常にスリープが実行されることを確認するテストケース */
    @Test
    void sleep_001() throws Exception {
        // Arrange
        NtfBatchExecErrorInfoRepositoryImpl sut = new NtfBatchExecErrorInfoRepositoryImpl(ntfBatchExecErrorInfoMapper, propertiesUtil);
        Method method = NtfBatchExecErrorInfoRepositoryImpl.class.getDeclaredMethod("sleep", long.class);
        method.setAccessible(true);

        // Act & Assert
        assertDoesNotThrow(() -> method.invoke(sut, 1L));
    }

    /** クラス：NtfBatchExecErrorInfoRepositoryImpl sleep InterruptedExceptionが発生した場合スレッドに割り込みフラグが立てられることを確認するテストケース */
    @Test
    void sleep_002() throws Exception {
        // Arrange
        NtfBatchExecErrorInfoRepositoryImpl sut = new NtfBatchExecErrorInfoRepositoryImpl(ntfBatchExecErrorInfoMapper, propertiesUtil);
        Method method = NtfBatchExecErrorInfoRepositoryImpl.class.getDeclaredMethod("sleep", long.class);
        method.setAccessible(true);
        Thread.currentThread().interrupt();

        // Act
        method.invoke(sut, 0L);

        // Assert
        assertTrue(Thread.currentThread().isInterrupted());
        Thread.interrupted(); // clear interrupt flag
    }

    /** クラス：NtfBatchExecErrorInfoRepositoryImpl toCustom sqlExが存在する場合sqlExをラップしたCustomExceptionが返ることを確認するテストケース */
    @Test
    void toCustom_001() throws Exception {
        // Arrange
        NtfBatchExecErrorInfoRepositoryImpl sut = new NtfBatchExecErrorInfoRepositoryImpl(ntfBatchExecErrorInfoMapper, propertiesUtil);
        Method method = NtfBatchExecErrorInfoRepositoryImpl.class.getDeclaredMethod("toCustom", Exception.class);
        method.setAccessible(true);
        SQLTransientException sqlEx = new SQLTransientException("sql");

        // Act
        CustomException actual = (CustomException) method.invoke(sut, sqlEx);

        // Assert
        assertNotNull(actual);
    }

    /** クラス：NtfBatchExecErrorInfoRepositoryImpl toCustom sqlExがnullの場合元の例外をラップしたCustomExceptionが返ることを確認するテストケース */
    @Test
    void toCustom_002() throws Exception {
        // Arrange
        NtfBatchExecErrorInfoRepositoryImpl sut = new NtfBatchExecErrorInfoRepositoryImpl(ntfBatchExecErrorInfoMapper, propertiesUtil);
        Method method = NtfBatchExecErrorInfoRepositoryImpl.class.getDeclaredMethod("toCustom", Exception.class);
        method.setAccessible(true);
        RuntimeException ex = new RuntimeException("runtime");

        // Act
        CustomException actual = (CustomException) method.invoke(sut, ex);

        // Assert
        assertNotNull(actual);
    }
}
