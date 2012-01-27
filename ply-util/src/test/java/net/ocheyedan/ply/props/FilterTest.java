package net.ocheyedan.ply.props;

import org.junit.Test;

import java.io.File;
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
        Context packageContext = new Context("package");
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
        props.add(new Prop.All(Prop.Loc.Local, Scope.Default, context, "artifact.name", "ply-util-1.0.jar"));
        props.add(new Prop.All(Prop.Loc.Local, Scope.named("test"), context, "artifact.name", "ply-util-1.0-test.jar"));
        Set<Context> contexts = new HashSet<Context>();
        contexts.add(context);
        contexts.add(compilerContext);
        contexts.add(packageContext);

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

    @Test public void filter() {
        Context context = new Context("project");
        Context packageContext = new Context("package");
        Set<Prop.All> props = new HashSet<Prop.All>();
        Prop.All name = new Prop.All(Prop.Loc.AdHoc, Scope.Default, context, "name", "ply");
        name.set(new Scope("test"), Prop.Loc.AdHoc, "ply-test", "ply-test");
        props.add(name);
        props.add(new Prop.All(Prop.Loc.AdHoc, Scope.Default, context, "version", "1.0"));
        Prop.All artifactNameProp = new Prop.All(Prop.Loc.Local, Scope.Default, context, "artifact.name", "ply-util-1.0.jar");
        artifactNameProp.set(Scope.named("test"), Prop.Loc.LocalScoped, "ply-util-1.0-test.jar",
                "ply-util-1.0-test.jar");
        props.add(artifactNameProp);
        Prop.All packageNameProp = new Prop.All(Prop.Loc.System, Scope.Default, packageContext, "name", "${project.artifact.name}");
        props.add(packageNameProp);
        // test case: package.name = ${project.artifact.name} where there is no test scope for package,
        // even though there is not test scope for package, the resolved value should come from project's
        // test scoped value
        Filter.filter(new File("./"), props);
        assertEquals("ply-util-1.0.jar", packageNameProp.get(Scope.Default).value);
        assertEquals("ply-util-1.0-test.jar", packageNameProp.get(Scope.named("test")).value);
    }


}
