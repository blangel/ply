package net.ocheyedan.ply.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * User: blangel
 * Date: 11/4/11
 * Time: 9:53 AM
 *
 * A vertex within a {@link Graph<T>}
 * Code influenced by {@see org.codehaus.plexus.util.dag.Vertex}
 */
public final class Vertex<T> {

    private final List<Vertex<T>> parents;

    private final List<Vertex<T>> children;

    private final T value;

    Vertex(T value) {
        this.parents = new ArrayList<Vertex<T>>();
        this.children = new ArrayList<Vertex<T>>();
        this.value = value;
    }

    public void addEdgeTo(Vertex<T> vertex) {
        children.add(vertex);
    }

    public void addEdgeFrom(Vertex<T> vertex) {
        parents.add(vertex);
    }

    public void removeEdgeTo(Vertex<T> vertex) {
        children.remove(vertex);
    }

    public void removeEdgeFrom(Vertex<T> vertex) {
        parents.remove(vertex);
    }

    public boolean hasEdgeTo(Vertex<T> to) {
        return children.contains(to);
    }

    public boolean hasEdgeFrom(Vertex<T> from) {
        return parents.contains(from);
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    public boolean isRoot() {
        return parents.isEmpty();
    }

    public T getValue() {
        return value;
    }

    public boolean isConnected() {
        return (isLeaf() || isRoot());
    }

    List<Vertex<T>> getChildren() {
        return Collections.unmodifiableList(children);
    }

    @Override public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Vertex vertex = (Vertex) o;
        return (value == null ? vertex.value == null : value.equals(vertex.value));
    }

    @Override public int hashCode() {
        return value == null ? 0 : value.hashCode();
    }
}
