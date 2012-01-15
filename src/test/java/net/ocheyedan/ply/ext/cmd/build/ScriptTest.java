package net.ocheyedan.ply.ext.cmd.build;

import net.ocheyedan.ply.ext.props.Scope;
import org.junit.Test;

import java.util.List;

import static junit.framework.Assert.*;

/**
 * User: blangel
 * Date: 1/5/12
 * Time: 1:10 PM
 */
public class ScriptTest {

    @Test public void splitScript() {
        List<String> scripts = Script.splitScript(null);
        assertEquals(0, scripts.size());
        String script = "";
        scripts = Script.splitScript(script);
        assertEquals(0, scripts.size());
        script = "test";
        scripts = Script.splitScript(script);
        assertEquals(1, scripts.size());
        assertEquals("test", scripts.get(0));
        script = "test another";
        scripts = Script.splitScript(script);
        assertEquals(2, scripts.size());
        assertEquals("test", scripts.get(0));
        assertEquals("another", scripts.get(1));
        script = "test another 'more'";
        scripts = Script.splitScript(script);
        assertEquals(3, scripts.size());
        assertEquals("test", scripts.get(0));
        assertEquals("another", scripts.get(1));
        assertEquals("more", scripts.get(2));
        script = "\"test\"";
        scripts = Script.splitScript(script);
        assertEquals(1, scripts.size());
        assertEquals("test", scripts.get(0));
        script = "\"test\" another";
        scripts = Script.splitScript(script);
        assertEquals(2, scripts.size());
        assertEquals("test", scripts.get(0));
        assertEquals("another", scripts.get(1));
        script = "another \"test\"";
        scripts = Script.splitScript(script);
        assertEquals(2, scripts.size());
        assertEquals("another", scripts.get(0));
        assertEquals("test", scripts.get(1));
        script = "\"another\" \"test\"";
        scripts = Script.splitScript(script);
        assertEquals(2, scripts.size());
        assertEquals("another", scripts.get(0));
        assertEquals("test", scripts.get(1));
        script = "another \"test\" diff";
        scripts = Script.splitScript(script);
        assertEquals(3, scripts.size());
        assertEquals("another", scripts.get(0));
        assertEquals("test", scripts.get(1));
        assertEquals("diff", scripts.get(2));
        script = "\"another\" \"test\" \"diff\"";
        scripts = Script.splitScript(script);
        assertEquals(3, scripts.size());
        assertEquals("another", scripts.get(0));
        assertEquals("test", scripts.get(1));
        assertEquals("diff", scripts.get(2));
        script = "\"another\" test \"diff\"";
        scripts = Script.splitScript(script);
        assertEquals(3, scripts.size());
        assertEquals("another", scripts.get(0));
        assertEquals("test", scripts.get(1));
        assertEquals("diff", scripts.get(2));

        script = "another \"test and\" diff";
        scripts = Script.splitScript(script);
        assertEquals(3, scripts.size());
        assertEquals("another", scripts.get(0));
        assertEquals("test and", scripts.get(1));
        assertEquals("diff", scripts.get(2));
        script = "\"another more\" \"test more\" \"diff more\"";
        scripts = Script.splitScript(script);
        assertEquals(3, scripts.size());
        assertEquals("another more", scripts.get(0));
        assertEquals("test more", scripts.get(1));
        assertEquals("diff more", scripts.get(2));
        script = "\"another more\" test \"diff more\"";
        scripts = Script.splitScript(script);
        assertEquals(3, scripts.size());
        assertEquals("another more", scripts.get(0));
        assertEquals("test", scripts.get(1));
        assertEquals("diff more", scripts.get(2));

        script = "another \"test and more\" diff";
        scripts = Script.splitScript(script);
        assertEquals(3, scripts.size());
        assertEquals("another", scripts.get(0));
        assertEquals("test and more", scripts.get(1));
        assertEquals("diff", scripts.get(2));
        script = "\"another and more\" \"test and more\" \"diff and more\"";
        scripts = Script.splitScript(script);
        assertEquals(3, scripts.size());
        assertEquals("another and more", scripts.get(0));
        assertEquals("test and more", scripts.get(1));
        assertEquals("diff and more", scripts.get(2));
        script = "\"another and more\" test \"diff and more\"";
        scripts = Script.splitScript(script);
        assertEquals(3, scripts.size());
        assertEquals("another and more", scripts.get(0));
        assertEquals("test", scripts.get(1));
        assertEquals("diff and more", scripts.get(2));

        // test nested
        script = "another \"test 'and' more\" diff";
        scripts = Script.splitScript(script);
        assertEquals(3, scripts.size());
        assertEquals("another", scripts.get(0));
        assertEquals("test 'and' more", scripts.get(1));
        assertEquals("diff", scripts.get(2));
        script = "\"another 'and more'\" \"test 'and more'\" \"diff 'and more'\"";
        scripts = Script.splitScript(script);
        assertEquals(3, scripts.size());
        assertEquals("another 'and more'", scripts.get(0));
        assertEquals("test 'and more'", scripts.get(1));
        assertEquals("diff 'and more'", scripts.get(2));
        script = "'\"another and\" more' test '\"diff and\" more'";
        scripts = Script.splitScript(script);
        assertEquals(3, scripts.size());
        assertEquals("\"another and\" more", scripts.get(0));
        assertEquals("test", scripts.get(1));
        assertEquals("\"diff and\" more", scripts.get(2));

        // test tick marks
        script = "`something`";
        scripts = Script.splitScript(script);
        assertEquals(1, scripts.size());
        assertEquals("`something`", scripts.get(0));

        script = "`some thing`";
        scripts = Script.splitScript(script);
        assertEquals(1, scripts.size());
        assertEquals("`some thing`", scripts.get(0));

        script = "`some thing` more";
        scripts = Script.splitScript(script);
        assertEquals(2, scripts.size());
        assertEquals("`some thing`", scripts.get(0));
        assertEquals("more", scripts.get(1));

        script = "`some thing` more \"and more\"";
        scripts = Script.splitScript(script);
        assertEquals(3, scripts.size());
        assertEquals("`some thing`", scripts.get(0));
        assertEquals("more", scripts.get(1));
        assertEquals("and more", scripts.get(2));

        script = "`some 'another' thing` more \"and more\"";
        scripts = Script.splitScript(script);
        assertEquals(3, scripts.size());
        assertEquals("`some 'another' thing`", scripts.get(0));
        assertEquals("more", scripts.get(1));
        assertEquals("and more", scripts.get(2));

        script = "`some \"more\" thing` more \"and more\"";
        scripts = Script.splitScript(script);
        assertEquals(3, scripts.size());
        assertEquals("`some \"more\" thing`", scripts.get(0));
        assertEquals("more", scripts.get(1));
        assertEquals("and more", scripts.get(2));

        script = "`some \"more\" thing` more \"and `diff diff` more\"";
        scripts = Script.splitScript(script);
        assertEquals(3, scripts.size());
        assertEquals("`some \"more\" thing`", scripts.get(0));
        assertEquals("more", scripts.get(1));
        assertEquals("and `diff diff` more", scripts.get(2));
    }

    @Test public void parseArgs() {
        Scope scope = Scope.Default;
        String scriptName = null;
        Script script;
        try {
            script = Script.parseArgs(scriptName, scope, scriptName);
            fail("Expecting an AssertionError because of the null scriptName.");
        } catch (AssertionError ae) {
            // expected
        }
        scriptName = "";
        try {
            script = Script.parseArgs(scriptName, scope, scriptName);
            fail("Expecting an AssertionError because of the empty scriptName.");
        } catch (AssertionError ae) {
            // expected
        }
        scriptName = "clean";
        script = Script.parse(scriptName, scope);
        assertEquals("clean", script.name);
        assertEquals("clean", script.unparsedName);
        assertEquals(scope, script.scope);
        assertEquals(0, script.arguments.size());

        scriptName = "clean arg1";
        script = Script.parse(scriptName, scope);
        assertEquals("clean", script.name);
        assertEquals("clean arg1", script.unparsedName);
        assertEquals(scope, script.scope);
        assertEquals(1, script.arguments.size());
        assertEquals("arg1", script.arguments.get(0));

        scriptName = "clean arg1 arg2";
        script = Script.parse(scriptName, scope);
        assertEquals("clean", script.name);
        assertEquals("clean arg1 arg2", script.unparsedName);
        assertEquals(scope, script.scope);
        assertEquals(2, script.arguments.size());
        assertEquals("arg1", script.arguments.get(0));
        assertEquals("arg2", script.arguments.get(1));

        scriptName = "\"clean arg1\"";
        script = Script.parse(scriptName, scope);
        assertEquals("clean arg1", script.name);
        assertEquals("\"clean arg1\"", script.unparsedName);
        assertEquals(scope, script.scope);
        assertEquals(0, script.arguments.size());

        scriptName = "'clean arg1'";
        script = Script.parse(scriptName, scope);
        assertEquals("clean arg1", script.name);
        assertEquals("'clean arg1'", script.unparsedName);
        assertEquals(scope, script.scope);
        assertEquals(0, script.arguments.size());

        scriptName = "`clean arg1`";
        script = Script.parse(scriptName, scope);
        assertEquals("`clean arg1`", script.name);
        assertEquals("`clean arg1`", script.unparsedName);
        assertEquals(scope, script.scope);
        assertEquals(0, script.arguments.size());
    }

    @Test public void parse() {
        Scope scope = Scope.Default;
        String scriptName = null;
        Script script = Script.parse(scriptName, scope);
        assertNull(script);

        scriptName = "";
        script = Script.parse(scriptName, scope);
        assertNull(script);

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
        assertEquals("java clean:test", script.name);
        assertEquals(scope, script.scope);
        
        scriptName = "java clean:test";
        script = Script.parse(scriptName, scope);
        assertEquals("java", script.name);
        assertEquals(scope, script.scope);
        assertEquals(1, script.arguments.size());
        assertEquals("clean:test", script.arguments.get(0));
        
        scriptName = "`java clean:test`";
        script = Script.parse(scriptName, scope);
        assertEquals("`java clean:test`", script.name);
        assertEquals(scope, script.scope);
        assertEquals(0, script.arguments.size());
    }
    
}
