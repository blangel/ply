package org.moxie.ply;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * User: blangel
 * Date: 10/13/11
 * Time: 8:50 PM
 */
public class BitUtilTest {

    @Test
    public void toHexString() {
        assertEquals("", BitUtil.toHexString(null));

        assertEquals("00", BitUtil.toHexString(new byte[] { 0 }));

        assertEquals("FFFFFFFF", BitUtil.toHexString(new byte[] { (byte) 0xff, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF }));
    }

}
