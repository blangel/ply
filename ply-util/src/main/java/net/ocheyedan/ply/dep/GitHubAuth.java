package net.ocheyedan.ply.dep;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.PwdUtil;
import net.ocheyedan.ply.SystemExit;
import net.ocheyedan.ply.props.*;
import org.jasypt.contrib.org.apache.commons.codec_1_3.binary.Base64;

import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * User: blangel
 * Date: 1/9/13
 * Time: 7:30 PM
 */
final class GitHubAuth implements Auth {

    private static final long ONE_MB = 1024L * 1024L;

    private final String username;

    private final String encryptedPwd;

    private final RepositoryAtom repositoryAtom;

    private final Map<String, String> headers;

    private final File configDir;

    private final Scope scope;

    GitHubAuth(String username, String encryptedPwd, RepositoryAtom repositoryAtom, File configDir, Scope scope) {
        this.username = username;
        this.encryptedPwd = encryptedPwd;
        this.repositoryAtom = repositoryAtom;
        this.headers = new HashMap<String, String>(2, 1.0f);
        this.configDir = configDir;
        this.scope = scope;
    }

    @Override public String getUsername() {
        return username;
    }

    @Override public String getEncryptedPwd() {
        return encryptedPwd;
    }

    @Override public RepositoryAtom getRepositoryAtom() {
        return repositoryAtom;
    }

    @Override public String getPropertyValue() {
        return String.format("git:%s:%s", username, encryptedPwd);
    }

    @Override public String getArtifactPath(String remotePathDir, DependencyAtom dependencyAtom) {
        return FileUtil.pathFromParts(remotePathDir, dependencyAtom.getArtifactName());
    }

    @Override public String getDependenciesPath(String remotePathDir, String name) {
        return getPath(remotePathDir, name);
    }

    @Override public String getPath(String remotePathDir, String fileName) {
        return FileUtil.pathFromParts(remotePathDir, fileName);
    }

    @Override public Map<String, String> getHeaders() {
        if (headers.isEmpty()) {
            String authToken = getAuthToken(configDir, scope);
            if ((authToken == null) || authToken.isEmpty()) {
                Output.print("^error^ Could not get git-hub auth token for ^b^%s^r^ in repo ^b^%s^r^", username, repositoryAtom.getPreResolvedUri());
                Output.print("^error^ Fix by running command: ^b^ply repo auth %s git %s^r^", repositoryAtom.getPreResolvedUri(), username);
                System.exit(1);
            }
            headers.put("Authorization", String.format("token %s", authToken));
            headers.put("Accept", "application/vnd.github.v3.raw");
        }
        return headers;
    }

    @Override public void acquireAccess(Auth.Acquisition acquisition) {
        if (!createAuthToken(acquisition)) {
            Output.print("^error^ Could not create git-hub auth token for ^b^%s^r^ in repo ^b^%s^r^", username, repositoryAtom.getPreResolvedUri());
            Output.print("^error^ Perhaps you entered the wrong password?");
            System.exit(1);
        }
    }

    @Override
    public boolean downloadFile(String remotePathDir, URL remoteUrl, Map<String, String> headers, File into, String name, String intoName, boolean ignoreFNF) {
        InputStream stream;
        try {
            URL remotePathUrl = FileUtil.getUrl(remotePathDir);
            if (remotePathUrl == null) {
                return false;
            }
            stream = FileUtil.downloadToStream(remotePathUrl, headers);
            String contents;
            try {
                contents = readToString(stream);
            } finally {
                stream.close();
            }
            long sizeInBytes = findSizeInBytes(contents, into.getName());
            // finding size implies file is there; if file is less than 1MB use typical download (not via blob)
            if (sizeInBytes < ONE_MB) {
                return FileUtil.download(remoteUrl, headers, into, name, intoName, ignoreFNF);
            }
            String sha = findSha(contents, into.getName());
            String remoteRepoPath = extractGitHubApiBlobPath(remotePathDir, sha);
            URL blobUrl = FileUtil.getUrl(remoteRepoPath);
            if (blobUrl == null) {
                return false;
            }
            return FileUtil.download(blobUrl, headers, into, name, intoName, ignoreFNF);
        } catch (IOException ioe) {
            return false; // directory/file does not exist
        } catch (IllegalArgumentException iae) {
            return false; // likely a classifier does not exist (e.g. dep-sources.jar)
        }
    }

    private String extractGitHubApiBlobPath(String remotePathDir, String sha) {
        int index = remotePathDir.indexOf("/contents"); // using GitHub 'contents' API
        if (index < 0) {
            throw new IllegalArgumentException("Invalid GitHub repo URL");
        }
        return String.format("%s/git/blobs/%s", remotePathDir.substring(0, index), sha);
    }

    private String findSha(String contents, String name) {
        String baselineContents = String.format("\"name\":\"%s\",", name);
        int baselineIndex = contents.indexOf(baselineContents);
        if (baselineIndex < 0) {
            throw new IllegalArgumentException("Not found");
        }
        String shaStartContents = "\"sha\":\"";
        baselineIndex += baselineContents.length();
        int shaStartIndex = contents.indexOf(shaStartContents, baselineIndex);
        if (shaStartIndex < 0) {
            throw new IllegalArgumentException("SHA not found");
        }
        shaStartIndex += shaStartContents.length();
        int shaEndIndex = contents.indexOf("\",", shaStartIndex);
        return contents.substring(shaStartIndex, shaEndIndex);
    }

    private long findSizeInBytes(String contents, String name) {
        String baselineContents = String.format("\"name\":\"%s\",", name);
        int baselineIndex = contents.indexOf(baselineContents);
        if (baselineIndex < 0) {
            throw new IllegalArgumentException("Not found");
        }
        String sizeStartContents = "\"size\":";
        baselineIndex += baselineContents.length();
        int sizeStartIndex = contents.indexOf(sizeStartContents, baselineIndex);
        if (sizeStartIndex < 0) {
            throw new IllegalArgumentException("SHA not found");
        }
        sizeStartIndex += sizeStartContents.length();
        int sizeEndIndex = contents.indexOf(",", sizeStartIndex);
        return Long.parseLong(contents.substring(sizeStartIndex, sizeEndIndex));
    }

    private String readToString(InputStream stream) throws IOException {
        final char[] buffer = new char[1024];
        final StringBuilder out = new StringBuilder();
        Reader reader = new InputStreamReader(stream, "UTF-8");
        int read;
        while ((read = reader.read(buffer, 0, buffer.length)) >= 0) {
            out.append(buffer, 0, read);
        }
        return out.toString();
    }

    private byte[] readToByteBuffer(InputStream stream) throws IOException {
        byte[] buffer = new byte[1024];
        ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        int read;
        while ((read = stream.read(buffer, 0, buffer.length)) >= 0) {
            byteArray.write(buffer, 0, read);
        }
        return byteArray.toByteArray();
    }

    private String getAuthToken(File configDir, Scope scope) {
        PropFile.Prop authToken = Props.get(username, Context.named("repogithub"), scope, configDir);
        if (PropFile.Prop.Empty.equals(authToken)) {
            return null;
        } else {
            return PwdUtil.decrypt(authToken.value());
        }
    }

    private boolean createAuthToken(Auth.Acquisition acquisition) {
        String authToken = acquisition.getAccess(username, encryptedPwd);
        if (authToken == null) {
            return false;
        }
        String encrypted = PwdUtil.encrypt(authToken);
        String propsFilePath = FileUtil.pathFromParts(configDir.getPath(), "repogithub" + scope.getFileSuffix() + ".properties");
        PropFile repogithub = PropFiles.load(propsFilePath, true, false);
        if (repogithub.contains(username)) {
            repogithub.remove(username);
        }
        repogithub.add(username, encrypted);
        return PropFiles.store(repogithub, propsFilePath, true);
    }
}
