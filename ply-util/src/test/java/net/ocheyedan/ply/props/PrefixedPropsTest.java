package net.ocheyedan.ply.props;

import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static junit.framework.Assert.assertEquals;

/**
 * User: blangel
 * Date: 3/20/15
 * Time: 9:14 AM
 */
public class PrefixedPropsTest {

    @Test
    public void getArguments() {
        PropFile container = new PropFile(Context.named(""), PropFile.Loc.System);
        container.add("argsFoo", "=bar");
        PropFileChain chain = new PropFileChain(Collections.<Context, PropFileChain>emptyMap());
        chain.set(container, PropFile.Loc.System);

        Map<Context, PropFileChain> filterConsultant = new ConcurrentHashMap<Context, PropFileChain>(2, 1.0f);
        filterConsultant.put(Context.named(""), chain);
        PropFileChain props = new PropFileChain(chain, filterConsultant);

        List<String> arguments = PrefixedProps.getArguments(props, "args");
        assertEquals(1, arguments.size());
        assertEquals("Foo=bar", arguments.get(0));

        container.add("argsfoo", "=Bar");
        arguments = PrefixedProps.getArguments(props, "args");
        assertEquals(2, arguments.size());
        assertEquals("Foo=bar", arguments.get(0));
        assertEquals("foo=Bar", arguments.get(1));

        container.add("argsARG", "VAL");
        arguments = PrefixedProps.getArguments(props, "args", "=");
        assertEquals(3, arguments.size());
        assertEquals("Foo==bar", arguments.get(0));
        assertEquals("foo==Bar", arguments.get(1));
        assertEquals("ARG=VAL", arguments.get(2));
    }

}
