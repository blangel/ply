package net.ocheyedan.ply.graph;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static junit.framework.Assert.*;

/**
 * User: blangel
 * Date: 11/4/11
 * Time: 11:34 AM
 */
public class DirectedAcyclicGraphTest {

    @Test
    public void isCyclic() {
        DirectedAcyclicGraph<String> graph = new DirectedAcyclicGraph<String>();
        assertFalse(graph.isCyclic());
    }

    @Test
    public void getVertex() {
        DirectedAcyclicGraph<String> graph = new DirectedAcyclicGraph<String>();
        assertNull(graph.getVertex("hello"));
        Vertex<String> helloVertex = graph.addVertex("hello");
        assertSame(helloVertex, graph.getVertex("hello"));
    }

    @Test
    public void hasVertex() {
        DirectedAcyclicGraph<String> graph = new DirectedAcyclicGraph<String>();
        assertFalse(graph.hasVertex("hello"));
        graph.addVertex("hello");
        assertTrue(graph.hasVertex("hello"));
    }

    @Test
    public void addVertex() {
        DirectedAcyclicGraph<String> graph = new DirectedAcyclicGraph<String>();
        Vertex<String> helloVertex = graph.addVertex("hello");
        assertSame(helloVertex, graph.getVertex("hello"));
        Vertex<String> helloVertex1 = graph.addVertex("hello");
        assertSame(helloVertex, helloVertex1);
    }

    @Test @SuppressWarnings("unchecked")
    public void addEdge() throws NoSuchMethodException, IllegalAccessException, InstantiationException,
            InvocationTargetException {
        DirectedAcyclicGraph<String> graph = new DirectedAcyclicGraph<String>();
        graph.addEdge(null, null);

        Constructor constructor = Vertex.class.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        Vertex<String> helloVertex = (Vertex<String>) constructor.newInstance("hello");
        graph.addEdge(helloVertex, null);
        assertFalse(graph.hasVertex(helloVertex.getValue()));
        graph.addEdge(null, helloVertex);
        assertFalse(graph.hasVertex(helloVertex.getValue()));
        Vertex<String> hello1Vertex = (Vertex<String>) constructor.newInstance("hello1");
        graph.addEdge(helloVertex, hello1Vertex);
        assertFalse(graph.hasVertex(helloVertex.getValue()));
        assertFalse(graph.hasVertex(hello1Vertex.getValue()));

        helloVertex = graph.addVertex("hello");
        graph.addEdge(helloVertex, hello1Vertex);
        assertFalse(graph.hasVertex(hello1Vertex.getValue()));

        hello1Vertex = graph.addVertex("hello1");
        graph.addEdge(helloVertex, hello1Vertex);
        assertTrue(graph.hasEdge(helloVertex, hello1Vertex));

        // now test cyclic edge
        try {
            graph.addEdge(hello1Vertex, helloVertex);
            fail("Expected a Graph.CycleException");
        } catch (Graph.CycleException gce) {
            List<Vertex<?>> cycle = gce.getCycle();
            assertEquals(3, cycle.size());
            assertEquals(helloVertex, cycle.get(0));
            assertEquals(hello1Vertex, cycle.get(1));
            assertEquals(helloVertex, cycle.get(2));
        }
    }

    @Test
    public void hasEdge() {
        DirectedAcyclicGraph<String> graph = new DirectedAcyclicGraph<String>();
        Vertex<String> helloVertex = graph.addVertex("hello");
        Vertex<String> hello1Vertex = graph.addVertex("hello1");
        graph.addEdge(helloVertex, hello1Vertex);
        assertTrue(graph.hasEdge(helloVertex, hello1Vertex));
        assertFalse(graph.hasEdge(hello1Vertex, helloVertex));
        Vertex<String> anotherVertex = graph.addVertex("another");
        assertFalse(graph.hasEdge(helloVertex, anotherVertex));
        assertFalse(graph.hasEdge(hello1Vertex, anotherVertex));
        assertFalse(graph.hasEdge(anotherVertex, helloVertex));
        assertFalse(graph.hasEdge(anotherVertex, hello1Vertex));
    }

    @Test
    public void removeEdge() {
        DirectedAcyclicGraph<String> graph = new DirectedAcyclicGraph<String>();
        Vertex<String> helloVertex = graph.addVertex("hello");
        Vertex<String> hello1Vertex = graph.addVertex("hello1");
        graph.addEdge(helloVertex, hello1Vertex);
        assertTrue(graph.hasEdge(helloVertex, hello1Vertex));
        graph.removeEdge(hello1Vertex, helloVertex);
        assertTrue(graph.hasEdge(helloVertex, hello1Vertex));
        graph.removeEdge(helloVertex, hello1Vertex);
        assertFalse(graph.hasEdge(helloVertex, hello1Vertex));
    }

    @Test @SuppressWarnings("unchecked")
    public void isReachable() {
        DirectedAcyclicGraph<String> graph = new DirectedAcyclicGraph<String>();
        Vertex<String> helloVertex = graph.addVertex("hello");
        Vertex<String> hello1Vertex = graph.addVertex("hello1");
        Vertex<String> hello2Vertex = graph.addVertex("hello2");
        graph.addEdge(helloVertex, hello1Vertex);
        assertTrue(graph.hasVertex("hello2"));
        assertFalse(graph.hasVertex("hello3"));
        Vertex<String> hello3Vertex = graph.addVertex("hello3");
        graph.addEdge(hello2Vertex, hello3Vertex);
        assertTrue(graph.hasVertex("hello3"));
    }

}
