package net.ocheyedan.ply.graph;

import java.util.*;

/**
 * User: blangel
 * Date: 11/4/11
 * Time: 10:49 AM
 *
 * Determines whether a {@link Graph} object contains cycles.
 * Code taken from {@see org.codehaus.plexus.util.dag.CycleDetector}
 */
public class CycleDetector {

    private final static Integer NOT_VISTITED = 0;
    private final static Integer VISITING = 1;
    private final static Integer VISITED = 2;

    public static <T> List<Vertex<T>> hasCycle(Graph<T> graph) {
        final List<Vertex<T>> vertices = graph.getVertices();
        final Map<Vertex<T>, Integer> vertexStateMap = new HashMap<Vertex<T>, Integer>();
        List<Vertex<T>> cycle = null;
        for (final Vertex<T> vertex : vertices) {
            if (isNotVisited(vertex, vertexStateMap)) {
                cycle = introducesCycle(vertex, vertexStateMap);
                if (cycle != null) {
                    break;
                }
            }
        }
        return cycle;
    }

    public static <T> List<Vertex<T>> introducesCycle(final Vertex<T> vertex) {
        final Map<Vertex<T>, Integer> vertexStateMap = new HashMap<Vertex<T>, Integer>();
        return introducesCycle(vertex, vertexStateMap);
    }

    public static <T> List<Vertex<T>> introducesCycle(final Vertex<T> vertex, final Map<Vertex<T>, Integer> vertexStateMap) {
        final LinkedList<Vertex<T>> cycleStack = new LinkedList<Vertex<T>>();
        final boolean hasCycle = dfsVisit(vertex, cycleStack, vertexStateMap);
        if (hasCycle) {
            // we have a situation like: [b, a, c, d, b, f, g, h].
            // Label of Vertex which introduced  the cycle is at the first position in the list
            // We have to find second occurence of this label and use its position in the list
            // for getting the sublist of vertex labels of cycle paricipants
            //
            // So in our case we are seraching for [b, a, c, d, b]
            final Vertex<T> first = cycleStack.getFirst();
            final int pos = cycleStack.lastIndexOf(first);
            final List<Vertex<T>> cycle = cycleStack.subList( 0, pos + 1 );
            Collections.reverse(cycle);
            return cycle;
        }

        return null;
    }

    private static <T> boolean isNotVisited(final Vertex<T> vertex, final Map<Vertex<T>, Integer> vertexStateMap) {
        if (!vertexStateMap.containsKey(vertex)) {
            return true;
        }
        final Integer state = vertexStateMap.get(vertex);
        return NOT_VISTITED.equals( state );
    }

    private static <T> boolean isVisiting(final Vertex<T> vertex, final Map<Vertex<T>, Integer> vertexStateMap) {
        final Integer state = vertexStateMap.get(vertex);
        return VISITING.equals(state);
    }

    private static <T> boolean dfsVisit(final Vertex<T> vertex, final LinkedList<Vertex<T>> cycle, final Map<Vertex<T>, Integer> vertexStateMap) {
        cycle.addFirst(vertex);
        vertexStateMap.put(vertex, VISITING);
        final List<Vertex<T>> vertices = vertex.getChildren();
        for (final Vertex<T> childVertex : vertices) {
            if (isNotVisited(childVertex, vertexStateMap)) {
                final boolean hasCycle = dfsVisit(childVertex, cycle, vertexStateMap);
                if (hasCycle) {
                    return true;
                }
            } else if (isVisiting(childVertex, vertexStateMap)) {
                cycle.addFirst(childVertex);
                return true;
            }
        }
        vertexStateMap.put(vertex, VISITED);
        cycle.removeFirst();
        return false;
    }

}
