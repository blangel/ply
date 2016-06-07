package net.ocheyedan.ply;

import org.junit.Test;
import java.util.UUID;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class PwdUtilTest {

    @Test
    public void testSimpleEncryption() {
        String text = UUID.randomUUID().toString();
        String enc = PwdUtil.encrypt(text);
        assertThat(text, not(equalTo(enc)));
        String dec = PwdUtil.decrypt(enc);
        assertEquals(text, dec);
    }

}