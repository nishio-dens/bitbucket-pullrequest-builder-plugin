package bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.model.Jenkins;
import hudson.ProxyConfiguration;

/**
 * Created by nishio
 */
public class BitbucketApiClient {
    private static final Logger logger = Logger.getLogger(BitbucketApiClient.class.getName());
    private static final String BITBUCKET_HOST = "bitbucket.org";
    private static final String V1_API_BASE_URL = "https://bitbucket.org/api/1.0/repositories/";
    private static final String V2_API_BASE_URL = "https://bitbucket.org/api/2.0/repositories/";
    private String owner;
    private String repositoryName;
    private Credentials credentials;

    public BitbucketApiClient(String username, String password, String owner, String repositoryName) {
        this.credentials = new UsernamePasswordCredentials(username, password);
        this.owner = owner;
        this.repositoryName = repositoryName;
    }

    public List<BitbucketPullRequestResponseValue> getPullRequests() {
        String response = getRequest(V2_API_BASE_URL + this.owner + "/" + this.repositoryName + "/pullrequests/");
        try {
            return parsePullRequestJson(response).getPrValues();
        } catch(Exception e) {
            logger.log(Level.WARNING, "invalid pull request response.", e);
        }
        return Collections.EMPTY_LIST;
    }

    public List<BitbucketPullRequestComment> getPullRequestComments(String commentOwnerName, String commentRepositoryName, String pullRequestId) {
        String response = getRequest(
            V1_API_BASE_URL + commentOwnerName + "/" + commentRepositoryName + "/pullrequests/" + pullRequestId + "/comments");
        try {
            return parseCommentJson(response);
        } catch(Exception e) {
            logger.log(Level.WARNING, "invalid pull request response.", e);
        }
        return Collections.EMPTY_LIST;
    }

    public void deletePullRequestComment(String pullRequestId, String commentId) {
        String path = V1_API_BASE_URL + this.owner + "/" + this.repositoryName + "/pullrequests/" + pullRequestId + "/comments/" + commentId;
        //https://bitbucket.org/api/1.0/repositories/{accountname}/{repo_slug}/pullrequests/{pull_request_id}/comments/{comment_id}
        deleteRequest(path);
    }


    public BitbucketPullRequestComment postPullRequestComment(String pullRequestId, String comment) {
        String path = V1_API_BASE_URL + this.owner + "/" + this.repositoryName + "/pullrequests/" + pullRequestId + "/comments";
        try {
            NameValuePair content = new NameValuePair("content", comment);
            String response = postRequest(path, new NameValuePair[]{ content });
            return parseSingleCommentJson(response);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private HttpClient getHttpClient() {
        HttpClient client = new HttpClient();
        if (Jenkins.getInstance() != null) {
            ProxyConfiguration proxy = Jenkins.getInstance().proxy;
            logger.info("Jenins proxy: " + proxy.name + ":" + proxy.port);
            if (proxy != null) {
                client.getHostConfiguration().setProxy(proxy.name, proxy.port);
                String username = proxy.getUserName();
                String password = proxy.getPassword();
                // Consider it to be passed if username specified. Sufficient?
                if (username != null && !"".equals(username.trim())) {
                    logger.info("Using proxy authentication (user=" + username + ")");
                    client.getState().setProxyCredentials(AuthScope.ANY,
                        new UsernamePasswordCredentials(username, password));
                }
            }
        }
        return client;
    }

    private String getRequest(String path) {
        HttpClient client = getHttpClient();
        client.getState().setCredentials(AuthScope.ANY, credentials);
        GetMethod httpget = new GetMethod(path);
        client.getParams().setAuthenticationPreemptive(true);
        String response = null;
        try {
            client.executeMethod(httpget);
            response = httpget.getResponseBodyAsString();
        } catch (HttpException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    public void deleteRequest(String path) {
        HttpClient client = getHttpClient();
        client.getState().setCredentials(AuthScope.ANY, credentials);
        DeleteMethod httppost = new DeleteMethod(path);
        client.getParams().setAuthenticationPreemptive(true);
        String response = "";
        try {
            client.executeMethod(httppost);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String postRequest(String path, NameValuePair[] params) throws UnsupportedEncodingException {
        HttpClient client = getHttpClient();
        client.getState().setCredentials(AuthScope.ANY, credentials);
        PostMethod httppost = new PostMethod(path);
        httppost.setRequestBody(params);
        client.getParams().setAuthenticationPreemptive(true);
        String response = "";
        try {
            client.executeMethod(httppost);
            response = httppost.getResponseBodyAsString();
            logger.info("API Request Response: " + response);
        } catch (HttpException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;

    }

    private BitbucketPullRequestResponse parsePullRequestJson(String response) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        BitbucketPullRequestResponse parsedResponse;
        parsedResponse = mapper.readValue(response, BitbucketPullRequestResponse.class);
        return parsedResponse;
    }

    private List<BitbucketPullRequestComment> parseCommentJson(String response) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        List<BitbucketPullRequestComment> parsedResponse;
        parsedResponse = mapper.readValue(
                response,
                new TypeReference<List<BitbucketPullRequestComment>>() {
                });
        return parsedResponse;
    }

    private BitbucketPullRequestComment parseSingleCommentJson(String response) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        BitbucketPullRequestComment parsedResponse;
        parsedResponse = mapper.readValue(
                response,
                BitbucketPullRequestComment.class);
        return parsedResponse;
    }
}

