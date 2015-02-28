package bitbucketpullrequestbuilder.bitbucketpullrequestbuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.BitbucketApiClient;
import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.BitbucketPullRequestComment;
import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.BitbucketPullRequestResponseValue;
import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.BitbucketPullRequestResponseValueRepository;

/**
 * Created by nishio
 */
public class BitbucketRepository {
    private static final Logger logger = Logger.getLogger(BitbucketRepository.class.getName());
    public static final String BUILD_START_MARKER = "[*BuildStarted* **%s**] %s into %s";
    public static final String BUILD_FINISH_MARKER = "[*BuildFinished* **%s**] %s into %s";

    public static final String BUILD_START_REGEX = "\\[\\*BuildStarted\\* \\*\\*%s\\*\\*\\] ([0-9a-fA-F]+) into ([0-9a-fA-F]+)";
    public static final String BUILD_FINISH_REGEX = "\\[\\*BuildFinished\\* \\*\\*%s\\*\\*\\] ([0-9a-fA-F]+) into ([0-9a-fA-F]+)";

    public static final String BUILD_FINISH_SENTENCE = BUILD_FINISH_MARKER + " \n\n **%s** - %s";
    public static final String BUILD_REQUEST_MARKER = "test this please";

    public static final String BUILD_SUCCESS_COMMENT =  "✓ SUCCESS";
    public static final String BUILD_FAILURE_COMMENT = "✕ FAILURE";
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

    public String postBuildStartCommentTo(BitbucketPullRequestResponseValue pullRequest) {
            String sourceCommit = pullRequest.getSource().getCommit().getHash();
            String destinationCommit = pullRequest.getDestination().getCommit().getHash();
            String comment = String.format(BUILD_START_MARKER, builder.getProject().getDisplayName(), sourceCommit, destinationCommit);
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
                    pullRequest.getDestination().getCommit().getHash(),
                    commentId);
            this.builder.getTrigger().startJob(cause);
        }
    }

    public void deletePullRequestComment(String pullRequestId, String commentId) {
        this.client.deletePullRequestComment(pullRequestId,commentId);
    }

    public void postFinishedComment(String pullRequestId, String sourceCommit,  String destinationCommit, boolean success, String buildUrl) {
        String message = BUILD_FAILURE_COMMENT;
        if (success){
            message = BUILD_SUCCESS_COMMENT;
        }
        String comment = String.format(BUILD_FINISH_SENTENCE, builder.getProject().getDisplayName(), sourceCommit, destinationCommit, message, buildUrl);

        this.client.postPullRequestComment(pullRequestId, comment);
    }

    private boolean isBuildTarget(BitbucketPullRequestResponseValue pullRequest) {
    	
        boolean shouldBuild = true;
        if (pullRequest.getState() != null && pullRequest.getState().equals("OPEN")) {
            if (isSkipBuild(pullRequest.getTitle())) {
                return false;
            }

            String sourceCommit = pullRequest.getSource().getCommit().getHash();

            BitbucketPullRequestResponseValueRepository destination = pullRequest.getDestination();
            String owner = destination.getRepository().getOwnerName();
            String repositoryName = destination.getRepository().getRepositoryName();
            String destinationCommit = destination.getCommit().getHash();
            
            String id = pullRequest.getId();
            List<BitbucketPullRequestComment> comments = client.getPullRequestComments(owner, repositoryName, id);
            
            if (comments != null) {
                Collections.sort(comments);
                Collections.reverse(comments);
                for (BitbucketPullRequestComment comment : comments) {
                    String content = comment.getContent();
                    if (content == null || content.isEmpty()) {
                        continue;
                    }

                    //These will match any start or finish message -- need to check commits
                    String project_build_start = String.format(BUILD_START_REGEX, builder.getProject().getDisplayName());
                    String project_build_finished = String.format(BUILD_FINISH_REGEX, builder.getProject().getDisplayName());
                    Matcher startMatcher = Pattern.compile(project_build_start, Pattern.CASE_INSENSITIVE).matcher(content);
                    Matcher finishMatcher = Pattern.compile(project_build_finished, Pattern.CASE_INSENSITIVE).matcher(content);

                    if (startMatcher.find() ||
                        finishMatcher.find()) {

                        String sourceCommitMatch;
                        String destinationCommitMatch;

                        if (startMatcher.find(0)) {
                            sourceCommitMatch = startMatcher.group(1);
                            destinationCommitMatch = startMatcher.group(2);
                        } else {
                            sourceCommitMatch = finishMatcher.group(1);
                            destinationCommitMatch = finishMatcher.group(2);
                        }

                        //first check source commit -- if it doesn't match, just move on. If it does, investigate further.
                        if (sourceCommitMatch.equalsIgnoreCase(sourceCommit)) {
                            // if we're checking destination commits, and if this doesn't match, then move on.
                            if (this.trigger.getCheckDestinationCommit() 
                                    && (!destinationCommitMatch.equalsIgnoreCase(destinationCommit))) {
                            	continue;
                            }
                            
                            shouldBuild = false;
                            break;
                        } 
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
