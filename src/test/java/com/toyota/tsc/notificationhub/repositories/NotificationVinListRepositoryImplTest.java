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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * NotificationVinListRepositoryImpl のテストクラス
 */
@SuppressWarnings("all")
@ExtendWith(MockitoExtension.class)
class NotificationVinListRepositoryImplTest {

    @Mock
    private NotificationVinListMapper notificationVinListMapper;

    @Mock
    private PropertiesUtil propertiesUtil;

    /** クラス：NotificationVinListRepositoryImpl 正常にselectが実行されることを確認するテストケース */
    @Test
    void select_001() {
        // Arrange
        NotificationVinListRepositoryImpl sut = new NotificationVinListRepositoryImpl(notificationVinListMapper, propertiesUtil);
        NotificationVinListEntity entity = mock(NotificationVinListEntity.class);
        List<NotificationVinListEntity> expected = Arrays.asList(entity);
        when(notificationVinListMapper.select(123, 0)).thenReturn(expected);

        // Act
        List<NotificationVinListEntity> actual = sut.select(123, 0);

        // Assert
        assertEquals(expected, actual);
        verify(notificationVinListMapper, times(1)).select(123, 0);
    }

    /** クラス：NotificationVinListRepositoryImpl 正常にupdateが実行されることを確認するテストケース */
    @Test
    void update_001() {
        // Arrange
        NotificationVinListRepositoryImpl sut = new NotificationVinListRepositoryImpl(notificationVinListMapper, propertiesUtil);
        when(propertiesUtil.getNtfinfoUpsertRetryCount()).thenReturn(3);
        when(notificationVinListMapper.update(123, "1", 2)).thenReturn(1);

        // Act
        int actual = sut.update(123, "1", 2);

        // Assert
        assertEquals(1, actual);
        verify(notificationVinListMapper, times(1)).update(123, "1", 2);
    }

    /** クラス：NotificationVinListRepositoryImpl SQLTransientException発生時にリトライして成功することを確認するテストケース */
    @Test
    void update_002() {
        // Arrange
        NotificationVinListRepositoryImpl sut = new NotificationVinListRepositoryImpl(notificationVinListMapper, propertiesUtil);
        when(propertiesUtil.getNtfinfoUpsertRetryCount()).thenReturn(3);
        when(propertiesUtil.getNtfinfoUpsertRetryBaseInterval()).thenReturn(1);
        when(propertiesUtil.getNtfinfoUpsertRetryMaxInterval()).thenReturn(100);
        when(notificationVinListMapper.update(123, "1", 2))
                .thenThrow(new RuntimeException(new SQLTransientException("transient")))
                .thenReturn(1);

        // Act
        int actual = sut.update(123, "1", 2);

        // Assert
        assertEquals(1, actual);
        verify(notificationVinListMapper, times(2)).update(123, "1", 2);
    }

    /** クラス：NotificationVinListRepositoryImpl SQLTransientExceptionでリトライ上限を超えCustomExceptionが投げられることを確認するテストケース */
    @Test
    void update_003() {
        // Arrange
        NotificationVinListRepositoryImpl sut = new NotificationVinListRepositoryImpl(notificationVinListMapper, propertiesUtil);
        when(propertiesUtil.getNtfinfoUpsertRetryCount()).thenReturn(0);
        when(notificationVinListMapper.update(123, "1", 2)).thenThrow(new RuntimeException(new SQLTransientException("transient")));

        // Act & Assert
        assertThrows(CustomException.class, () -> sut.update(123, "1", 2));
    }

    /** クラス：NotificationVinListRepositoryImpl 非SQL例外発生時にCustomExceptionが投げられることを確認するテストケース */
    @Test
    void update_004() {
        // Arrange
        NotificationVinListRepositoryImpl sut = new NotificationVinListRepositoryImpl(notificationVinListMapper, propertiesUtil);
        when(propertiesUtil.getNtfinfoUpsertRetryCount()).thenReturn(3);
        when(notificationVinListMapper.update(123, "1", 2)).thenThrow(new RuntimeException("runtime"));

        // Act & Assert
        assertThrows(CustomException.class, () -> sut.update(123, "1", 2));
    }

    /** クラス：NotificationVinListRepositoryImpl SQLNonTransientException発生時にCustomExceptionが投げられることを確認するテストケース */
    @Test
    void update_005() {
        // Arrange
        NotificationVinListRepositoryImpl sut = new NotificationVinListRepositoryImpl(notificationVinListMapper, propertiesUtil);
        when(propertiesUtil.getNtfinfoUpsertRetryCount()).thenReturn(3);
        when(notificationVinListMapper.update(123, "1", 2)).thenThrow(new RuntimeException(new SQLNonTransientException("non-transient")));

        // Act & Assert
        assertThrows(CustomException.class, () -> sut.update(123, "1", 2));
    }

    /** クラス：NotificationVinListRepositoryImpl shouldThrow sqlExがnullの場合trueを返すことを確認するテストケース */
    @Test
    void shouldThrow_001() throws Exception {
        // Arrange
        NotificationVinListRepositoryImpl sut = new NotificationVinListRepositoryImpl(notificationVinListMapper, propertiesUtil);
        Method method = NotificationVinListRepositoryImpl.class.getDeclaredMethod("shouldThrow", Exception.class, int.class, int.class);
        method.setAccessible(true);

        // Act
        boolean actual = (boolean) method.invoke(sut, new RuntimeException("not sql"), 0, 3);

        // Assert
        assertTrue(actual);
    }

    /** クラス：NotificationVinListRepositoryImpl shouldThrow SQLNonTransientの場合trueを返すことを確認するテストケース */
    @Test
    void shouldThrow_002() throws Exception {
        // Arrange
        NotificationVinListRepositoryImpl sut = new NotificationVinListRepositoryImpl(notificationVinListMapper, propertiesUtil);
        Method method = NotificationVinListRepositoryImpl.class.getDeclaredMethod("shouldThrow", Exception.class, int.class, int.class);
        method.setAccessible(true);

        // Act
        boolean actual = (boolean) method.invoke(sut, new SQLNonTransientException("non-transient"), 0, 3);

        // Assert
        assertTrue(actual);
    }

    /** クラス：NotificationVinListRepositoryImpl shouldThrow SQLTransientかつretry>=retryCountの場合trueを返すことを確認するテストケース */
    @Test
    void shouldThrow_003() throws Exception {
        // Arrange
        NotificationVinListRepositoryImpl sut = new NotificationVinListRepositoryImpl(notificationVinListMapper, propertiesUtil);
        Method method = NotificationVinListRepositoryImpl.class.getDeclaredMethod("shouldThrow", Exception.class, int.class, int.class);
        method.setAccessible(true);

        // Act
        boolean actual = (boolean) method.invoke(sut, new SQLTransientException("transient"), 3, 3);

        // Assert
        assertTrue(actual);
    }

    /** クラス：NotificationVinListRepositoryImpl shouldThrow SQLTransientかつretry<retryCountの場合falseを返すことを確認するテストケース */
    @Test
    void shouldThrow_004() throws Exception {
        // Arrange
        NotificationVinListRepositoryImpl sut = new NotificationVinListRepositoryImpl(notificationVinListMapper, propertiesUtil);
        Method method = NotificationVinListRepositoryImpl.class.getDeclaredMethod("shouldThrow", Exception.class, int.class, int.class);
        method.setAccessible(true);

        // Act
        boolean actual = (boolean) method.invoke(sut, new SQLTransientException("transient"), 0, 3);

        // Assert
        assertFalse(actual);
    }

    /** クラス：NotificationVinListRepositoryImpl backoffMillis baseがmaxInterval以下の場合baseが返ることを確認するテストケース */
    @Test
    void backoffMillis_001() throws Exception {
        // Arrange
        NotificationVinListRepositoryImpl sut = new NotificationVinListRepositoryImpl(notificationVinListMapper, propertiesUtil);
        when(propertiesUtil.getNtfinfoUpsertRetryBaseInterval()).thenReturn(1);
        when(propertiesUtil.getNtfinfoUpsertRetryMaxInterval()).thenReturn(10000);
        Method method = NotificationVinListRepositoryImpl.class.getDeclaredMethod("backoffMillis", int.class);
        method.setAccessible(true);

        // Act
        long actual = (long) method.invoke(sut, 1);

        // Assert
        assertEquals(1L, actual);
    }

    /** クラス：NotificationVinListRepositoryImpl backoffMillis baseがmaxIntervalを超える場合maxIntervalが返ることを確認するテストケース */
    @Test
    void backoffMillis_002() throws Exception {
        // Arrange
        NotificationVinListRepositoryImpl sut = new NotificationVinListRepositoryImpl(notificationVinListMapper, propertiesUtil);
        when(propertiesUtil.getNtfinfoUpsertRetryBaseInterval()).thenReturn(1000);
        when(propertiesUtil.getNtfinfoUpsertRetryMaxInterval()).thenReturn(100);
        Method method = NotificationVinListRepositoryImpl.class.getDeclaredMethod("backoffMillis", int.class);
        method.setAccessible(true);

        // Act
        long actual = (long) method.invoke(sut, 10);

        // Assert
        assertEquals(100L, actual);
    }

    /** クラス：NotificationVinListRepositoryImpl sleep 正常にスリープが実行されることを確認するテストケース */
    @Test
    void sleep_001() throws Exception {
        // Arrange
        NotificationVinListRepositoryImpl sut = new NotificationVinListRepositoryImpl(notificationVinListMapper, propertiesUtil);
        Method method = NotificationVinListRepositoryImpl.class.getDeclaredMethod("sleep", long.class);
        method.setAccessible(true);

        // Act & Assert
        assertDoesNotThrow(() -> method.invoke(sut, 1L));
    }

    /** クラス：NotificationVinListRepositoryImpl sleep InterruptedExceptionが発生した場合スレッドに割り込みフラグが立てられることを確認するテストケース */
    @Test
    void sleep_002() throws Exception {
        // Arrange
        NotificationVinListRepositoryImpl sut = new NotificationVinListRepositoryImpl(notificationVinListMapper, propertiesUtil);
        Method method = NotificationVinListRepositoryImpl.class.getDeclaredMethod("sleep", long.class);
        method.setAccessible(true);
        Thread.currentThread().interrupt();

        // Act
        method.invoke(sut, 0L);

        // Assert
        assertTrue(Thread.currentThread().isInterrupted());
        Thread.interrupted(); // clear interrupt flag
    }

    /** クラス：NotificationVinListRepositoryImpl toCustom sqlExが存在する場合sqlExをラップしたCustomExceptionが返ることを確認するテストケース */
    @Test
    void toCustom_001() throws Exception {
        // Arrange
        NotificationVinListRepositoryImpl sut = new NotificationVinListRepositoryImpl(notificationVinListMapper, propertiesUtil);
        Method method = NotificationVinListRepositoryImpl.class.getDeclaredMethod("toCustom", Exception.class);
        method.setAccessible(true);

        // Act
        CustomException actual = (CustomException) method.invoke(sut, new SQLTransientException("sql"));

        // Assert
        assertNotNull(actual);
    }

    /** クラス：NotificationVinListRepositoryImpl toCustom sqlExがnullの場合元の例外をラップしたCustomExceptionが返ることを確認するテストケース */
    @Test
    void toCustom_002() throws Exception {
        // Arrange
        NotificationVinListRepositoryImpl sut = new NotificationVinListRepositoryImpl(notificationVinListMapper, propertiesUtil);
        Method method = NotificationVinListRepositoryImpl.class.getDeclaredMethod("toCustom", Exception.class);
        method.setAccessible(true);

        // Act
        CustomException actual = (CustomException) method.invoke(sut, new RuntimeException("runtime"));

        // Assert
        assertNotNull(actual);
    }
}
