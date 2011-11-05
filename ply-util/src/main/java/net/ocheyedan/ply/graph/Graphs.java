package net.ocheyedan.ply.graph;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: blangel
 * Date: 11/5/11
 * Time: 9:01 AM
 *
 * A utility class for {@link Graph} objects.
 */
public final class Graphs {

    public static interface Visitor<T> {
        void visit(Vertex<T> vertex);
    }

    private final static Integer NOT_VISTITED = 0;
    private final static Integer VISITING = 1;
    private final static Integer VISITED = 2;

    /**
     * TODO - augment to support CycleDetector needs
     * Visits each {@link Vertex} within {@code graph} once and only once.
     * @param graph to visit
     * @param visitor the visitor object
     * @param <T> type of the value of {@link Vertex} objects within {@code graph}
     */
    public static <T> void visit(Graph<T> graph, Visitor<T> visitor) {
        final List<Vertex<T>> vertices = graph.getVertices();
        final Map<Vertex<T>, Integer> vertexStateMap = new HashMap<Vertex<T>, Integer>();
        for (final Vertex<T> vertex : vertices) {
            visit(vertex, visitor, vertexStateMap);
        }
    }

    private static <T> void visit(Vertex<T> vertex, Visitor<T> visitor, Map<Vertex<T>, Integer> vertexStateMap) {
        if (isNotVisited(vertex, vertexStateMap)) {
            vertexStateMap.put(vertex, VISITING);
            visitor.visit(vertex);
            for (Vertex<T> child : vertex.getChildren()) {
                visit(child, visitor, vertexStateMap);
            }
            vertexStateMap.put(vertex, VISITED);
        }
    }

    private static <T> boolean isNotVisited(final Vertex<T> vertex, final Map<Vertex<T>, Integer> vertexStateMap) {
        if (!vertexStateMap.containsKey(vertex)) {
            return true;
        }
        final Integer state = vertexStateMap.get(vertex);
        return NOT_VISTITED.equals(state);
    }

}
