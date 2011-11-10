package net.ocheyedan.ply.dep;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

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
        String path = tmp.getPath();
        atom = RepositoryAtom.parse(path);
        assertNotNull(atom);
        assertEquals(RepositoryAtom.Type.ply, atom.type);
        assertEquals("file://" + path, atom.getPropertyName());

        atom = RepositoryAtom.parse("~/");
        assertNotNull(atom);
        assertEquals(RepositoryAtom.Type.ply, atom.type);
        assertEquals("file://" + System.getProperty("user.home"), atom.getPropertyName());
        
    }

}
