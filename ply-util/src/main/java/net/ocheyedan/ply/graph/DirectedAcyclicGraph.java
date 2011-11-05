package net.ocheyedan.ply.graph;

import net.ocheyedan.ply.Output;

import java.util.*;

/**
 * User: blangel
 * Date: 11/4/11
 * Time: 9:48 AM
 *
 * A directed acyclic graph data structure.
 * Code influenced by {@see org.codehaus.plexus.util.dag.DAG}
 */
public class DirectedAcyclicGraph<T> implements Graph<T> {

    private final Map<T, Vertex<T>> vertices;

    public DirectedAcyclicGraph() {
        this.vertices = new LinkedHashMap<T, Vertex<T>>();
    }

    @Override public Vertex<T> addVertex(T of) {
        Vertex<T> vertex;
        if (vertices.containsKey(of)) {
            vertex = vertices.get(of);
        } else {
            vertex = new Vertex<T>(of);
            vertices.put(of, vertex);
        }
        return vertex;
    }

    @Override public void addEdge(Vertex<T> from, Vertex<T> to) throws CycleException {
        if ((from == null) || (to == null) || !vertices.containsKey(from.getValue())
                || !vertices.containsKey(to.getValue())) {
            return;
        }
        from.addEdgeTo(to);
        to.addEdgeFrom(from);
        List<Vertex<T>> cycle = CycleDetector.introducesCycle(to);
        if (cycle != null) {
            removeEdge(from, to);
            String message = String.format("Edge between '%s' and '%s' would introduce a cycle into the graph.",
                                           from.getValue().toString(),
                                           to.getValue().toString());
            throw new CycleException(message, cycle);
        }
    }

    @Override public void removeEdge(Vertex<T> from, Vertex<T> to) {
        if ((from == null) || (to == null) || !vertices.containsKey(from.getValue())
                || !vertices.containsKey(to.getValue())) {
            return;
        }
        from.removeEdgeTo(to);
        to.removeEdgeFrom(from);
    }

    @Override public Vertex<T> getVertex(T of) {
        return vertices.get(of);
    }

    @Override public boolean hasVertex(T of) {
        return vertices.containsKey(of);
    }

    @Override public boolean hasEdge(Vertex<T> from, Vertex<T> to) {
        return (from != null) && (to != null) && vertices.containsKey(from.getValue())
                && vertices.containsKey(to.getValue()) && from.hasEdgeTo(to);
    }

    @Override public boolean isReachable(Vertex<T> vertex) {
        return (vertex != null) && (vertices.containsKey(vertex.getValue()) || isReachableFromVertices(vertex));
    }

    @Override public boolean isReachable(T vertexValue) {
        return (vertexValue != null) && (hasVertex(vertexValue) || isReachableFromVertices(new Vertex<T>(vertexValue)));
    }

    private boolean isReachableFromVertices(Vertex<T> vertex) {
        for (Vertex<T> ver : vertices.values()) {
            if (ver.isReachable(vertex)) {
                return true;
            }
        }
        return false;
    }

    @Override public boolean isCyclic() {
        return false;
    }

    @Override public List<Vertex<T>> getVertices() {
        return new ArrayList<Vertex<T>>(vertices.values());
    }
}
