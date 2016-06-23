package net.ocheyedan.ply.script;

import net.ocheyedan.ply.props.Context;
import net.ocheyedan.ply.props.PropFile;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * User: blangel
 * Date: 8/20/14
 * Time: 8:43 AM
 */
public class CompilableFilesTest {

    @Test
    public void getSourceFile() throws Exception {
        Method getSourceFileMethod = CompilableFiles.class.getDeclaredMethod("getSourceFile", PropFile.Prop.class, String.class);
        getSourceFileMethod.setAccessible(true);

        CompilableFiles compilableFiles = new CompilableFiles();

        // test without inner class
        PropFile.Prop prop = new PropFile(Context.named("test"), PropFile.Loc.AdHoc).add("net.ocheyedan.ply.script.CompilerScript", "");
        String sourceFilePath = (String) getSourceFileMethod.invoke(compilableFiles, prop, "foo");
        assertTrue(sourceFilePath.endsWith("foo/net/ocheyedan/ply/script/CompilerScript.java"));

        // test with inner class
        prop = new PropFile(Context.named("test"), PropFile.Loc.AdHoc).add("net.ocheyedan.ply.script.CompilerScript$1", "");
        sourceFilePath = (String) getSourceFileMethod.invoke(compilableFiles, prop, "foo");
        assertTrue(sourceFilePath.endsWith("foo/net/ocheyedan/ply/script/CompilerScript.java"));
        prop = new PropFile(Context.named("test"), PropFile.Loc.AdHoc).add("net.ocheyedan.ply.script.CompilerScript$InnerName", "");
        sourceFilePath = (String) getSourceFileMethod.invoke(compilableFiles, prop, "foo");
        assertTrue(sourceFilePath.endsWith("foo/net/ocheyedan/ply/script/CompilerScript.java"));
    }

}
