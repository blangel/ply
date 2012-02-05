package net.ocheyedan.ply.cmd;

import net.ocheyedan.ply.*;
import net.ocheyedan.ply.cmd.config.Get;
import net.ocheyedan.ply.dep.DependencyAtom;
import net.ocheyedan.ply.dep.RepositoryAtom;
import net.ocheyedan.ply.mvn.MavenPom;
import net.ocheyedan.ply.mvn.MavenPomParser;
import net.ocheyedan.ply.props.*;

import java.io.*;
import java.nio.CharBuffer;
import java.util.*;

/**
 * User: blangel
 * Date: 12/30/11
 * Time: 2:51 PM
 *
 * A {@link Command} to initialize a directory as a {@literal ply} project.
 */
public final class Init extends Command {

    private static final Set<File> CLEANUP_FILES = new HashSet<File>();

    public Init(Args args) {
        super(args);
    }

    public void run() {
        try {
            OutputExt.init("true", "true", "warn,info"); // dis-regard ad-hoc props and defined properties, simply init
            init(new File("."), args);
        } catch (PomNotFound pnf) {
            Output.print("^ply^ ^error^ Specified maven pom file [ ^b^%s^r^ ] does not exist.", pnf.pom);
            cleanupAfterFailure();
            throw new SystemExit(1);
        } catch (NoRepositories nr) {
            Output.print("^ply^ ^error^ No global repositories.  Reinstall ply or add a repository to the ^b^$PLY_HOME/config/repositories.properties^r^ file.");
            cleanupAfterFailure();
            throw new SystemExit(1);
        } catch (PomParseException ppe) {
            Output.print("^ply^ ^error^ Could not parse pom [ %s ].", ppe.getMessage());
            cleanupAfterFailure();
            throw new SystemExit(1);
        } catch (InitException ie) {
            Output.print("^ply^ ^error^ Could not initialize project [ %s ].", (ie.getCause() != null ? ie.getCause() : ""));
            cleanupAfterFailure();
            throw new SystemExit(1);
        } catch (AlreadyInitialized ai) {
            Output.print("^ply^ Current directory is already initialized.");
        } catch (Throwable t) {
            t.printStackTrace(); // exceptional case - print to std-err
            cleanupAfterFailure();
            throw new SystemExit(1);
        }
    }

    private static void init(File from, Args args) throws AlreadyInitialized, PomNotFound {
        // check for existing init.
        File ply = FileUtil.fromParts(from.getPath(), ".ply");
        if (ply.exists()) {
            throw new AlreadyInitialized();
        }
        CLEANUP_FILES.add(ply);
        // now create the .ply/config directories
        File configDir = FileUtil.fromParts(from.getPath(), ".ply", "config");
        configDir.mkdirs();
        // check for an existing maven project
        File mavenPom;
        if ((mavenPom = getMavenPom(from, args)) != null) {
            if (!mavenPom.exists()) {
                throw new PomNotFound(mavenPom.getPath());
            }
            List<RepositoryAtom> repositoryAtoms = getRepositories();
            MavenPomParser parser = new MavenPomParser();
            PrintStream old = setupTabOutput();
            MavenPom pom = null;
            for (RepositoryAtom repositoryAtom : repositoryAtoms) {
                try {
                    pom = parser.parsePom(mavenPom.getPath(), repositoryAtom);
                    if (pom != null) {
                        break;
                    }
                } catch (Exception e) {
                    // try next...
                }
            }
            revertTabOutput(old);
            if ((pom == null) || !createProperties(from, pom)) {
                throw new PomParseException(mavenPom.getPath());
            }
            if ((pom.modules != null) && !pom.modules.isEmpty()) {
                for (String submodule : pom.modules.stringPropertyNames()) {
                    File pomFile = FileUtil.fromParts(from.getPath(), submodule);
                    try {
                        if (pomFile.exists()) {
                            List<String> rawArgs = new ArrayList<String>(2);
                            rawArgs.add("init");
                            rawArgs.add("--from-pom=pom.xml");
                            Args pomArgs = new Args(rawArgs, args.adHocProps);
                            init(pomFile, pomArgs);
                        } else {
                            Output.print("^warn^ Module [ ^b^%s^r^ ] specified in %s but directory not found.", submodule, mavenPom.getPath());
                        }
                    } catch (AlreadyInitialized ai) {
                        // ignore, this is fine
                    } catch (PomNotFound pnf) {
                        Output.print("^warn^ Could not find ^b^%s^r^'s pom file. For init of sub-modules the pom must be named ^b^pom.xml^r^", pomFile.getPath());
                    }
                }
            }
        } else {
            if (!createDefaultProperties(from)) {
                throw new InitException(null);
            }
        }
        // print out the local properties
        Output.print("^ply^ Created the following project properties:");
        Output.print("^ply^");
        PrintStream old = setupTabOutput();
        Map<Context, Collection<Prop>> props = Props.getLocal(configDir, Scope.Default);
        Get get = new Get(null);
        get.print(props, Scope.Default, false);
        revertTabOutput(old);
        Output.print("^ply^");
        Output.print("^ply^ Project ^b^%s^r^ initialized successfully.", getValue(props, Context.named("project"), "name"));
    }

    private static String getValue(Map<Context, Collection<Prop>> props, Context context, String propertyName) {
        Collection<Prop> contextProps = props.get(context);
        if (contextProps == null) {
            return "";
        }
        for (Prop prop : contextProps) {
            if (prop.name.equals(propertyName)) {
                return prop.value;
            }
        }
        return "";
    }

    private static File getMavenPom(File from, Args args) {
        if ((args.args.size() > 1) && args.args.get(1).startsWith("--from-pom=")) {
            return FileUtil.fromParts(from.getPath(), args.args.get(1).substring("--from-pom=".length()));
        } else if (isHeadless()) {
            return null;
        }
        File[] poms = findPomFiles(from);
        if ((poms != null) && poms.length > 0) {
            String options;
            if (poms.length == 1) {
                options = "[Y/n]";
                Output.printNoLine("^ply^ Found a pom file [ ^b^%s^r^ ], parse configuration from it %s ", poms[0].getPath(), options);
            } else {
                options = "[num/n]";
                Output.print("^ply^ Found pom files:");
                int choice = 1;
                for (File pom : poms) {
                    Output.print("^ply^ [^b^%d^r^] %s", choice++, pom.getPath());
                }
                Output.printNoLine("^ply^ parse configuration from %s? ", options);
            }
            while (true) {
                try {
                    CharBuffer buffer = CharBuffer.allocate(Integer.valueOf(poms.length).toString().length() + 1);
                    new InputStreamReader(System.in).read(buffer);
                    buffer.rewind();
                    String answer = buffer.toString().trim();
                    Integer answerAsNumber = null;
                    try {
                        answerAsNumber = Integer.parseInt(answer);
                    } catch (NumberFormatException nfe) {
                        answerAsNumber = null;
                    }
                    if ((poms.length == 1) && answer.equalsIgnoreCase("y")) {
                        return poms[0];
                    } else if ((poms.length > 1) && (answerAsNumber != null)) {
                        int index = answerAsNumber - 1;
                        if ((index >= 0) && (index < poms.length)) {
                            return poms[index];
                        }
                        Output.printNoLine("^ply^ ^red^invalid number^r^; must be between %d and %d, parse configuration %s ", 1, poms.length, options);
                    } else if (!answer.equalsIgnoreCase("n")) {
                        Output.printNoLine("^ply^ ^red^invalid option^r^, parse configuration %s ", options);
                    } else {
                        break;
                    }
                } catch (IOException ioe) {
                    throw new InitException(ioe);
                }
            }
        }
        return null;
    }

    private static void cleanupAfterFailure() {
        for (File file : CLEANUP_FILES) {
            FileUtil.delete(file);
        }
    }

    /**
     * Exists as an alternative to {@link net.ocheyedan.ply.PlyUtil#isHeadless()} as during init the process
     * cannot load properties until the local project properties have been initialized; otherwise, the resolved
     * properties (which will not yet contain the, just created, local properties) will not be within the cache.
     * @return true if {@literal ply} is running as headless
     * @see net.ocheyedan.ply.PlyUtil#isHeadless()
     */
    private static boolean isHeadless() {
        Properties plySystemProps = PropertiesFileUtil.load(FileUtil.pathFromParts(PlyUtil.SYSTEM_CONFIG_DIR.getPath(), "ply.properties"));
        return (plySystemProps != null) && "true".equalsIgnoreCase(plySystemProps.getProperty("headless"));
    }

    /**
     * Initializes the {@literal project.properties} file with the following values:
     * namespace = {@link MavenPom#groupId}
     * name = {@link MavenPom#artifactId}
     * version = {@link MavenPom#version}
     * packaging = {@link MavenPom#packaging}
     * Initializes the {@literal dependencies.properties} file with {@link MavenPom#dependencies}
     * and {@literal repositories.properties} file with {@link MavenPom#repositories}.
     * @param from directory from which to save property files
     * @param pom which to extract configuration values.
     * @return true is success; false, otherwise
     */
    private static boolean createProperties(File from, MavenPom pom) {
        Map<String, Properties> fileToProps = new HashMap<String, Properties>(3);

        Properties projectProps = new OrderedProperties();
        projectProps.put("namespace", pom.groupId);
        projectProps.put("name", pom.artifactId);
        projectProps.put("version", pom.version);
        if ((pom.packaging != null) && !DependencyAtom.DEFAULT_PACKAGING.equals(pom.packaging)
                && !"pom".equals(pom.packaging)) { // maven's pom packaging will be considered default packaging in ply
            projectProps.put("packaging", pom.packaging);
        }
        if (pom.buildDirectory != null) {
            projectProps.put("build.dir", pom.buildDirectory);
        }
        if (pom.buildSourceDirectory != null) {
            projectProps.put("src.dir", pom.buildSourceDirectory);
        }
        if (pom.buildFinalName != null) {
            projectProps.put("artifact.name", pom.buildFinalName);
        }
        fileToProps.put(FileUtil.pathFromParts(from.getPath(), ".ply", "config", "project.properties"), projectProps);

        if ((pom.dependencies != null) && !pom.dependencies.isEmpty()) {
            fileToProps.put(FileUtil.pathFromParts(from.getPath(), ".ply", "config", "dependencies.properties"), pom.dependencies);
        }

        if ((pom.testDependencies != null) && !pom.testDependencies.isEmpty()) {
            fileToProps.put(FileUtil.pathFromParts(from.getPath(), ".ply", "config", "dependencies.test.properties"), pom.testDependencies);
        }

        if ((pom.repositories != null) && !pom.repositories.isEmpty()) {
            fileToProps.put(FileUtil.pathFromParts(from.getPath(), ".ply", "config", "repositories.properties"), pom.repositories);
        }

        if (pom.buildOutputDirectory != null) {
            Properties compilerProps = new OrderedProperties();
            compilerProps.put("build.path", pom.buildOutputDirectory);
            fileToProps.put(FileUtil.pathFromParts(from.getPath(), ".ply", "config", "compiler.properties"), compilerProps);
        }

        if (pom.buildTestOutputDirectory != null) {
            Properties compilerTestProps = new OrderedProperties();
            compilerTestProps.put("build.path", pom.buildTestOutputDirectory);
            fileToProps.put(FileUtil.pathFromParts(from.getPath(), ".ply", "config", "compiler.test.properties"), compilerTestProps);
        }

        if (pom.buildTestSourceDirectory != null) {
            Properties projectTestProps = new OrderedProperties();
            projectTestProps.put("src.dir", pom.buildTestSourceDirectory);
            fileToProps.put(FileUtil.pathFromParts(from.getPath(), ".ply", "config", "project.test.properties"), projectTestProps);
        }

        if ((pom.modules != null) && !pom.modules.isEmpty()) {
            fileToProps.put(FileUtil.pathFromParts(from.getPath(), ".ply", "config", "submodules.properties"), pom.modules);
        }

        return createProperties(fileToProps);
    }

    /**
     * Initializing the {@literal project.properties} file with the following values:
     * namespace = current working directory
     * name = current working directory
     * version = 1.0
     * @param from directory from which to save property files
     * @return true on success
     */
    private static boolean createDefaultProperties(File from) {
        Map<String, Properties> projectMap = new HashMap<String, Properties>(1);
        Properties projectProps = new OrderedProperties();
        projectMap.put(FileUtil.pathFromParts(from.getPath(), ".ply", "config", "project.properties"), projectProps);
        try {
            File projectDirectory = new File(".");
            String path = projectDirectory.getCanonicalPath();
            if (path.endsWith(File.separator)) {
                path = path.substring(0, path.length() - 1);
            }
            int lastPathIndex = path.lastIndexOf(File.separator);
            if (lastPathIndex != -1) {
                path = path.substring(lastPathIndex + 1);
            }
            projectProps.put("namespace", path);
            projectProps.put("name", path);
            projectProps.put("version", "1.0");
            return createProperties(projectMap);
        } catch (IOException ioe) {
            Output.print("^error^ could not create the local project.properties file.");
            Output.print(ioe);
            return false;
        }
    }

    /**
     * Saves each {@code fileToProps}
     * @param fileToProps mapping from file name to {@link Properties}
     * @return true if all saves succeeded; false otherwise
     */
    private static boolean createProperties(Map<String, Properties> fileToProps) {
        for (String filePath : fileToProps.keySet()) {
            Properties localProperties = fileToProps.get(filePath);
            if (!PropertiesFileUtil.store(localProperties, filePath, true)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Causes all output to be indented four spaces.
     * @return the current {@link System#out} at time of this call.
     */
    private static PrintStream setupTabOutput() {
        final PrintStream old = System.out;
        PrintStream tabbed = new PrintStream(new ByteArrayOutputStream() /* spurious as calls are delegated to 'old' */) {
            final Object[] nil = new Object[0];
            @Override public void print(String out) {
                old.print(String.format("%s %s", OutputExt.resolve("^ply^", nil), out));
            }
            @Override public void println(String out) {
                old.println(String.format("%s %s", OutputExt.resolve("^ply^", nil), out));
            }
        };
        System.setOut(tabbed);
        return old;
    }

    /**
     * Sets the {@link System#out} to {@code old}.
     * @param old the existing {@link PrintStream} before any call to {@link #setupTabOutput()}
     */
    private static void revertTabOutput(PrintStream old) {
        System.setOut(old);
    }

    /**
     * @param from the base directory in which to look for pom files.
     * @return all files within the {@code from} directory ending with "pom.xml"
     */
    private static File[] findPomFiles(File from) {
        return from.listFiles(new FilenameFilter() {
            @Override public boolean accept(File dir, String name) {
                return name.endsWith("pom.xml");
            }
        });
    }

    /**
     * @return all repositories available
     * @throws NoRepositories if there are no repositories
     */
    private static List<RepositoryAtom> getRepositories() throws NoRepositories {
        List<RepositoryAtom> repositories = new ArrayList<RepositoryAtom>();
        String localRepoPath = getSystemLocalRepo();
        RepositoryAtom local = RepositoryAtom.parse(localRepoPath);
        if (local != null) {
            repositories.add(local);
        }
        Collection<Prop> repositoryProperties = getSystemRepositories();
        RepositoryAtom repo;
        for (Prop prop : repositoryProperties) {
            repo = RepositoryAtom.parse(RepositoryAtom.atomFromProp(prop));
            if (repo != null) {
                repositories.add(repo);
            }
        }
        if (repositories.isEmpty()) {
            throw new NoRepositories();
        }
        Collections.sort(repositories, RepositoryAtom.LOCAL_COMPARATOR);
        return repositories;
    }

    /**
     * @return the system value for the {@literal depmngr.localRepo} property for the default scope
     */
    private static String getSystemLocalRepo() {
        Properties systemDepmngrProps = PropertiesFileUtil.load(FileUtil.pathFromParts(PlyUtil.SYSTEM_CONFIG_DIR.getPath(), "depmngr.properties"));
        return (systemDepmngrProps == null ? null : systemDepmngrProps.getProperty("localRepo"));
    }

    /**
     * @return the system defined repositories for the default scope
     */
    private static Collection<Prop> getSystemRepositories() {
        Properties systemRepositoriesProps = PropertiesFileUtil.load(FileUtil.pathFromParts(PlyUtil.SYSTEM_CONFIG_DIR.getPath(), "repositories.properties"));
        if (systemRepositoriesProps == null) {
            return Collections.emptyList();
        }
        Context repositoriesContext = Context.named("repositories");
        List<Prop> props = new ArrayList<Prop>(systemRepositoriesProps.size());
        for (String propName : systemRepositoriesProps.stringPropertyNames()) {
            String propValue = systemRepositoriesProps.getProperty(propName);
            props.add(new Prop(repositoriesContext, propName, propValue, propValue, Prop.Loc.System));
        }
        return props;
    }

    /**
     * Thrown to indicate the directory has already been initialized.
     */
    @SuppressWarnings("serial")
    private static class AlreadyInitialized extends RuntimeException { }

    /**
     * Thrown to indicate that a specified pom file could not be found.
     */
    @SuppressWarnings("serial")
    private static class PomNotFound extends RuntimeException {
        final String pom;
        private PomNotFound(String pom) {
            super();
            this.pom = pom;
        }
    }

    /**
     * Thrown to indicate that a pom could not be parsed
     */
    @SuppressWarnings("serial")
    private static class PomParseException extends RuntimeException {
        private PomParseException(String message) {
            super(message);
        }
    }

    @SuppressWarnings("serial")
    private static class InitException extends RuntimeException {
        private InitException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * Thrown to indicate that no repositories could be found while looking up the pom.
     */
    @SuppressWarnings("serial")
    private static class NoRepositories extends RuntimeException { }

}
