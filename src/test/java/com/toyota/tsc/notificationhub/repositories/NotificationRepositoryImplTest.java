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
 * NotificationRepositoryImpl のテストクラス
 */
@SuppressWarnings("all")
@ExtendWith(MockitoExtension.class)
class NotificationRepositoryImplTest {

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private PropertiesUtil propertiesUtil;

    /** クラス：NotificationRepositoryImpl 正常にupdateが実行されることを確認するテストケース */
    @Test
    void update_001() {
        // Arrange
        NotificationRepositoryImpl sut = new NotificationRepositoryImpl(notificationMapper, propertiesUtil);
        when(propertiesUtil.getNtfinfoUpsertRetryCount()).thenReturn(3);
        when(notificationMapper.update("ME", 123)).thenReturn(1);

        // Act
        int actual = sut.update("ME", 123);

        // Assert
        assertEquals(1, actual);
        verify(notificationMapper, times(1)).update("ME", 123);
    }

    /** クラス：NotificationRepositoryImpl SQLTransientException発生時にリトライして成功することを確認するテストケース */
    @Test
    void update_002() {
        // Arrange
        NotificationRepositoryImpl sut = new NotificationRepositoryImpl(notificationMapper, propertiesUtil);
        when(propertiesUtil.getNtfinfoUpsertRetryCount()).thenReturn(3);
        when(propertiesUtil.getNtfinfoUpsertRetryBaseInterval()).thenReturn(1);
        when(propertiesUtil.getNtfinfoUpsertRetryMaxInterval()).thenReturn(100);
        when(notificationMapper.update("ME", 123))
                .thenThrow(new RuntimeException(new SQLTransientException("transient")))
                .thenReturn(1);

        // Act
        int actual = sut.update("ME", 123);

        // Assert
        assertEquals(1, actual);
        verify(notificationMapper, times(2)).update("ME", 123);
    }

    /** クラス：NotificationRepositoryImpl SQLTransientExceptionでリトライ上限を超えCustomExceptionが投げられることを確認するテストケース */
    @Test
    void update_003() {
        // Arrange
        NotificationRepositoryImpl sut = new NotificationRepositoryImpl(notificationMapper, propertiesUtil);
        when(propertiesUtil.getNtfinfoUpsertRetryCount()).thenReturn(0);
        when(notificationMapper.update("ME", 123)).thenThrow(new RuntimeException(new SQLTransientException("transient")));

        // Act & Assert
        assertThrows(CustomException.class, () -> sut.update("ME", 123));
    }

    /** クラス：NotificationRepositoryImpl 非SQL例外発生時にCustomExceptionが投げられることを確認するテストケース */
    @Test
    void update_004() {
        // Arrange
        NotificationRepositoryImpl sut = new NotificationRepositoryImpl(notificationMapper, propertiesUtil);
        when(propertiesUtil.getNtfinfoUpsertRetryCount()).thenReturn(3);
        when(notificationMapper.update("ME", 123)).thenThrow(new RuntimeException("runtime"));

        // Act & Assert
        assertThrows(CustomException.class, () -> sut.update("ME", 123));
    }

    /** クラス：NotificationRepositoryImpl SQLNonTransientException発生時にCustomExceptionが投げられることを確認するテストケース */
    @Test
    void update_005() {
        // Arrange
        NotificationRepositoryImpl sut = new NotificationRepositoryImpl(notificationMapper, propertiesUtil);
        when(propertiesUtil.getNtfinfoUpsertRetryCount()).thenReturn(3);
        when(notificationMapper.update("ME", 123)).thenThrow(new RuntimeException(new SQLNonTransientException("non-transient")));

        // Act & Assert
        assertThrows(CustomException.class, () -> sut.update("ME", 123));
    }

    /** クラス：NotificationRepositoryImpl shouldThrow sqlExがnullの場合trueを返すことを確認するテストケース */
    @Test
    void shouldThrow_001() throws Exception {
        // Arrange
        NotificationRepositoryImpl sut = new NotificationRepositoryImpl(notificationMapper, propertiesUtil);
        Method method = NotificationRepositoryImpl.class.getDeclaredMethod("shouldThrow", Exception.class, int.class, int.class);
        method.setAccessible(true);

        // Act
        boolean actual = (boolean) method.invoke(sut, new RuntimeException("not sql"), 0, 3);

        // Assert
        assertTrue(actual);
    }

    /** クラス：NotificationRepositoryImpl shouldThrow SQLNonTransientの場合trueを返すことを確認するテストケース */
    @Test
    void shouldThrow_002() throws Exception {
        // Arrange
        NotificationRepositoryImpl sut = new NotificationRepositoryImpl(notificationMapper, propertiesUtil);
        Method method = NotificationRepositoryImpl.class.getDeclaredMethod("shouldThrow", Exception.class, int.class, int.class);
        method.setAccessible(true);

        // Act
        boolean actual = (boolean) method.invoke(sut, new SQLNonTransientException("non-transient"), 0, 3);

        // Assert
        assertTrue(actual);
    }

    /** クラス：NotificationRepositoryImpl shouldThrow SQLTransientかつretry>=retryCountの場合trueを返すことを確認するテストケース */
    @Test
    void shouldThrow_003() throws Exception {
        // Arrange
        NotificationRepositoryImpl sut = new NotificationRepositoryImpl(notificationMapper, propertiesUtil);
        Method method = NotificationRepositoryImpl.class.getDeclaredMethod("shouldThrow", Exception.class, int.class, int.class);
        method.setAccessible(true);

        // Act
        boolean actual = (boolean) method.invoke(sut, new SQLTransientException("transient"), 3, 3);

        // Assert
        assertTrue(actual);
    }

    /** クラス：NotificationRepositoryImpl shouldThrow SQLTransientかつretry<retryCountの場合falseを返すことを確認するテストケース */
    @Test
    void shouldThrow_004() throws Exception {
        // Arrange
        NotificationRepositoryImpl sut = new NotificationRepositoryImpl(notificationMapper, propertiesUtil);
        Method method = NotificationRepositoryImpl.class.getDeclaredMethod("shouldThrow", Exception.class, int.class, int.class);
        method.setAccessible(true);

        // Act
        boolean actual = (boolean) method.invoke(sut, new SQLTransientException("transient"), 0, 3);

        // Assert
        assertFalse(actual);
    }

    /** クラス：NotificationRepositoryImpl backoffMillis baseがmaxInterval以下の場合baseが返ることを確認するテストケース */
    @Test
    void backoffMillis_001() throws Exception {
        // Arrange
        NotificationRepositoryImpl sut = new NotificationRepositoryImpl(notificationMapper, propertiesUtil);
        when(propertiesUtil.getNtfinfoUpsertRetryBaseInterval()).thenReturn(1);
        when(propertiesUtil.getNtfinfoUpsertRetryMaxInterval()).thenReturn(10000);
        Method method = NotificationRepositoryImpl.class.getDeclaredMethod("backoffMillis", int.class);
        method.setAccessible(true);

        // Act
        long actual = (long) method.invoke(sut, 1);

        // Assert
        assertEquals(1L, actual);
    }

    /** クラス：NotificationRepositoryImpl backoffMillis baseがmaxIntervalを超える場合maxIntervalが返ることを確認するテストケース */
    @Test
    void backoffMillis_002() throws Exception {
        // Arrange
        NotificationRepositoryImpl sut = new NotificationRepositoryImpl(notificationMapper, propertiesUtil);
        when(propertiesUtil.getNtfinfoUpsertRetryBaseInterval()).thenReturn(1000);
        when(propertiesUtil.getNtfinfoUpsertRetryMaxInterval()).thenReturn(100);
        Method method = NotificationRepositoryImpl.class.getDeclaredMethod("backoffMillis", int.class);
        method.setAccessible(true);

        // Act
        long actual = (long) method.invoke(sut, 10);

        // Assert
        assertEquals(100L, actual);
    }

    /** クラス：NotificationRepositoryImpl sleep 正常にスリープが実行されることを確認するテストケース */
    @Test
    void sleep_001() throws Exception {
        // Arrange
        NotificationRepositoryImpl sut = new NotificationRepositoryImpl(notificationMapper, propertiesUtil);
        Method method = NotificationRepositoryImpl.class.getDeclaredMethod("sleep", long.class);
        method.setAccessible(true);

        // Act & Assert
        assertDoesNotThrow(() -> method.invoke(sut, 1L));
    }

    /** クラス：NotificationRepositoryImpl sleep InterruptedExceptionが発生した場合スレッドに割り込みフラグが立てられることを確認するテストケース */
    @Test
    void sleep_002() throws Exception {
        // Arrange
        NotificationRepositoryImpl sut = new NotificationRepositoryImpl(notificationMapper, propertiesUtil);
        Method method = NotificationRepositoryImpl.class.getDeclaredMethod("sleep", long.class);
        method.setAccessible(true);
        Thread.currentThread().interrupt();

        // Act
        method.invoke(sut, 0L);

        // Assert
        assertTrue(Thread.currentThread().isInterrupted());
        Thread.interrupted(); // clear interrupt flag
    }

    /** クラス：NotificationRepositoryImpl toCustom sqlExが存在する場合sqlExをラップしたCustomExceptionが返ることを確認するテストケース */
    @Test
    void toCustom_001() throws Exception {
        // Arrange
        NotificationRepositoryImpl sut = new NotificationRepositoryImpl(notificationMapper, propertiesUtil);
        Method method = NotificationRepositoryImpl.class.getDeclaredMethod("toCustom", Exception.class);
        method.setAccessible(true);

        // Act
        CustomException actual = (CustomException) method.invoke(sut, new SQLTransientException("sql"));

        // Assert
        assertNotNull(actual);
    }

    /** クラス：NotificationRepositoryImpl toCustom sqlExがnullの場合元の例外をラップしたCustomExceptionが返ることを確認するテストケース */
    @Test
    void toCustom_002() throws Exception {
        // Arrange
        NotificationRepositoryImpl sut = new NotificationRepositoryImpl(notificationMapper, propertiesUtil);
        Method method = NotificationRepositoryImpl.class.getDeclaredMethod("toCustom", Exception.class);
        method.setAccessible(true);

        // Act
        CustomException actual = (CustomException) method.invoke(sut, new RuntimeException("runtime"));

        // Assert
        assertNotNull(actual);
    }
}
