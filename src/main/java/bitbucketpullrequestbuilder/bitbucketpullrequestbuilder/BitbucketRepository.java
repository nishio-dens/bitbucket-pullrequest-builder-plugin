package bitbucketpullrequestbuilder.bitbucketpullrequestbuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.util.LogTaskListener;

import jenkins.model.Jenkins;
import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.BitbucketApiClient;
import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.BitbucketPullRequestComment;
import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.BitbucketPullRequestResponseValue;
import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.BitbucketPullRequestResponseValueRepository;

/**
 * Created by nishio
 */
public class BitbucketRepository {
    private static final Logger logger = Logger.getLogger(BitbucketRepository.class.getName());
    public static final String BUILD_START_MARKER = "[*BuildStarted*] %s into %s";
    public static final String BUILD_FINISH_MARKER = "[*BuildFinished*] %s into %s";

    public static final String BUILD_START_REGEX = "\\[\\*BuildStarted\\*\\] ([0-9a-fA-F]+) into ([0-9a-fA-F]+)";
    public static final String BUILD_FINISH_REGEX = "\\[\\*BuildFinished\\*\\] ([0-9a-fA-F]+) into ([0-9a-fA-F]+)";

    public static final String BUILD_FINISH_SENTENCE = BUILD_FINISH_MARKER + " \n\n**%s**\n\n%s\n\n%s";
    public static final String BUILD_REQUEST_MARKER = "test this please";

    public static final String BUILD_SUCCESS_COMMENT =  ":star:SUCCESS";
    public static final String BUILD_FAILURE_COMMENT = ":x:FAILURE";
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
            String comment = String.format(BUILD_START_MARKER, sourceCommit, destinationCommit);
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

    public void postFinishedComment(String pullRequestId, String sourceCommit,  String destinationCommit, boolean success, AbstractBuild build) {
        String rootUrl = Jenkins.getInstance().getRootUrl();
        String commentFilePath = this.trigger.getCommentFilePath();
        String buildUrl = "";
        String buildStatus = BUILD_FAILURE_COMMENT;

        if (rootUrl == null) {
            buildUrl = " PLEASE SET JENKINS ROOT URL FROM GLOBAL CONFIGURATION " + build.getUrl();
        }
        else {
            buildUrl = rootUrl + build.getUrl();
        }

        StringBuilder message = new StringBuilder();

        if (success){
            buildStatus = BUILD_SUCCESS_COMMENT;
        }

        if (commentFilePath != null && commentFilePath != "") {
            Map<String, String> scriptPathExecutionEnvVars = new HashMap<String, String>();
                try {
                    scriptPathExecutionEnvVars.putAll(build.getCharacteristicEnvVars());
                    scriptPathExecutionEnvVars.putAll(build.getBuildVariables());
                    scriptPathExecutionEnvVars.putAll(build.getEnvironment(new LogTaskListener(logger, Level.INFO)));

                    String scriptFilePathResolved = Util.replaceMacro(commentFilePath, scriptPathExecutionEnvVars);

                    String content = FileUtils.readFileToString(new File(scriptFilePathResolved));
                    message.append("# Build comment file #\n\n");
                    message.append(content);
                    message.append("\n");
                 } catch (Exception e) {
                     message.append("# Couldn't read commit file #\n\n");
                     logger.log(Level.SEVERE, "Couldn't read comment file: ", e);
                 }
        }

        String comment = String.format(BUILD_FINISH_SENTENCE, sourceCommit, destinationCommit, buildStatus, message, buildUrl);

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
                    Matcher startMatcher = Pattern.compile(BUILD_START_REGEX, Pattern.CASE_INSENSITIVE).matcher(content);
                    Matcher finishMatcher = Pattern.compile(BUILD_FINISH_REGEX, Pattern.CASE_INSENSITIVE).matcher(content);

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
