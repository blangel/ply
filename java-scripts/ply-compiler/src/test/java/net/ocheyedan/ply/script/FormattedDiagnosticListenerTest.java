package net.ocheyedan.ply.script;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * User: blangel
 * Date: 10/21/11
 * Time: 5:20 PM
 */
public class FormattedDiagnosticListenerTest {

    @Test
    public void getErrors() {
        FormattedDiagnosticListener formattedDiagnosticListener = new FormattedDiagnosticListener("test");
        assertEquals(0, formattedDiagnosticListener.getErrors().size());
    }

}
