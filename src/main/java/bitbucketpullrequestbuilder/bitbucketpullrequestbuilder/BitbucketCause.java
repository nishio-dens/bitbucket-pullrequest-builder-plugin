package bitbucketpullrequestbuilder.bitbucketpullrequestbuilder;

import hudson.model.Cause;

/**
 * Created by nishio
 */
public class BitbucketCause extends Cause {
    private final String sourceBranch;
    private final String targetBranch;
    private final String repositoryOwner;
    private final String repositoryName;
    private final String bitbucketServerUrl;
    private final String pullRequestId;
    private final String destinationRepositoryOwner;
    private final String destinationRepositoryName;
    private final String pullRequestTitle;
    private final String sourceCommitHash;
    private final String destinationCommitHash;
    private final String pullRequestAuthor;
    public static final String BITBUCKET_URL = "https://bitbucket.org/";

    public BitbucketCause(String sourceBranch,
                          String targetBranch,
                          String repositoryOwner,
                          String repositoryName,
                          String bitbucketServerUrl,
                          String pullRequestId,
                          String destinationRepositoryOwner,
                          String destinationRepositoryName,
                          String pullRequestTitle,
                          String sourceCommitHash,
                          String destinationCommitHash,
                          String pullRequestAuthor) {
        this.sourceBranch = sourceBranch;
        this.targetBranch = targetBranch;
        this.repositoryOwner = repositoryOwner;
        this.repositoryName = repositoryName;
        this.bitbucketServerUrl = bitbucketServerUrl;
        this.pullRequestId = pullRequestId;
        this.destinationRepositoryOwner = destinationRepositoryOwner;
        this.destinationRepositoryName = destinationRepositoryName;
        this.pullRequestTitle = pullRequestTitle;
        this.sourceCommitHash = sourceCommitHash;
        this.destinationCommitHash = destinationCommitHash;
        this.pullRequestAuthor = pullRequestAuthor;
    }

    public String getSourceBranch() {
        return sourceBranch;
    }
    public String getTargetBranch() {
        return targetBranch;
    }

    public String getRepositoryOwner() {
        return repositoryOwner;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public String getBitbucketServerUrl() {
        return bitbucketServerUrl;
    }

    public String getPullRequestId() {
        return pullRequestId;
    }

    public String getDestinationRepositoryOwner() {
        return destinationRepositoryOwner;
    }

    public String getDestinationRepositoryName() {
        return destinationRepositoryName;
    }

    public String getPullRequestTitle() {
        return pullRequestTitle;
    }

    public String getSourceCommitHash() { return sourceCommitHash; }

    public String getDestinationCommitHash() { return destinationCommitHash; }

    @Override
    public String getShortDescription() {
        String description = "<a href=\"" + BITBUCKET_URL + this.getDestinationRepositoryOwner() + "/";
        description += this.getDestinationRepositoryName() + "/pull-request/" + this.getPullRequestId();
        description += "\">#" + this.getPullRequestId() + " " + this.getPullRequestTitle() + "</a>";
        return description;
    }

    public String getPullRequestAuthor() {
      return this.pullRequestAuthor;
    }
}
