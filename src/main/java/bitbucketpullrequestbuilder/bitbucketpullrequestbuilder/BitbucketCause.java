package bitbucketpullrequestbuilder.bitbucketpullrequestbuilder;

import hudson.model.Cause;

public abstract class BitbucketCause extends Cause {
    private final String sourceBranch;
    private final String targetBranch;
    private final String repositoryOwner;
    private final String repositoryName;
    private final String repositoryUri;
    private final String pullRequestId;
    private final String destinationRepositoryOwner;
    private final String destinationRepositoryName;
    private final String pullRequestTitle;
    private final String sourceCommitHash;
    private final String destinationCommitHash;
    private final String pullRequestAuthor;

    protected BitbucketCause(String sourceBranch, String targetBranch, String repositoryOwner, String repositoryName,
                             String repositoryUri, String pullRequestId, String destinationRepositoryOwner,
                             String destinationRepositoryName, String pullRequestTitle, String sourceCommitHash,
                             String destinationCommitHash, String pullRequestAuthor) {
        this.sourceBranch = sourceBranch;
        this.targetBranch = targetBranch;
        this.repositoryOwner = repositoryOwner;
        this.repositoryName = repositoryName;
        this.repositoryUri = repositoryUri;
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

    public String getRepositoryUri() {
        if (repositoryUri == null) {
            // If nil, generate it from our repo name and owner (assume bitbucket cloud)
            // Would be nice to go ahead and set repositoryUri too, for efficiency, but NOOOOOOO, it's gotta be final

            // repositoryUri = String.format("git@bitbucket.org:%s/%s.git", repositoryOwner, repositoryName);
            return String.format("git@bitbucket.org:%s/%s.git", repositoryOwner, repositoryName);
        }

        return repositoryUri;
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

    public String getPullRequestAuthor() {
        return this.pullRequestAuthor;
    }
}
