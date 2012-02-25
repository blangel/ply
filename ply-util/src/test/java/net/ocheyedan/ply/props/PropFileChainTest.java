package net.ocheyedan.ply.props;

import org.junit.Test;

import java.util.Collections;
import java.util.Iterator;

import static junit.framework.Assert.*;

/**
 * User: blangel
 * Date: 2/22/12
 * Time: 9:02 PM
 */
public class PropFileChainTest {
    
    @Test
    public void get() {
        PropFileChain chain = new PropFileChain(Collections.<Context, PropFileChain>emptyMap());
        PropFile.Prop empty = chain.get("test");
        assertSame(PropFile.Prop.Empty, empty);
        
        PropFile systemFile = new PropFile(Context.named("test"), PropFile.Loc.System);
        systemFile.add("test", "system-value");
        PropFile localFile = new PropFile(Context.named("test"), PropFile.Loc.Local);
        localFile.add("test", "local-value");
        PropFile adHocFile = new PropFile(Context.named("test"), PropFile.Loc.AdHoc);
        adHocFile.add("test", "adhoc-value");

        chain.set(systemFile, PropFile.Loc.System);
        
        PropFile.Prop systemProp = chain.get("test");
        assertNotSame(empty, systemProp);
        assertEquals("system-value", systemProp.value());
        
        chain.set(localFile, PropFile.Loc.Local);
        
        PropFile.Prop localProp = chain.get("test");
        assertNotSame(systemProp, localProp);
        assertEquals("local-value", localProp.value());
        
        chain.set(adHocFile, PropFile.Loc.AdHoc);
        
        PropFile.Prop adhocProp = chain.get("test");
        assertNotSame(localProp, adhocProp);
        assertEquals("adhoc-value", adhocProp.value());
    }
    
    @Test
    public void set() {
        PropFileChain chain = new PropFileChain(Collections.<Context, PropFileChain>emptyMap());
        PropFile.Prop empty = chain.get("test");
        assertSame(PropFile.Prop.Empty, empty);

        PropFile systemFile = new PropFile(Context.named("test"), PropFile.Loc.System);
        systemFile.add("test", "system-value");
        PropFile overriddenSystemFile = new PropFile(Context.named("test"), PropFile.Loc.System);
        overriddenSystemFile.add("test", "overridden-system-value");

        chain.set(systemFile, PropFile.Loc.System);

        PropFile.Prop systemProp = chain.get("test");
        assertNotSame(empty, systemProp);
        assertEquals("system-value", systemProp.value());

        chain.set(overriddenSystemFile, PropFile.Loc.System);

        PropFile.Prop localProp = chain.get("test");
        assertNotSame(systemProp, localProp);
        assertEquals("overridden-system-value", localProp.value());
    }
    
    @Test
    public void props() {
        PropFileChain chain = new PropFileChain(Collections.<Context, PropFileChain>emptyMap());
        PropFile.Prop empty = chain.get("test");
        assertSame(PropFile.Prop.Empty, empty);
        
        Iterable<PropFile.Prop> iter = chain.props();
        assertFalse(iter.iterator().hasNext());
        assertSame(PropFile.Prop.Empty, iter.iterator().next());

        PropFile systemFile = new PropFile(Context.named("test"), PropFile.Loc.System);
        PropFile.Prop systemTestProp = systemFile.add("test", "system-value");

        chain.set(systemFile, PropFile.Loc.System);

        iter = chain.props();
        Iterator<PropFile.Prop> iterator = iter.iterator();
        assertTrue(iterator.hasNext());
        assertEquals(systemTestProp, iterator.next());
        assertFalse(iterator.hasNext());

        PropFile localFile = new PropFile(Context.named("test"), PropFile.Loc.Local);
        PropFile.Prop localTestProp = localFile.add("test", "local-value");
        PropFile.Prop localAnotherProp = localFile.add("another", "local-another-value");
        chain.set(localFile, PropFile.Loc.Local);

        iter = chain.props();
        iterator = iter.iterator();
        assertTrue(iterator.hasNext());
        assertEquals(localTestProp, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(localAnotherProp, iterator.next());
        assertFalse(iterator.hasNext());
        
        PropFileChain scopedChain = new PropFileChain(chain, Collections.<Context, PropFileChain>emptyMap());
        PropFile localScopedFile = new PropFile(Context.named("test"), Scope.named("test"), PropFile.Loc.Local);
        PropFile.Prop localScopedTestProp = localFile.add("test", "local-test-value");
        scopedChain.set(localScopedFile, PropFile.Loc.Local);

        iter = scopedChain.props();
        iterator = iter.iterator();
        assertTrue(iterator.hasNext());
        assertEquals(localScopedTestProp, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(localAnotherProp, iterator.next());
        assertFalse(iterator.hasNext());

    }
    
}
