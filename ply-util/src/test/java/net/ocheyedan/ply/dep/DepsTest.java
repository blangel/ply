package net.ocheyedan.ply.dep;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.PlyUtil;
import net.ocheyedan.ply.graph.DirectedAcyclicGraph;
import net.ocheyedan.ply.graph.Vertex;
import net.ocheyedan.ply.mvn.MavenPom;
import net.ocheyedan.ply.mvn.MavenPomParser;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertSame;

/**
 * User: blangel
 * Date: 12/10/11
 * Time: 8:20 AM
 */
public class DepsTest {

    @Test
    public void getMinimumVersion() {

        DependencyAtom left = DependencyAtom.parse("foo:foo:1.0", new AtomicReference<String>());
        DependencyAtom right = DependencyAtom.parse("foo:foo:1.0", new AtomicReference<String>());
        DependencyAtom result = Deps.getMinimumVersion(left, right);
        assertNull(result);

        left = DependencyAtom.parse("foo:foo:1.0.0", new AtomicReference<String>());
        right = DependencyAtom.parse("foo:foo:1.0", new AtomicReference<String>());
        result = Deps.getMinimumVersion(left, right);
        assertSame(right, result);

        left = DependencyAtom.parse("foo:foo:1.0.0", new AtomicReference<String>());
        right = DependencyAtom.parse("foo:foo:1.0.1", new AtomicReference<String>());
        result = Deps.getMinimumVersion(left, right);
        assertSame(left, result);

        left = DependencyAtom.parse("foo:foo:2.0.0", new AtomicReference<String>());
        right = DependencyAtom.parse("foo:foo:1.0.1", new AtomicReference<String>());
        result = Deps.getMinimumVersion(left, right);
        assertSame(right, result);

        left = DependencyAtom.parse("foo:foo:2.9", new AtomicReference<String>());
        right = DependencyAtom.parse("foo:foo:2.10", new AtomicReference<String>());
        result = Deps.getMinimumVersion(left, right);
        assertSame(left, result);

        left = DependencyAtom.parse("foo:foo:2.9-rc", new AtomicReference<String>());
        right = DependencyAtom.parse("foo:foo:2.10", new AtomicReference<String>());
        result = Deps.getMinimumVersion(left, right);
        assertSame(left, result);

        left = DependencyAtom.parse("foo:foo:2.10-rc", new AtomicReference<String>());
        right = DependencyAtom.parse("foo:foo:2.10", new AtomicReference<String>());
        result = Deps.getMinimumVersion(left, right);
        assertSame(left, result);

        left = DependencyAtom.parse("foo:foo:2.10", new AtomicReference<String>());
        right = DependencyAtom.parse("foo:foo:2.10-SNAPSHOT", new AtomicReference<String>());
        result = Deps.getMinimumVersion(left, right);
        assertSame(right, result);

        left = DependencyAtom.parse("foo:foo:2.10.v20150330", new AtomicReference<String>());
        right = DependencyAtom.parse("foo:foo:2.10.v20150331", new AtomicReference<String>());
        result = Deps.getMinimumVersion(left, right);
        assertSame(left, result);

        left = DependencyAtom.parse("foo:foo:2.10.v20150330", new AtomicReference<String>());
        right = DependencyAtom.parse("foo:foo:2.10.V20150401", new AtomicReference<String>());
        result = Deps.getMinimumVersion(left, right);
        assertSame(left, result);

        left = DependencyAtom.parse("foo:foo:2.10.V20150509", new AtomicReference<String>());
        right = DependencyAtom.parse("foo:foo:2.10.V20150401", new AtomicReference<String>());
        result = Deps.getMinimumVersion(left, right);
        assertSame(right, result);
    }

    @Test
    public void getDependencyGraph() throws URISyntaxException {

        MavenPomParser parser = new MavenPomParser();
        String plyUtilPath = FileUtil.pathFromParts(PlyUtil.LOCAL_PROJECT_DIR.getAbsolutePath(), "..");
        if (!plyUtilPath.contains("ply-util")) {
            plyUtilPath = plyUtilPath.replace(".ply", FileUtil.pathFromParts("ply-util", ".ply"));
        }
        String repoPath = FileUtil.pathFromParts(plyUtilPath, "src", "test", "resources", "mock-mvn-repo");
        RepositoryAtom mockRepo = new RepositoryAtom(repoPath, new URI("file://" + repoPath), RepositoryAtom.Type.maven);

        MavenPom pom = parser.parsePom("file://" + repoPath + "/net/sf/ehcache/ehcache-core/2.2.0/ehcache-core-2.2.0.pom", mockRepo);
        List<DependencyAtom> deps = Deps.parse(pom.dependencies, null);
        assertEquals(3, deps.size());

        Map<DependencyAtom, List<DependencyAtom>> synthetic = new HashMap<DependencyAtom, List<DependencyAtom>>(1);
        for (DependencyAtom atom : deps) {
            if ("slf4j-api".equals(atom.name)) {
                synthetic.put(atom, Collections.<DependencyAtom>emptyList());
            }
        }
        DirectedAcyclicGraph<Dep> graph = Deps.getDependencyGraph(deps, Collections.<DependencyAtom>emptySet(), new RepositoryRegistry(mockRepo, null, synthetic));
        List<Vertex<Dep>> resolved = graph.getVertices();
        assertEquals(3, resolved.size());

        // now, rerun but include stax:stax-api:1.0.1 as a transitive dependency
        synthetic = new HashMap<DependencyAtom, List<DependencyAtom>>(1);
        for (DependencyAtom atom : deps) {
            if ("slf4j-api".equals(atom.name)) {
                List<DependencyAtom> slf4jDependencies = new ArrayList<DependencyAtom>(1);
                slf4jDependencies.add(new DependencyAtom("stax", "stax-api", "1.0.1"));
                synthetic.put(atom, slf4jDependencies);
            }
        }
        graph = Deps.getDependencyGraph(deps, Collections.<DependencyAtom>emptySet(), new RepositoryRegistry(mockRepo, null, synthetic));
        resolved = graph.getVertices();
        assertEquals(4, resolved.size());

        // now, rerun but exclude the stax:stax-api:1.0.1 transitive dependency
        Set<DependencyAtom> exclusions = new HashSet<DependencyAtom>(1);
        exclusions.add(new DependencyAtom("stax", "stax-api", "1.0.1"));
        graph = Deps.getDependencyGraph(deps, exclusions, new RepositoryRegistry(mockRepo, null, synthetic));
        resolved = graph.getVertices();
        assertEquals(3, resolved.size());
    }

}
