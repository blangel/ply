package net.ocheyedan.ply.script;

import net.ocheyedan.ply.AntStyleWildcardUtil;
import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.props.Context;
import net.ocheyedan.ply.props.Filter;
import net.ocheyedan.ply.props.PropFileChain;
import net.ocheyedan.ply.props.Props;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Pattern;

import static net.ocheyedan.ply.props.PropFile.Prop;

/**
 * User: blangel
 * Date: 10/15/11
 * Time: 11:13 AM
 *
 * Filters files from {@literal project[.scope].build.dir} in place.  Only those files matching ant-style patterns
 * specified in {@literal filter-files[.scope].properties} (thus the properties have context
 * {@literal [scope.]filter-files}) are filtered; i.e., nothing is filtered by default.
 * The property name is interpreted as the ant-style wildcard pattern to match and the value is either null or 'include', in
 * which case the pattern is considered to represent a file or set of files to be filtered or 'exclude' which
 * means the pattern is considered to represent a file or set of files to explicitly exclude from filtering.  By default
 * everything is excluded so the explicit excludes are to limit the set matched by other inclusions.
 *
 * All files are filtered in place and the ant-style wildcard pattern is always relative to {@literal project[.scope].filter.dir}
 * thus any file reachable from {@literal project[.scope].filter.dir} can be filtered (which includes resources
 * or even source files before compilation if they were first copied into the build directory).
 * TODO - should, by default, ply copy source files to the build dir and then compile, allowing filtering easily?
 *
 * The files included to be filtered are filtered for unix-style property placeholders; i.e., ${xxxx}, where 'xxxx'
 * is the property name for which to filter.  The property names available to be used in filtering are all those
 * available to {@literal ply} which includes all context properties as well as environmental variables.  For instance,
 * if there is a {@literal $\{compiler.buildDir\}} then the property {@literal compiler[.scope].buildDir} is looked
 * up and its value is used in filtering.
 *
 */
public final class FilterScript {

    /**
     * Wraps {@link Pattern} with a boolean expression indicating whether the pattern should include or exclude.
     */
    private final static class FilterPattern implements FileFilter {

        private final Pattern pattern;

        private final boolean include;

        private FilterPattern(Pattern pattern, boolean include) {
            this.pattern = pattern;
            this.include = include;
        }

        @Override public boolean accept(File pathname) {
            return (pattern.matcher(pathname.getPath()).matches() == include);
        }
    }

    private final static class CollectingFileFilter implements FileFilter {

        private final List<FilterPattern> filterPatterns;

        private final Set<File> collecting;

        private CollectingFileFilter(List<FilterPattern> filterPatterns, Set<File> collecting) {
            this.filterPatterns = filterPatterns;
            this.collecting = collecting;
        }

        @Override public boolean accept(File pathname) {
            if (pathname.isDirectory()) {
                FilterScript.getApplicableFiles(filterPatterns, pathname, collecting);
                return false;
            } else {
                for (FilterPattern filterPattern : filterPatterns) {
                    if (!filterPattern.accept(pathname)) {
                        return false;
                    }
                }
                return true;
            }
        }
    }

    public static void main(String[] args) {
        Prop filterDirProp = Props.get("filter.dir", Context.named("project"));
        if (Prop.Empty.equals(filterDirProp)) {
            Output.print("^error^ Could not find project.filter.dir property.");
            System.exit(1);
        }
        PropFileChain filterFiles = Props.get(Context.named("filter-files"));
        if ((filterFiles == null) || !filterFiles.props().iterator().hasNext()) {
            Output.print("^dbug^ No filter sets specified, nothing to filter.");
            return;
        }
        File filterDir = new File(filterDirProp.value());

        List<FilterPattern> filterPatterns = new ArrayList<FilterPattern>();
        for (Prop filterProp : filterFiles.props()) {
            String filterExp = filterProp.name;
            // if the filterExp is not prefixed with a directory, hard-code to be the filter.dir
            if (!filterExp.startsWith("**")) {
                filterExp = FileUtil.pathFromParts(filterDir.getPath(), filterExp);
            }
            Pattern filterPattern = AntStyleWildcardUtil.regex(filterExp);
            boolean include = false;
            if ("include".equalsIgnoreCase(filterProp.value())) {
                include = true;
            }
            filterPatterns.add(new FilterPattern(filterPattern, include));
        }

        Set<File> files = new HashSet<File>();
        getApplicableFiles(filterPatterns, filterDir, files);
        if (files.isEmpty()) {
            Output.print("^dbug^ No files matched the filter set.");
            return;
        }

        for (File file : files) {
            Output.print("^info^ Filtering file %s.", file.getPath());
            filter(file);
        }
    }

    private static void getApplicableFiles(List<FilterPattern> filterPatterns, File dir, Set<File> collecting) {
        if (dir.isDirectory()) {
            File[] subfiles = dir.listFiles(new CollectingFileFilter(filterPatterns, collecting));
            Collections.addAll(collecting, subfiles);
        }
    }

    private static void filter(File file) {
        FileChannel fc = null;
        try {
            RandomAccessFile stream = new RandomAccessFile(file, "rw");
            fc = stream.getChannel();
            MappedByteBuffer mappedByteBuffer = fc.map(FileChannel.MapMode.READ_WRITE, 0, fc.size());
            String text = Charset.defaultCharset().decode(mappedByteBuffer).toString();
            text = Filter.filter(text, Context.named("ply"), String.valueOf(System.identityHashCode(file)), Props.get());
            fc.position(0);
            ByteBuffer filtered = ByteBuffer.wrap(text.getBytes());
            long currentSize = fc.size();
            long newSize = filtered.capacity();
            while (filtered.hasRemaining()) {
                fc.write(filtered);
            }
            if (newSize < currentSize) {
                fc.truncate(newSize);
            }
        } catch (IOException ioe) {
            Output.print(ioe);
            System.exit(1);
        } finally {
            if (fc != null) {
                try {
                    fc.close();
                } catch (IOException ioe) {
                    throw new AssertionError(ioe);
                }
            }
        }
    }

}
