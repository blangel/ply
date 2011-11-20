package net.ocheyedan.ply;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static junit.framework.Assert.assertTrue;

/**
 * User: blangel
 * Date: 11/20/11
 * Time: 2:08 PM
 */
public class ListUtilTest {

    private static class Mock {
        private String mock;
        private Mock(String mock) {
            this.mock = mock;
        }
        @Override public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Mock other = (Mock) o;
            return (mock == null ? other.mock == null : mock.equals(other.mock));
        }
        @Override public int hashCode() {
            return mock != null ? mock.hashCode() : 0;
        }
    }

    @Test
    public void sort() {
        List<Integer> ints = new ArrayList<Integer>(100);
        Random random = new Random();
        for (int i = 0; i < 100; i++) {
            ints.add(random.nextInt());
        }
        ListUtil.sort(ints);
        Integer last = Integer.MIN_VALUE;
        for (Integer item : ints) {
            assertTrue(last <= item);
            last = item;
        }

        List<Mock> mocks = new ArrayList<Mock>(100);
        for (int i = 0; i < 100; i++) {
            mocks.add(new Mock(String.valueOf(random.nextInt())));
        }
        Comparator<Mock> comparator = new Comparator<Mock>() {
            @Override public int compare(Mock o1, Mock o2) {
                return o1.mock.compareTo(o2.mock);
            }
        };
        ListUtil.sort(mocks, comparator);
        Mock lastMockValue = new Mock("");
        for (Mock mock : mocks) {
            assertTrue(comparator.compare(lastMockValue, mock) < 0);
            lastMockValue = mock;
        }
    }

}
