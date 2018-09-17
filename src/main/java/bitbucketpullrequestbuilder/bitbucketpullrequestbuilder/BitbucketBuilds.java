package bitbucketpullrequestbuilder.bitbucketpullrequestbuilder;

import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.BuildState;
import hudson.model.*;
import jenkins.model.JenkinsLocationConfiguration;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by nishio
 */
public class BitbucketBuilds {
    private static final Logger logger = Logger.getLogger(BitbucketBuilds.class.getName());
    private BitbucketBuildTrigger trigger;
    private BitbucketRepository repository;

    public BitbucketBuilds(BitbucketBuildTrigger trigger, BitbucketRepository repository) {
        this.trigger = trigger;
        this.repository = repository;
        this.repository.init();
    }

    void onStarted(BitbucketCause cause, Run<?, ?> build) {
        if (cause == null) {
            return;
        }
        try {
            build.setDescription(cause.getShortDescription());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Can't update build description", e);
        }
    }

    void onCompleted(BitbucketCause cause, Result result, String buildUrl) {
        if (cause == null) {
            return;
        }
        JenkinsLocationConfiguration globalConfig = JenkinsLocationConfiguration.get();
        if (null!= globalConfig && globalConfig.getUrl() == null) {
            logger.warning("PLEASE SET JENKINS ROOT URL IN GLOBAL CONFIGURATION FOR BUILD STATE REPORTING");
        } else if (null!= globalConfig){
            buildUrl = globalConfig.getUrl() + buildUrl;
            BuildState state = result == Result.SUCCESS ? BuildState.SUCCESSFUL : BuildState.FAILED;
            repository.setBuildStatus(cause, state, buildUrl);
        }

        if (this.trigger.getApproveIfSuccess() && result == Result.SUCCESS) {
            this.repository.postPullRequestApproval(cause.getPullRequestId());
        }
        if(this.trigger.getDeclineIfFailure() && result == Result.FAILURE){
            this.repository.postPullRequestDecline(cause.getPullRequestId());
        }
    }
}
