package com.vessel.optimizer.service;

import com.vessel.optimizer.model.Port;
import com.vessel.optimizer.repository.PortRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RouteOptimizer {
    private static final double FUEL_CONSUMPTION_PER_KM = 0.015; // tons/km for typical vessel

    private final PortRepository portRepository;
    private final DistanceProvider distanceProvider;
    private final Map<String, Port> portCache = new HashMap<>();
    private final Map<String, Map<String, Double>> adjacencyCache = new HashMap<>();

    public RouteOptimizer(PortRepository portRepository, DistanceProvider distanceProvider) {
        this.portRepository = portRepository;
        this.distanceProvider = distanceProvider;
        buildGraph();
    }

    @Transactional(readOnly = true)
    public void buildGraph() {
        List<Port> ports = portRepository.findByIsActiveTrue();
        portCache.clear();
        adjacencyCache.clear();

        for (Port port : ports) {
            portCache.put(port.getCode(), port);
        }

        for (Port from : ports) {
            Map<String, Double> edges = new HashMap<>();
            for (Port to : ports) {
                if (!from.getCode().equals(to.getCode())) {
                    double dist = distanceProvider.calculateDistance(
                        from.getLatitude(), from.getLongitude(),
                        to.getLatitude(), to.getLongitude()
                    );
                    edges.put(to.getCode(), dist);
                }
            }
            adjacencyCache.put(from.getCode(), edges);
        }
        log.info("Built graph with {} nodes", portCache.size());
    }

    public RouteResult findOptimalRoute(List<String> portCodes) {
        if (portCodes == null || portCodes.size() < 2) {
            throw new IllegalArgumentException("At least 2 ports required");
        }

        List<String> validCodes = portCodes.stream()
            .map(String::toUpperCase)
            .filter(portCache::containsKey)
            .collect(Collectors.toList());

        if (validCodes.size() != portCodes.size()) {
            throw new IllegalArgumentException("One or more port codes not found");
        }

        String start = validCodes.get(0);
        List<String> waypoints = validCodes.subList(1, validCodes.size());

        List<String> fullRoute = new ArrayList<>();
        fullRoute.add(start);
        double totalDistance = 0.0;
        List<RouteLeg> legs = new ArrayList<>();

        String current = start;
        for (String next : waypoints) {
            List<String> segment = dijkstra(current, next);
            if (segment.size() < 2) {
                throw new IllegalStateException("No path between " + current + " and " + next);
            }
            if (!fullRoute.isEmpty() && fullRoute.get(fullRoute.size() - 1).equals(segment.get(0))) {
                segment = segment.subList(1, segment.size());
            }
            for (int i = 0; i < segment.size() - 1; i++) {
                String from = segment.get(i);
                String to = segment.get(i + 1);
                double dist = adjacencyCache.get(from).get(to);
                totalDistance += dist;
                legs.add(new RouteLeg(from, to, dist));
            }
            fullRoute.addAll(segment);
            current = next;
        }

        return new RouteResult(fullRoute, totalDistance, totalDistance * FUEL_CONSUMPTION_PER_KM, legs);
    }

    private List<String> dijkstra(String start, String target) {
        Map<String, Double> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingDouble(n -> n.distance));
        Set<String> visited = new HashSet<>();

        for (String code : portCache.keySet()) {
            dist.put(code, Double.POSITIVE_INFINITY);
        }
        dist.put(start, 0.0);
        pq.offer(new Node(start, 0.0));

        while (!pq.isEmpty()) {
            Node current = pq.poll();
            if (visited.contains(current.code)) continue;
            visited.add(current.code);

            if (current.code.equals(target)) break;

            Map<String, Double> neighbors = adjacencyCache.get(current.code);
            if (neighbors == null) continue;

            for (Map.Entry<String, Double> edge : neighbors.entrySet()) {
                String neighbor = edge.getKey();
                double weight = edge.getValue();
                double newDist = current.distance + weight;
                if (newDist < dist.get(neighbor)) {
                    dist.put(neighbor, newDist);
                    prev.put(neighbor, current.code);
                    pq.offer(new Node(neighbor, newDist));
                }
            }
        }

        return reconstructPath(prev, start, target);
    }

    private List<String> reconstructPath(Map<String, String> prev, String start, String target) {
        List<String> path = new ArrayList<>();
        String current = target;
        while (current != null) {
            path.add(current);
            if (current.equals(start)) break;
            current = prev.get(current);
        }
        Collections.reverse(path);
        return path;
    }

    private record Node(String code, double distance) {}

    public record RouteResult(
        List<String> route,
        double totalDistanceKm,
        double estimatedFuelTons,
        List<RouteLeg> legs
    ) {}

    public record RouteLeg(String from, String to, double distanceKm) {}

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RouteOptimizer.class);
}