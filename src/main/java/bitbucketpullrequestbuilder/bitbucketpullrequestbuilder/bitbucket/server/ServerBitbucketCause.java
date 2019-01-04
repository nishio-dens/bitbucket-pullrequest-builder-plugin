package bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.server;

import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.BitbucketCause;

public class ServerBitbucketCause extends BitbucketCause {

    private final String serverUrl;

    public ServerBitbucketCause(String serverUrl, String sourceBranch, String targetBranch, String repositoryOwner,
                                String repositoryName, String pullRequestId, String destinationRepositoryOwner,
                                String destinationRepositoryName, String pullRequestTitle, String sourceCommitHash,
                                String destinationCommitHash, String pullRequestAuthor) {
        super(sourceBranch, targetBranch, repositoryOwner, repositoryName, pullRequestId, destinationRepositoryOwner, destinationRepositoryName, pullRequestTitle, sourceCommitHash, destinationCommitHash, pullRequestAuthor);
        this.serverUrl = serverUrl;
    }

    @Override
    public String getShortDescription() {
        return "#" + getPullRequestId() + " " + getPullRequestTitle()
                + " (" + getServerUrl() + "/projects/" + getRepositoryOwner() + "/repos/" + getRepositoryName() + "/pull-requests/" + getPullRequestId() + ")";
    }

    public String getServerUrl() {
        return serverUrl;
    }

}