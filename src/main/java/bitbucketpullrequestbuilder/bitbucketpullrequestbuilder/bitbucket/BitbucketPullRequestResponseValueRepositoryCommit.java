package bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BitbucketPullRequestResponseValueRepositoryCommit {
    private String Hash;

    public String getHash() {
        return Hash;
    }

    public void setHash(String hash) {
        Hash = hash;
    }
}