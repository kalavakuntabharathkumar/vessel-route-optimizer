package com.vessel.optimizer.controller;

import com.vessel.optimizer.dto.RouteRequest;
import com.vessel.optimizer.dto.RouteResponse;
import com.vessel.optimizer.model.Port;
import com.vessel.optimizer.repository.PortRepository;
import com.vessel.optimizer.service.RouteOptimizer;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class RouteController {

    private final RouteOptimizer routeOptimizer;
    private final PortRepository portRepository;

    public RouteController(RouteOptimizer routeOptimizer, PortRepository portRepository) {
        this.routeOptimizer = routeOptimizer;
        this.portRepository = portRepository;
    }

    @PostMapping("/routes")
    public ResponseEntity<RouteResponse> calculateRoute(@Valid @RequestBody RouteRequest request) {
        var result = routeOptimizer.findOptimalRoute(request.portCodes());
        List<RouteResponse.RouteLeg> legs = result.legs().stream()
            .map(l -> new RouteResponse.RouteLeg(l.from(), l.to(), l.distanceKm()))
            .collect(Collectors.toList());
        return ResponseEntity.ok(new RouteResponse(
            result.route(),
            result.totalDistanceKm(),
            result.estimatedFuelTons(),
            legs
        ));
    }

    @GetMapping("/ports")
    public ResponseEntity<List<PortSummary>> listPorts() {
        List<PortSummary> ports = portRepository.findByIsActiveTrue().stream()
            .map(p -> new PortSummary(p.getCode(), p.getName(), p.getCountry(), p.getLatitude(), p.getLongitude()))
            .collect(Collectors.toList());
        return ResponseEntity.ok(ports);
    }

    @GetMapping("/ports/{code}")
    public ResponseEntity<PortSummary> getPort(@PathVariable String code) {
        return portRepository.findByCode(code.toUpperCase())
            .map(p -> new PortSummary(p.getCode(), p.getName(), p.getCountry(), p.getLatitude(), p.getLongitude()))
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    public record PortSummary(String code, String name, String country, Double latitude, Double longitude) {}
}