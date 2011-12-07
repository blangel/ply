package net.ocheyedan.ply.graph;

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
            List<Vertex<T>> path = getAnyPathToRoot(to); // a path, not necessarily the only or shortest
            String message = String.format("Edge between '%s' and '%s' would introduce a cycle into the graph.",
                    from.getValue().toString(),
                    to.getValue().toString());
            throw new CycleException(message, cycle, path);
        }
    }

    protected List<Vertex<T>> getAnyPathToRoot(Vertex<T> from) {
        List<Vertex<T>> path = new ArrayList<Vertex<T>>();
        while ((from != null) && !from.isRoot()) {
            path.add(from);
            from = from.getAnyParent();
        }
        if ((from != null) && from.isRoot() && !path.isEmpty()) {
            path.add(from);
        }
        Collections.reverse(path);
        return path;
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

    @Override public boolean isCyclic() {
        return false;
    }

    @Override public List<Vertex<T>> getVertices() {
        return new ArrayList<Vertex<T>>(vertices.values());
    }

    @Override public List<Vertex<T>> getRootVertices() {
        ArrayList<Vertex<T>> roots = new ArrayList<Vertex<T>>();
        for (Vertex<T> vertex : vertices.values()) {
            if (vertex.isRoot() && !roots.contains(vertex)) {
                roots.add(vertex);
            }
        }
        return roots;
    }

    @Override public String toString() {
        StringBuilder buffer = new StringBuilder();
        for (Vertex<T> vertex : getRootVertices()) {
            if (buffer.length() > 0) {
                buffer.append("\n");
            }
            buffer.append("[ ");
            buffer.append(vertex.toExtendedString());
            buffer.append(" ]");
        }
        return buffer.toString();
    }
}
