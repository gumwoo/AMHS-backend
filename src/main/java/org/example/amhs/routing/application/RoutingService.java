package org.example.amhs.routing.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import org.example.amhs.common.exception.ErrorCode;
import org.example.amhs.common.exception.ResourceNotFoundException;
import org.example.amhs.common.exception.RouteNotFoundException;
import org.example.amhs.fab.domain.FabEdge;
import org.example.amhs.fab.repository.FabEdgeRepository;
import org.example.amhs.fab.repository.FabNodeRepository;
import org.example.amhs.routing.domain.RouteResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoutingService {

    private final FabNodeRepository fabNodeRepository;
    private final FabEdgeRepository fabEdgeRepository;

    public RoutingService(
            FabNodeRepository fabNodeRepository,
            FabEdgeRepository fabEdgeRepository
    ) {
        this.fabNodeRepository = fabNodeRepository;
        this.fabEdgeRepository = fabEdgeRepository;
    }

    @Transactional(
            readOnly = true,
            noRollbackFor = {RouteNotFoundException.class, ResourceNotFoundException.class}
    )
    public RouteResult findShortestPath(String sourceNodeId, String destinationNodeId) {
        validateNodeExists(sourceNodeId);
        validateNodeExists(destinationNodeId);

        Map<String, List<EdgeConnection>> graph = buildGraph();
        Map<String, PathState> shortest = new HashMap<>();
        PriorityQueue<PathState> queue = new PriorityQueue<>(Comparator.comparingDouble(PathState::distanceMeters));

        PathState start = new PathState(sourceNodeId, 0, 0, List.of(sourceNodeId), List.of());
        shortest.put(sourceNodeId, start);
        queue.add(start);

        while (!queue.isEmpty()) {
            PathState current = queue.poll();
            if (current.nodeId().equals(destinationNodeId)) {
                return new RouteResult(
                        sourceNodeId,
                        destinationNodeId,
                        current.pathNodeIds(),
                        current.pathEdgeIds(),
                        current.distanceMeters(),
                        current.travelSeconds()
                );
            }
            if (current.distanceMeters() > shortest.get(current.nodeId()).distanceMeters()) {
                continue;
            }
            for (EdgeConnection connection : graph.getOrDefault(current.nodeId(), List.of())) {
                double nextDistance = current.distanceMeters() + connection.distanceMeters();
                PathState previousBest = shortest.get(connection.toNodeId());
                if (previousBest != null && previousBest.distanceMeters() <= nextDistance) {
                    continue;
                }
                List<String> nextPath = new ArrayList<>(current.pathNodeIds());
                nextPath.add(connection.toNodeId());
                List<String> nextEdges = new ArrayList<>(current.pathEdgeIds());
                nextEdges.add(connection.edgeId());
                PathState nextState = new PathState(
                        connection.toNodeId(),
                        nextDistance,
                        current.travelSeconds() + connection.estimatedTravelSeconds(),
                        List.copyOf(nextPath),
                        List.copyOf(nextEdges)
                );
                shortest.put(connection.toNodeId(), nextState);
                queue.add(nextState);
            }
        }

        throw new RouteNotFoundException(Map.of(
                "sourceNodeId", sourceNodeId,
                "destinationNodeId", destinationNodeId
        ));
    }

    private void validateNodeExists(String nodeId) {
        if (!fabNodeRepository.existsById(nodeId)) {
            throw new ResourceNotFoundException(ErrorCode.FAB_NODE_NOT_FOUND, Map.of("nodeId", nodeId));
        }
    }

    private Map<String, List<EdgeConnection>> buildGraph() {
        Set<String> nodeIds = new HashSet<>();
        fabNodeRepository.findAll().forEach(node -> nodeIds.add(node.getNodeId()));

        Map<String, List<EdgeConnection>> graph = new HashMap<>();
        for (FabEdge edge : fabEdgeRepository.findAll()) {
            if (edge.isBlocked()
                    || edge.getDistanceMeters() <= 0
                    || !nodeIds.contains(edge.getFromNodeId())
                    || !nodeIds.contains(edge.getToNodeId())) {
                continue;
            }
            addConnection(graph, edge.getFromNodeId(), edge.getToNodeId(), edge);
            addConnection(graph, edge.getToNodeId(), edge.getFromNodeId(), edge);
        }
        return graph;
    }

    private void addConnection(
            Map<String, List<EdgeConnection>> graph,
            String fromNodeId,
            String toNodeId,
            FabEdge edge
    ) {
        graph.computeIfAbsent(fromNodeId, key -> new ArrayList<>())
                .add(new EdgeConnection(
                        edge.getEdgeId(),
                        toNodeId,
                        edge.getDistanceMeters(),
                        edge.getEstimatedTravelSeconds()
                ));
    }

    private record EdgeConnection(
            String edgeId,
            String toNodeId,
            double distanceMeters,
            long estimatedTravelSeconds
    ) {
    }

    private record PathState(
            String nodeId,
            double distanceMeters,
            long travelSeconds,
            List<String> pathNodeIds,
            List<String> pathEdgeIds
    ) {
    }
}
