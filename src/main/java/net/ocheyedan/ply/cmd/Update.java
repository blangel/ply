package net.ocheyedan.ply.cmd;

import net.ocheyedan.ply.*;
import net.ocheyedan.ply.props.Context;
import net.ocheyedan.ply.props.Filter;
import net.ocheyedan.ply.props.Props;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * User: blangel
 * Date: 2/9/12
 * Time: 3:45 PM
 *
 * Updates ply by downloading the update-instructions from {@literal ply.update.url} and executing them.
 */
public final class Update extends Command.SystemReliant {

    public Update(Args args) {
        super(args);
    }

    @Override protected void runBeforeAssumptionsCheck() {
        OutputExt.init();
    }

    @Override public void runAfterAssumptionsCheck() {
        try {
            final String currentVersion = getCurrentVersion();
            Output.print("Ply is at version ^b^%s^r^, checking for updates.", currentVersion);
            final String updateUrl = Props.getValue(Context.named("ply"), "update.url");
            Map<String, List<String>> updateInstructions;
            updateInstructions = SlowTaskThread.<Map<String, List<String>>>after(2000)
                .warn("It's taking longer than expected to download the update instructions. Still trying...")
                .onlyIfNotLoggingInfo()
                .whenDoing(new Callable<Map<String, List<String>>>() {
                    @Override public Map<String, List<String>> call() throws Exception {
                        return validateUpdateInstructions(downloadUpdateInstr(updateUrl), currentVersion);
                    }
                }).start();
            update(updateInstructions, currentVersion);
        } catch (SystemExit se) {
            System.exit(se.exitCode);
        } catch (Throwable t) {
            Output.print(t);
            System.exit(1);
        }
    }

    private void update(Map<String, List<String>> updateInstructions, String currentVersion) {
        List<String> versions = updateInstructions.get("VERSIONS");
        int currentVersionIndex = versions.indexOf(currentVersion);
        int numberOfUpdates = (versions.size() - 1) - currentVersionIndex;
        if (numberOfUpdates == 0) {
            Output.print("No updates needed, ply is already up to date.");
            return;
        } else {
            Output.print("Found %d updates to ply.", numberOfUpdates);
        }
        String plyHome = PlyUtil.INSTALL_DIRECTORY;
        File plyHomeDir;
        if ((plyHome == null) || !(plyHomeDir = new File(plyHome)).exists()) {
            Output.print("^error^ Could not find the PLY_HOME environment variable. Cannot update ply.");
            throw new SystemExit(1);
        }
        File backupDir = backup(plyHomeDir, currentVersion);
        try {
            int warnings = 0;
            for (int i = currentVersionIndex; i < versions.size(); i++) {
                String version = versions.get(i);
                List<String> instructions = updateInstructions.get(version);
                warnings += update(version, instructions, plyHomeDir);
            }
            String numberOfUpdatesText = (numberOfUpdates > 1 ? String.format(" (^b^%d^r^ updates)", numberOfUpdates) : "");
            String mostUpToDateVersion = versions.get(versions.size() -1);
            if (warnings == 0) {
                Output.print("Successfully updated ply from ^yellow^%s^r^ to ^green^%s^r^%s!", currentVersion,
                        mostUpToDateVersion, numberOfUpdatesText);
            } else {
                Output.print("Updated ply from ^yellow^%s^r^ to ^b^%s^r^%s but need %d manual correction%s.",
                        currentVersion, mostUpToDateVersion, numberOfUpdatesText, warnings, (warnings == 1 ? "" : "s"));
            }
        } catch (Exception e) {
            Output.print(e);
            if (!restore(plyHomeDir, backupDir, currentVersion)) {
                Output.print("^error^ Ah! Failed to restore from backup. So sorry, we suck, you may have to reinstall ply.");
            }
            throw new SystemExit(1, e);
        }
    }

    /**
     * Updates {@code version} by executing each instruction within {@code instructions}
     * @param version to update
     * @param instructions the instructions necessary for the update
     * @param plyHomeDir the {@literal PLY_HOME} directory
     * @return the amount of manual warnings encountered
     */
    private int update(String version, List<String> instructions, File plyHomeDir) {
        int warnings = 0;
        if (!instructions.isEmpty()) {
            Output.print("^info^ Updating from ^b^%s^r^", version);
        }
        for (String instruction : instructions) {
            warnings += update(instruction, plyHomeDir);
        }
        return warnings;
    }

    /**
     * Parses {@code instruction} and performs the appropriate update.
     * @param instruction to parse and execute
     * @param plyHomeDir the ply home directory
     * @return the number of warnings which occurred when executing the parsed {@code instruction}
     */
    private int update(String instruction, final File plyHomeDir) {
        int warnings = 0;
        if (instruction.startsWith("OUTPUT=")) {
            String output = instruction.substring("OUTPUT=".length());
            Output.print("^dbug^ %s", output);
        } else if (instruction.startsWith("METHOD=")) {
            String methodInstruction = instruction.substring("METHOD=".length());
            int methodIndex = methodInstruction.indexOf("=");
            if (methodIndex == -1) {
                Output.print("^error^ Invalid method instruction [ %s ].", methodInstruction);
                throw new SystemExit(1);
            }
            String method = methodInstruction.substring(0, methodIndex);
            instruction = methodInstruction.substring(methodIndex + 1);
            if ("property".equals(method)) {
                warnings += updateProperty(instruction, FileUtil.fromParts(plyHomeDir.getPath(), "config"));
            } else if ("download".equals(method)) {
                final String parsedInstruction = instruction;
                try {
                    warnings += SlowTaskThread.<Integer>after(2000)
                            .warn("It's taking longer than expected to download an update. Still trying...")
                            .onlyIfNotLoggingDebug()
                            .whenDoing(new Callable<Integer>() {
                                @Override public Integer call() throws Exception {
                                    return download(parsedInstruction, plyHomeDir);
                                }
                            }).start();
                } catch (SystemExit se) {
                    throw se;
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            } else {
                Output.print("^error^ Unrecognized method [ %s ].", method);
                throw new SystemExit(1);
            }
        } else {
            throw new AssertionError(String.format("Unknown instruction ^b^%s^r^.", instruction));
        }
        return warnings;
    }

    /**
     * Assumes {@code instruction} is in the format: context.propName=propVal|expectedVal
     * Updates {@literal propName} to {@literal propVal} in {@literal context} provided the existing
     * value is {@literal expectedVal}, unless {@literal expectedVal} is not present then the update happens
     * regardless.
     * @param instruction to parse for property update instructions
     * @param configDirectory the directory in which to look for the {@literal context} parsed from {@code instruction}
     * @return the amount of manual warnings encountered
     * @throws SystemExit if {@code instruction} is not in the expected format: context.propName=propVal|expectedVal
     */
    @SuppressWarnings("fallthrough")
    private int updateProperty(String instruction, File configDirectory) {
        int contextIndex = (instruction == null ? -1 : instruction.indexOf("."));
        if (contextIndex == -1) {
            Output.print("^error^ Invalid property instruction [ %s ].", instruction);
            throw new SystemExit(1);
        }
        String context = instruction.substring(0, contextIndex);
        if (context.contains("#")) {
            context = context.replaceAll("#", ".");
        }
        instruction = instruction.substring(contextIndex + 1);
        int propertyNameIndex = instruction.indexOf("=");
        if (propertyNameIndex == -1) {
            Output.print("^error^ Invalid property instruction [ %s ].", instruction);
            throw new SystemExit(1);
        }
        String propName = instruction.substring(0, propertyNameIndex);
        instruction = instruction.substring(propertyNameIndex + 1);
        // now loop for the next pipe which is not escaped (prop-values may contain pipe characters)
        char[] characters = instruction.toCharArray();
        boolean escaped = false;
        int index = -1;
        String propValue = null, expectedPropValue = null;
        find:for (char character : characters) {
            index++;
            switch (character) {
                case '\\':
                    escaped = true;
                    break;
                case '|':
                    if (!escaped) {
                        propValue = instruction.substring(0, index);
                        expectedPropValue = (index < (characters.length - 1)) ? instruction.substring(index + 1) : null;
                        break find;
                    }
                default:
                    escaped = false;
            }
        }
        if (propValue == null) {
            Output.print("^error^ Invalid property instruction [ %s ].", instruction);
            throw new SystemExit(1);
        }
        String contextFile = FileUtil.pathFromParts(configDirectory.getPath(), context + ".properties");
        Properties properties = PropertiesFileUtil.load(contextFile, false);
        propValue = propValue.replaceAll("\\\\\\|", "|"); // replace escaped pipe characters
        if (expectedPropValue != null) {
            boolean modifiedAndNewTheSame = (propValue == null ? !properties.containsKey(propName) : propValue.equals(properties.getProperty(propName)));
            if (!properties.containsKey(propName) || (!expectedPropValue.equals(properties.getProperty(propName))
                    && !modifiedAndNewTheSame)) {
                Output.print("^warn^ Your ply has set ^b^%s^r^=\"%s\" (in context ^b^%s^r^) but ply wants to set it to ^b^%s^r^=\"%s\". Please manually resolve.",
                        propName, properties.get(propName), context, propName, propValue);
                return 1;
            } else if (modifiedAndNewTheSame) {
                return 0; // no need to resave file
            }
        } else if (properties.containsKey(propName)) {
            Output.print("^warn^ Your ply has set ^b^%s^r^=\"%s\" (in context ^b^%s^r^) but ply wants to set it to ^b^%s^r^=\"%s\". Please manually resolve.",
                    propName, properties.get(propName), context, propName, propValue);
            return 1;
        }
        properties.put(propName, propValue);
        PropertiesFileUtil.store(properties, contextFile, true);
        return 0;
    }

    /**
     * Assumes {@code instruction} is in the format: 'file to location'
     * @param instruction to parse for the file to download and the location in which to save it
     * @param plyHomeDir the home directory of ply
     * @return 0; this method either succeeds or throws an exception
     * @throws SystemExit if {@code instruction} is not in the expected format: 'file to location' or there was an error
     *                    downloading the file
     */
    private int download(String instruction, File plyHomeDir) {
        int locationIndex = (instruction == null ? -1 : instruction.indexOf(" to "));
        if ((locationIndex == -1) || (instruction.length() <= locationIndex + 4)) {
            Output.print("^error^ Invalid download instruction [ %s ].", instruction);
            throw new SystemExit(1);
        }
        String filePath = instruction.substring(0, locationIndex);
        String locationPath = instruction.substring(locationIndex + 4); // " to ".length() == 4
        // ensure file and location are valid
        if (filePath.isEmpty() || locationPath.isEmpty()) {
            Output.print("^error^ Invalid download instruction [ %s ].", instruction);
            throw new SystemExit(1);
        }
        URL file;
        try {
            file = new URL(filePath);
        } catch (MalformedURLException murle) {
            Output.print(murle);
            throw new SystemExit(1, murle);
        }
        File location = FileUtil.fromParts(plyHomeDir.getPath(), locationPath);
        if (!FileUtil.copy(file, location)) {
            Output.print("^error^ Could not download ^b^%s^r^ to ^b^%s^r^.", filePath, locationPath);
            FileNotFoundException fnfe = new FileNotFoundException(String.format("Could not download %s", filePath));
            throw new SystemExit(1, fnfe);
        }
        return 0;
    }
    
    /**
     * Creates a backup of {@code plyHomeDir} placing it into the {@literal java.io.tmpdir} with name
     * "{@literal ply-}" + {@code currentVersion} + ".backup"
     * @param plyHomeDir to backup
     * @param currentVersion of ply
     * @return the location of the backup
     * @throws SystemExit if the directory could not be updated.
     */
    private File backup(File plyHomeDir, String currentVersion) {
        String tmpDir = System.getProperty("java.io.tmpdir");
        File backDir = FileUtil.fromParts(tmpDir, "ply-" + currentVersion + ".backup");
        if (!FileUtil.copyDir(plyHomeDir, backDir)) {
            Output.print("^error^ Could not create a backup of ply at ^b^%s^r^", backDir.getPath());
            throw new SystemExit(1);
        }
        return backDir;
    }

    /**
     * Restores {@code backup} to {@code plyHomeDir} by copying it.
     * @param plyHomeDir the directory in which to restore {@code backup}
     * @param backup the backup directory which to copy to {@code plyHomeDir}
     * @param currentVersion for which to revert
     * @return true if the copy succeeded
     */
    private boolean restore(File plyHomeDir, File backup, String currentVersion) {
        Output.print("Restoring ply to version ^b^%s^r^", currentVersion);
        return FileUtil.copyDir(backup, plyHomeDir);
    }

    /**
     * @return the current version of ply (from system property {@literal ply.version})
     * @throws SystemExit if the {@literal ply.version} system property is not present or the empty string
     */
    private String getCurrentVersion() {
        String currentVersion = System.getProperty("ply.version");
        if ((currentVersion == null) || currentVersion.isEmpty()) {
            Output.print("^error^ Could not determine the current ply version.");
            throw new SystemExit(1);
        }
        return currentVersion;
    }

    /**
     * Downloads the update instructions file from {@literal ply.update.url} and parses it into a mapping
     * from version to update-instructions for the next version.
     * @param updateUrl the url from which to download the update instructions file
     * @return a mapping of version to the update-instructions to move it to the next version
     *         additionally a mapping from {@literal VERSIONS} to the ordered list of versions
     * @throws SystemExit on error
     */
    private Map<String, List<String>> downloadUpdateInstr(String updateUrl) throws SystemExit {
        Map<String, List<String>> updateInstructions = new HashMap<String, List<String>>();
        InputStream stream = null;
        try {
            Output.print("^info^ Downloading update instructions.");
            URL url = new URL(updateUrl);
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(1000);
            stream = connection.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(stream));
            String line;
            List<String> versions = new ArrayList<String>();
            List<String> instructions = null;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() ||   line.startsWith("#")) {
                    continue; // ignore comments and empty lines
                }
                if (line.startsWith("VERSION=")) {
                    String version = line.substring("VERSION=".length());
                    versions.add(version);
                    instructions = new ArrayList<String>();
                    updateInstructions.put(version, instructions);
                } else {
                    if (instructions == null) {
                        Output.print("^error^ The update instructions file is in an invalid format. Cannot update ply.");
                        throw new SystemExit(1);
                    }
                    instructions.add(line);
                }
            }
            updateInstructions.put("VERSIONS", versions);
        } catch (MalformedURLException murle) {
            Output.print("^error^ Invalid url specified in ^b^ply.update.url^r^ property: %s", updateUrl);
            Output.print(murle);
            throw new SystemExit(1, murle);
        } catch (FileNotFoundException fnfe) {
            fnfe = new FileNotFoundException("Unable to download " + fnfe.getMessage());
            Output.print(fnfe);
            throw new SystemExit(1, fnfe);
        } catch (IOException ioe) {
            Output.print(ioe);
            throw new SystemExit(1, ioe);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ioe) {
                    Output.print(ioe);
                    throw new SystemExit(1, ioe);
                }
            }
        }
        return updateInstructions;
    }

    /**
     * Validates that {@code updateInstructions} are not null and have the {@literal VERSIONS} specifier.
     * Also validates that the {@code updateInstructions} contain {@code currentVersion}.  If {@code updateInstructions}
     * are found to be invalid, a {@link SystemExit} exception is thrown
     * @param updateInstructions to validate
     * @param currentVersion of ply to ensure {@code updateInstructions} contain information pertinent to it
     * @return {@code updateInstructions} for method chaining
     * @throws SystemExit if {@code updateInstructions} are found to be invalid.
     */
    private Map<String, List<String>> validateUpdateInstructions(Map<String, List<String>> updateInstructions, String currentVersion) {
        if ((updateInstructions == null) || !updateInstructions.containsKey("VERSIONS")) {
            Output.print("^error^ Could not download the update instructions file. Cannot update ply.");
            throw new SystemExit(1);
        }
        if (!updateInstructions.containsKey(currentVersion)) {
            Output.print("^error^ Current ply version [ ^b^%s^r^ ] not supported for automatic update. Please manually update by reinstalling ply.",
                    currentVersion);
            throw new SystemExit(1);
        }
        return updateInstructions;
    }
}
