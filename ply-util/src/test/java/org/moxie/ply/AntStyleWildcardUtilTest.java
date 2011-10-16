package org.moxie.ply;

import org.junit.Test;

import java.util.regex.Pattern;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * User: blangel
 * Date: 10/16/11
 * Time: 2:12 PM
 */
public class AntStyleWildcardUtilTest {

    @Test
    public void regexString() {
        String antStyleWildcard = "**/*Test.???";
        String regex = AntStyleWildcardUtil.regexString(antStyleWildcard);
        assertEquals(".+?/[^/]*?Test..{1}.{1}.{1}", regex);
    }

    @Test
    public void regex() {
        Pattern pattern = AntStyleWildcardUtil.regex("**/*Test.???");
        assertTrue(pattern.matcher("/more/and/more/somethingTest.xml").matches());
        assertTrue(pattern.matcher("test/1Test.111").matches());
        assertTrue(pattern.matcher("brian/SomethingTest....").matches());
        assertTrue(pattern.matcher("a/testTest.cvs").matches());
        assertTrue(pattern.matcher("something/Test.xml").matches());

        assertFalse(pattern.matcher("/testTest.cvs").matches());
        assertFalse(pattern.matcher("aTest.xml").matches());
        assertFalse(pattern.matcher("something/aTest.xmld").matches());
        assertFalse(pattern.matcher("something/aTest.xm").matches());
        assertFalse(pattern.matcher("/testTestXcvs").matches());
        
    }

}
