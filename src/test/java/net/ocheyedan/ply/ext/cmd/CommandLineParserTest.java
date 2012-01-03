package net.ocheyedan.ply.ext.cmd;

import net.ocheyedan.ply.ext.cmd.build.Build;
import net.ocheyedan.ply.ext.cmd.config.Get;
import net.ocheyedan.ply.ext.cmd.config.GetAll;
import net.ocheyedan.ply.ext.cmd.config.Set;
import org.junit.Test;

import static junit.framework.Assert.*;

/**
 * User: blangel
 * Date: 12/29/11
 * Time: 3:06 PM
 */
public class CommandLineParserTest {

    @Test
    public void parse() {

        Command command = CommandLineParser.parse(null);
        assertNotNull(command);
        assertTrue(command instanceof Usage);
        
        String[] args = {};
        command = CommandLineParser.parse(args);
        assertNotNull(command);
        assertTrue(command instanceof Usage);
        assertEquals(0, command.args.args.size());
        assertEquals(0, command.args.adHocProps.size());

        args = new String[] { "--usage" };
        command = CommandLineParser.parse(args);
        assertNotNull(command);
        assertTrue(command instanceof Usage);
        assertEquals(1, command.args.args.size());
        assertEquals("--usage", command.args.args.get(0));
        assertEquals(0, command.args.adHocProps.size());

        args = new String[] { "--help" };
        command = CommandLineParser.parse(args);
        assertNotNull(command);
        assertTrue(command instanceof Usage);
        assertEquals(1, command.args.args.size());
        assertEquals("--help", command.args.args.get(0));
        assertEquals(0, command.args.adHocProps.size());

        args = new String[] { "--usage", "-Pply.decorated=false" };
        command = CommandLineParser.parse(args);
        assertNotNull(command);
        assertTrue(command instanceof Usage);
        assertEquals(1, command.args.args.size());
        assertEquals("--usage", command.args.args.get(0));
        assertEquals(1, command.args.adHocProps.size());
        assertEquals("ply.decorated=false", command.args.adHocProps.get(0));

        args = new String[] { "--help", "-Pply.decorated=false" };
        command = CommandLineParser.parse(args);
        assertNotNull(command);
        assertTrue(command instanceof Usage);
        assertEquals(1, command.args.args.size());
        assertEquals("--help", command.args.args.get(0));
        assertEquals(1, command.args.adHocProps.size());
        assertEquals("ply.decorated=false", command.args.adHocProps.get(0));

        args = new String[] { "-Pply.decorated=false", "--usage" }; // order matters
        command = CommandLineParser.parse(args);
        assertNotNull(command);
        assertTrue(command instanceof Build);
        assertEquals(1, command.args.args.size());
        assertEquals("--usage", command.args.args.get(0));
        assertEquals(1, command.args.adHocProps.size());
        assertEquals("ply.decorated=false", command.args.adHocProps.get(0));

        args = new String[] { "-Pply.decorated=false", "get" }; // order matters
        command = CommandLineParser.parse(args);
        assertNotNull(command);
        assertTrue(command instanceof Build);
        assertEquals(1, command.args.args.size());
        assertEquals("get", command.args.args.get(0));
        assertEquals(1, command.args.adHocProps.size());
        assertEquals("ply.decorated=false", command.args.adHocProps.get(0));

        args = new String[] { "-Pply.decorated=false", "clean" }; // order matters
        command = CommandLineParser.parse(args);
        assertNotNull(command);
        assertTrue(command instanceof Build);
        assertEquals(1, command.args.args.size());
        assertEquals("clean", command.args.args.get(0));
        assertEquals(1, command.args.adHocProps.size());
        assertEquals("ply.decorated=false", command.args.adHocProps.get(0));

        args = new String[] { "init" };
        command = CommandLineParser.parse(args);
        assertTrue(command instanceof Init);

        args = new String[] { "get" };
        command = CommandLineParser.parse(args);
        assertTrue(command instanceof Get);

        args = new String[] { "get-all" };
        command = CommandLineParser.parse(args);
        assertTrue(command instanceof GetAll);

        args = new String[] { "set" };
        command = CommandLineParser.parse(args);
        assertTrue(command instanceof Set);

        args = new String[] { "clean" };
        command = CommandLineParser.parse(args);
        assertTrue(command instanceof Build);

        args = new String[] { "compile", "package" };
        command = CommandLineParser.parse(args);
        assertTrue(command instanceof Build);

        args = new String[] { "compile", "-Pcompiler.src.dir=src" };
        command = CommandLineParser.parse(args);
        assertTrue(command instanceof Build);
        assertEquals(1, command.args.adHocProps.size());
        assertEquals("compiler.src.dir=src", command.args.adHocProps.get(0));
        assertEquals(1, command.args.args.size());
        assertEquals("compile", command.args.args.get(0));

        args = new String[] { "get", "-Pproject.name=ply" };
        command = CommandLineParser.parse(args);
        assertTrue(command instanceof Get);
        assertEquals(1, command.args.adHocProps.size());
        assertEquals("project.name=ply", command.args.adHocProps.get(0));
        assertEquals(1, command.args.args.size());
        assertEquals("get", command.args.args.get(0));

    }

    @Test
    public void parseArgs() {
        String[] rawArgs = null;
        Args args = CommandLineParser.parseArgs(rawArgs);
        assertNotNull(args);
        assertEquals(0, args.args.size());
        assertEquals(0, args.adHocProps.size());

        rawArgs = new String[] {};
        args = CommandLineParser.parseArgs(rawArgs);
        assertNotNull(args);
        assertEquals(0, args.args.size());
        assertEquals(0, args.adHocProps.size());

        rawArgs = new String[] { "-Pcompiler.src.dir=src", "clean" };
        args = CommandLineParser.parseArgs(rawArgs);
        assertNotNull(args);
        assertEquals(1, args.args.size());
        assertEquals("clean", args.args.get(0));
        assertEquals(1, args.adHocProps.size());
        assertEquals("compiler.src.dir=src", args.adHocProps.get(0));

        rawArgs = new String[] { "-Pcompiler.build.dir=target", "-Pproject.name=ply", "-Pproject#test.name=ply-test" };
        args = CommandLineParser.parseArgs(rawArgs);
        assertNotNull(args);
        assertEquals(0, args.args.size());
        assertEquals(3, args.adHocProps.size());
        assertEquals("compiler.build.dir=target", args.adHocProps.get(0));
        assertEquals("project.name=ply", args.adHocProps.get(1));
        assertEquals("project#test.name=ply-test", args.adHocProps.get(2));

        rawArgs = new String[] { "clean", "compile", "package" };
        args = CommandLineParser.parseArgs(rawArgs);
        assertNotNull(args);
        assertEquals(3, args.args.size());
        assertEquals("clean", args.args.get(0));
        assertEquals("compile", args.args.get(1));
        assertEquals("package", args.args.get(2));
        assertEquals(0, args.adHocProps.size());
    }

}
