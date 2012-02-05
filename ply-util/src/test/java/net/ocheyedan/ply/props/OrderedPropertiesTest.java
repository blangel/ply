package net.ocheyedan.ply.props;

import org.junit.Test;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;

import static junit.framework.Assert.assertEquals;

/**
 * User: blangel
 * Date: 2/5/12
 * Time: 3:48 PM
 */
public class OrderedPropertiesTest {

    @Test
    public void keys() {
        OrderedProperties props = new OrderedProperties();
        props.put("first", "1");
        props.put("second", "2");
        props.put("third", "3");
        props.put("fourth", "4");
        props.put("fifth", "5");
        Enumeration<Object> keys = props.keys();
        assertEquals("first", keys.nextElement());
        assertEquals("second", keys.nextElement());
        assertEquals("third", keys.nextElement());
        assertEquals("fourth", keys.nextElement());
        assertEquals("fifth", keys.nextElement());
    }

    @Test
    public void keySet() {
        OrderedProperties props = new OrderedProperties();
        props.put("first", "1");
        props.put("second", "2");
        props.put("third", "3");
        props.put("fourth", "4");
        props.put("fifth", "5");
        Set<Object> keys = props.keySet();
        Iterator<Object> iterator = keys.iterator();
        assertEquals("first", iterator.next());
        assertEquals("second", iterator.next());
        assertEquals("third", iterator.next());
        assertEquals("fourth", iterator.next());
        assertEquals("fifth", iterator.next());
    }

}
