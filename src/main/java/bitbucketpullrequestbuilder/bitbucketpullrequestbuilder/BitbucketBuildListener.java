package bitbucketpullrequestbuilder.bitbucketpullrequestbuilder;

import hudson.Extension;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.triggers.Trigger;
import jenkins.model.ParameterizedJobMixIn;

import javax.annotation.Nonnull;
import java.util.logging.Logger;

/**
 * Created by nishio
 */
@Extension
public class BitbucketBuildListener extends RunListener<Run<?, ?>> {
    private static final Logger logger = Logger.getLogger(BitbucketBuildListener.class.getName());

    @Override
    public void onStarted(Run<?,?> r, TaskListener listener) {
        logger.fine("BitbucketBuildListener onStarted called.");
        BitbucketBuilds builds = builds(r);
        if (builds != null) {
            builds.onStarted((BitbucketCause) r.getCause(BitbucketCause.class), r);
        }
    }

    @Override
    public void onCompleted(Run<?,?> r, @Nonnull TaskListener listener) {
        logger.fine("BitbucketBuildListener onCompleted called.");
        BitbucketBuilds builds = builds(r);
        if (builds != null) {
            builds.onCompleted((BitbucketCause) r.getCause(BitbucketCause.class), r.getResult(), r.getUrl());
        }
    }

    private BitbucketBuilds builds(Run<?, ?> r) {
        BitbucketBuildTrigger trigger = null;

        Job<?,?> job = r.getParent();
        if (job instanceof ParameterizedJobMixIn.ParameterizedJob) {
            for (Trigger<?> t : ((ParameterizedJobMixIn.ParameterizedJob) job).getTriggers().values()) {
                if (t instanceof BitbucketBuildTrigger) {
                    trigger = (BitbucketBuildTrigger) t;
                }
            }
        }

        return trigger == null ? null : trigger.getBuilder().getBuilds();
    }
}
