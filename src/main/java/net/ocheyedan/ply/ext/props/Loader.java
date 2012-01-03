package net.ocheyedan.ply.ext.props;

import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.PlyUtil;
import net.ocheyedan.ply.PropertiesFileUtil;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;

/**
 * User: blangel
 * Date: 12/29/11
 * Time: 5:41 PM
 *
 * Resolves properties from the project's property files or from the environment variables.
 * Environment properties can be leveraged by scripts because the {@literal ply} runtime will have already
 * resolved the appropriate properties (based on the runtime project/scope/etc).
 */
final class Loader {

    /**
     * A {@link java.io.FilenameFilter} for {@link java.util.Properties} files.
     */
    static final FilenameFilter PROPERTIES_FILENAME_FILTER = new FilenameFilter() {
        @Override public boolean accept(File dir, String name) {
            return name.endsWith(".properties");
        }
    };

    /**
     * @param configDirectory from which to extract project properties
     * @return the loaded and filtered properties within {@code configDirectory}
     */
    static Collection<Prop.All> get(File configDirectory) {
        if (configDirectory == null) {
            throw new AssertionError("Argument cannot be null.");
        }
        if (Cache.contains(configDirectory)) {
            return Cache.get(configDirectory);
        } else {
            Collection<Prop.All> props = load(configDirectory);
            Filter.filter(configDirectory, props);
            Cache.put(configDirectory, props);
            return props;
        }
    }

    static List<Prop.All> load(File configDirectory) {
        List<Prop.All> props = new ArrayList<Prop.All>();
        // first add the properties from the system directory (everyone gets these).
        load(PlyUtil.SYSTEM_CONFIG_DIR, props, Prop.Loc.System);
        // TODO - now look for parental properties
        // TODO - how to do this? force parent to be local (then fairly easy) or can it be by dep-atom ref., if so
        // TODO - a bit harder as we'll need to export properties to the jar and pull from there
        // next, override with the project's config directory.
        load(configDirectory, props, Prop.Loc.Local);
        // finally, override all with the ad-hoc properties (from the command-line), if any.
        loadAdHoc(props);
        return props;
    }

    private static void load(File fromDirectory, List<Prop.All> props, Prop.Loc type) {
        File[] subFiles = fromDirectory.listFiles(PROPERTIES_FILENAME_FILTER);
        if (subFiles == null) {
            return;
        }
        for (File subFile : subFiles) {
            if (!subFile.isDirectory()) {
                String fileName = subFile.getName();
                int index = fileName.lastIndexOf(".properties"); // not == -1 because of {@link #PROPERTIES_FILENAME_FILTER}
                fileName = fileName.substring(0, index);
                Context context; Scope scope = Scope.Default;
                if (fileName.lastIndexOf(".") != -1) {
                    index = fileName.lastIndexOf(".");
                    context = new Context(fileName.substring(0, index));
                    scope = new Scope(fileName.substring(index + 1));
                } else {
                    context = new Context(fileName.substring(0, index));
                }
                Properties properties = PropertiesFileUtil.load(subFile.getPath());
                for (String propName : properties.stringPropertyNames()) {
                    String propValue = properties.getProperty(propName);
                    Prop.All prop = new Prop.All(type, scope, context, propName, propValue);
                    if (props.contains(prop)) {
                        Prop.All existing = props.get(props.indexOf(prop));
                        existing.set(scope, type, propValue, propValue);
                    } else {
                        props.add(prop);
                    }
                }
            }
        }
    }

    private static void loadAdHoc(List<Prop.All> props) {
        for (Prop.All prop : AdHoc.get()) {
            if (props.contains(prop)) {
                Prop.All existing = props.get(props.indexOf(prop));
                for (Scope scope : prop.getScopes()) {
                    Prop.Val val = prop.get(scope);
                    String value = (val == null ? null : val.value);
                    String unfiltered = (val == null ? null : val.unfiltered);
                    existing.set(scope, Prop.Loc.AdHoc, value, unfiltered);
                }
            } else {
                props.add(prop);
            }
        }
    }

}
