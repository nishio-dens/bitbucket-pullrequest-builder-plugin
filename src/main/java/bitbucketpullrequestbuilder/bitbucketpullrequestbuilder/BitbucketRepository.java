package bitbucketpullrequestbuilder.bitbucketpullrequestbuilder;

import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.BitbucketApiClient;
import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.BitbucketPullRequestComment;
import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.BitbucketPullRequestResponseValue;
import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.BitbucketPullRequestResponseValueRepository;

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
    public static final String BUILD_START_MARKER = "[*BuildStarted*] %s";
    public static final String BUILD_FINISH_MARKER = "[*BuildFinished*] %s";
    public static final String BUILD_FINISH_SENTENCE = BUILD_FINISH_MARKER + " \n\n **%s** - %s";

    public static final String BUILD_SUCCESS_COMMENT =  ":star:SUCCESS";
    public static final String BUILD_FAILURE_COMMENT = ":x:FAILURE";
    public String BUILD_REQUEST_MARKER = "test this please";

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
        BUILD_REQUEST_MARKER = this.trigger.getTriggerPhrase();
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

    public String postBuildStartCommentTo(BitbucketPullRequestResponseValue pullRequest) {
            String commit = pullRequest.getSource().getCommit().getHash();
            String comment = String.format(BUILD_START_MARKER, commit);
            BitbucketPullRequestComment commentResponse = this.client.postPullRequestComment(pullRequest.getId(), comment);
            return commentResponse.getCommentId().toString();
    }

    public void addFutureBuildTasks(Collection<BitbucketPullRequestResponseValue> pullRequests) {
        for(BitbucketPullRequestResponseValue pullRequest : pullRequests) {
            String commentId = postBuildStartCommentTo(pullRequest);
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
                    commentId);
            this.builder.getTrigger().startJob(cause);
        }
    }

    public void deletePullRequestComment(String pullRequestId, String commentId) {
        this.client.deletePullRequestComment(pullRequestId,commentId);
    }

    public void postFinishedComment(String pullRequestId, String commit,  boolean success, String buildUrl) {
        String message = BUILD_FAILURE_COMMENT;
        if (success){
            message = BUILD_SUCCESS_COMMENT;
        }
        String comment = String.format(BUILD_FINISH_SENTENCE, commit, message, buildUrl);

        this.client.postPullRequestComment(pullRequestId, comment);
    }

    private boolean isBuildTarget(BitbucketPullRequestResponseValue pullRequest) {
        boolean shouldBuild = true;
        if (pullRequest.getState() != null && pullRequest.getState().equals("OPEN")) {
            if (isSkipBuild(pullRequest.getTitle())) {
                return false;
            }

            String commit = pullRequest.getSource().getCommit().getHash();
            BitbucketPullRequestResponseValueRepository destination = pullRequest.getDestination();
            String owner = destination.getRepository().getOwnerName();
            String repositoryName = destination.getRepository().getRepositoryName();
            String id = pullRequest.getId();
            List<BitbucketPullRequestComment> comments = client.getPullRequestComments(owner, repositoryName, id);
            String searchStartMarker = String.format(BUILD_START_MARKER, commit).toLowerCase();
            String searchFinishMarker = String.format(BUILD_FINISH_MARKER, commit).toLowerCase();

            if (comments != null) {
                Collections.sort(comments);
                Collections.reverse(comments);
                for(BitbucketPullRequestComment comment : comments) {
                    String content = comment.getContent();
                    if (content == null || content.isEmpty()) {
                        continue;
                    }
                    content = content.toLowerCase();
                    if (content.contains(searchStartMarker) ||
                        content.contains(searchFinishMarker)) {
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
