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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;

/**
 * User: blangel
 * Date: 12/10/11
 * Time: 8:20 AM
 */
public class DepsTest {

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
        List<DependencyAtom> deps = Deps.parse(pom.dependencies);
        assertEquals(3, deps.size());

        Map<DependencyAtom, List<DependencyAtom>> synthetic = new HashMap<DependencyAtom, List<DependencyAtom>>(1);
        for (DependencyAtom atom : deps) {
            if ("slf4j-api".equals(atom.name)) {
                synthetic.put(atom, Collections.<DependencyAtom>emptyList());
            }
        }
        DirectedAcyclicGraph<Dep> graph = Deps.getDependencyGraph(deps, new RepositoryRegistry(mockRepo, null, synthetic));
        List<Vertex<Dep>> resolved = graph.getVertices();
        assertEquals(3, resolved.size());
    }

}
