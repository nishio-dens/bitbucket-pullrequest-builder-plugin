package bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Created by nishio
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BitbucketPullRequestResponseValueRepository {
    private BitbucketPullRequestResponseValueRepositoryRepository repository;
    private BitbucketPullRequestResponseValueRepositoryBranch branch;

    @JsonProperty("repository")
    public BitbucketPullRequestResponseValueRepositoryRepository getRepository() {
        return repository;
    }

    @JsonProperty("repository")
    public void setRepository(BitbucketPullRequestResponseValueRepositoryRepository repository) {
        this.repository = repository;
    }

    @JsonProperty("branch")
    public BitbucketPullRequestResponseValueRepositoryBranch getBranch() {
        return branch;
    }

    @JsonProperty("branch")
    public void setBranch(BitbucketPullRequestResponseValueRepositoryBranch branch) {
        this.branch = branch;
    }
}


