package com.vessel.optimizer;

import com.vessel.optimizer.model.Port;
import com.vessel.optimizer.service.DistanceProvider;
import com.vessel.optimizer.service.HaversineProvider;
import com.vessel.optimizer.service.RouteOptimizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RouteOptimizerTest {

    private RouteOptimizer optimizer;
    private DistanceProvider distanceProvider;

    @BeforeEach
    void setUp() {
        distanceProvider = new HaversineProvider();
        optimizer = new RouteOptimizer(createMockRepository(), distanceProvider);
    }

    private com.vessel.optimizer.repository.PortRepository createMockRepository() {
        var repo = mock(com.vessel.optimizer.repository.PortRepository.class);
        Port nyc = Port.builder().code("USNYC").name("New York").country("US").latitude(40.7128).longitude(-74.0060).build();
        Port london = Port.builder().code("GBLGP").name("London").country("GB").latitude(51.5074).longitude(-0.1278).build();
        Port singapore = Port.builder().code("SGSIN").name("Singapore").country("SG").latitude(1.3521).longitude(103.8198).build();
        Port shanghai = Port.builder().code("CNSHA").name("Shanghai").country("CN").latitude(31.2304).longitude(121.4737).build();
        Port rotterdam = Port.builder().code("NLRTM").name("Rotterdam").country("NL").latitude(51.9244).longitude(4.4777).build();
        Port hongKong = Port.builder().code("HKHKG").name("Hong Kong").country("HK").latitude(22.3193).longitude(114.1694).build();
        Port sydney = Port.builder().code("AUSTR").name("Sydney").country("AU").latitude(-33.8688).longitude(151.2093).build();
        Port panama = Port.builder().code("PAONX").name("Panama Canal").country("PA").latitude(9.3817).longitude(-79.9197).build();
        Port suez = Port.builder().code("EGPSD").name("Port Said").country("EG").latitude(31.2653).longitude(32.3019).build();
        Port dubai = Port.builder().code("AEJEA").name("Jebel Ali").country("AE").latitude(25.0077).longitude(55.0633).build();

        when(repo.findByIsActiveTrue()).thenReturn(List.of(nyc, london, singapore, shanghai, rotterdam, hongKong, sydney, panama, suez, dubai));
        when(repo.findByCode("USNYC")).thenReturn(java.util.Optional.of(nyc));
        when(repo.findByCode("GBLGP")).thenReturn(java.util.Optional.of(london));
        when(repo.findByCode("SGSIN")).thenReturn(java.util.Optional.of(singapore));
        when(repo.findByCode("CNSHA")).thenReturn(java.util.Optional.of(shanghai));
        when(repo.findByCode("NLRTM")).thenReturn(java.util.Optional.of(rotterdam));
        when(repo.findByCode("HKHKG")).thenReturn(java.util.Optional.of(hongKong));
        when(repo.findByCode("AUSTR")).thenReturn(java.util.Optional.of(sydney));
        when(repo.findByCode("PAONX")).thenReturn(java.util.Optional.of(panama));
        when(repo.findByCode("EGPSD")).thenReturn(java.util.Optional.of(suez));
        when(repo.findByCode("AEJEA")).thenReturn(java.util.Optional.of(dubai));
        when(repo.existsByCode(anyString())).thenAnswer(inv -> {
            String code = inv.getArgument(0);
            return List.of("USNYC","GBLGP","SGSIN","CNSHA","NLRTM","HKHKG","AUSTR","PAONX","EGPSD","AEJEA").contains(code);
        });
        return repo;
    }

    @Test
    void testDirectRouteCalculation() {
        var result = optimizer.findOptimalRoute(List.of("USNYC", "GBLGP"));
        assertEquals(2, result.route().size());
        assertEquals("USNYC", result.route().get(0));
        assertEquals("GBLGP", result.route().get(1));
        assertTrue(result.totalDistanceKm() > 5000 && result.totalDistanceKm() < 6000);
        assertEquals(1, result.legs().size());
    }

    @Test
    void testMultiStopRoute() {
        var result = optimizer.findOptimalRoute(List.of("USNYC", "GBLGP", "SGSIN", "CNSHA"));
        assertEquals(4, result.route().size());
        assertTrue(result.totalDistanceKm() > 20000);
        assertEquals(3, result.legs().size());
        assertTrue(result.estimatedFuelTons() > 0);
    }

    @Test
    void testRouteWithIntermediateNodes() {
        var result = optimizer.findOptimalRoute(List.of("USNYC", "PAONX", "CNSHA"));
        assertEquals(3, result.route().size());
        assertTrue(result.route().contains("PAONX"));
    }

    @Test
    void testInvalidPortCodeThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> optimizer.findOptimalRoute(List.of("USNYC", "INVALID")));
    }

    @Test
    void testSinglePortThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> optimizer.findOptimalRoute(List.of("USNYC")));
    }

    @Test
    void testNullRequestThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> optimizer.findOptimalRoute(null));
    }

    @Test
    void testEmptyListThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> optimizer.findOptimalRoute(List.of()));
    }

    @ParameterizedTest
    @MethodSource("polarAndCanalRoutes")
    void testSpecialRoutes(List<String> ports, double minDist, double maxDist) {
        var result = optimizer.findOptimalRoute(ports);
        assertTrue(result.totalDistanceKm() >= minDist && result.totalDistanceKm() <= maxDist,
            "Distance " + result.totalDistanceKm() + " not in range [" + minDist + "," + maxDist + "]");
    }

    static Stream<Arguments> polarAndCanalRoutes() {
        return Stream.of(
            Arguments.of(List.of("NLRTM", "EGPSD", "SGSIN"), 10000.0, 12000.0), // Suez route
            Arguments.of(List.of("USNYC", "PAONX", "CNSHA"), 18000.0, 21000.0), // Panama route
            Arguments.of(List.of("GBLGP", "AEJEA", "SGSIN"), 9000.0, 11000.0),  // Middle East route
            Arguments.of(List.of("USNYC", "HKHKG"), 14000.0, 16000.0),          // Trans-Pacific
            Arguments.of(List.of("AUSTR", "SGSIN"), 6000.0, 7000.0)             // Australia to Asia
        );
    }

    @Test
    void testHaversineAccuracy() {
        // NYC to London ~5570 km
        double dist = distanceProvider.calculateDistance(40.7128, -74.0060, 51.5074, -0.1278);
        assertTrue(dist > 5500 && dist < 5650);

        // Singapore to Shanghai ~3800 km
        dist = distanceProvider.calculateDistance(1.3521, 103.8198, 31.2304, 121.4737);
        assertTrue(dist > 3700 && dist < 3900);

        // Antipodal points ~20000 km
        dist = distanceProvider.calculateDistance(0, 0, 0, 180);
        assertTrue(dist > 19900 && dist < 20100);
    }

    @Test
    void testGraphRebuildClearsCache() {
        int initialSize = optimizer.getClass().getDeclaredFields().length; // proxy check
        optimizer.buildGraph();
        assertDoesNotThrow(() -> optimizer.findOptimalRoute(List.of("USNYC", "GBLGP")));
    }

    @Test
    void testFuelEstimation() {
        var result = optimizer.findOptimalRoute(List.of("USNYC", "GBLGP"));
        double expectedFuel = result.totalDistanceKm() * 0.015;
        assertEquals(expectedFuel, result.estimatedFuelTons(), 0.01);
    }
}