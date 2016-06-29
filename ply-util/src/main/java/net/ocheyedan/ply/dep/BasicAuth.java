package net.ocheyedan.ply.dep;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.PwdUtil;
import sun.misc.BASE64Encoder;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

/**
 * User: blangel
 * Date: 8/1/14
 * Time: 2:10 PM
 */
final class BasicAuth implements Auth {

    private final String username;

    private final String encryptedPwd;

    private final RepositoryAtom repositoryAtom;

    private final Map<String, String> headers;

    BasicAuth(String username, String encryptedPwd, RepositoryAtom repositoryAtom) {
        this.username = username;
        this.encryptedPwd = encryptedPwd;
        this.repositoryAtom = repositoryAtom;
        this.headers = new HashMap<String, String>(2, 1.0f);
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
        return String.format("basic:%s:%s", username, encryptedPwd);
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
            String basicAuth = getBasicAuthenticationProperty(username, PwdUtil.decrypt(encryptedPwd));
            headers.put("Authorization", basicAuth);
        }
        return headers;
    }

    @Override public void acquireAccess(Auth.Acquisition acquisition) {
        String password = acquisition.getAccess(username, encryptedPwd);
        if (!head(repositoryAtom.getPreResolvedUri(), username, password)) {
            Output.print("^error^ Could not create auth token for ^b^%s^r^ in repo ^b^%s^r^", username,
                    repositoryAtom.getPreResolvedUri());
            Output.print("^error^ Perhaps you entered the wrong password?");
            System.exit(1);
        }
    }

    private String getBasicAuthenticationProperty(String username, String pwd) {
        try {
            String urlEncodedUsername = URLEncoder.encode(username, "UTF-8");
            String urlEncodedPwd = URLEncoder.encode(pwd, "UTF-8");
            String userpass = String.format("%s:%s", urlEncodedUsername, urlEncodedPwd);
            BASE64Encoder encoder = new sun.misc.BASE64Encoder();
            String encodedUserpass = encoder.encode(userpass.getBytes());
            return "Basic " + encodedUserpass;
        } catch (UnsupportedEncodingException uee) {
            Output.print(uee);
            System.exit(1);
            return null;
        }
    }

    private boolean head(String urlPath, String username, String pwd) {
        URL url;
        URLConnection urlConnection = null;
        try {
            url = URI.create(urlPath).toURL();
            urlConnection = url.openConnection();
            if (urlConnection instanceof HttpURLConnection) {
                ((HttpURLConnection) urlConnection).setRequestMethod("HEAD");
            }
            String basicAuth = getBasicAuthenticationProperty(username, pwd);
            urlConnection.setRequestProperty("Authorization", basicAuth);
            urlConnection.setDoOutput(false);
            urlConnection.connect();
            return true;
        } catch (IOException ioe) {
            Output.print(ioe);
            return false;
        }
    }
}
