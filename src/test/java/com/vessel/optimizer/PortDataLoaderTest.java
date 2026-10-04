package com.vessel.optimizer;

import com.vessel.optimizer.model.Port;
import com.vessel.optimizer.repository.PortRepository;
import com.vessel.optimizer.service.PortDataLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortDataLoaderTest {

    @Mock
    private PortRepository portRepository;

    @InjectMocks
    private PortDataLoader portDataLoader;

    @Test
    void testSkipLoadWhenDataExists() {
        when(portRepository.count()).thenReturn(100L);
        portDataLoader.run();
        verify(portRepository, never()).saveAll(anyList());
    }

    @Test
    void testParsePortValid() {
        // Test via reflection or just verify the loader doesn't crash
        when(portRepository.count()).thenReturn(0L);
        // CSV loading tested in integration tests
        assertDoesNotThrow(() -> portDataLoader.run());
    }
}