package bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket;

import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import org.apache.commons.codec.binary.Hex;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.type.TypeReference;
import org.scribe.model.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by nishio
 */
public class ApiClient {
    public static final byte MAX_KEY_SIZE_BB_API = 40;
    private static final Logger logger = Logger.getLogger(ApiClient.class.getName());
    private static final String V1_API_BASE_URL = "https://bitbucket.org/api/1.0/repositories/";
    private static final String V2_API_BASE_URL = "https://bitbucket.org/api/2.0/repositories/";
    private static final String COMPUTED_KEY_FORMAT = "%s-%s";
    private static MessageDigest SHA1 = null;
    private final OAuthConfig authConfig;
    private final BitbucketApiService apiService;
    private String owner;
    private String repositoryName;
    private UsernamePasswordCredentials credentials;
    private String key;
    private String name;
    private Token token = null;

    public ApiClient(
            UsernamePasswordCredentials credentials,
            String owner, String repositoryName,
            String key, String name
    ) {
        this.credentials = credentials;
        this.owner = owner;
        this.repositoryName = repositoryName;
        this.key = key;
        this.name = name;

        authConfig = new OAuthConfig(credentials.getUsername(), credentials.getPassword().getPlainText());
        apiService = (BitbucketApiService) new BitbucketApi().createService(authConfig);
    }

    public List<Pullrequest> getPullRequests() {
        return getAllValues(v2("/pullrequests/"), 50, Pullrequest.class);
    }

    public List<Pullrequest.Comment> getPullRequestComments(String commentOwnerName, String commentRepositoryName,
            String pullRequestId) {
        return getAllValues(v2("/pullrequests/" + pullRequestId + "/comments"), 100, Pullrequest.Comment.class);
    }

    public String getName() {
        return this.name;
    }

    /**
     * Retrun
     *
     * @param keyExPart
     * @return key parameter for call BitBucket API
     */
    private String computeAPIKey(String keyExPart) {
        String computedKey = String.format(COMPUTED_KEY_FORMAT, this.key, keyExPart);

        if (computedKey.length() > MAX_KEY_SIZE_BB_API) {
            try {
                if (SHA1 == null) {
                    SHA1 = MessageDigest.getInstance("SHA1");
                }
                return new String(Hex.encodeHex(SHA1.digest(computedKey.getBytes("UTF-8"))));
            } catch (NoSuchAlgorithmException e) {
                logger.log(Level.WARNING, "Failed to create hash provider", e);
            } catch (UnsupportedEncodingException e) {
                logger.log(Level.WARNING, "Failed to create hash provider", e);
            }
        }
        return (computedKey.length() <= MAX_KEY_SIZE_BB_API) ? computedKey :
               computedKey.substring(0, MAX_KEY_SIZE_BB_API);
    }

    public String buildStatusKey(String bsKey) {
        return this.computeAPIKey(bsKey);
    }

    public boolean hasBuildStatus(String owner, String repositoryName, String revision, String keyEx) {
        String url = v2(owner, repositoryName, "/commit/" + revision + "/statuses/build/" + this.computeAPIKey(keyEx));
        String reqBody = get(url);
        return reqBody != null && reqBody.contains("\"state\"");
    }

    public void setBuildStatus(String owner, String repositoryName, String revision, BuildState state, String buildUrl,
            String comment, String keyEx) {
        String url = v2(owner, repositoryName, "/commit/" + revision + "/statuses/build");
        String computedKey = this.computeAPIKey(keyEx);
        ParameterList data = new ParameterList();
        data.add("description", comment);
        data.add("key", computedKey);
        data.add("name", this.name);
        data.add("state", state.toString());
        data.add("url", buildUrl);
        logger.log(Level.FINE, "POST state {0} to {1} with key {2} with response {3}", new Object[]{
                state, url, computedKey, post(url, data)}
        );
    }

    public void deletePullRequestApproval(String pullRequestId) {
        delete(v2("/pullrequests/" + pullRequestId + "/approve"));
    }

    public void deletePullRequestComment(String pullRequestId, String commentId) {
        delete(v1("/pullrequests/" + pullRequestId + "/comments/" + commentId));
    }

    public void updatePullRequestComment(String pullRequestId, String content, String commentId) {
        ParameterList data = new ParameterList();
        data.add("content", content);
        put(v1("/pullrequests/" + pullRequestId + "/comments/" + commentId), data);
    }

    public Pullrequest.Participant postPullRequestApproval(String pullRequestId) {
        try {
            return parse(post(v2("/pullrequests/" + pullRequestId + "/approve"),
                    new ParameterList()), Pullrequest.Participant.class);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Invalid pull request approval response.", e);
        }
        return null;
    }

    public Pullrequest.Comment postPullRequestComment(String pullRequestId, String content) {
        ParameterList data = new ParameterList();
        data.add("content", content);
        try {
            return parse(post(v1("/pullrequests/" + pullRequestId + "/comments"), data),
                    new TypeReference<Pullrequest.Comment>() {
                    });
        } catch (Exception e) {
            logger.log(Level.WARNING, "Invalid pull request comment response.", e);
        }
        return null;
    }

    private <T> List<T> getAllValues(String rootUrl, int pageLen, Class<T> cls) {
        List<T> values = new ArrayList<T>();
        try {
            String url = rootUrl + "?pagelen=" + pageLen;
            do {
                final JavaType type =
                        TypeFactory.defaultInstance().constructParametricType(Pullrequest.Response.class, cls);
                Pullrequest.Response<T> response = parse(get(url), type);
                values.addAll(response.getValues());
                url = response.getNext();
            } while (url != null);
        } catch (Exception e) {
            logger.log(Level.WARNING, "invalid response.", e);
        }
        return values;
    }

    private String v1(String url) {
        return V1_API_BASE_URL + this.owner + "/" + this.repositoryName + url;
    }

    private String v2(String path) {
        return v2(this.owner, this.repositoryName, path);
    }

    private String v2(String owner, String repositoryName, String path) {
        return V2_API_BASE_URL + owner + "/" + repositoryName + path;
    }

    private String get(String path) {
        return send(new OAuthRequest(Verb.GET, path));
    }

    private String post(String path, ParameterList data) {
        OAuthRequest req = new OAuthRequest(Verb.POST, path);
        req.getBodyParams().addAll(data);
        req.setCharset("utf-8");
        return send(req);
    }

    private void delete(String path) {
        send(new OAuthRequest(Verb.DELETE, path));
    }

    private void put(String path, ParameterList data) {
        OAuthRequest req = new OAuthRequest(Verb.PUT, path);
        req.getBodyParams().addAll(data);
        req.setCharset("utf-8");
        send(req);
    }

    private String send(OAuthRequest request) {
        token = apiService.getAccessToken(OAuthConstants.EMPTY_TOKEN, null);
        apiService.signRequest(token, request);

        try {
            Response response = request.send();
            if (!response.isSuccessful()) {
                logger.log(Level.WARNING, "Response status " + response.getCode() + ": " + response.getBody() +
                                          " URI: " + request.getCompleteUrl());
            } else {
                return response.getBody();
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to send request.", e);
        }

        return null;
    }

    private <R> R parse(String response, Class<R> cls) throws IOException {
        return new ObjectMapper().readValue(response, cls);
    }

    private <R> R parse(String response, JavaType type) throws IOException {
        return new ObjectMapper().readValue(response, type);
    }

    private <R> R parse(String response, TypeReference<R> ref) throws IOException {
        return new ObjectMapper().readValue(response, ref);
    }
}
