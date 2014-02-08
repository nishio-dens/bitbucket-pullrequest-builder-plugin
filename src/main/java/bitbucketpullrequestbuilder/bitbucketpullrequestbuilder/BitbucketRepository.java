package bitbucketpullrequestbuilder.bitbucketpullrequestbuilder;

import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.BitbucketApiClient;
import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.BitbucketPullRequestComment;
import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.BitbucketPullRequestResponseValue;
import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.BitbucketPullRequestResponseValueRepository;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by nishio
 */
public class BitbucketRepository {
    private static final Logger logger = Logger.getLogger(BitbucketRepository.class.getName());
    public static final String BUILD_START_MARKER = "[BuildStart]";
    public static final String BUILD_FINISH_MARKER = "[BuildFinished]";
    public static final String BUILD_REQUEST_MARKER = "test this please";
    private String projectPath;
    private BitbucketPullRequestsBuilder builder;
    private BitbucketBuildTrigger trigger;
    private BitbucketApiClient client;

    public BitbucketRepository(String projectPath, BitbucketPullRequestsBuilder builder) {
        this.projectPath = projectPath;
        this.builder = builder;
    }

    public void init() {
        trigger = this.builder.getTrigger();
        client = new BitbucketApiClient(
                trigger.getUsername(),
                trigger.getPassword(),
                trigger.getRepositoryOwner(),
                trigger.getRepositoryName());
    }

    public Collection<BitbucketPullRequestResponseValue> getTargetPullRequests() {
        logger.info("Fetch PullRequests.");
        List<BitbucketPullRequestResponseValue> pullRequests = client.getPullRequests();
        List<BitbucketPullRequestResponseValue> targetPullRequests = new ArrayList<BitbucketPullRequestResponseValue>();
        for(BitbucketPullRequestResponseValue pullRequest : pullRequests) {
            if (isBuildTarget(pullRequest)) {
                targetPullRequests.add(pullRequest);
            }
        }
        return targetPullRequests;
    }

    public void postBuildStartCommentTo(Collection<BitbucketPullRequestResponseValue> pullRequests) {
        for(BitbucketPullRequestResponseValue pullRequest : pullRequests) {
            String comment = BUILD_START_MARKER;
            comment += " Build Triggered. Waiting to hear about " + pullRequest.getSource().getRepository().getFullName();
            this.client.postPullRequestComment(pullRequest.getId(), comment);
        }
    }

    public void addFutureBuildTasks(Collection<BitbucketPullRequestResponseValue> pullRequests) {
        for(BitbucketPullRequestResponseValue pullRequest : pullRequests) {
            BitbucketCause cause = new BitbucketCause(
                    pullRequest.getSource().getBranch().getName(),
                    pullRequest.getDestination().getBranch().getName(),
                    pullRequest.getSource().getRepository().getOwnerName(),
                    pullRequest.getSource().getRepository().getRepositoryName(),
                    pullRequest.getId(),
                    pullRequest.getDestination().getRepository().getOwnerName(),
                    pullRequest.getDestination().getRepository().getRepositoryName(),
                    pullRequest.getTitle());
            this.builder.getTrigger().startJob(cause);
        }
    }

    public void postFinishedComment(String pullRequestId, boolean success, String buildUrl) {
        String comment = BUILD_FINISH_MARKER;
        if (success) {
            comment += " Test PASSed. Refer to this link for build results.";
        } else {
            comment += " Test FAILed. Refer to this link for build results.";
        }
        comment += buildUrl;
        this.client.postPullRequestComment(pullRequestId, comment);
    }

    private boolean isBuildTarget(BitbucketPullRequestResponseValue pullRequest) {
        boolean shouldBuild = true;
        if (pullRequest.getState() != null && pullRequest.getState().equals("OPEN")) {
            BitbucketPullRequestResponseValueRepository destination = pullRequest.getDestination();
            String owner = destination.getRepository().getOwnerName();
            String repositoryName = destination.getRepository().getRepositoryName();
            String id = pullRequest.getId();
            List<BitbucketPullRequestComment> comments = client.getPullRequestComments(owner, repositoryName, id);
            if (comments != null) {
                Collections.sort(comments);
                Collections.reverse(comments);
                for(BitbucketPullRequestComment comment : comments) {
                    String content = comment.getContent();
                    if (content == null || content.isEmpty()) {
                        continue;
                    }
                    content = content.toLowerCase();
                    if (content.contains(BUILD_START_MARKER.toLowerCase())) {
                        shouldBuild = false;
                        break;
                    }
                    if (content.contains(BUILD_REQUEST_MARKER.toLowerCase())) {
                        shouldBuild = true;
                        break;
                    }
                }
            }
        }
        return shouldBuild;
    }
}
