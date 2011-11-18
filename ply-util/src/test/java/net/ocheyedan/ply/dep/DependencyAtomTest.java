package net.ocheyedan.ply.dep;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import static junit.framework.Assert.*;

/**
 * User: blangel
 * Date: 10/13/11
 * Time: 9:19 PM
 */
public class DependencyAtomTest {

    @Test
    public void parse() {
        DependencyAtom atom = DependencyAtom.parse(null, null);
        assertNull(atom);

        atom = DependencyAtom.parse("", null);
        assertNull(atom);

        AtomicReference<String> error = new AtomicReference<String>();
        atom = DependencyAtom.parse("", error);
        assertNull(atom);
        assertEquals("namespace, name and version", error.get());

        error.set(null);
        atom = DependencyAtom.parse("test", error);
        assertNull(atom);
        assertEquals("name and version", error.get());

        error.set(null);
        atom = DependencyAtom.parse("test and more", error);
        assertNull(atom);
        assertEquals("Spaces not allowed in dependency atoms.", error.get());

        error.set(null);
        atom = DependencyAtom.parse(" test ", error); // will trim though
        assertNull(atom);
        assertEquals("name and version", error.get());

        error.set(null);
        atom = DependencyAtom.parse("namespace:name", error);
        assertNull(atom);
        assertEquals("version", error.get());

        error.set(null);
        atom = DependencyAtom.parse("namespace:name:version:artifact-name.zip:nottransient", error);
        assertNull(atom);
        assertEquals("transient", error.get());

        error.set(null);
        atom = DependencyAtom.parse("namespace:name:version", error);
        assertNotNull(atom);
        assertNull(error.get());
        assertEquals("namespace", atom.namespace);
        assertEquals("name", atom.name);
        assertEquals("version", atom.version);
        assertNull(atom.artifactName);
        assertEquals("version:name-version." + DependencyAtom.DEFAULT_PACKAGING, atom.getResolvedPropertyValue());
        assertFalse(atom.transientDep);

        error.set(null);
        atom = DependencyAtom.parse("namespace:name:version:artifact-name.zip", error);
        assertNotNull(atom);
        assertNull(error.get());
        assertEquals("namespace", atom.namespace);
        assertEquals("name", atom.name);
        assertEquals("version", atom.version);
        assertEquals("artifact-name.zip", atom.artifactName);
        assertEquals("version:artifact-name.zip", atom.getResolvedPropertyValue());
        assertFalse(atom.transientDep);

        error.set(null);
        atom = DependencyAtom.parse("namespace:name:version:transient", error);
        assertNotNull(atom);
        assertNull(error.get());
        assertEquals("namespace", atom.namespace);
        assertEquals("name", atom.name);
        assertEquals("version", atom.version);
        assertNull(atom.artifactName);
        assertEquals("version:name-version." + DependencyAtom.DEFAULT_PACKAGING + ":transient", atom.getResolvedPropertyValue());
        assertTrue(atom.transientDep);

        error.set(null);
        atom = DependencyAtom.parse("namespace:name:version:artifact-name.zip:transient", error);
        assertNotNull(atom);
        assertNull(error.get());
        assertEquals("namespace", atom.namespace);
        assertEquals("name", atom.name);
        assertEquals("version", atom.version);
        assertEquals("artifact-name.zip", atom.artifactName);
        assertEquals("version:artifact-name.zip:transient", atom.getResolvedPropertyValue());
        assertTrue(atom.transientDep);

    }

}
