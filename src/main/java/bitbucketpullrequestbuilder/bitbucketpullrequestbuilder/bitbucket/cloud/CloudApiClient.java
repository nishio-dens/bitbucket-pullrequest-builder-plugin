package bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.cloud;

import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.AbstractPullrequest;
import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.ApiClient;
import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.BuildState;
import org.apache.commons.httpclient.NameValuePair;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.type.TypeReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
        return getAllValues(v2("/pullrequests/"), 50, CloudPullrequest.class);
    }

    @Override
    public List<AbstractPullrequest.Comment> getPullRequestComments(String commentOwnerName, String commentRepositoryName, String pullRequestId) {
        return getAllValues(v2("/pullrequests/" + pullRequestId + "/comments"), 100, AbstractPullrequest.Comment.class);
    }

    @Override
    public boolean hasBuildStatus(String owner, String repositoryName, String revision, String keyEx) {
        String url = v2(owner, repositoryName, "/commit/" + revision + "/statuses/build/" + computeAPIKey(keyEx));
        String reqBody = get(url);
        return reqBody != null && reqBody.contains("\"state\"");
    }

    @Override
    public void setBuildStatus(String owner, String repositoryName, String revision, BuildState state, String buildUrl, String comment, String keyEx) {
        String url = v2(owner, repositoryName, "/commit/" + revision + "/statuses/build");
        setBuildStatus(state, buildUrl, comment, keyEx, url);
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
                new NameValuePair[]{}), AbstractPullrequest.Participant.class);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Invalid pull request approval response.", e);
        }
        return null;
    }

    @Override
    public AbstractPullrequest.Comment postPullRequestComment(String pullRequestId, String content) {
        NameValuePair[] data = new NameValuePair[] {
            new NameValuePair("content", content),
        };
        try {
            String response = post(v2("/pullrequests/" + pullRequestId + "/comments"), data);
            return parse(response, new TypeReference<AbstractPullrequest.Comment>() {});
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
                AbstractPullrequest.Response<T> response = parse(get(url), type);
                values.addAll(response.getValues());
                url = response.getNext();
            } while (url != null);
        } catch (Exception e) {
            logger.log(Level.WARNING, "invalid response.", e);
        }
        return values;
    }
}
