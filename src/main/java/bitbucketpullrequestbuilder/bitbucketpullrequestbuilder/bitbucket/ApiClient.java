package bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.model.Jenkins;
import hudson.ProxyConfiguration;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.util.EncodingUtil;

/**
 * Created by nishio
 */
public class ApiClient {
    private static final Logger logger = Logger.getLogger(ApiClient.class.getName());
    private static final String V1_API_BASE_URL = "https://bitbucket.org/api/1.0/repositories/";
    private static final String V2_API_BASE_URL = "https://bitbucket.org/api/2.0/repositories/";
    private static final String COMPUTED_KEY_FORMAT = "%s-%s";
    private String owner;
    private String repositoryName;
    private Credentials credentials;
    private String key;
    private String name;

    public ApiClient(String username, String password, String owner, String repositoryName, String key, String name) {
        this.credentials = new UsernamePasswordCredentials(username, password);
        this.owner = owner;
        this.repositoryName = repositoryName;
        this.key = key;
        this.name = name;
    }

    public List<Pullrequest> getPullRequests() {
        try {
            return parse(get(v2("/pullrequests/")), Pullrequest.Response.class).getPullrequests();
        } catch(Exception e) {
            logger.log(Level.WARNING, "invalid pull request response.", e);
        }
        return Collections.EMPTY_LIST;
    }

    public List<Pullrequest.Comment> getPullRequestComments(String commentOwnerName, String commentRepositoryName, String pullRequestId) {
        try {
            return parse(get(v1("/pullrequests/" + pullRequestId + "/comments")), new TypeReference<List<Pullrequest.Comment>>() {});
        } catch(Exception e) {
            logger.log(Level.WARNING, "invalid pull request response.", e);
        }
        return Collections.EMPTY_LIST;
    }
    
    public String getName() {
      return this.name;
    }
    
    private String computeAPIKey(String keyExPart) {
      return String.format(COMPUTED_KEY_FORMAT, this.key, keyExPart);
    }
    
    public String buildStatusKey(String bsKey) {
      return this.computeAPIKey(bsKey);
    }

    public boolean hasBuildStatus(String owner, String repositoryName, String revision, String keyEx) {
        String url = v2(owner, repositoryName, "/commit/" + revision + "/statuses/build/" + this.computeAPIKey(keyEx));
        return get(url).contains("\"state\"");
    }

    public void setBuildStatus(String owner, String repositoryName, String revision, BuildState state, String buildUrl, String comment, String keyEx) {
        String url = v2(owner, repositoryName, "/commit/" + revision + "/statuses/build");
        String computedKey = this.computeAPIKey(keyEx);
        NameValuePair[] data = new NameValuePair[]{
                new NameValuePair("description", comment),
                new NameValuePair("key", computedKey),
                new NameValuePair("name", this.name),
                new NameValuePair("state", state.toString()),
                new NameValuePair("url", buildUrl),
        };
        logger.log(Level.INFO, "POST state {0} to {1} with key {2} with response {3}", new Object[]{
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
        NameValuePair[] data = new NameValuePair[] {
                new NameValuePair("content", content),
        };
        put(v1("/pullrequests/" + pullRequestId + "/comments/" + commentId), data);
    }

    public Pullrequest.Participant postPullRequestApproval(String pullRequestId) {
        try {
            return parse(post(v2("/pullrequests/" + pullRequestId + "/approve"),
                new NameValuePair[]{}), Pullrequest.Participant.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public Pullrequest.Comment postPullRequestComment(String pullRequestId, String content) {
        NameValuePair[] data = new NameValuePair[] {
                new NameValuePair("content", content),
        };
        try {
            return parse(post(v1("/pullrequests/" + pullRequestId + "/comments"), data), new TypeReference<Pullrequest.Comment>() {});
        } catch(Exception e) {
            logger.log(Level.WARNING, "Invalid pull request comment response.", e);
            e.printStackTrace();
        }
        return null;
    }

    private HttpClient getHttpClient() {
        HttpClient client = new HttpClient();
        if (Jenkins.getInstance() == null) return client;

        ProxyConfiguration proxy = Jenkins.getInstance().proxy;
        if (proxy == null) return client;

        logger.info("Jenkins proxy: " + proxy.name + ":" + proxy.port);
        client.getHostConfiguration().setProxy(proxy.name, proxy.port);
        String username = proxy.getUserName();
        String password = proxy.getPassword();

        // Consider it to be passed if username specified. Sufficient?
        if (username != null && !"".equals(username.trim())) {
            logger.info("Using proxy authentication (user=" + username + ")");
            client.getState().setProxyCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(username, password));
        }
        return client;
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
        return send(new GetMethod(path));
    }

    private String post(String path, NameValuePair[] data) {
        PostMethod req = new PostMethod(path);
        req.setRequestBody(data);
        req.getParams().setContentCharset("utf-8");
        return send(req);
    }

    private void delete(String path) {
         send(new DeleteMethod(path));
    }
    
    private void put(String path, NameValuePair[] data) {
        PutMethod req = new PutMethod(path);
        req.setRequestBody(EncodingUtil.formUrlEncode(data, "utf-8"));
        req.getParams().setContentCharset("utf-8");
        send(req);
    }

    private String send(HttpMethodBase req) {
        HttpClient client = getHttpClient();
        client.getState().setCredentials(AuthScope.ANY, credentials);
        client.getParams().setAuthenticationPreemptive(true);
        try {
            client.executeMethod(req);
            return req.getResponseBodyAsString();
        } catch (HttpException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private <R> R parse(String response, Class<R> cls) throws IOException {
        return new ObjectMapper().readValue(response, cls);
    }
    private <R> R parse(String response, TypeReference<R> ref) throws IOException {
        return new ObjectMapper().readValue(response, ref);
    }
}
