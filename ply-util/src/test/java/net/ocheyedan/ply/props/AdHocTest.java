package net.ocheyedan.ply.props;

import org.junit.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static junit.framework.Assert.*;

/**
 * User: blangel
 * Date: 12/30/11
 * Time: 10:08 AM
 */
public class AdHocTest {
    
    @Test public void parseAndAdd() {
        int size = AdHoc.adHocProps.size();
        AdHoc.parseAndAdd(null);
        assertEquals(size, AdHoc.adHocProps.size());

        AdHoc.parseAndAdd("");
        assertEquals(size, AdHoc.adHocProps.size());

        AdHoc.parseAndAdd("compiler");
        assertEquals(size, AdHoc.adHocProps.size());

        AdHoc.parseAndAdd("compiler.src");
        assertEquals(size, AdHoc.adHocProps.size());

        AdHoc.parseAndAdd("compiler.src.dir");
        assertEquals(size, AdHoc.adHocProps.size());

        AdHoc.parseAndAdd("compiler.src=");
        
        Map<Context, PropFile> defaultScopes = AdHoc.adHocProps.get(Scope.Default);
        Context compilerContext = Context.named("compiler");
        assertTrue(defaultScopes.containsKey(compilerContext));
        PropFile compilerPropFile = defaultScopes.get(compilerContext);
        PropFile.Prop compilerSrcProp = compilerPropFile.get("src");
        assertNotSame(PropFile.Prop.Empty, compilerSrcProp);
        assertEquals("", compilerSrcProp.value());

        AdHoc.adHocProps.clear();

        AdHoc.parseAndAdd("compiler.src=src");
        defaultScopes = AdHoc.adHocProps.get(Scope.Default);
        assertTrue(defaultScopes.containsKey(compilerContext));
        compilerPropFile = defaultScopes.get(compilerContext);
        compilerSrcProp = compilerPropFile.get("src");
        assertNotSame(PropFile.Prop.Empty, compilerSrcProp);
        assertEquals("src", compilerSrcProp.value());

        AdHoc.parseAndAdd("compiler.src.dir=src");
        defaultScopes = AdHoc.adHocProps.get(Scope.Default);
        assertTrue(defaultScopes.containsKey(compilerContext));
        compilerPropFile = defaultScopes.get(compilerContext);
        PropFile.Prop compilerSrcDirProp = compilerPropFile.get("src.dir");
        assertNotSame(PropFile.Prop.Empty, compilerSrcDirProp);
        assertEquals("src", compilerSrcDirProp.value());
        compilerSrcProp = compilerPropFile.get("src");
        assertNotSame(PropFile.Prop.Empty, compilerSrcProp);
        assertEquals("src", compilerSrcProp.value());
        
        AdHoc.parseAndAdd("compiler#test.src=test-src");

        assertTrue(defaultScopes.containsKey(compilerContext));
        compilerPropFile = defaultScopes.get(compilerContext);
        compilerSrcDirProp = compilerPropFile.get("src.dir");
        assertNotSame(PropFile.Prop.Empty, compilerSrcDirProp);
        assertEquals("src", compilerSrcDirProp.value());
        compilerSrcProp = compilerPropFile.get("src");
        assertNotSame(PropFile.Prop.Empty, compilerSrcProp);
        assertEquals("src", compilerSrcProp.value());

        Map<Context, PropFile> testScopes = AdHoc.adHocProps.get(Scope.named("test"));
        assertTrue(testScopes.containsKey(compilerContext));
        compilerPropFile = testScopes.get(compilerContext);
        compilerSrcProp = compilerPropFile.get("src");
        assertNotSame(PropFile.Prop.Empty, compilerSrcProp);
        assertEquals("test-src", compilerSrcProp.value());

        AdHoc.parseAndAdd("compiler#test.src.dir=test-src");

        assertTrue(defaultScopes.containsKey(compilerContext));
        compilerPropFile = defaultScopes.get(compilerContext);
        compilerSrcDirProp = compilerPropFile.get("src.dir");
        assertNotSame(PropFile.Prop.Empty, compilerSrcDirProp);
        assertEquals("src", compilerSrcDirProp.value());
        compilerSrcProp = compilerPropFile.get("src");
        assertNotSame(PropFile.Prop.Empty, compilerSrcProp);
        assertEquals("src", compilerSrcProp.value());

        testScopes = AdHoc.adHocProps.get(Scope.named("test"));
        assertTrue(testScopes.containsKey(compilerContext));
        compilerPropFile = testScopes.get(compilerContext);
        compilerSrcProp = compilerPropFile.get("src");
        assertNotSame(PropFile.Prop.Empty, compilerSrcProp);
        assertEquals("test-src", compilerSrcProp.value());
        compilerSrcProp = compilerPropFile.get("src.dir");
        assertNotSame(PropFile.Prop.Empty, compilerSrcProp);
        assertEquals("test-src", compilerSrcProp.value());

    }
    
    @Test
    public void produceFor() {
        
        AdHoc.adHocProps.clear();
        
        Map<Scope, Map<Context, PropFile>> system = new ConcurrentHashMap<Scope, Map<Context, PropFile>>();
        Map<Scope, Map<Context, PropFile>> local = new ConcurrentHashMap<Scope, Map<Context, PropFile>>();
        
        Map<Scope, Map<Context, PropFile>> produced = AdHoc.produceFor(system, local);
        
        assertEquals(0, produced.size());
        assertEquals(0, AdHoc.adHocProps.size());
        
        Map<Context, PropFile> systemContexts = new ConcurrentHashMap<Context, PropFile>();
        Map<Context, PropFile> localContexts = new ConcurrentHashMap<Context, PropFile>();
        Map<Context, PropFile> localTestContexts = new ConcurrentHashMap<Context, PropFile>();
        system.put(Scope.Default, systemContexts);
        local.put(Scope.Default, localContexts);
        local.put(Scope.named("test"), localTestContexts);
        
        produced = AdHoc.produceFor(system, local);
        assertEquals(2, produced.size());
        assertEquals(2, AdHoc.adHocProps.size());

        assertEquals(0, produced.get(Scope.Default).size());
        assertEquals(0, produced.get(Scope.named("test")).size());
        assertEquals(0, AdHoc.adHocProps.get(Scope.Default).size());
        assertEquals(0, AdHoc.adHocProps.get(Scope.named("test")).size());
        
        systemContexts.put(Context.named("foo"), new PropFile(Context.named("foo"), PropFile.Loc.System));
        localContexts.put(Context.named("bar"), new PropFile(Context.named("bar"), PropFile.Loc.Local));
        localContexts.put(Context.named("foo"), new PropFile(Context.named("foo"), PropFile.Loc.Local));
        localTestContexts.put(Context.named("foobar"), new PropFile(Context.named("foobar"), Scope.named("test"), PropFile.Loc.Local));

        produced = AdHoc.produceFor(system, local);
        assertEquals(2, produced.size());
        assertEquals(2, AdHoc.adHocProps.size());

        assertEquals(2, produced.get(Scope.Default).size());
        assertEquals(1, produced.get(Scope.named("test")).size());
        assertEquals(2, AdHoc.adHocProps.get(Scope.Default).size());
        assertEquals(1, AdHoc.adHocProps.get(Scope.named("test")).size());
        assertSame(AdHoc.adHocProps.get(Scope.Default).get(Context.named("foo")), produced.get(Scope.Default).get(Context.named("foo")));
        assertSame(AdHoc.adHocProps.get(Scope.Default).get(Context.named("bar")), produced.get(Scope.Default).get(Context.named("bar")));
        assertSame(AdHoc.adHocProps.get(Scope.named("test")).get(Context.named("foobar")), produced.get(Scope.named("test")).get(Context.named("foobar")));
                
    }

}
