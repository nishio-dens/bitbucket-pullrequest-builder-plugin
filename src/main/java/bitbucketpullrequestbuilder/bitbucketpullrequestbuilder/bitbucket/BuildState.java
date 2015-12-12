package bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket;

/**
 * Valid build states for a pull request
 *
 * @see https://confluence.atlassian.com/bitbucket/buildstatus-resource-779295267.html
 */
public enum BuildState {
    FAILED, INPROGRESS, SUCCESSFUL
}
