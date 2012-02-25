package net.ocheyedan.ply.props;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: blangel
 * Date: 2/21/12
 * Time: 9:57 PM
 *
 * Represents a chain of {@link PropFile} objects which are related in a hierarchical manner.
 * The hierarchy looks like this:
 * <pre>
 *     
 *       AdHoc
 *         |
 *         v
 *     Scope (Local)
 *         |
 *         v
 *     Scope (System)
 *         |
 *         v
 *  Default Scope (Local)
 *         |
 *         v
 *  Default Scope (System)
 *  
 * </pre>
 * When property values are desired, users consult the appropriate {@link PropFileChain} (for the correctly
 * associated {@code Context}) and the chain is consulted top down.  So if a property value for a name is found
 * within the {@literal AdHoc} it is returned, otherwise, the check continues down the chain.
 * <p/>
 * Additionally, since filtering is applied against chains of the same {@link Context}, this class contains a mapping
 * to the filtered value for each given {@link PropFile.Prop} object within the chain.
 */
public final class PropFileChain {

    /**
     * Internal representation of the chain's data.  This nested class exists so that {@link PropFileChain}
     * can be marked final but yet the root, empty, default delegate can be an extension which always returns
     * the {@link PropFile.Prop#Empty} value.  This eliminates the need for null checking.
     */
    private static class Impl {

        /**
         * The empty {@link Impl} implementation.
         */
        private static final Impl Empty = new Impl(null, Collections.<Context, PropFileChain>emptyMap()) {
            @Override protected PropFile.Prop get(String named) {
                return PropFile.Prop.Empty;
            }
            @Override protected PropFile.Prop internalGet(String named) {
                return PropFile.Prop.Empty;
            }
            @Override protected Iterator<PropFile.Prop> iterator() {
                return PropFile.EmptyIterator;
            }
        };
        
        private final List<PropFile> chain;
        
        private final Impl defaultChain;

        private final Map<Context, PropFileChain> filterConsultant;
        
        private final Map<String, PropFile.Prop> filteredCache;
        
        private Impl(Impl defaultChain, Map<Context, PropFileChain> filterConsultant) {
            chain = new ArrayList<PropFile>(3);
            chain.add(PropFile.Empty); // ad-hoc
            chain.add(PropFile.Empty); // local
            chain.add(PropFile.Empty); // system
            this.defaultChain = defaultChain;
            this.filterConsultant = filterConsultant;
            this.filteredCache = new ConcurrentHashMap<String, PropFile.Prop>();
        }
        
        private void set(PropFile propFile, PropFile.Loc at) {
            filteredCache.clear(); // invalidation of filter cache
            switch (at) {
                case AdHoc:
                    chain.set(0, propFile); break;
                case Local:
                    chain.set(1, propFile); break;
                case System:
                    chain.set(2, propFile); break;
                default:
                    throw new AssertionError(String.format("Unsupported PropFile.Loc value %s", at.name()));
            }
        }
        
        protected PropFile.Prop get(String named) {
            if (filteredCache.containsKey(named)) {
                return filteredCache.get(named);
            }
            PropFile.Prop unfiltered = internalGet(named);
            PropFile.Prop filtered = unfiltered;
            if (PropFile.Prop.Empty != unfiltered) {
                filtered = Filter.filter(unfiltered, String.valueOf(System.identityHashCode(this)), filterConsultant);
            }
            filteredCache.put(named, filtered);
            return filtered;
        }
        
        protected PropFile.Prop internalGet(String named) {
            for (PropFile propFile : chain) {
                if (propFile == PropFile.Empty) {
                    continue;
                }
                PropFile.Prop prop = propFile.get(named);
                if (prop != PropFile.Prop.Empty) {
                    return prop;
                }
            }
            return defaultChain.get(named);
        }

        protected Iterator<PropFile.Prop> iterator() {
            return new Iterator<PropFile.Prop>() {
                final Set<PropFile.Prop> encountered = new HashSet<PropFile.Prop>();
                int index = 0;
                Iterator<PropFile.Prop> curIter;
                PropFile.Prop current = PropFile.Prop.Empty;
                boolean incremented = false;
                boolean hasNext = true;
                @Override public boolean hasNext() {
                    if (incremented) {
                        return hasNext;
                    }
                    incremented = true;
                    if ((curIter == null) || !curIter.hasNext()) {
                        if (index < 3) {
                            curIter = chain.get(index++).props().iterator();
                        } else if (index++ == 3) {
                            curIter = defaultChain.iterator();
                        } else {
                            return (hasNext = false);
                        }
                    }
                    while ((hasNext = curIter.hasNext()) && !encountered.add(current = curIter.next())) { }
                    if (!hasNext && (index < 4)) {
                        incremented = false;
                        return hasNext();
                    }
                    return hasNext;
                }
                @Override public PropFile.Prop next() {
                    if (!incremented) {
                        hasNext();
                    }
                    incremented = false;
                    return get(current.name); //ensures filtering happens...
                }
                @Override public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
        
    }
    
    private final Impl delegate;

    private final Iterable<PropFile.Prop> props;

    /**
     * Creates a chain with no default.
     * @param filterConsultant to use when filtering the property values
     */
    PropFileChain(Map<Context, PropFileChain> filterConsultant) {
        this(Impl.Empty, filterConsultant);
    }

    /**
     * Creates a chain which delegates to {@code defaultChain} if a value cannot be found within this chain's
     * associated {@link PropFile} objects.
     * @param defaultChain the delegate to consult if a property value cannot be found within this chain's associated
     *                     {@link PropFile} objects.
     * @param filterConsultant to use when filtering the property values
     */
    PropFileChain(PropFileChain defaultChain, Map<Context, PropFileChain> filterConsultant) {
        this((defaultChain == null ? Impl.Empty : defaultChain.delegate), filterConsultant);
    }
    
    private PropFileChain(Impl defaultChain, Map<Context, PropFileChain> filterConsultant) {
        this.delegate = new Impl(defaultChain, filterConsultant);
        this.props = new Iterable<PropFile.Prop>() {
            @Override public Iterator<PropFile.Prop> iterator() {
                return PropFileChain.this.delegate.iterator();
            }
        };
    }

    /**
     * Sets {@code propFile} at location {@code at} within this chain.
     * @param propFile to set at location {@code at}
     * @param at the location at which to set {@code propFile}
     */
    void set(PropFile propFile, PropFile.Loc at) {
        if ((propFile == null) || (at == null)) {
            throw new NullPointerException("PropFile and PropFile.Loc must not be null.");
        }
        delegate.set(propFile, at);
    }

    /**
     * @param named the name of the property object for which to retrieve.
     * @return the property named {@code named} within this chain (or this chain's delegate) if it exists, otherwise
     *         {@link PropFile.Prop#Empty} is returned.
     */
    public PropFile.Prop get(String named) {
        return delegate.get(named);
    }

    /**
     * @return an {@link Iterable} of type {@link PropFile.Prop} over the chain's properties.
     */
    public final Iterable<PropFile.Prop> props() {
        return props;
    }
    
}
