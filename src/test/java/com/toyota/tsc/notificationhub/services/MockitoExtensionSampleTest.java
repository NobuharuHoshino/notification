package com.toyota.tsc.notificationhub.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MockitoExtensionSampleTest {
    @Mock
    private Runnable mockRunnable;

    @Test
    void testMock() {
        // 単純なMockの動作確認
        org.junit.jupiter.api.Assertions.assertNotNull(mockRunnable);
    }
}
