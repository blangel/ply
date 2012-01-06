package net.ocheyedan.ply;

import java.util.Collection;
import java.util.Iterator;

/**
 * User: blangel
 * Date: 1/5/12
 * Time: 2:49 PM
 *
 * Provides utility methods when interacting with iterable object in java.
 */
public final class Iter {

    /**
     * Extends {@link Iterable} to expose size information.
     * @param <T> type of the underlying collection/array
     */
    public static interface Sized<T> extends Iterable<T> {
        int size();
    }

    private static final class ArrayIterable<T> implements Sized<T> {
        private final T[] array;
        private ArrayIterable(T[] array) {
            this.array = array;
        }
        @Override public Iterator<T> iterator() {
            return new Iterator<T>() {
                int index = 0;
                @Override public boolean hasNext() {
                    return (index < array.length);
                }
                @Override public T next() {
                    return array[index++];
                }
                @Override public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
        @Override public int size() {
            return array.length;
        }
    }

    private static final class CollectionIterable<T> implements Sized<T> {
        private final Collection<T> collection;
        private CollectionIterable(Collection<T> collection) {
            this.collection = collection;
        }
        @Override public Iterator<T> iterator() {
            return collection.iterator();
        }
        @Override public int size() {
            return collection.size();
        }
    }

    /**
     * Converts {@code array} into an {@link Sized} object without copying the array into a collection.
     * @param array to convert
     * @param <T> type of the elements within {@code array}
     * @return an {@link Sized} over the elements of {@code collection}
     */
    public static <T> Sized<T> sized(T[] array) {
        return (array == null ? null : new ArrayIterable<T>(array));
    }

    /**
     * Converts {@code collection} into an {@link Sized} object without copying the collection.
     * @param collection to convert
     * @param <T> type of the elements within {@code collection}
     * @return an {@link Sized} over the elements of {@code collection}
     */
    public static <T> Sized<T> sized(Collection<T> collection) {
        return (collection == null ? null : new CollectionIterable<T>(collection));
    }

    private Iter() { }

}
