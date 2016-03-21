package net.ocheyedan.ply.dep;

import net.ocheyedan.ply.graph.Graph;
import net.ocheyedan.ply.graph.Vertex;

/**
 * User: blangel
 * Date: 3/20/16
 * Time: 12:31 PM
 */
public interface ConflictingVersionVisitor {

    void visit(Dep diffVersionDep, Dep resolvedDep, Vertex<Dep> parentVertex,
               DependencyAtom dependencyAtom, Graph<Dep> graph);

}
