package com.vessel.optimizer.service;

public interface DistanceProvider {
    double calculateDistance(double lat1, double lon1, double lat2, double lon2);
    String getProviderName();
}