package bitbucketpullrequestbuilder.bitbucketpullrequestbuilder;

import hudson.model.BuildBadgeAction;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * Created by nishio
 */
@ExportedBean
public class BitbucketAction implements BuildBadgeAction {
    private final String commitHash;
    private final String pullRequestId;
    private final String repositoryOwner;
    private final String repositoryName;
    private final String sourceBranch;
    
    public BitbucketAction(String commitHash, String pullRequestId,
            String repositoryOwner, String repositoryName,String sourceBranch) {
        this.commitHash = commitHash;
        this.pullRequestId = pullRequestId;
        this.repositoryOwner = repositoryOwner;
        this.repositoryName = repositoryName;
        this.sourceBranch = sourceBranch;
    }

    @Exported
    public String getCommitHash() {
        return commitHash;
    }

    @Exported
    public String getPullRequestId() {
        return pullRequestId;
    }

    @Exported
    public String getRepositoryOwner() {
        return repositoryOwner;
    }

    @Exported
    public String getRepositoryName() {
        return repositoryName;
    }

    @Exported
    public String getSourceBranch() {
        return sourceBranch;
    }

    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return null;
    }

    public String getUrlName() {
        return null;
    }
}
