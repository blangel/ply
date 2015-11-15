package net.ocheyedan.ply.dep;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static junit.framework.Assert.*;

/**
 * User: blangel
 * Date: 10/13/11
 * Time: 9:31 PM
 */
public class RepositoryAtomTest {

    @Test
    public void parse() throws IOException {

        RepositoryAtom atom = RepositoryAtom.parse(null);
        assertNull(atom);

        atom = RepositoryAtom.parse("test:file:///anothertest/test.txt");
        assertNotNull(atom);
        assertEquals(RepositoryAtom.Type.ply, atom.type);
        assertEquals("test:file:///anothertest/test.txt", atom.getPropertyName());

        atom = RepositoryAtom.parse("ply:file:///anothertest/test.txt");
        assertNotNull(atom);
        assertEquals(RepositoryAtom.Type.ply, atom.type);
        assertEquals("file:///anothertest/test.txt", atom.getPropertyName());

        atom = RepositoryAtom.parse("maven:http://www.anothertest.com/test.txt");
        assertNotNull(atom);
        assertEquals(RepositoryAtom.Type.maven, atom.type);
        assertEquals("http://www.anothertest.com/test.txt", atom.getPropertyName());

        File tmp = File.createTempFile("test", "parse");
        String path = tmp.getParent(); // parent directory
        atom = RepositoryAtom.parse(path);
        assertNotNull(atom);
        assertEquals(RepositoryAtom.Type.ply, atom.type);
        // mac-adds private/ onto var directories
        String actualPath = atom.getPropertyName();
        if (actualPath.startsWith("file:///private")) {
            assertEquals("file:///private" + path, atom.getPropertyName());
        } else {
            assertEquals("file://" + path, atom.getPropertyName());
        }

        atom = RepositoryAtom.parse("~/");
        assertNotNull(atom);
        assertEquals(RepositoryAtom.Type.ply, atom.type);
        assertEquals("file://" + System.getProperty("user.home"), atom.getPropertyName());

        // local directory reference
        atom = RepositoryAtom.parse("./");
        assertNotNull(atom);
        assertEquals(RepositoryAtom.Type.ply, atom.type);
        assertTrue(atom.getPropertyName().startsWith("file://"));
    }

    @Test
    public void localComparator() {
        RepositoryAtom local = RepositoryAtom.parse("/opt/ply/repo");
        RepositoryAtom remote = RepositoryAtom.parse("http://repo1.maven.org/maven2/");
        List<RepositoryAtom> repos = new ArrayList<RepositoryAtom>(2);
        repos.add(remote);
        repos.add(local);
        Collections.sort(repos, RepositoryAtom.LOCAL_COMPARATOR);
        assertSame(local, repos.get(0));
        assertSame(remote, repos.get(1));
        repos.clear();
        repos.add(local);
        repos.add(remote);
        Collections.sort(repos, RepositoryAtom.LOCAL_COMPARATOR);
        assertSame(local, repos.get(0));
        assertSame(remote, repos.get(1));
    }

}
