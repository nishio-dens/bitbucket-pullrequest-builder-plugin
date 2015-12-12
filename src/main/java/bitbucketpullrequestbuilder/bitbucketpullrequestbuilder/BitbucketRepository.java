package bitbucketpullrequestbuilder.bitbucketpullrequestbuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.ApiClient;
import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.BuildState;
import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.Pullrequest;
import jenkins.model.Jenkins;

/**
 * Created by nishio
 */
public class BitbucketRepository {
    private static final Logger logger = Logger.getLogger(BitbucketRepository.class.getName());
    private static final String BUILD_DESCRIPTION = "%s: %s into %s";
    private static final String BUILD_REQUEST_MARKER = "test this please";

    private String projectPath;
    private BitbucketPullRequestsBuilder builder;
    private BitbucketBuildTrigger trigger;
    private ApiClient client;

    public BitbucketRepository(String projectPath, BitbucketPullRequestsBuilder builder) {
        this.projectPath = projectPath;
        this.builder = builder;
    }

    public void init() {
        trigger = this.builder.getTrigger();
        client = new ApiClient(
                trigger.getUsername(),
                trigger.getPassword(),
                trigger.getRepositoryOwner(),
                trigger.getRepositoryName(),
                trigger.getCiKey(),
                trigger.getCiName()
        );
    }

    public Collection<Pullrequest> getTargetPullRequests() {
        logger.info("Fetch PullRequests.");
        List<Pullrequest> pullRequests = client.getPullRequests();
        List<Pullrequest> targetPullRequests = new ArrayList<Pullrequest>();
        for(Pullrequest pullRequest : pullRequests) {
            if (isBuildTarget(pullRequest)) {
                targetPullRequests.add(pullRequest);
            }
        }
        return targetPullRequests;
    }

    public void addFutureBuildTasks(Collection<Pullrequest> pullRequests) {
        for(Pullrequest pullRequest : pullRequests) {
            if ( this.trigger.getApproveIfSuccess() ) {
                deletePullRequestApproval(pullRequest.getId());
            }
            BitbucketCause cause = new BitbucketCause(
                    pullRequest.getSource().getBranch().getName(),
                    pullRequest.getDestination().getBranch().getName(),
                    pullRequest.getSource().getRepository().getOwnerName(),
                    pullRequest.getSource().getRepository().getRepositoryName(),
                    pullRequest.getId(),
                    pullRequest.getDestination().getRepository().getOwnerName(),
                    pullRequest.getDestination().getRepository().getRepositoryName(),
                    pullRequest.getTitle(),
                    pullRequest.getSource().getCommit().getHash(),
                    pullRequest.getDestination().getCommit().getHash());
            setBuildStatus(cause, BuildState.INPROGRESS, Jenkins.getInstance().getRootUrl());
            this.builder.getTrigger().startJob(cause);
        }
    }

    public void setBuildStatus(BitbucketCause cause, BuildState state, String buildUrl) {
        String comment = null;
        String sourceCommit = cause.getSourceCommitHash();
        String owner = cause.getRepositoryOwner();
        String repository = cause.getRepositoryName();
        String destinationBranch = cause.getTargetBranch();

        logger.info("setBuildStatus " + state + " for commit: " + sourceCommit + " with url " + buildUrl);

        if (state == BuildState.FAILED || state == BuildState.SUCCESSFUL) {
            comment = String.format(BUILD_DESCRIPTION, builder.getProject().getDisplayName(), sourceCommit, destinationBranch);
        }

        this.client.setBuildStatus(owner, repository, sourceCommit, state, buildUrl, comment);
    }

    public void deletePullRequestApproval(String pullRequestId) {
        this.client.deletePullRequestApproval(pullRequestId);
    }

    public void postPullRequestApproval(String pullRequestId) {
        this.client.postPullRequestApproval(pullRequestId);
    }

    private boolean isBuildTarget(Pullrequest pullRequest) {
        if (pullRequest.getState() != null && pullRequest.getState().equals("OPEN")) {
            if (isSkipBuild(pullRequest.getTitle())) {
                return false;
            }

            Pullrequest.Revision source = pullRequest.getSource();
            String sourceCommit = source.getCommit().getHash();
            Pullrequest.Revision destination = pullRequest.getDestination();
            String owner = destination.getRepository().getOwnerName();
            String repositoryName = destination.getRepository().getRepositoryName();

            String id = pullRequest.getId();
            List<Pullrequest.Comment> comments = client.getPullRequestComments(owner, repositoryName, id);

            if (comments != null) {
                Collections.sort(comments);
                Collections.reverse(comments);
                for (Pullrequest.Comment comment : comments) {
                    String content = comment.getContent();
                    if (content == null || content.isEmpty()) {
                        continue;
                    }

                    if (content.contains(BUILD_REQUEST_MARKER.toLowerCase())) {
                        return true;
                    }
                }
            }

            Pullrequest.Repository sourceRepository = source.getRepository();

            if (this.client.hasBuildStatus(sourceRepository.getOwnerName(), sourceRepository.getRepositoryName(), sourceCommit)) {
                logger.info("Commit " + sourceCommit + " has already been processed");
                return false;
            }

            return true;
        }

        return false;
    }

    private boolean isSkipBuild(String pullRequestTitle) {
        String skipPhrases = this.trigger.getCiSkipPhrases();
        if (skipPhrases != null && !"".equals(skipPhrases)) {
            String[] phrases = skipPhrases.split(",");
            for(String phrase : phrases) {
                if (pullRequestTitle.toLowerCase().contains(phrase.trim().toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }
}
