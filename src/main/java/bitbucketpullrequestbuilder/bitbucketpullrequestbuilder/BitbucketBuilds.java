package bitbucketpullrequestbuilder.bitbucketpullrequestbuilder;

import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.BuildState;
import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.model.Result;
import jenkins.model.Jenkins;
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
    }

    public BitbucketCause getCause(AbstractBuild build) {
        Cause cause = build.getCause(BitbucketCause.class);
        if (cause == null || !(cause instanceof BitbucketCause)) {
            return null;
        }
        return (BitbucketCause) cause;
    }

    public void onStarted(AbstractBuild build) {
        BitbucketCause cause = this.getCause(build);
        if (cause == null) {
            return;
        }
        try {
            build.setDescription(cause.getShortDescription());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Can't update build description", e);
        }
    }

    public void onCompleted(AbstractBuild build) {
        BitbucketCause cause = this.getCause(build);
        if (cause == null) {
            return;
        }
        Result result = build.getResult();
        JenkinsLocationConfiguration globalConfig = new JenkinsLocationConfiguration();
        String rootUrl = globalConfig.getUrl();
        String buildUrl = "";
        if (rootUrl == null) {
            logger.warning("PLEASE SET JENKINS ROOT URL IN GLOBAL CONFIGURATION FOR BUILD STATE REPORTING");
        } else {
            buildUrl = rootUrl + build.getUrl();
            BuildState state = result == Result.SUCCESS ? BuildState.SUCCESSFUL : BuildState.FAILED;
            repository.setBuildStatus(cause, state, buildUrl);
        }

        if ( this.trigger.getApproveIfSuccess() && result == Result.SUCCESS ) {
            this.repository.postPullRequestApproval(cause.getPullRequestId());
        }
    }
}
