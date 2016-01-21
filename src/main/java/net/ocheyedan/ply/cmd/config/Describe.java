package net.ocheyedan.ply.cmd.config;

import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.PlyUtil;
import net.ocheyedan.ply.cmd.Args;
import net.ocheyedan.ply.props.*;
import net.ocheyedan.ply.props.Scope;

import java.io.File;
import java.util.*;

import static net.ocheyedan.ply.props.PropFile.Prop;

/**
 * User: blangel
 * Date: 1/21/16
 * Time: 12:38 PM
 *
 * A {@link net.ocheyedan.ply.cmd.Command} to print all contexts and their scopes accessible
 */
public class Describe extends Config {

    private static class Info implements Comparable<Info> {

        private final String contextName;

        private final Map<String, Integer> scopeToPropertyCount;

        public Info(String contextName, Map<String, Integer> scopeToPropertyCount) {
            this.contextName = contextName;
            this.scopeToPropertyCount = scopeToPropertyCount;
        }

        @Override public int compareTo(Info other) {
            return contextName.compareTo(other.contextName);
        }

        @Override public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Info info = (Info) o;
            return (contextName == null ? info.contextName == null : contextName.equals(info.contextName));
        }

        @Override
        public int hashCode() {
            return contextName != null ? contextName.hashCode() : 0;
        }
    }

    public Describe(Args args) {
        super(args);
    }

    @Override protected void runAfterAssumptionsCheck() {
        print(PlyUtil.LOCAL_CONFIG_DIR);
    }

    /**
     * Prints information about the contexts and scopes (for those having at least one property which passes {@link #accept(Prop)})
     * from directory {@code configurationDirectory}.
     *
     * @param configurationDirectory from which to get properties to print
     */
    public void print(File configurationDirectory) {
        List<String> printStatements = new ArrayList<String>();
        List<Info> infos = extractInfo(Props.get(configurationDirectory));
        int count = 0;
        for (Info info : infos) {
            if (printContext(info, printStatements)) {
                count++;
            }
        }
        Output.print("There are ^b^%d^r^ local contexts", count);
        for (String printStatement : printStatements) {
            Output.print(printStatement);
        }
        Output.print("Property counts are for the default scope");
    }

    protected List<Info> extractInfo(Map<Scope, Map<Context, PropFileChain>> data) {
        List<Info> infos = new ArrayList<Info>();
        Map<String, Info> mapping = new HashMap<String, Info>();
        for (Map.Entry<Scope, Map<Context, PropFileChain>> entry : data.entrySet()) {
            Scope scope = entry.getKey();
            for (Map.Entry<Context, PropFileChain> contextEntry : entry.getValue().entrySet()) {
                Info info = mapping.get(contextEntry.getKey().name);
                if (info == null) {
                    info = new Info(contextEntry.getKey().name, new HashMap<String, Integer>());
                    mapping.put(contextEntry.getKey().name, info);
                    infos.add(info);
                }
                Integer count = info.scopeToPropertyCount.get(scope.name);
                if (count == null) {
                    count = 0;
                }
                List<Prop> props = collect(contextEntry.getValue().props(), scope);
                if (!props.isEmpty()) {
                    count += props.size();
                    info.scopeToPropertyCount.put(scope.name, count);
                }
            }
        }
        Collections.sort(infos);
        return infos;
    }

    protected List<Prop> collect(Iterable<Prop> props, Scope scope) {
        List<Prop> properties = new ArrayList<Prop>();
        for (Prop prop : props) {
            if (accept(prop) && (prop.scope().equals(scope))) {
                properties.add(prop);
            }
        }
        return properties;
    }

    protected boolean accept(Prop prop) {
        return (prop.loc() != PropFile.Loc.System);
    }

    protected boolean printContext(Info info, List<String> printStatements) {
        if ((info == null) || (info.scopeToPropertyCount == null) || info.scopeToPropertyCount.isEmpty()) {
            return false;
        }
        // process default scope
        Integer count = info.scopeToPropertyCount.get(Scope.Default.name);
        List<String> scopeNames = new ArrayList<String>(info.scopeToPropertyCount.keySet());
        Collections.sort(scopeNames);
        StringBuilder scopes = new StringBuilder();
        int scopeCount = 0;
        for (String scopeName : scopeNames) {
            if (scopeName.isEmpty()
                    || (info.scopeToPropertyCount.get(scopeName) == null)
                    || (info.scopeToPropertyCount.get(scopeName) < 1)) {
                continue;
            }
            if (scopes.length() > 0) {
                scopes.append(", ");
            }
            scopeCount++;
            scopes.append("^magenta^");
            scopes.append(scopeName);
            scopes.append("^r^");
        }
        String appendage = "";
        if (scopes.length() > 0) {
            appendage = String.format(" (and non-default scope%s %s)", (scopeCount > 1 ? "s" : ""), scopes.toString());
        }
        printStatements.add(String.format("    ^b^%s^r^ contains ^b^%d^r^ properties%s", info.contextName, (count == null ? 0 : count), appendage));
        return true;
    }
}
