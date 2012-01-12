package net.ocheyedan.ply.ext.cmd.build;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.ext.cmd.Args;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static junit.framework.Assert.assertEquals;

/**
 * User: blangel
 * Date: 1/12/12
 * Time: 12:44 PM
 */
public class ModuleTest {

    @Test public void resolve() {
        File mockConfigDir = new File("./src/test/resources/dot-ply/config");
        File mockScriptsDir = FileUtil.fromParts(mockConfigDir.getPath(), "..", "..", "scripts");
        // test simple script resolution
        List<String> rawArguments = new ArrayList<String>();
        rawArguments.add("build"); // TODO - remove this switch
        rawArguments.add("mock-clean.jar");
        Args args = new Args(rawArguments, Collections.<String>emptyList());
        List<Execution> executions = Module.resolve(args, mockConfigDir);
        assertEquals(1, executions.size());
        assertEquals("mock-clean.jar", executions.get(0).script.name);
        assertEquals(1, executions.get(0).executionArgs.length);
        assertEquals(FileUtil.getCanonicalPath(FileUtil.fromParts(mockScriptsDir.getPath(), "mock-clean.jar")),
                     executions.get(0).executionArgs[0]);
        // test alias resolution
        rawArguments.set(1, "clean");
        executions = Module.resolve(args, mockConfigDir);
        assertEquals(1, executions.size());
        assertEquals("clean", executions.get(0).script.name);
        assertEquals(1, executions.get(0).executionArgs.length);
        assertEquals(FileUtil.getCanonicalPath(FileUtil.fromParts(mockScriptsDir.getPath(), "mock-clean.jar")),
                     executions.get(0).executionArgs[0]);
        // test alias resolution
        rawArguments.set(1, "compile");
        executions = Module.resolve(args, mockConfigDir);
        assertEquals(1, executions.size());
        assertEquals("compile", executions.get(0).script.name);
        assertEquals(1, executions.get(0).executionArgs.length);
        assertEquals(FileUtil.getCanonicalPath(FileUtil.fromParts(mockScriptsDir.getPath(), "mock-compile.jar")),
                     executions.get(0).executionArgs[0]);
        // test double-alias resolution
        rawArguments.set(1, "run");
        executions = Module.resolve(args, mockConfigDir);
        assertEquals(2, executions.size());
        assertEquals("clean", executions.get(0).script.name);
        assertEquals(1, executions.get(0).executionArgs.length);
        assertEquals(FileUtil.getCanonicalPath(FileUtil.fromParts(mockScriptsDir.getPath(), "mock-clean.jar")),
                     executions.get(0).executionArgs[0]);
        assertEquals("compile", executions.get(1).script.name);
        assertEquals(1, executions.get(1).executionArgs.length);
        assertEquals(FileUtil.getCanonicalPath(FileUtil.fromParts(mockScriptsDir.getPath(), "mock-compile.jar")),
                     executions.get(1).executionArgs[0]);
        // test mixed script/alias resolution
        rawArguments.set(1, "mock-clean.jar");
        rawArguments.add("run");
        executions = Module.resolve(args, mockConfigDir);
        assertEquals(3, executions.size());
        assertEquals("mock-clean.jar", executions.get(0).script.name);
        assertEquals(1, executions.get(0).executionArgs.length);
        assertEquals(FileUtil.getCanonicalPath(FileUtil.fromParts(mockScriptsDir.getPath(), "mock-clean.jar")),
                     executions.get(0).executionArgs[0]);
        assertEquals("clean", executions.get(1).script.name);
        assertEquals(1, executions.get(1).executionArgs.length);
        assertEquals(FileUtil.getCanonicalPath(FileUtil.fromParts(mockScriptsDir.getPath(), "mock-clean.jar")),
                     executions.get(1).executionArgs[0]);
        assertEquals("compile", executions.get(2).script.name);
        assertEquals(1, executions.get(2).executionArgs.length);
        assertEquals(FileUtil.getCanonicalPath(FileUtil.fromParts(mockScriptsDir.getPath(), "mock-compile.jar")),
                     executions.get(2).executionArgs[0]);
        // test mixed script/alias resolution, inverted from above
        rawArguments.set(1, "run");
        rawArguments.set(2, "mock-clean.jar");
        executions = Module.resolve(args, mockConfigDir);
        assertEquals(3, executions.size());
        assertEquals("clean", executions.get(0).script.name);
        assertEquals(1, executions.get(0).executionArgs.length);
        assertEquals(FileUtil.getCanonicalPath(FileUtil.fromParts(mockScriptsDir.getPath(), "mock-clean.jar")),
                     executions.get(0).executionArgs[0]);
        assertEquals("compile", executions.get(1).script.name);
        assertEquals(1, executions.get(1).executionArgs.length);
        assertEquals(FileUtil.getCanonicalPath(FileUtil.fromParts(mockScriptsDir.getPath(), "mock-compile.jar")),
                     executions.get(1).executionArgs[0]);
        assertEquals("mock-clean.jar", executions.get(2).script.name);
        assertEquals(1, executions.get(2).executionArgs.length);
        assertEquals(FileUtil.getCanonicalPath(FileUtil.fromParts(mockScriptsDir.getPath(), "mock-clean.jar")),
                     executions.get(2).executionArgs[0]);
        // test with arguments.
        rawArguments.set(1, "run");
        rawArguments.set(2, "arg1");
        rawArguments.add("mock-clean.jar");
        rawArguments.add("arg1-to-mock-clean");
        executions = Module.resolve(args, mockConfigDir);
        assertEquals(3, executions.size());
        assertEquals("clean", executions.get(0).script.name);
        assertEquals(1, executions.get(0).executionArgs.length);
        assertEquals(FileUtil.getCanonicalPath(FileUtil.fromParts(mockScriptsDir.getPath(), "mock-clean.jar")),
                     executions.get(0).executionArgs[0]);
        assertEquals("compile", executions.get(1).script.name);
        // the arg1 is only passed to last alias resolution
        assertEquals(2, executions.get(1).executionArgs.length);
        assertEquals(FileUtil.getCanonicalPath(FileUtil.fromParts(mockScriptsDir.getPath(), "mock-compile.jar")),
                     executions.get(1).executionArgs[0]);
        assertEquals("arg1", executions.get(1).executionArgs[1]);
        assertEquals("mock-clean.jar", executions.get(2).script.name);
        assertEquals(2, executions.get(2).executionArgs.length);
        assertEquals(FileUtil.getCanonicalPath(FileUtil.fromParts(mockScriptsDir.getPath(), "mock-clean.jar")),
                     executions.get(2).executionArgs[0]);
        assertEquals("arg1-to-mock-clean", executions.get(2).executionArgs[1]);
    }

}
