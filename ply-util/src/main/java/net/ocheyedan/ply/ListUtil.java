package net.ocheyedan.ply;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * User: blangel
 * Date: 11/20/11
 * Time: 1:50 PM
 *
 * Provides utility methods for {@link List} objects.
 */
public class ListUtil {

    private static class NaturalOrderComparator<T extends Comparable<? super T>> implements Comparator<T> {
        @Override public int compare(T o1, T o2) {
            return o1.compareTo(o2);
        }
    }

    /**
     * Sorts {@code list} according to its natural ordering using the quick-sort algorithm.
     * @param list to sort
     * @param <T> implements {@link Comparable}
     */
    public static <T extends Comparable<? super T>> void sort(List<T> list) {
        int low = 0;
        int high = list.size() - 1;
        sort(list, new NaturalOrderComparator<T>(), low, high);
    }

    /**
     * Sorts {@code list} according to {@code comparator} using the quick-sort algorithm.
     * @param list to sort
     * @param comparator to use in determining order for type {@code T}.
     * @param <T> type of the elements of the list
     */
    public static <T> void sort(List<T> list, Comparator<T> comparator) {
        int low = 0;
        int high = list.size() - 1;
        sort(list, comparator, low, high);
    }

    private static <T> void sort(List<T> list, Comparator<T> comparator, int low, int high) {
        if (high <= low) { return; }
        int pivot = partition(list, comparator, low, high);
        sort(list, comparator, low, pivot - 1);
        sort(list, comparator, pivot + 1, high);
    }

    private static <T> int partition(List<T> list, Comparator<T> comparator, int low, int high) {
        int i = low;
        int j = high + 1;
        T pivot = list.get(low);
        while (true) {
            // find item on low to swap
            while (comparator.compare(list.get(++i), pivot) < 0) {
                if (i == high) {
                    break;
                }
            }
            // find item on high to swap
            while (comparator.compare(pivot, list.get(--j)) < 0) {
                if (j == low) {
                    break;
                }
            }
            // check if pointers cross
            if (i >= j) {
                break;
            }
            Collections.swap(list, i, j);
        }
        // put pivot in list.get(j)
        Collections.swap(list, low, j);
        return j;
    }

}
