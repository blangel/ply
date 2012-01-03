package net.ocheyedan.ply.ext.cmd.build;

import net.ocheyedan.ply.ext.props.Context;
import net.ocheyedan.ply.ext.props.Prop;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * User: blangel
 * Date: 1/2/12
 * Time: 5:36 PM
 */
public class AliasTest {

    @Test public void parseAlias() {
        // test alias, no expansion
        String name = "clean", value = "\"rm -rf target\"";
        Map<String, Prop> unparsedAliases = new HashMap<String, Prop>();
        Map<String, Alias> aliases = new HashMap<String, Alias>();
        unparsedAliases.put(name, new Prop(new Context("aliases"), name, value, "", Prop.Loc.Local));
        Alias alias = Alias.parseAlias(name, value, unparsedAliases, aliases, new HashSet<String>());
        assertEquals("clean", alias.name);
        assertEquals(1, alias.scripts.size());
        assertEquals("rm -rf target", alias.scripts.get(0).name);
        // test circular reference
        name = "clean";
        value = "\"rm -rf target\" clean";
        aliases = new HashMap<String, Alias>();
        unparsedAliases.clear();
        unparsedAliases.put(name, new Prop(new Context("aliases"), name, value, "", Prop.Loc.Local));
        try {
            Alias.parseAlias(name, value, unparsedAliases, aliases, new HashSet<String>());
            fail("Expected a circular reference exception");
        } catch (Alias.CircularReference cr) {
            // expected
        }
        // test alias expansion
        name = "clean";
        value = "\"rm -rf target\"";
        aliases = new HashMap<String, Alias>();
        unparsedAliases.clear();
        unparsedAliases.put(name, new Prop(new Context("aliases"), name, value, "", Prop.Loc.Local));
        name = "compile";
        value = "clean compiler.jar";
        unparsedAliases.put(name, new Prop(new Context("aliases"), name, value, "", Prop.Loc.Local));
        alias = Alias.parseAlias(name, value, unparsedAliases, aliases, new HashSet<String>());
        assertEquals("compile", alias.name);
        assertEquals(2, alias.scripts.size());
        assertEquals("clean", alias.scripts.get(0).name);
        assertTrue(alias.scripts.get(0).getClass() == Alias.class);
        assertEquals(1, ((Alias) alias.scripts.get(0)).scripts.size());
        assertEquals("rm -rf target", ((Alias) alias.scripts.get(0)).scripts.get(0).name);
        assertEquals("compiler.jar", alias.scripts.get(1).name);
        assertTrue(alias.scripts.get(1).getClass() == Script.class);
        // augment clean for double-alias expansion
        name = "clean";
        value = "\"rm -rf target\" remove";
        aliases = new HashMap<String, Alias>();
        unparsedAliases.put(name, new Prop(new Context("aliases"), name, value, "", Prop.Loc.Local));
        name = "remove";
        value = "remove-1.0.jar";
        unparsedAliases.put(name, new Prop(new Context("aliases"), name, value, "", Prop.Loc.Local));
        name = "compile";
        value = "clean compiler.jar";
        alias = Alias.parseAlias(name, value, unparsedAliases, aliases, new HashSet<String>());
        assertEquals("compile", alias.name);
        assertEquals(2, alias.scripts.size());
        assertEquals("clean", alias.scripts.get(0).name);
        assertTrue(alias.scripts.get(0).getClass() == Alias.class);
        assertEquals(2, ((Alias) alias.scripts.get(0)).scripts.size());
        assertEquals("rm -rf target", ((Alias) alias.scripts.get(0)).scripts.get(0).name);
        assertTrue(((Alias) alias.scripts.get(0)).scripts.get(0).getClass() == Script.class);
        assertEquals("remove", ((Alias) alias.scripts.get(0)).scripts.get(1).name);
        assertTrue(((Alias) alias.scripts.get(0)).scripts.get(1).getClass() == Alias.class);
        assertEquals(1, ((Alias) ((Alias) alias.scripts.get(0)).scripts.get(1)).scripts.size());
        assertEquals("remove-1.0.jar", ((Alias) ((Alias) alias.scripts.get(0)).scripts.get(1)).scripts.get(0).name);
        assertTrue(((Alias) ((Alias) alias.scripts.get(0)).scripts.get(1)).scripts.get(0).getClass() == Script.class);
        assertEquals("compiler.jar", alias.scripts.get(1).name);
        assertTrue(alias.scripts.get(1).getClass() == Script.class);
        // circular exception from double-alias expansion
        aliases = new HashMap<String, Alias>();
        name = "remove";
        value = "remove-1.0.jar clean";
        unparsedAliases.put(name, new Prop(new Context("aliases"), name, value, "", Prop.Loc.Local));
        name = "compile";
        value = "clean compiler.jar";
        try {
            Alias.parseAlias(name, value, unparsedAliases, aliases, new HashSet<String>());
            fail("Expected a circular reference exception");
        } catch (Alias.CircularReference cr) {
            // expected
        }
    }

}
