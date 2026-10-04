package com.vessel.optimizer;

import com.vessel.optimizer.dto.RouteRequest;
import com.vessel.optimizer.dto.RouteResponse;
import com.vessel.optimizer.model.Port;
import com.vessel.optimizer.repository.PortRepository;
import com.vessel.optimizer.service.RouteOptimizer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RouteController.class)
class RouteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RouteOptimizer routeOptimizer;

    @MockBean
    private PortRepository portRepository;

    @Test
    void testCalculateRouteSuccess() throws Exception {
        var routeResult = new RouteOptimizer.RouteResult(
            List.of("USNYC", "GBLGP", "SGSIN"),
            16400.5,
            246.0,
            List.of(
                new RouteOptimizer.RouteLeg("USNYC", "GBLGP", 5570.2),
                new RouteOptimizer.RouteLeg("GBLGP", "SGSIN", 10830.3)
            )
        );
        when(routeOptimizer.findOptimalRoute(anyList())).thenReturn(routeResult);

        mockMvc.perform(post("/api/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"portCodes\": [\"USNYC\", \"GBLGP\", \"SGSIN\"]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.route").isArray())
            .andExpect(jsonPath("$.route.length()").value(3))
            .andExpect(jsonPath("$.totalDistanceKm").value(16400.5))
            .andExpect(jsonPath("$.estimatedFuelTons").value(246.0))
            .andExpect(jsonPath("$.legs.length()").value(2));
    }

    @Test
    void testCalculateRouteValidationError() throws Exception {
        mockMvc.perform(post("/api/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"portCodes\": [\"USNYC\"]}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.portCodes").exists());
    }

    @Test
    void testCalculateRouteNotFound() throws Exception {
        when(routeOptimizer.findOptimalRoute(anyList()))
            .thenThrow(new IllegalArgumentException("Port not found: INVALID"));

        mockMvc.perform(post("/api/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"portCodes\": [\"USNYC\", \"INVALID\"]}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Port not found: INVALID"));
    }

    @Test
    void testListPorts() throws Exception {
        Port port = Port.builder().code("USNYC").name("New York").country("US")
            .latitude(40.7128).longitude(-74.0060).build();
        when(portRepository.findByIsActiveTrue()).thenReturn(List.of(port));

        mockMvc.perform(get("/api/ports"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].code").value("USNYC"))
            .andExpect(jsonPath("$[0].name").value("New York"));
    }

    @Test
    void testGetPortFound() throws Exception {
        Port port = Port.builder().code("USNYC").name("New York").country("US")
            .latitude(40.7128).longitude(-74.0060).build();
        when(portRepository.findByCode("USNYC")).thenReturn(java.util.Optional.of(port));

        mockMvc.perform(get("/api/ports/USNYC"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("USNYC"));
    }

    @Test
    void testGetPortNotFound() throws Exception {
        when(portRepository.findByCode("INVALID")).thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/api/ports/INVALID"))
            .andExpect(status().isNotFound());
    }
}