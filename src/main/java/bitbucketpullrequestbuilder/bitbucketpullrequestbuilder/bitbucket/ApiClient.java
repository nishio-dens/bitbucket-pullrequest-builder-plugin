package bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.type.TypeReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.model.Jenkins;
import hudson.ProxyConfiguration;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.httpclient.methods.PutMethod;
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
    private HttpClientFactory factory;
    
    public static final byte MAX_KEY_SIZE_BB_API = 40;

    public static class HttpClientFactory {    
        public static final HttpClientFactory INSTANCE = new HttpClientFactory();
        private static final int DEFAULT_TIMEOUT = 60000;
        
        public HttpClient getInstanceHttpClient() {
            HttpClient client = new HttpClient();

            HttpClientParams params = client.getParams();
            params.setConnectionManagerTimeout(DEFAULT_TIMEOUT);
            params.setSoTimeout(DEFAULT_TIMEOUT);

            if (Jenkins.getInstance() == null) return client;

            ProxyConfiguration proxy = Jenkins.getInstance().proxy;
            if (proxy == null) return client;

            logger.log(Level.INFO, "Jenkins proxy: {0}:{1}", new Object[]{ proxy.name, proxy.port });
            client.getHostConfiguration().setProxy(proxy.name, proxy.port);
            String username = proxy.getUserName();
            String password = proxy.getPassword();

            // Consider it to be passed if username specified. Sufficient?
            if (username != null && !"".equals(username.trim())) {
                logger.log(Level.INFO, "Using proxy authentication (user={0})", username);
                client.getState().setProxyCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(username, password));
            }
            
            return client;
        }
    }
    
    public <T extends HttpClientFactory> ApiClient(
        String username, String password, 
        String owner, String repositoryName, 
        String key, String name, 
        T httpFactory
    ) {
        this.credentials = new UsernamePasswordCredentials(username, password);
        this.owner = owner;
        this.repositoryName = repositoryName;
        this.key = key;
        this.name = name;        
        this.factory = httpFactory != null ? httpFactory : HttpClientFactory.INSTANCE;
    }

    public List<Pullrequest> getPullRequests() {
        return getAllValues(v2("/pullrequests/"), 50, Pullrequest.class);
    }

    public List<Pullrequest.Comment> getPullRequestComments(String commentOwnerName, String commentRepositoryName, String pullRequestId) {
        return getAllValues(v2("/pullrequests/" + pullRequestId + "/comments"), 100, Pullrequest.Comment.class);
    }
    
    public String getName() {
      return this.name;
    }
    
    private static MessageDigest SHA1 = null;
    
    /**
     * Retrun 
     * @param keyExPart
     * @return key parameter for call BitBucket API 
     */
    private String computeAPIKey(String keyExPart) {
      String computedKey = String.format(COMPUTED_KEY_FORMAT, this.key, keyExPart);
      
      if (computedKey.length() > MAX_KEY_SIZE_BB_API) {
        try { 
          if (SHA1 == null) SHA1 = MessageDigest.getInstance("SHA1"); 
          return new String(Hex.encodeHex(SHA1.digest(computedKey.getBytes("UTF-8"))));
        } catch(NoSuchAlgorithmException e) { 
          logger.log(Level.WARNING, "Failed to create hash provider", e);
          e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
          logger.log(Level.WARNING, "Failed to create hash provider", e);
          e.printStackTrace();
        }
      }      
      return (computedKey.length() <= MAX_KEY_SIZE_BB_API) ?  computedKey : computedKey.substring(0, MAX_KEY_SIZE_BB_API);
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

    public Pullrequest.Participant postPullRequestDecline(String pullRequestId){
        try {
            return parse(post(v2("/pullrequests/" + pullRequestId + "/decline"),
                    new NameValuePair[]{}), Pullrequest.Participant.class);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Invalid pull request declined response.", e);
            e.printStackTrace();
        }
        return null;
    }
    public Pullrequest.Participant postPullRequestApproval(String pullRequestId) {
        try {
            return parse(post(v2("/pullrequests/" + pullRequestId + "/approve"),
                new NameValuePair[]{}), Pullrequest.Participant.class);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Invalid pull request approval response.", e);
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

    private <T> List<T> getAllValues(String rootUrl, int pageLen, Class<T> cls) {
        List<T> values = new ArrayList<T>();
        try {
            String url = rootUrl + "?pagelen=" + pageLen;
            do {
                final JavaType type = TypeFactory.defaultInstance().constructParametricType(Pullrequest.Response.class, cls);
                Pullrequest.Response<T> response = parse(get(url), type);
                values.addAll(response.getValues());
                url = response.getNext();
            } while (url != null);
        } catch (Exception e) {
            logger.log(Level.WARNING, "invalid response.", e);
            e.printStackTrace();
        }
        return values;
    }

    private HttpClient getHttpClient() {
        return this.factory.getInstanceHttpClient();
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
            logger.log(Level.WARNING, "Failed to send request.", e);
            e.printStackTrace();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to send request.", e);
            e.printStackTrace();
        } finally {
          req.releaseConnection();
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
