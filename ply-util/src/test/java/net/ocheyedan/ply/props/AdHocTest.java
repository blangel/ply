package net.ocheyedan.ply.props;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

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
        assertEquals(size + 1, AdHoc.adHocProps.size());
        assertEquals("compiler", AdHoc.adHocProps.get(size).context.name);
        assertEquals("src", AdHoc.adHocProps.get(size).name);
        assertEquals("", AdHoc.adHocProps.get(size).get(Scope.Default).value);

        AdHoc.parseAndAdd("compiler.src=src");
        assertEquals(size + 1, AdHoc.adHocProps.size());
        assertEquals("compiler", AdHoc.adHocProps.get(size).context.name);
        assertEquals("src", AdHoc.adHocProps.get(size).name);
        assertEquals("src", AdHoc.adHocProps.get(size).get(Scope.Default).value);

        AdHoc.parseAndAdd("compiler.src.dir=src");
        assertEquals(size + 2, AdHoc.adHocProps.size());
        assertEquals("compiler", AdHoc.adHocProps.get(size + 1).context.name);
        assertEquals("src.dir", AdHoc.adHocProps.get(size + 1).name);
        assertEquals("src", AdHoc.adHocProps.get(size).get(Scope.Default).value);

        AdHoc.parseAndAdd("compiler#test.src=src");
        assertEquals(size + 2, AdHoc.adHocProps.size());
        assertEquals("compiler", AdHoc.adHocProps.get(size).context.name);
        assertEquals("src", AdHoc.adHocProps.get(size).name);
        assertEquals("src", AdHoc.adHocProps.get(size).get(new Scope("test")).value);
        
        AdHoc.parseAndAdd("compiler#test.src.dir=src");
        assertEquals(size + 2, AdHoc.adHocProps.size());
        assertEquals("compiler", AdHoc.adHocProps.get(size + 1).context.name);
        assertEquals("src.dir", AdHoc.adHocProps.get(size + 1).name);
        assertEquals("src", AdHoc.adHocProps.get(size + 1).get(new Scope("test")).value);

    }

}
