package com.vessel.optimizer.dto;

import java.util.List;

public record RouteResponse(
    List<String> route,
    double totalDistanceKm,
    double estimatedFuelTons,
    List<RouteLeg> legs
) {
    public record RouteLeg(String from, String to, double distanceKm) {}
}