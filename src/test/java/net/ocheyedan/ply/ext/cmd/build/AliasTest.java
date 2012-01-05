package net.ocheyedan.ply.ext.cmd.build;

import net.ocheyedan.ply.ext.props.Context;
import net.ocheyedan.ply.ext.props.Prop;
import net.ocheyedan.ply.ext.props.Scope;
import net.ocheyedan.ply.graph.DirectedAcyclicGraph;
import org.junit.Test;

import java.util.*;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * User: blangel
 * Date: 1/2/12
 * Time: 5:36 PM
 */
public class AliasTest {

    @Test public void splitScript() {
        List<String> scripts = Alias.splitScript(null);
        assertEquals(0, scripts.size());
        String script = "";
        scripts = Alias.splitScript(script);
        assertEquals(0, scripts.size());
        script = "test";
        scripts = Alias.splitScript(script);
        assertEquals(1, scripts.size());
        assertEquals("test", scripts.get(0));
        script = "test another";
        scripts = Alias.splitScript(script);
        assertEquals(2, scripts.size());
        assertEquals("test", scripts.get(0));
        assertEquals("another", scripts.get(1));
        script = "test another 'more'";
        scripts = Alias.splitScript(script);
        assertEquals(3, scripts.size());
        assertEquals("test", scripts.get(0));
        assertEquals("another", scripts.get(1));
        assertEquals("more", scripts.get(2));
        script = "\"test\"";
        scripts = Alias.splitScript(script);
        assertEquals(1, scripts.size());
        assertEquals("test", scripts.get(0));
        script = "\"test\" another";
        scripts = Alias.splitScript(script);
        assertEquals(2, scripts.size());
        assertEquals("test", scripts.get(0));
        assertEquals("another", scripts.get(1));
        script = "another \"test\"";
        scripts = Alias.splitScript(script);
        assertEquals(2, scripts.size());
        assertEquals("another", scripts.get(0));
        assertEquals("test", scripts.get(1));
        script = "\"another\" \"test\"";
        scripts = Alias.splitScript(script);
        assertEquals(2, scripts.size());
        assertEquals("another", scripts.get(0));
        assertEquals("test", scripts.get(1));
        script = "another \"test\" diff";
        scripts = Alias.splitScript(script);
        assertEquals(3, scripts.size());
        assertEquals("another", scripts.get(0));
        assertEquals("test", scripts.get(1));
        assertEquals("diff", scripts.get(2));
        script = "\"another\" \"test\" \"diff\"";
        scripts = Alias.splitScript(script);
        assertEquals(3, scripts.size());
        assertEquals("another", scripts.get(0));
        assertEquals("test", scripts.get(1));
        assertEquals("diff", scripts.get(2));
        script = "\"another\" test \"diff\"";
        scripts = Alias.splitScript(script);
        assertEquals(3, scripts.size());
        assertEquals("another", scripts.get(0));
        assertEquals("test", scripts.get(1));
        assertEquals("diff", scripts.get(2));

        script = "another \"test and\" diff";
        scripts = Alias.splitScript(script);
        assertEquals(3, scripts.size());
        assertEquals("another", scripts.get(0));
        assertEquals("test and", scripts.get(1));
        assertEquals("diff", scripts.get(2));
        script = "\"another more\" \"test more\" \"diff more\"";
        scripts = Alias.splitScript(script);
        assertEquals(3, scripts.size());
        assertEquals("another more", scripts.get(0));
        assertEquals("test more", scripts.get(1));
        assertEquals("diff more", scripts.get(2));
        script = "\"another more\" test \"diff more\"";
        scripts = Alias.splitScript(script);
        assertEquals(3, scripts.size());
        assertEquals("another more", scripts.get(0));
        assertEquals("test", scripts.get(1));
        assertEquals("diff more", scripts.get(2));

        script = "another \"test and more\" diff";
        scripts = Alias.splitScript(script);
        assertEquals(3, scripts.size());
        assertEquals("another", scripts.get(0));
        assertEquals("test and more", scripts.get(1));
        assertEquals("diff", scripts.get(2));
        script = "\"another and more\" \"test and more\" \"diff and more\"";
        scripts = Alias.splitScript(script);
        assertEquals(3, scripts.size());
        assertEquals("another and more", scripts.get(0));
        assertEquals("test and more", scripts.get(1));
        assertEquals("diff and more", scripts.get(2));
        script = "\"another and more\" test \"diff and more\"";
        scripts = Alias.splitScript(script);
        assertEquals(3, scripts.size());
        assertEquals("another and more", scripts.get(0));
        assertEquals("test", scripts.get(1));
        assertEquals("diff and more", scripts.get(2));

        // test nested
        script = "another \"test 'and' more\" diff";
        scripts = Alias.splitScript(script);
        assertEquals(3, scripts.size());
        assertEquals("another", scripts.get(0));
        assertEquals("test 'and' more", scripts.get(1));
        assertEquals("diff", scripts.get(2));
        script = "\"another 'and more'\" \"test 'and more'\" \"diff 'and more'\"";
        scripts = Alias.splitScript(script);
        assertEquals(3, scripts.size());
        assertEquals("another 'and more'", scripts.get(0));
        assertEquals("test 'and more'", scripts.get(1));
        assertEquals("diff 'and more'", scripts.get(2));
        script = "'\"another and\" more' test '\"diff and\" more'";
        scripts = Alias.splitScript(script);
        assertEquals(3, scripts.size());
        assertEquals("\"another and\" more", scripts.get(0));
        assertEquals("test", scripts.get(1));
        assertEquals("\"diff and\" more", scripts.get(2));
    }

    @Test public void parseAlias() {
        // test alias, no expansion
        String name = "clean", value = "\"rm -rf target\"";
        Map<String, Prop> unparsedAliases = new HashMap<String, Prop>();
        unparsedAliases.put(name, new Prop(new Context("aliases"), name, value, "", Prop.Loc.Local));
        Alias alias = Alias.parseAlias(Scope.Default, name, value, unparsedAliases, new DirectedAcyclicGraph<String>());
        assertEquals("clean", alias.name);
        assertEquals(1, alias.scripts.size());
        assertEquals("rm -rf target", alias.scripts.get(0).name);
        // test circular reference
        name = "clean";
        value = "\"rm -rf target\" clean";
        unparsedAliases.clear();
        unparsedAliases.put(name, new Prop(new Context("aliases"), name, value, "", Prop.Loc.Local));
        try {
            Alias.parseAlias(Scope.Default, name, value, unparsedAliases, new DirectedAcyclicGraph<String>());
            fail("Expected a circular reference exception");
        } catch (Alias.CircularReference cr) {
            // expected
        }
        // test alias expansion
        name = "clean";
        value = "\"rm -rf target\"";
        unparsedAliases.clear();
        unparsedAliases.put(name, new Prop(new Context("aliases"), name, value, "", Prop.Loc.Local));
        name = "compile";
        value = "clean compiler.jar";
        unparsedAliases.put(name, new Prop(new Context("aliases"), name, value, "", Prop.Loc.Local));
        alias = Alias.parseAlias(Scope.Default, name, value, unparsedAliases, new DirectedAcyclicGraph<String>());
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
        unparsedAliases.put(name, new Prop(new Context("aliases"), name, value, "", Prop.Loc.Local));
        name = "remove";
        value = "remove-1.0.jar";
        unparsedAliases.put(name, new Prop(new Context("aliases"), name, value, "", Prop.Loc.Local));
        name = "compile";
        value = "clean compiler.jar";
        alias = Alias.parseAlias(Scope.Default, name, value, unparsedAliases, new DirectedAcyclicGraph<String>());
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
        name = "remove";
        value = "remove-1.0.jar clean";
        unparsedAliases.put(name, new Prop(new Context("aliases"), name, value, "", Prop.Loc.Local));
        name = "compile";
        value = "clean compiler.jar";
        try {
            Alias.parseAlias(Scope.Default, name, value, unparsedAliases, new DirectedAcyclicGraph<String>());
            fail("Expected a circular reference exception");
        } catch (Alias.CircularReference cr) {
            // expected
        }
        // pre-seed cache with 'test' scope for this test
        Map<String, Alias> testAliases = new HashMap<String, Alias>(1);
        List<Script> removeAliasScripts = new ArrayList<Script>(1);
        removeAliasScripts.add(new Script("remove-1.0.jar", new Scope("test")));
        testAliases.put("remove", new Alias("remove", new Scope("test"), removeAliasScripts));
        Alias.cache.put(new Scope("test"), testAliases);
        // test alias/script mapped to different scope
        name = "clean";
        value = "\"rm -rf target\" test:remove";
        unparsedAliases.clear();
        unparsedAliases.put(name, new Prop(new Context("aliases"), name, value, "", Prop.Loc.Local));
        name = "remove"; // since clean references this alias from a scope, needs to be part of ply's own aliases
        value = "remove-1.0.jar";
        unparsedAliases.put(name, new Prop(new Context("aliases"), name, value, "", Prop.Loc.Local));
        name = "compile";
        value = "clean test:compiler.jar";
        unparsedAliases.put(name, new Prop(new Context("aliases"), name, value, "", Prop.Loc.Local));
        alias = Alias.parseAlias(Scope.Default, name, value, unparsedAliases, new DirectedAcyclicGraph<String>());
        assertEquals("compile", alias.name);
        assertEquals(Scope.Default, alias.scope);
        assertEquals(2, alias.scripts.size());
        assertEquals("clean", alias.scripts.get(0).name);
        assertEquals(Scope.Default, alias.scripts.get(0).scope);
        assertTrue(alias.scripts.get(0).getClass() == Alias.class);
        assertEquals(2, ((Alias) alias.scripts.get(0)).scripts.size());
        assertEquals("rm -rf target", ((Alias) alias.scripts.get(0)).scripts.get(0).name);
        assertEquals(Scope.Default, ((Alias) alias.scripts.get(0)).scripts.get(0).scope);
        assertTrue(((Alias) alias.scripts.get(0)).scripts.get(0).getClass() == Script.class);
        assertEquals("remove", ((Alias) alias.scripts.get(0)).scripts.get(1).name);
        assertEquals(new Scope("test"), ((Alias) alias.scripts.get(0)).scripts.get(1).scope);
        assertTrue(((Alias) alias.scripts.get(0)).scripts.get(1).getClass() == Alias.class);
        assertEquals(1, ((Alias) ((Alias) alias.scripts.get(0)).scripts.get(1)).scripts.size());
        assertEquals("remove-1.0.jar", ((Alias) ((Alias) alias.scripts.get(0)).scripts.get(1)).scripts.get(0).name);
        assertEquals(new Scope("test"), ((Alias) ((Alias) alias.scripts.get(0)).scripts.get(1)).scripts.get(0).scope);
        assertTrue(((Alias) ((Alias) alias.scripts.get(0)).scripts.get(1)).scripts.get(0).getClass() == Script.class);
        assertEquals("compiler.jar", alias.scripts.get(1).name);
        assertTrue(alias.scripts.get(1).getClass() == Script.class);
        assertEquals("test", alias.scripts.get(1).scope.name);
        // test where an alias maps to two aliases which themselves both map to the same alias (but non-cyclic) which
        // is a valid case
        name = "clean";
        value = "resolve clean-1.0.jar";
        unparsedAliases.clear();
        unparsedAliases.put(name, new Prop(new Context("aliases"), name, value, "", Prop.Loc.Local));
        name = "resolve";
        value = "resolve-1.0.jar";
        unparsedAliases.put(name, new Prop(new Context("aliases"), name, value, "", Prop.Loc.Local));
        name = "compile";
        value = "clean resolve compiler.jar";
        unparsedAliases.put(name, new Prop(new Context("aliases"), name, value, "", Prop.Loc.Local));
        alias = Alias.parseAlias(Scope.Default, name, value, unparsedAliases, new DirectedAcyclicGraph<String>());
        assertEquals("compile", alias.name);
        assertEquals(Scope.Default, alias.scope);
        assertEquals(3, alias.scripts.size());
        assertEquals("clean", alias.scripts.get(0).name);
        assertEquals(Scope.Default, alias.scripts.get(0).scope);
        assertTrue(alias.scripts.get(0).getClass() == Alias.class);
        assertEquals(2, ((Alias) alias.scripts.get(0)).scripts.size());
        assertEquals("resolve", ((Alias) alias.scripts.get(0)).scripts.get(0).name);
        assertEquals(Scope.Default, ((Alias) alias.scripts.get(0)).scripts.get(0).scope);
        assertTrue(((Alias) alias.scripts.get(0)).scripts.get(0).getClass() == Alias.class);
        assertEquals(1, ((Alias) ((Alias) alias.scripts.get(0)).scripts.get(0)).scripts.size());
        assertEquals("resolve-1.0.jar", ((Alias) ((Alias) alias.scripts.get(0)).scripts.get(0)).scripts.get(0).name);
        assertEquals(Scope.Default, ((Alias) ((Alias) alias.scripts.get(0)).scripts.get(0)).scripts.get(0).scope);
        assertTrue(((Alias) ((Alias) alias.scripts.get(0)).scripts.get(0)).scripts.get(0).getClass() == Script.class);
        assertEquals("clean-1.0.jar", ((Alias) alias.scripts.get(0)).scripts.get(1).name);
        assertEquals(Scope.Default, ((Alias) alias.scripts.get(0)).scripts.get(1).scope);
        assertTrue(((Alias) alias.scripts.get(0)).scripts.get(1).getClass() == Script.class);
        assertEquals("resolve", alias.scripts.get(1).name);
        assertEquals(Scope.Default, alias.scripts.get(1).scope);
        assertTrue(alias.scripts.get(1).getClass() == Alias.class);
        assertEquals(1, ((Alias) alias.scripts.get(1)).scripts.size());
        assertEquals("resolve-1.0.jar", ((Alias) alias.scripts.get(1)).scripts.get(0).name);
        assertTrue(((Alias) alias.scripts.get(1)).scripts.get(0).getClass() == Script.class);
        assertEquals(Scope.Default, ((Alias) alias.scripts.get(1)).scripts.get(0).scope);
        assertEquals("compiler.jar", alias.scripts.get(2).name);
        assertEquals(Scope.Default, alias.scripts.get(2).scope);
        assertTrue(alias.scripts.get(2).getClass() == Script.class);
    }

}