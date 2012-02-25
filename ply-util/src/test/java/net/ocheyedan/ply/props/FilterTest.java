package net.ocheyedan.ply.props;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * User: blangel
 * Date: 12/31/11
 * Time: 9:12 PM
 */
public class FilterTest {

    @Test
    public void filterNull() {
        try {
            Filter.filter(null, null, null);
            fail("Expecting a NullPointerException");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            Filter.filter(null, null, null, null);
            fail("Expecting a NullPointerException");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            Filter.filter(PropFile.Prop.Empty, null, null);
            fail("Expecting a NullPointerException");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            Filter.filter("", null, null, null);
            fail("Expecting a NullPointerException");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            Filter.filter(null, "", null);
            fail("Expecting a NullPointerException");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            Filter.filter(null, Context.named(""), null, null);
            fail("Expecting a NullPointerException");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            Filter.filter(null, null, Collections.<Context, PropFileChain>emptyMap());
            fail("Expecting a NullPointerException");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            Filter.filter(null, null, "", null);
            fail("Expecting a NullPointerException");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            Filter.filter(null, null, null, Collections.<Context, PropFileChain>emptyMap());
            fail("Expecting a NullPointerException");
        } catch (NullPointerException npe) {
            // expected
        }

        try {
            Filter.filter(PropFile.Prop.Empty, "", null);
            fail("Expecting a NullPointerException");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            Filter.filter("", Context.named(""), null, null);
            fail("Expecting a NullPointerException");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            Filter.filter(PropFile.Prop.Empty, null, Collections.<Context, PropFileChain>emptyMap());
            fail("Expecting a NullPointerException");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            Filter.filter("", null, "", null);
            fail("Expecting a NullPointerException");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            Filter.filter(null, "", Collections.<Context, PropFileChain>emptyMap());
            fail("Expecting a NullPointerException");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            Filter.filter("", null, null, Collections.<Context, PropFileChain>emptyMap());
            fail("Expecting a NullPointerException");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            Filter.filter(null, Context.named(""), "", null);
            fail("Expecting a NullPointerException");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            Filter.filter(null, Context.named(""), null, Collections.<Context, PropFileChain>emptyMap());
            fail("Expecting a NullPointerException");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            Filter.filter(null, null, "", Collections.<Context, PropFileChain>emptyMap());
            fail("Expecting a NullPointerException");
        } catch (NullPointerException npe) {
            // expected
        }

        try {
            Filter.filter("", Context.named(""), "", null);
            fail("Expecting a NullPointerException");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            Filter.filter("", Context.named(""), null, Collections.<Context, PropFileChain>emptyMap());
            fail("Expecting a NullPointerException");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            Filter.filter("", null, "", Collections.<Context, PropFileChain>emptyMap());
            fail("Expecting a NullPointerException");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            Filter.filter(null, Context.named(""), "", Collections.<Context, PropFileChain>emptyMap());
            fail("Expecting a NullPointerException");
        } catch (NullPointerException npe) {
            // expected
        }
    }

    @Test @SuppressWarnings("unchecked")
    public void filter() throws NoSuchFieldException, IllegalAccessException {

        PropFile container = new PropFile(Context.named("test"), PropFile.Loc.System);
        PropFile.Prop testProp = container.add("test", "hello");

        // test value doesn't contain the ${...} property placeholder
        PropFile.Prop filteredProp = Filter.filter(testProp, "test", Collections.<Context, PropFileChain>emptyMap());
        Assert.assertEquals(testProp, filteredProp);
        Assert.assertEquals("hello", filteredProp.value());

        String filtered = Filter.filter("hello", Context.named("test"), "test", Collections.<Context, PropFileChain>emptyMap());
        Assert.assertEquals("hello", filtered);

        // test within context provided
        PropFile.Prop contextProvidedProp = container.add("context-provided", "${build.dir}/something-else");
        Map<Context, PropFileChain> filterConsultant = new ConcurrentHashMap<Context, PropFileChain>(2, 1.0f);
        PropFileChain chain = new PropFileChain(filterConsultant);
        PropFile.Prop buildDir = container.add("build.dir", "usr/src");
        chain.set(container, PropFile.Loc.System);
        filterConsultant.put(Context.named("test"), chain);

        PropFile.Prop expected = contextProvidedProp.with(buildDir.value());
        PropFile.Prop filteredContextProvidedProp = Filter.filter(contextProvidedProp, "test", filterConsultant);

        Assert.assertEquals(expected, filteredContextProvidedProp);
        Assert.assertEquals("usr/src/something-else", filteredContextProvidedProp.value());
        Assert.assertEquals("${build.dir}/something-else", filteredContextProvidedProp.unfilteredValue);

        filtered = Filter.filter("${build.dir}/something-else", Context.named("test"), "test", filterConsultant);
        Assert.assertEquals("usr/src/something-else", filtered);
        filtered = Filter.filter("ahhhh-${build.dir}-ehhhh", Context.named("test"), "test", filterConsultant);
        Assert.assertEquals("ahhhh-usr/src-ehhhh", filtered);

        // test within different context
        PropFile otherContainer = new PropFile(Context.named("other-context"), PropFile.Loc.System);
        PropFileChain otherChain = new PropFileChain(filterConsultant);
        otherChain.set(otherContainer, PropFile.Loc.System);
        filterConsultant.put(Context.named("other-context"), otherChain);
        PropFile.Prop otherContextProp = container.add("other-context-prop", "{before/${test.build.dir}/other$context}");

        expected = otherContextProp.with("{before/" + buildDir.value() + "/other$context}");
        PropFile.Prop filteredOtherContextProp = Filter.filter(otherContextProp, "test", filterConsultant);

        Assert.assertEquals(expected, filteredOtherContextProp);
        Assert.assertEquals("{before/usr/src/other$context}", filteredOtherContextProp.value());
        Assert.assertEquals("{before/${test.build.dir}/other$context}", filteredOtherContextProp.unfilteredValue);

        filtered = Filter.filter("{before/${test.build.dir}/other$context}", Context.named("other-context"), "test", filterConsultant);
        Assert.assertEquals("{before/usr/src/other$context}", filtered);
        filtered = Filter.filter("something-${test.build.dir}", Context.named("other-context"), "test", filterConsultant);
        Assert.assertEquals("something-usr/src", filtered);

        // test within environment variables
        if (System.getenv().keySet().isEmpty()) {
            throw new AssertionError("No environment variables, cannot test environment-variable filtering.");
        }
        String environVarKey = System.getenv().keySet().iterator().next();
        PropFile.Prop environmentProp = otherContainer.add("environ-var", "something-${" + environVarKey + "}");
        expected = environmentProp.with("something-" + System.getenv(environVarKey));

        PropFile.Prop filteredEnvironmentProp = Filter.filter(environmentProp, "test", filterConsultant);
        Assert.assertEquals(expected, filteredEnvironmentProp);
        Assert.assertEquals(expected.value(), filteredEnvironmentProp.value());

        filtered = Filter.filter("something-${" + environVarKey + "}", Context.named("test"), "test", filterConsultant);
        Assert.assertEquals(expected.value(), filtered);
        filtered = Filter.filter("new-something-${" + environVarKey + "}-end", Context.named("different-doesn't-matter"), "test", filterConsultant);
        Assert.assertEquals("new-something-" + System.getenv(environVarKey) + "-end", filtered);

        // test multiple properties within one value

        // multiple of same context
        PropFile.Prop srcDirProp = container.add("src.dir", "something");
        PropFile.Prop multipleSameContextProp = container.add("multiple-context", "${src.dir} and ${build.dir}");
        expected = multipleSameContextProp.with(srcDirProp.value() + " and " + buildDir.value());

        PropFile.Prop filteredMultiSameContextProp = Filter.filter(multipleSameContextProp, "test", filterConsultant);
        Assert.assertEquals(expected, filteredMultiSameContextProp);
        Assert.assertEquals(expected.value(), filteredMultiSameContextProp.value());
        Assert.assertEquals("${src.dir} and ${build.dir}", filteredMultiSameContextProp.unfilteredValue);

        filtered = Filter.filter("${src.dir} diff than ${build.dir}", Context.named("test"), "test", filterConsultant);
        Assert.assertEquals(srcDirProp.value() + " diff than " + buildDir.value(), filtered);

        // multiple of different context
        PropFile.Prop multiDiffContextProp = otherContainer.add("multi-diff-context", "${test.src.dir} with ${test.build.dir}");
        expected = multiDiffContextProp.with(srcDirProp.value() + " with " + buildDir.value());

        PropFile.Prop filteredMultiDiffContextProp = Filter.filter(multiDiffContextProp, "test", filterConsultant);
        Assert.assertEquals(expected, filteredMultiDiffContextProp);
        Assert.assertEquals(expected.value(), filteredMultiDiffContextProp.value());
        Assert.assertEquals("${test.src.dir} with ${test.build.dir}", filteredMultiDiffContextProp.unfilteredValue);

        filtered = Filter.filter("${test.src.dir} else ${test.build.dir}", multiDiffContextProp.context(), "test", filterConsultant);
        Assert.assertEquals(srcDirProp.value() + " else " + buildDir.value(), filtered);

        // multiple of environment
        if (System.getenv().size() < 2) {
            throw new AssertionError("Need two environmental variables to test filtering.");
        }
        Iterator<String> environIter = System.getenv().keySet().iterator();
        environVarKey = environIter.next();
        String environVarKey2 = environIter.next();

        PropFile.Prop multiEnvVarProp = container.add("multi-env-var", "${" + environVarKey + "} env with env of ${" + environVarKey2 + "}");
        expected = multiEnvVarProp.with(System.getenv(environVarKey) + " env with env of " + System.getenv(environVarKey2));

        PropFile.Prop filteredMultiEnvVarProp = Filter.filter(multiEnvVarProp, "test", filterConsultant);
        Assert.assertEquals(expected, filteredMultiEnvVarProp);
        Assert.assertEquals(expected.value(), filteredMultiEnvVarProp.value());
        Assert.assertEquals("${" + environVarKey + "} env with env of ${" + environVarKey2 + "}",
                filteredMultiEnvVarProp.unfilteredValue);

        filtered = Filter.filter("${" + environVarKey + "} env-vars ${" + environVarKey2 + "}", Context.named("test"), "test", filterConsultant);
        Assert.assertEquals(System.getenv(environVarKey) + " env-vars " + System.getenv(environVarKey2), filtered);

        // multiple mix of same-context, diff-context and environment
        PropFile.Prop multiEverythingProp = otherContainer.add("multi-everything", "${multi-diff-context} and ${test.src.dir} and ${" + environVarKey + "}");
        expected = multiEverythingProp.with(filteredMultiDiffContextProp.value() + " and " + srcDirProp.value() + " and " + System.getenv(environVarKey));

        PropFile.Prop filteredMultiEverythingProp = Filter.filter(multiEverythingProp, "test", filterConsultant);
        Assert.assertEquals(expected, filteredMultiEverythingProp);
        Assert.assertEquals(expected.value(), filteredMultiEverythingProp.value());
        Assert.assertEquals("${multi-diff-context} and ${test.src.dir} and ${" + environVarKey + "}",
                filteredMultiEverythingProp.unfilteredValue);

        filtered = Filter.filter("${multi-diff-context} with ${test.src.dir} and this env of ${" + environVarKey + "}", multiDiffContextProp.context(), "test", filterConsultant);
        Assert.assertEquals(
                filteredMultiDiffContextProp.value() + " with " + srcDirProp.value() + " and this env of " + System
                        .getenv(environVarKey), filtered);

        // test nested (un-resolved before invocation) within same context
        PropFile.Prop unresolvedOneProp = container.add("unresolved-one", "${unresolved-two} value");
        container.add("unresolved-two", "${build.dir} value");
        expected = unresolvedOneProp.with(buildDir.value() + " value value");

        PropFile.Prop filteredUnresolvedOneProp = Filter.filter(unresolvedOneProp, "test", filterConsultant);
        Assert.assertEquals(unresolvedOneProp, filteredUnresolvedOneProp);
        Assert.assertEquals(expected.value(), filteredUnresolvedOneProp.value());
        Assert.assertEquals("${unresolved-two} value", filteredUnresolvedOneProp.unfilteredValue);

        // test nested (un-resolved before invocation) across different contexts
        PropFile.Prop unresolvedThreeProp = container.add("unresolved-three", "${" + multiDiffContextProp.context() + ".unresolved-four} val");
        otherContainer.add("unresolved-four", "${test.src.dir} val");
        expected = unresolvedThreeProp.with(srcDirProp.value() + " val val");

        PropFile.Prop filteredUnresolvedThreeProp = Filter.filter(unresolvedThreeProp, "test", filterConsultant);
        Assert.assertEquals(expected, filteredUnresolvedThreeProp);
        Assert.assertEquals(expected.value(), filteredUnresolvedThreeProp.value());
        Assert.assertEquals("${" + multiDiffContextProp.context() + ".unresolved-four} val",
                filteredUnresolvedThreeProp.unfilteredValue);

        // ensure there's nothing vestigial in the resolvingCacheKeys property
        Field resolvingCacheKeysField = Filter.class.getDeclaredField("resolvingCacheKeys");
        resolvingCacheKeysField.setAccessible(true);
        Set<String> resolvingCacheKeys = (Set<String>) resolvingCacheKeysField.get(null);
        Assert.assertEquals(0, resolvingCacheKeys.size());

        // test circular reference
        PropFile.Prop circularRefOneProp = container.add("cir-ref-one", "${cir-ref-two} val");
        container.add("cir-ref-two", "${cir-ref-one} val");
        try {
            Filter.filter(circularRefOneProp, "test", filterConsultant);
            fail("Circular reference within the property values; should have thrown an exception.");
        } catch (Filter.Circular pffc) {
            // expected
        }
        
        // test scenario where non-scoped context has a value dependent upon a scoped context value
        filterConsultant.clear();
        
        PropFileChain projectChain = new PropFileChain(filterConsultant);
        PropFile project = new PropFile(Context.named("project"), PropFile.Loc.System);
        project.add("artifact.name", "default-name");
        projectChain.set(project, PropFile.Loc.System);
        PropFileChain testScopedProjectChain = new PropFileChain(projectChain, filterConsultant);
        PropFile testScopedProject = new PropFile(Context.named("project"), Scope.named("test"), PropFile.Loc.System);
        PropFile.Prop testScopedProjectArtifactNameProp = testScopedProject.add("artifact.name", "test-name");
        testScopedProjectChain.set(testScopedProject, PropFile.Loc.System);
        filterConsultant.put(Context.named("project"), testScopedProjectChain);
        
        PropFileChain packageChain = new PropFileChain(filterConsultant);
        PropFile packageFile = new PropFile(Context.named("package"), PropFile.Loc.System);
        PropFile.Prop packageNameProp = packageFile.add("name", "${project.artifact.name}");
        packageChain.set(packageFile, PropFile.Loc.System);
        filterConsultant.put(Context.named("package"), packageChain);
        
        expected = packageNameProp.with(testScopedProjectArtifactNameProp.value());
        PropFile.Prop filteredPackageNameProp = Filter.filter(packageNameProp, "test", filterConsultant);
        assertEquals(expected, filteredPackageNameProp);
        assertEquals(expected.value(), filteredPackageNameProp.value());

    }

}
