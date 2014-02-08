package bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        return null;
    }

    public List<BitbucketPullRequestComment> getPullRequestComments(String commentOwnerName, String commentRepositoryName, String pullRequestId) {
        String response = getRequest(
            V1_API_BASE_URL + commentOwnerName + "/" + commentRepositoryName + "/pullrequests/" + pullRequestId + "/comments");
        try {
            return parseCommentJson(response);
        } catch(Exception e) {
            logger.log(Level.WARNING, "invalid pull request response.", e);
        }
        return null;
    }

    public void postPullRequestComment(String pullRequestId, String comment) {
        String path = V1_API_BASE_URL + this.owner + "/" + this.repositoryName + "/pullrequests/" + pullRequestId + "/comments";
        try {
            NameValuePair content = new NameValuePair("content", comment);
            postRequest(path, new NameValuePair[]{ content });
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private String getRequest(String path) {
        HttpClient client = new HttpClient();
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

    private void postRequest(String path, NameValuePair[] params) throws UnsupportedEncodingException {
        HttpClient client = new HttpClient();
        client.getState().setCredentials(AuthScope.ANY, credentials);
        PostMethod httppost = new PostMethod(path);
        httppost.setRequestBody(params);
        client.getParams().setAuthenticationPreemptive(true);
        try {
            client.executeMethod(httppost);
            String response = httppost.getResponseBodyAsString();
            logger.info("API Request Response: " + response);
        } catch (HttpException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

}

