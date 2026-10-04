package com.vessel.optimizer;

import com.vessel.optimizer.service.OsrmClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OsrmClientTest {

    @Mock
    private RestClient restClient;
    @Mock
    private RestClient.RequestBodyUriSpec uriSpec;
    @Mock
    private RestClient.RequestBodySpec bodySpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;

    @Test
    void testFallbackToHaversineOnError() {
        OsrmClient client = new OsrmClient("http://localhost:5000");
        // Since we can't easily mock RestClient internals, test the fallback behavior
        // by verifying the interface contract
        double dist = client.calculateDistance(40.7128, -74.0060, 51.5074, -0.1278);
        // Should return a reasonable distance (haversine fallback)
        assertTrue(dist > 5000 && dist < 6000);
        assertEquals("OSRM", client.getProviderName());
    }

    @Test
    void testHaversineProviderDirect() {
        var provider = new com.vessel.optimizer.service.HaversineProvider();
        double dist = provider.calculateDistance(0, 0, 0, 180);
        assertTrue(dist > 19900 && dist < 20100);
        assertEquals("Haversine", provider.getProviderName());
    }
}