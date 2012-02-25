package net.ocheyedan.ply.props;

import org.junit.Test;

import java.util.Iterator;

import static junit.framework.Assert.*;
import static net.ocheyedan.ply.props.PropFile.Loc;

/**
 * User: blangel
 * Date: 2/17/12
 * Time: 12:49 PM
 */
public class PropFileTest {

    @Test
    public void newInstance() {
        // two argument constructor
        try {
            new PropFile(null, null);
            fail("Context and Loc must be non-null.");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            new PropFile(Context.named("not null"), null);
            fail("Loc must be non-null.");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            new PropFile(null, Loc.System);
            fail("Context must be non-null.");
        } catch (NullPointerException npe) {
            // expected
        }

        // three argument constructor
        try {
            new PropFile(null, null, null);
            fail("Context and Scope and Loc must be non-null.");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            new PropFile(Context.named("not null"), null, null);
            fail("Scope and Loc must be non-null.");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            new PropFile(null, Scope.named("not null"), null);
            fail("Context and Loc must be non-null.");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            new PropFile(null, null, Loc.System);
            fail("Context and Scope must be non-null.");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            new PropFile(Context.named("not null"), Scope.named("not null"), null);
            fail("Loc must be non-null.");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            new PropFile(null, Scope.named("not null"), Loc.System);
            fail("Context must be non-null.");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            new PropFile(Context.named("not null"), null, Loc.System);
            fail("Scope must be non-null.");
        } catch (NullPointerException npe) {
            // expected
        }

    }
    
    @Test
    public void add() {
        
        PropFile propFile = new PropFile(Context.named("test"), Loc.System);
        PropFile.Prop prop = propFile.add("test", "value");
        assertNotNull(prop);
        assertEquals("test", prop.name);
        assertEquals("value", prop.value());
        assertEquals("value", prop.unfilteredValue);
        assertEquals(Context.named("test"), prop.context());
        assertEquals(Scope.Default, prop.scope());
        assertEquals(Loc.System, prop.loc());
        assertEquals("", prop.comments());
        assertTrue(propFile.contains("test"));

        PropFile.Prop sameProp = propFile.add("test", "different");
        assertSame(prop, sameProp);
        assertEquals("test", sameProp.name);
        assertEquals("value", sameProp.value());
        assertEquals("value", sameProp.unfilteredValue);
        assertEquals("", sameProp.comments());
        assertTrue(propFile.contains("test"));

       PropFile.Prop spacedProp = propFile.add(" test-spaced ", "value");
        assertNotNull(spacedProp);
        assertEquals("test-spaced", spacedProp.name);
        assertEquals("value", spacedProp.value());
        assertEquals("", spacedProp.comments());
        assertTrue(propFile.contains("test-spaced"));

        prop = propFile.add("test-comments", "value", "comments");
        assertNotNull(prop);
        assertEquals("test-comments", prop.name);
        assertEquals("value", prop.value());
        assertEquals("comments", prop.comments());
        assertTrue(propFile.contains("test-comments"));

        sameProp = propFile.add("test-comments", "different", "different");
        assertSame(prop, sameProp);
        assertEquals("test-comments", sameProp.name);
        assertEquals("value", sameProp.value());
        assertEquals("comments", sameProp.comments());
        assertTrue(propFile.contains("test-comments"));

        prop = propFile.add("test-unfiltered-comments", "value", "comments");
        assertNotNull(prop);
        assertEquals("test-unfiltered-comments", prop.name);
        assertEquals("value", prop.value());
        assertEquals("comments", prop.comments());
        assertTrue(propFile.contains("test-unfiltered-comments"));

        sameProp = propFile.add("test-unfiltered-comments", "different", "different");
        assertSame(prop, sameProp);
        assertEquals("test-unfiltered-comments", sameProp.name);
        assertEquals("value", sameProp.value());
        assertEquals("comments", sameProp.comments());
        assertTrue(propFile.contains("test-unfiltered-comments"));

    }

    @Test
    public void get() {

        PropFile propFile = new PropFile(Context.named("context"), Loc.System);
        assertSame(PropFile.Prop.Empty, propFile.get("test"));
        PropFile.Prop testProp = propFile.add("test", "value");
        assertSame(testProp, propFile.get("test"));

        PropFile localTestScoped = new PropFile(Context.named("context"), Scope.named("test"), Loc.Local);
        PropFile.Prop testScopedProp = localTestScoped.add("test", "scoped-as-test");
        assertSame(testScopedProp, localTestScoped.get("test"));
        assertSame(testProp, propFile.get("test"));

        PropFile adHocFile = new PropFile(Context.named("context"), Scope.Default, Loc.AdHoc);
        assertSame(PropFile.Prop.Empty, adHocFile.get("test"));
        PropFile.Prop adHocProp = adHocFile.add("test", "ad hoc value");
        assertSame(adHocProp, adHocFile.get("test"));
    }

    @Test
    public void contains() {
        PropFile systemProps = new PropFile(Context.named("context"), Loc.System);
        assertFalse(systemProps.contains("test"));
        systemProps.add("test", "value");
        assertTrue(systemProps.contains("test"));
        PropFile localProps = new PropFile(Context.named("context"), Scope.Default, Loc.Local);
        assertFalse(localProps.contains("test"));
        PropFile adHocProps = new PropFile(Context.named("context"), Scope.Default, Loc.AdHoc);
        assertFalse(adHocProps.contains("test"));

        localProps.add("test", "different");
        assertTrue(systemProps.contains("test"));
        assertEquals("value", systemProps.get("test").value());
        assertTrue(localProps.contains("test"));
        assertEquals("different", localProps.get("test").value());
        assertFalse(adHocProps.contains("test"));
        assertSame(PropFile.Prop.Empty, adHocProps.get("test"));
    }
    
    @Test
    public void remove() {
        PropFile systemProps = new PropFile(Context.named("context"), Loc.System);
        assertNull(systemProps.remove("test"));
        PropFile.Prop testProp = systemProps.add("test", "value");
        assertSame(testProp, systemProps.remove("test"));
        assertNull(systemProps.remove("test"));
        
        PropFile localProps = new PropFile(Context.named("context"), Scope.Default, Loc.Local);
        assertNull(localProps.remove("test"));
        systemProps.add("test", "value");
        assertFalse(localProps.contains("test"));
        assertNull(localProps.remove("test"));
        assertFalse(localProps.contains("test"));
        testProp = localProps.add("test", "diff");
        assertSame(testProp, localProps.remove("test"));
        assertFalse(localProps.contains("test"));
    }
    
    @Test
    public void props() {
        PropFile systemProps = new PropFile(Context.named("context"), Loc.System);
        systemProps.add("one", "one-value");
        systemProps.add("two", "two-value");
        systemProps.add("three", "three-value");
        Iterator<PropFile.Prop> iter = systemProps.props().iterator();
        assertEquals("one", iter.next().name);
        assertEquals("two", iter.next().name);
        assertEquals("three", iter.next().name);
        assertFalse(iter.hasNext());
        // test that multiple invocations produce the same results (i.e., reset)
        iter = systemProps.props().iterator();
        assertEquals("one", iter.next().name);
        assertEquals("two", iter.next().name);
        assertEquals("three", iter.next().name);
        assertFalse(iter.hasNext());
        // test that inserts after first iteration are represented by additional calls to the iterable
        systemProps.add("four", "four-value");
        iter = systemProps.props().iterator();
        assertEquals("one", iter.next().name);
        assertEquals("two", iter.next().name);
        assertEquals("three", iter.next().name);
        assertEquals("four", iter.next().name);
        assertFalse(iter.hasNext());
    }
    
}
