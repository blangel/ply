package net.ocheyedan.ply.script.github;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * User: blangel
 * Date: 1/10/13
 * Time: 10:01 AM
 *
 * From calling {@literal https://api.github.com/authorizations}
 * {
 *   "created_at": "2012-04-26T16:32:16Z",
 *   "app": {
 *     "url": "http://dillinger.io",
 *     "name": "Dillinger"
 *   },
 *   "note": null,
 *   "token": "xxxxxx",
 *   "token_last_eight": "xxxx",
 *   "hashed_token": "xxxxxxxxxx",
 *   "fingerprint": "xxxxxx",
 *   "url": "https://api.github.com/authorizations/323907",
 *   "updated_at": "2012-04-26T16:32:16Z",
 *   "scopes": [
 *     "repo"
 *   ],
 *   "note_url": null,
 *   "id": 323907
 * }
 */
public class Authorization {

    private final String note;

    private final String token;

    @JsonProperty("token_last_eight")
    private final String tokenLastEight;

    @JsonProperty("hashed_token")
    private final String hashToken;

    @JsonProperty("fingerprint")
    private final String fingerPrint;

    private final App app;

    private final String url;

    private final String[] scopes;

    private final String id;

    private Authorization() {
        this(null, null, null, null, null, null, null, null, null);
    }

    public Authorization(String note, String token, String tokenLastEight, String hashToken, String fingerPrint, App app, String url, String[] scopes, String id) {
        this.note = note;
        this.token = token;
        this.tokenLastEight = tokenLastEight;
        this.hashToken = hashToken;
        this.fingerPrint = fingerPrint;
        this.app = app;
        this.url = url;
        this.scopes = scopes;
        this.id = id;
    }

    public String getNote() {
        return note;
    }

    public String getToken() {
        return token;
    }

    public String getTokenLastEight() {
        return tokenLastEight;
    }

    public String getHashToken() {
        return hashToken;
    }

    public String getFingerPrint() {
        return fingerPrint;
    }

    public App getApp() {
        return app;
    }

    public String getUrl() {
        return url;
    }

    public String[] getScopes() {
        return scopes;
    }

    public String getId() {
        return id;
    }
}
