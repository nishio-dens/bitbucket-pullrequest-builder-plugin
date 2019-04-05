package bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.cloud;

import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.AbstractPullrequest;
import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.ApiClient;
import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.BuildState;
import org.apache.commons.httpclient.NameValuePair;
import org.codehaus.jackson.map.type.MapType;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.type.TypeReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CloudApiClient extends ApiClient {

    private static final Logger logger = Logger.getLogger(CloudApiClient.class.getName());

    private static final String V2_API_BASE_URL = "https://bitbucket.org/api/2.0/repositories/";

    public <T extends HttpClientFactory> CloudApiClient(String username, String password, String owner, String repositoryName, String key, String name, T httpFactory) {
        super(username, password, owner, repositoryName, key, name, httpFactory);
    }

    @Override
    public List<CloudPullrequest> getPullRequests() {
        final List<CloudPullrequest> pullrequests = getAllValues(v2("/pullrequests/"), 50, CloudPullrequest.class);
        return addRepositoryUris(pullrequests);
    }

    private List<CloudPullrequest> addRepositoryUris(List<CloudPullrequest> pullrequests) {
        final AbstractPullrequest.RepositoryLinks repositoryLinks = getRepositoryLinks();
        if (repositoryLinks != null) {
            for (CloudPullrequest pullrequest : pullrequests) {
                final CloudPullrequest.Repository sourceRepository = pullrequest.getSource().getRepository();
                final AbstractPullrequest.RepositoryLinks links = sourceRepository.getLinks();
                if(links.getClone().isEmpty()) {
                    sourceRepository.setLinks(repositoryLinks);
                }
            }
        }
        return pullrequests;
    }

    @Override
    public List<AbstractPullrequest.Comment> getPullRequestComments(String commentOwnerName, String commentRepositoryName, String pullRequestId) {

        final List<CloudPullrequest.Comment> comments = getAllValues(v2("/pullrequests/" + pullRequestId + "/comments"), 100, CloudPullrequest.Comment.class);
        return cloudToAbstractComments(comments);
    }

    public AbstractPullrequest.RepositoryLinks getRepositoryLinks() {
        final String url = v2("/?fields=links");
        final String body = get(url);
        logger.log(Level.FINE, "****Received("+url+")****:\n" + body + "\n");
        AbstractPullrequest.RepositoryLinks ret = null;
        try {
            final TypeFactory typeFactory = TypeFactory.defaultInstance();
            final MapType responseType = typeFactory.constructMapType(
                    HashMap.class,
                    String.class,
                    AbstractPullrequest.RepositoryLinks.class
            );
            final Map<String, AbstractPullrequest.RepositoryLinks> map = parse(body, responseType);
            ret = map.get("links");
        } catch (IOException e) {
            logger.log(Level.WARNING, "invalid response.", e);
        }
        return ret;
    }

    private List<AbstractPullrequest.Comment> cloudToAbstractComments(List<CloudPullrequest.Comment> comments) {
        // There has got to be a better way to do this?
        // Sorry - my java OO-fu is weak.

        final List<AbstractPullrequest.Comment> resultComments = new ArrayList<>();
        for (final CloudPullrequest.Comment comment : comments) {
            resultComments.add(comment);
        }

        return resultComments;
    }

    @Override
    public boolean hasBuildStatus(String owner, String repositoryName, String revision, String keyEx) {
        String url = v2(owner, repositoryName, "/commit/" + revision + "/statuses/build/" + computeAPIKey(keyEx));
        String reqBody = get(url);
        logger.log(Level.FINE, "hasBuildStatus response: " + reqBody);
        return reqBody != null && reqBody.contains("\"state\"");
    }

    @Override
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

        String resp = post(url, data);

        logger.log(Level.FINE, "POST state {0} to {1} with key {2} with response {3}", new Object[]{
          state, url, computedKey, resp}
        );
    }

    @Override
    public void deletePullRequestApproval(String pullRequestId) {
        delete(v2("/pullrequests/" + pullRequestId + "/approve"));
    }

    public void deletePullRequestComment(String pullRequestId, String commentId) {
        delete(v2("/pullrequests/" + pullRequestId + "/comments/" + commentId));
    }

    public void updatePullRequestComment(String pullRequestId, String content, String commentId) {
        NameValuePair[] data = new NameValuePair[] {
            new NameValuePair("content", content),
        };
        put(v2("/pullrequests/" + pullRequestId + "/comments/" + commentId), data);
    }

    @Override
    public AbstractPullrequest.Participant postPullRequestApproval(String pullRequestId) {
        try {
            return parse(post(v2("/pullrequests/" + pullRequestId + "/approve"),
                new NameValuePair[]{}), CloudPullrequest.Participant.class);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Invalid pull request approval response.", e);
        }
        return null;
    }

    @Override
    public AbstractPullrequest.Comment postPullRequestComment(String pullRequestId, String content) {
        CloudPullrequest.Comment data = new CloudPullrequest.Comment(content);
        try {
            String response = post(v2("/pullrequests/" + pullRequestId + "/comments"), data);
            logger.log(Level.FINE, "postCommentResponse: " + response);
            return parse(response, new TypeReference<CloudPullrequest.Comment>() {});
        } catch(Exception e) {
            logger.log(Level.WARNING, "Invalid pull request comment response.", e);
        }
        return null;
    }

    private String v2(String path) {
        return v2(this.owner, this.repositoryName, path);
    }

    private String v2(String owner, String repositoryName, String path) {
        return V2_API_BASE_URL + owner + "/" + repositoryName + path;
    }

    private <T> List<T> getAllValues(String rootUrl, int pageLen, Class<T> cls) {
        List<T> values = new ArrayList<T>();
        try {
            String url = rootUrl + "?pagelen=" + pageLen;
            do {
                final JavaType type = TypeFactory.defaultInstance().constructParametricType(AbstractPullrequest.Response.class, cls);
                final String body = get(url);
                logger.log(Level.FINE, "****Received("+url+")****:\n" + body + "\n");
                AbstractPullrequest.Response<T> response = parse(body, type);
                values.addAll(response.getValues());
                url = response.getNext();
            } while (url != null);
        } catch (Exception e) {
            logger.log(Level.WARNING, "invalid response.", e);
        }
        return values;
    }
}
