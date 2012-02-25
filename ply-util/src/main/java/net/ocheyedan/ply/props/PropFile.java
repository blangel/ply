package net.ocheyedan.ply.props;

import net.ocheyedan.ply.EmptyConcurrentMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * User: blangel
 * Date: 2/16/12
 * Time: 11:41 AM
 * 
 * A representation of a properties file (i.e., key=value pairs, new line delimited with lines
 * starting with the '#' character considered comments).
 * 
 * This file exists in replace of {@link java.util.Properties} for the following reasons:
 * -1- No way to include comments on specific lines (its either none or all in the beginning of the file)
 * -2- No way to preserve order
 * -3- No way of knowing whether a property value comes from the properties file itself or the given default
 * -4- Confusing API (it is a String Hashtable but not typed and so one can create a, as it itself states, "comprised" object)
 */
public final class PropFile {

    /**
     * Represents an individual property within a {@link PropFile}.
     */
    public final static class Prop implements Comparable<Prop> {
        
        public static final Prop Empty = new Prop(Impl.Empty, "", "", "");

        public final String name;
        public final String unfilteredValue;
        private final Impl owner;
        private final AtomicReference<String> comments;
        private final AtomicReference<String> filteredValue;
        
        Prop(Impl owner, String name, String unfilteredValue, String comments) {
            if ((name == null) || (owner == null)) {
                throw new NullPointerException("Properties' names cannot be null.");
            }
            this.name = name;
            this.unfilteredValue = unfilteredValue;
            this.owner = owner;
            this.comments = new AtomicReference<String>(comments);
            this.filteredValue = new AtomicReference<String>(unfilteredValue);
        }
        
        public final String comments() {
            return comments.get();
        }
        
        public final String value() {
            return this.filteredValue.get();
        }
        
        public final Context context() {
            return owner.context;
        }
        
        public final Scope scope() {
            return owner.scope;
        }
        
        public final Loc loc() {
            return owner.loc;
        }
        
        public final Prop withComments(String comments) {
            this.comments.set(comments);
            return this;
        }

        // TODO - perhaps this should be package-protected as all filtering should happen via the PropFileFilter methods
        public final Prop with(String filteredValue) {
            Prop filtered = new Prop(this.owner, this.name, this.unfilteredValue, this.comments.get());
            filtered.filteredValue.set(filteredValue);
            return filtered;
        }
        
        @Override public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Prop that = (Prop) o;
            return this.name.equals(that.name) && owner.context.equals(that.owner.context);
        }

        @Override public int hashCode() {
            int result = owner.context.hashCode();
            return 31 * result + name.hashCode();
        }

        @Override public int compareTo(Prop prop) {
            if (prop == null) {
                return 1;
            }
            int nameCompare = name.compareTo(prop.name);
            if (nameCompare != 0) {
                return nameCompare;
            }
            return owner.context.compareTo(prop.owner.context);
        }
    }

    /**
     * Represents the location from which a {@link Prop} was resolved.
     */
    public static enum Loc {
        System, Local, AdHoc
    }

    /**
     * Internal representation of the properties file's data.  This nested class exists so that {@link PropFile}
     * can be marked final but yet the root, empty, default delegate can be an extension which always returns
     * the {@link Prop#Empty} value.  This eliminates the need for null checking.
     */
    private static class Impl {
        
        private static final Impl Empty = new Impl(Context.named(""), Scope.Default, Loc.System,
                                                   EmptyConcurrentMap.<String, Prop>get(), Collections.<Prop>emptyList()) {
            
            @Override protected boolean contains(String name) {
                return false;
            }
            @Override protected Prop get(String name) {
                return Prop.Empty;
            }
            @Override protected int size() {
                return 0;
            }
            @Override protected boolean isEmpty() {
                return true;
            }
            @Override protected Iterator<Prop> iterator() {
                return EmptyIterator;
            }
            @Override public boolean equals(Object o) {
                return (this == o);
            }
            @Override public int hashCode() {
                return System.identityHashCode(this);
            }
        };

        private final Context context;
        
        private final Scope scope;

        private final Loc loc;

        private final ConcurrentMap<String, Prop> props;

        private final List<Prop> order;

        private Impl(Context context, Scope scope, Loc loc) {
            this(context, scope, loc, new ConcurrentHashMap<String, Prop>(), new ArrayList<Prop>());
        }
        
        private Impl(Context context, Scope scope, Loc loc, ConcurrentMap<String, Prop> props, List<Prop> order) {
            this.context = context;
            this.scope = scope;
            this.loc = loc;
            this.props = props;
            this.order = order;
        }

        private Prop add(String name, String value, String comments) {
            name = name.trim();
            Prop prop = new Prop(this, name, value, comments);
            Prop existing;
            if ((existing = this.props.putIfAbsent(name, prop)) == null) {
                this.order.add(prop);
            } else {
                prop = existing;
            }
            return prop;
        }
        
        protected boolean contains(String name) {
            return props.containsKey(name);
        }

        protected Prop get(String name) {
            return (props.containsKey(name) ? props.get(name) : Prop.Empty);
        }
        
        protected int size() {
            return order.size();
        }
        
        protected boolean isEmpty() {
            return order.isEmpty();
        }
        
        private Prop remove(String name) {
            Prop removed = props.remove(name);
            if (removed != null) {
                order.remove(removed);       
            }
            return removed;
        }

        protected Iterator<Prop> iterator() {
            return order.iterator();
        }

        @Override public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if ((o == null) || (getClass() != o.getClass())) {
                return false;
            }
            if (o == Empty) {
                return false; // (this == Empty); but we know from above that's false as (this != o)
            }
            Impl impl = (Impl) o;
            return (context.equals(impl.context) && scope.equals(impl.scope) && (loc == impl.loc));
        }

        @Override public int hashCode() {
            int result = context.hashCode();
            result = 31 * result + scope.hashCode();
            result = 31 * result + loc.hashCode();
            return result;
        }
    }

    /**
     * An empty, immutable, properties file.
     */
    static final PropFile Empty = new PropFile(Context.named(""), Scope.Default, Loc.System, Impl.Empty);

    /**
     * An empty {@link Iterator} for {@link Prop} objects.
     */
    static final Iterator<Prop> EmptyIterator = new Iterator<Prop>() {
        @Override public boolean hasNext() {
            return false;
        }
        @Override public Prop next() {
            return Prop.Empty;
        }
        @Override public void remove() {
            throw new UnsupportedOperationException();
        }
    };
    
    private final Impl delegate;
    
    private final Iterable<Prop> props;
    
    /**
     * Creates a {@link PropFile} for the given {@code context} and {@code loc} with the {@link Scope#Default} scope.
     * @param context of the properties file.
     * @param loc of the properties file.
     */
    public PropFile(Context context, Loc loc) {
        this(context, Scope.Default, loc);
    }

    /**
     * Creates a {@link PropFile} for the given {@code context}, {@code loc} and {@code scope}.
     * @param context of the properties file.
     * @param scope of the properties file.
     * @param loc of the properties file.
     */
    public PropFile(Context context, Scope scope, Loc loc) {
        this(context, scope, loc, null);
    }
    
    private PropFile(Context context, Scope scope, Loc loc, Impl delegate) {
        if ((context == null) || (scope == null) || (loc == null)) {
            throw new NullPointerException("Context|Scope|Loc cannot be null.");
        }
        this.delegate = (delegate == null ? new Impl(context, scope, loc) : delegate);
        this.props = new Iterable<Prop>() {
            @Override public Iterator<Prop> iterator() {
                return PropFile.this.delegate.iterator();
            }
        };
    }

    /**
     * Adds a property named {@code name} with value {@code value} to this properties file.
     * <p/>
     * Note, the insertion order of properties dictates their position within the saved file.  This addition will
     * be the last property in the file until another property is added, if ever.
     * <p/>
     * Note, if this properties file already contains a property named {@code name} this method has no effect.  This
     * implies that this method should not be used to set the filtered value of an existing property, that operation
     * should be done with {@link Prop#withComments}.
     * @param name of the property
     * @param value of the property
     * @return the created {@link Prop} object or the existing object if this properties file itself already contained a
     *         property named {@code name} (i.e., if {@link #contains(String)} returns true for {@code name}).
     */
    public final Prop add(String name, String value) {
        return delegate.add(name, value, "");
    }

    /**
     * Adds a property named {@code name} with value {@code value} to this properties file.
     * <p/>
     * Note, the insertion order of properties dictates their position within the saved file.  This addition will
     * be the last property in the file until another property is added, if ever.
     * <p/>
     * Note, if this properties file already contains a property named {@code name} this method has no effect.  This
     * implies that this method should not be used to set the comments of an existing property, that operation should
     * be done with {@link Prop#withComments}.
     * @param name of the property
     * @param value of the property
     * @param comments to be saved within the properties file above the entry for {@code name}.  This value does not
     *                 need to be prefixed with the hash character, '#', as this class will automatically add the character
     *                 on save.
     * @return the created {@link Prop} object or the existing object if this properties file itself already contained a
     *         property named {@code name} (i.e., if {@link #contains(String)} returns true for {@code name}).
     */
    public final Prop add(String name, String value, String comments) {
        return delegate.add(name, value, comments);
    }

    /**
     * @param name of the property
     * @return true if {@code name} is within this properties file.
     */
    public final boolean contains(String name) {
        return delegate.contains(name);
    }

    /**
     * @param name of the property
     * @return the {@link Prop} named {@code name} if it is present within this properties file or
     *         {@link Prop#Empty} if it cannot be found.
     */
    public final Prop get(String name) {
        return delegate.get(name);
    }

    /**
     * @return the amount of properties within this file.
     */
    public final int size() {
        return delegate.size();
    }

    /**
     * @return true if {@link #size()} == 0.
     */
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    /**
     * Removes the property named {@code name} from this properties file.
     * @param name of the property to remove from this properties file.
     * @return the existing {@link Prop} or null if there was no property named {@code name} in this properties file
     */
    public final Prop remove(String name) {
        return delegate.remove(name);
    }

    /**
     * @return an {@link Iterable} of type {@link Prop} over the properties file's properties.
     */
    public final Iterable<Prop> props() {
        return props;
    }

    @Override public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        return delegate.equals(((PropFile) o).delegate);
    }

    @Override public final int hashCode() {
        return delegate.hashCode();
    }
}
