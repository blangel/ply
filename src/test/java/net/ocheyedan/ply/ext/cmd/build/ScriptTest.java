package net.ocheyedan.ply.ext.cmd.build;

import net.ocheyedan.ply.ext.props.Scope;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

/**
 * User: blangel
 * Date: 1/5/12
 * Time: 1:10 PM
 */
public class ScriptTest {

    @Test public void parse() {
        Scope scope = Scope.Default;
        String scriptName = null;
        Script script = Script.parse(scriptName, scope);
        assertNull(script.name);
        assertEquals(scope, script.scope);

        scriptName = "";
        script = Script.parse(scriptName, scope);
        assertEquals("", script.name);
        assertEquals(scope, script.scope);

        scriptName = "clean";
        script = Script.parse(scriptName, scope);
        assertEquals("clean", script.name);
        assertEquals(scope, script.scope);

        scriptName = "test:clean";
        script = Script.parse(scriptName, scope);
        assertEquals("clean", script.name);
        assertEquals(new Scope("test"), script.scope);

        scriptName = "\"java clean:test\"";
        script = Script.parse(scriptName, scope);
        assertEquals("\"java clean:test\"", script.name);
        assertEquals(scope, script.scope);
    }

}
