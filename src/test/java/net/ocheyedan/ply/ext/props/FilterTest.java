package net.ocheyedan.ply.ext.props;

import org.junit.Test;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static junit.framework.Assert.assertEquals;

/**
 * User: blangel
 * Date: 12/31/11
 * Time: 9:12 PM
 */
public class FilterTest {

    @Test public void filterValue() {
        Context context = new Context("project");
        Context compilerContext = new Context("compiler");
        String prefix = "";
        String value = "nothing";
        Collection<Prop.All> props = new HashSet<Prop.All>();
        Prop.All name = new Prop.All(Prop.Loc.AdHoc, Scope.Default, context, "name", "ply");
        name.set(new Scope("test"), Prop.Loc.AdHoc, "ply-test", "ply-test");
        props.add(name);
        props.add(new Prop.All(Prop.Loc.AdHoc, Scope.Default, context, "version", "1.0"));
        props.add(new Prop.All(Prop.Loc.Local, Scope.Default, compilerContext, "build.dir", "target/${project.name}"));
        props.add(new Prop.All(Prop.Loc.Local, Scope.Default, compilerContext, "src.dir", "src/main/java"));
        props.add(new Prop.All(Prop.Loc.Local, Scope.Default, compilerContext, "alt.dir", "${src.dir}"));
        Set<Context> contexts = new HashSet<Context>();
        contexts.add(context);
        contexts.add(compilerContext);

        String filtered = Filter.filterValue(context, Scope.Default, prefix, value, props, contexts);
        assertEquals(value, filtered);

        value = "${name}";
        filtered = Filter.filterValue(context, Scope.Default, prefix, value, props, contexts);
        assertEquals("ply", filtered);
        
        value = "something/${name}/somethingelse";
        filtered = Filter.filterValue(context, Scope.Default, prefix, value, props, contexts);
        assertEquals("something/ply/somethingelse", filtered);

        value = "something/${name}/somethingelse-${name}";
        filtered = Filter.filterValue(context, Scope.Default, prefix, value, props, contexts);
        assertEquals("something/ply/somethingelse-ply", filtered);

        value = "${name}";
        filtered = Filter.filterValue(context, new Scope("test"), prefix, value, props, contexts);
        assertEquals("ply-test", filtered);
        
        value = "${name}";
        filtered = Filter.filterValue(compilerContext, Scope.Default, prefix, value, props, contexts);
        assertEquals("${name}", filtered);

        value = "${name}";
        filtered = Filter.filterValue(compilerContext, new Scope("test"), prefix, value, props, contexts);
        assertEquals("${name}", filtered);

        value = "${src.dir}";
        filtered = Filter.filterValue(compilerContext, Scope.Default, prefix, value, props, contexts);
        assertEquals("src/main/java", filtered);

        value = "${src.dir}";
        filtered = Filter.filterValue(compilerContext, new Scope("test"), prefix, value, props, contexts);
        assertEquals("src/main/java", filtered);

        value = "${alt.dir}";
        filtered = Filter.filterValue(compilerContext, Scope.Default, prefix, value, props, contexts);
        assertEquals("src/main/java", filtered);

        value = "${alt.dir}";
        filtered = Filter.filterValue(compilerContext, new Scope("test"), prefix, value, props, contexts);
        assertEquals("src/main/java", filtered);

        value = "${build.dir}";
        filtered = Filter.filterValue(compilerContext, Scope.Default, prefix, value, props, contexts);
        assertEquals("target/ply", filtered);

        value = "${build.dir}";
        filtered = Filter.filterValue(compilerContext, new Scope("test"), prefix, value, props, contexts);
        assertEquals("target/ply-test", filtered);
    }

}
