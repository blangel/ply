package net.ocheyedan.ply.props;

import net.ocheyedan.ply.FileUtil;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * User: blangel
 * Date: 2/23/12
 * Time: 7:41 AM
 */
public class LoaderTest {

    @Test
    public void load() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method loadMethod = Loader.class.getDeclaredMethod("load", File.class, PropFile.Loc.class, Map.class);
        loadMethod.setAccessible(true);

        File base;
        if (FileUtil.getCanonicalPath(new File("./")).contains("ply-util")) {
            base = new File("./");
        } else {
            base = new File("ply-util/");
        }
        
        File configurationDirectory = FileUtil.fromParts(base.getPath(), "src/test/resources/mock-prop-loader-files");
        PropFile.Loc local = PropFile.Loc.Local;
        Map<Scope, Map<Context, PropFile>> propFiles = new ConcurrentHashMap<Scope, Map<Context, PropFile>>();

        loadMethod.invoke(null, configurationDirectory, local, propFiles);
        assertTrue(propFiles.containsKey(Scope.Default));
        assertTrue(propFiles.containsKey(Scope.named("test")));
        assertEquals(2, propFiles.size());
        Map<Context, PropFile> defaultContexts = propFiles.get(Scope.Default);
        assertEquals(1, defaultContexts.size());
        PropFile loadedFiles = defaultContexts.get(Context.named("loader"));
        assertEquals("mock_value", loadedFiles.get("mock_key").value());
        assertEquals("mock_value_2", loadedFiles.get("mock_key_2").value());
        Map<Context, PropFile> testContexts = propFiles.get(Scope.named("test"));
        assertEquals(1, testContexts.size());
        loadedFiles = testContexts.get(Context.named("loader"));
        assertEquals("mock_value_test", loadedFiles.get("mock_key").value());
        assertEquals("mock_value_test_2", loadedFiles.get("mock_key_2").value());
        
        propFiles.clear();
        PropFile defaultFile = new PropFile(Context.named("loader"), PropFile.Loc.System);
        defaultFile.add("mock_key", "system_mock_value");
        defaultFile.add("mock_key_2", "system_mock_value_2");
        defaultFile.add("mock_key_3", "system_mock_value_3");
        PropFile testFile = new PropFile(Context.named("loader"), Scope.named("test"), PropFile.Loc.System);
        testFile.add("mock_key", "system_mock_value_test");
        testFile.add("mock_key_2", "system_mock_value_test_2");
        testFile.add("mock_key_3", "system_mock_value_test_3");
        Map<Context, PropFile> defaultSystemContexts = new ConcurrentHashMap<Context, PropFile>(2, 1.0f);
        defaultSystemContexts.put(Context.named("loader"), defaultFile);
        Map<Context, PropFile> testSystemContexts = new ConcurrentHashMap<Context, PropFile>(2, 1.0f);
        testSystemContexts.put(Context.named("loader"), testFile);
        propFiles.put(Scope.Default, defaultSystemContexts);
        propFiles.put(Scope.named("test"), testSystemContexts);

        // the load method will override the system props entirely
        loadMethod.invoke(null, configurationDirectory, local, propFiles);

        assertTrue(propFiles.containsKey(Scope.Default));
        assertTrue(propFiles.containsKey(Scope.named("test")));
        assertEquals(2, propFiles.size());
        defaultSystemContexts = propFiles.get(Scope.Default);
        assertEquals(1, defaultSystemContexts.size());
        assertTrue(defaultSystemContexts.containsKey(Context.named("loader")));
        loadedFiles = defaultSystemContexts.get(Context.named("loader"));
        assertEquals("mock_value", loadedFiles.get("mock_key").value());
        assertEquals("mock_value_2", loadedFiles.get("mock_key_2").value());
        assertEquals("", loadedFiles.get("mock_key_3").value());
        testSystemContexts = propFiles.get(Scope.named("test"));
        assertEquals(1, testSystemContexts.size());
        assertTrue(testSystemContexts.containsKey(Context.named("loader")));
        loadedFiles = testSystemContexts.get(Context.named("loader"));
        assertEquals("mock_value_test", loadedFiles.get("mock_key").value());
        assertEquals("mock_value_test_2", loadedFiles.get("mock_key_2").value());
        assertEquals("", loadedFiles.get("mock_key_3").value());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void chain() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method chainMethod = Loader.class.getDeclaredMethod("chain", Map.class, Map.class, Map.class);
        chainMethod.setAccessible(true);

        Map<Scope, Map<Context, PropFileChain>> chain;
        Map<Scope, Map<Context, PropFile>> system = new ConcurrentHashMap<Scope, Map<Context, PropFile>>();
        Map<Scope, Map<Context, PropFile>> local = new ConcurrentHashMap<Scope, Map<Context, PropFile>>();
        Map<Scope, Map<Context, PropFile>> adHoc = new ConcurrentHashMap<Scope, Map<Context, PropFile>>();

        Map<Context, PropFile> defaultSystemContexts = new ConcurrentHashMap<Context, PropFile>(2, 1.0f);
        Map<Context, PropFile> testSystemContexts = new ConcurrentHashMap<Context, PropFile>(2, 1.0f);

        PropFile defaultFile = new PropFile(Context.named("loader"), PropFile.Loc.System);
        defaultFile.add("mock_key", "system_mock_value");
        defaultFile.add("mock_key_2", "system_mock_value_2");
        defaultFile.add("mock_key_3", "system_mock_value_3");
        defaultFile.add("mock_key_4", "system_mock_value_4");
        defaultSystemContexts.put(Context.named("loader"), defaultFile);

        PropFile testFile = new PropFile(Context.named("loader"), Scope.named("test"), PropFile.Loc.System);
        testFile.add("mock_key", "system_mock_value_test");
        testFile.add("mock_key_3", "system_mock_value_test_3");
        testSystemContexts.put(Context.named("loader"), testFile);

        system.put(Scope.Default, defaultSystemContexts);
        system.put(Scope.named("test"), testSystemContexts);

        Map<Context, PropFile> defaultLocalContexts = new ConcurrentHashMap<Context, PropFile>(3, 1.0f);
        Map<Context, PropFile> testLocalContexts = new ConcurrentHashMap<Context, PropFile>(3, 1.0f);
        defaultFile = new PropFile(Context.named("loader"), PropFile.Loc.Local);
        defaultFile.add("mock_key", "local_mock_value");
        defaultFile.add("mock_key_2", "local_mock_value_2");
        defaultLocalContexts.put(Context.named("loader"), defaultFile);
        defaultFile = new PropFile(Context.named("context-without-system"), PropFile.Loc.Local);
        defaultFile.add("mock_nosystem_key", "local_mock_nosystem_value");
        defaultFile.add("mock_nosystem_key_2", "local_mock_nosystem_value_2");
        defaultLocalContexts.put(Context.named("context-without-system"), defaultFile);
        testFile = new PropFile(Context.named("loader"), Scope.named("test"), PropFile.Loc.Local);
        testFile.add("mock_key", "local_mock_value_test");
        testLocalContexts.put(Context.named("loader"), testFile);
        local.put(Scope.Default, defaultLocalContexts);
        local.put(Scope.named("test"), testLocalContexts);

        chain = (Map<Scope, Map<Context, PropFileChain>>) chainMethod.invoke(null, system, local, adHoc);

        assertTrue(chain.containsKey(Scope.Default));
        assertTrue(chain.containsKey(Scope.named("test")));
        assertEquals(2, chain.size());

        Map<Context, PropFileChain> defaultSystemChain = chain.get(Scope.Default);
        assertEquals(2, defaultSystemChain.size());
        Map<Context, PropFileChain> testSystemChain = chain.get(Scope.named("test"));
        assertEquals(2, testSystemChain.size());
        PropFileChain defaultChain = defaultSystemChain.get(Context.named("loader"));
        assertEquals("local_mock_value", defaultChain.get("mock_key").value());
        assertEquals("local_mock_value_2", defaultChain.get("mock_key_2").value());
        assertEquals("system_mock_value_3", defaultChain.get("mock_key_3").value());
        assertEquals("system_mock_value_4", defaultChain.get("mock_key_4").value());
        defaultChain = defaultSystemChain.get(Context.named("context-without-system"));
        assertEquals("local_mock_nosystem_value", defaultChain.get("mock_nosystem_key").value());
        assertEquals("local_mock_nosystem_value_2", defaultChain.get("mock_nosystem_key_2").value());
        PropFileChain testChain = testSystemChain.get(Context.named("loader"));
        assertEquals("local_mock_value_test", testChain.get("mock_key").value());
        assertEquals("local_mock_value_2", testChain.get("mock_key_2").value());
        assertEquals("system_mock_value_test_3", testChain.get("mock_key_3").value());
        assertEquals("system_mock_value_4", testChain.get("mock_key_4").value());
        testChain = testSystemChain.get(Context.named("context-without-system"));
        assertEquals("local_mock_nosystem_value", testChain.get("mock_nosystem_key").value());
        assertEquals("local_mock_nosystem_value_2", testChain.get("mock_nosystem_key_2").value());

        // test case : no context within a scope but there is that context within the default-scope

        // setup the system props
        defaultSystemContexts = new ConcurrentHashMap<Context, PropFile>(2, 1.0f);

        defaultFile = new PropFile(Context.named("loader"), PropFile.Loc.System);
        defaultFile.add("mock_key", "system_mock_value");
        defaultSystemContexts.put(Context.named("loader"), defaultFile);

        system.clear();
        system.put(Scope.Default, defaultSystemContexts);

        // setup the local props
        testLocalContexts = new ConcurrentHashMap<Context, PropFile>(3, 1.0f);
        testFile = new PropFile(Context.named("different"), Scope.named("test"), PropFile.Loc.Local);
        testFile.add("mock_key", "local_mock_value_test");
        testLocalContexts.put(Context.named("different"), testFile);
        
        local.clear();
        local.put(Scope.named("test"), testLocalContexts);

        chain = (Map<Scope, Map<Context, PropFileChain>>) chainMethod.invoke(null, system, local, adHoc);

        assertTrue(chain.containsKey(Scope.Default));
        assertTrue(chain.containsKey(Scope.named("test")));
        assertEquals(2, chain.size());

        defaultSystemChain = chain.get(Scope.Default);
        assertEquals(2, defaultSystemChain.size());
        defaultChain = defaultSystemChain.get(Context.named("loader"));
        assertEquals("system_mock_value", defaultChain.get("mock_key").value());

        testSystemChain = chain.get(Scope.named("test"));
        assertEquals(2, testSystemChain.size()); // the explicitly added 'different' and the inherited 'loader' from the default-scope
        testChain = testSystemChain.get(Context.named("loader"));
        assertEquals("system_mock_value", testChain.get("mock_key").value()); // again, inherited
        testChain = testSystemChain.get(Context.named("different"));
        assertEquals("local_mock_value_test", testChain.get("mock_key").value());

        // test case: ensure all default-scoped contexts are represented in the scoped contexts

        Map<Context, PropFile> systemContexts = new ConcurrentHashMap<Context, PropFile>(3, 1.0f);
        PropFile projectFile = new PropFile(Context.named("project"), PropFile.Loc.System);
        projectFile.add("artifact.name", "default-name");
        systemContexts.put(Context.named("project"), projectFile);
        PropFile packageFile = new PropFile(Context.named("package"), PropFile.Loc.System);
        packageFile.add("name", "${project.artifact.name}");
        systemContexts.put(Context.named("package"), packageFile);

        Map<Context, PropFile> systemTestContexts = new ConcurrentHashMap<Context, PropFile>(3, 1.0f);
        PropFile testScopedProjectFile = new PropFile(Context.named("project"), Scope.named("test"), PropFile.Loc.System);
        testScopedProjectFile.add("artifact.name", "test-name");
        systemTestContexts.put(Context.named("project"), testScopedProjectFile);

        system.clear();
        local.clear();
        system.put(Scope.Default, systemContexts);
        system.put(Scope.named("test"), systemTestContexts);

        chain = (Map<Scope, Map<Context, PropFileChain>>) chainMethod.invoke(null, system, local, adHoc);
        
        assertEquals(2, chain.size());
        Map<Context, PropFileChain> defaultScopeChain = chain.get(Scope.Default);
        Map<Context, PropFileChain> testScopedChain = chain.get(Scope.named("test"));

        assertTrue(defaultScopeChain.containsKey(Context.named("project")));
        assertTrue(defaultScopeChain.containsKey(Context.named("package")));
        assertEquals("default-name", defaultScopeChain.get(Context.named("project")).get("artifact.name").value());
        // filtered by project.artifact.name
        assertEquals("default-name", defaultScopeChain.get(Context.named("package")).get("name").value());

        assertTrue(testScopedChain.containsKey(Context.named("project")));
        assertTrue(testScopedChain.containsKey(Context.named("package")));
        assertEquals("test-name", testScopedChain.get(Context.named("project")).get("artifact.name").value());
        // filtered by project.artifact.name
        assertEquals("test-name", testScopedChain.get(Context.named("package")).get("name").value());

        // test case: depmngr's default-system 'localRepo' == default-system,
        //            depmngr's default-local 'localRepo' == default-local => scoped-local 'localRepo' == default-local

        systemContexts = new ConcurrentHashMap<Context, PropFile>(3, 1.0f);
        PropFile depmngrFile = new PropFile(Context.named("depmngr"), PropFile.Loc.System);
        depmngrFile.add("localRepo", "default-system");
        systemContexts.put(Context.named("depmngr"), depmngrFile);

        system.clear();
        system.put(Scope.Default, systemContexts);

        Map<Context, PropFile> localContexts = new ConcurrentHashMap<Context, PropFile>(3, 1.0f);
        depmngrFile = new PropFile(Context.named("depmngr"), PropFile.Loc.Local);
        depmngrFile.add("localRepo", "default-local");
        localContexts.put(Context.named("depmngr"), depmngrFile);
        Map<Context, PropFile> localTestScopedContexts = new ConcurrentHashMap<Context, PropFile>(3, 1.0f);
        PropFile testScopedDependenciesFile = new PropFile(Context.named("dependencies"), Scope.named("test"), PropFile.Loc.Local);
        testScopedDependenciesFile.add("junit:junit", "4.10");
        localTestScopedContexts.put(Context.named("dependencies"), testScopedDependenciesFile);

        local.clear();
        local.put(Scope.Default, localContexts);
        local.put(Scope.named("test"), localTestScopedContexts);

        chain = (Map<Scope, Map<Context, PropFileChain>>) chainMethod.invoke(null, system, local, adHoc);

        assertEquals(2, chain.size());
        assertTrue(chain.containsKey(Scope.Default));
        assertTrue(chain.containsKey(Scope.named("test")));
        defaultScopeChain = chain.get(Scope.Default);
        testScopedChain = chain.get(Scope.named("test"));

        assertEquals(2, defaultScopeChain.size());
        assertTrue(defaultScopeChain.containsKey(Context.named("depmngr")));
        assertEquals("default-local", defaultScopeChain.get(Context.named("depmngr")).get("localRepo").value());

        assertEquals(2, testScopedChain.size());
        assertTrue(testScopedChain.containsKey(Context.named("dependencies")));
        assertTrue(testScopedChain.containsKey(Context.named("depmngr")));
        assertEquals("4.10", testScopedChain.get(Context.named("dependencies")).get("junit:junit").value());
        assertEquals(PropFile.Loc.Local, testScopedChain.get(Context.named("depmngr")).get("localRepo").loc());
        assertEquals("default-local", testScopedChain.get(Context.named("depmngr")).get("localRepo").value());
        
    }

}
