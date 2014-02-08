package bitbucketpullrequestbuilder.bitbucketpullrequestbuilder;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

import javax.annotation.Nonnull;
import java.util.logging.Logger;

/**
 * Created by nishio
 */
@Extension
public class BitbucketBuildListener extends RunListener<AbstractBuild> {
    private static final Logger logger = Logger.getLogger(BitbucketBuildTrigger.class.getName());

    @Override
    public void onStarted(AbstractBuild abstractBuild, TaskListener listener) {
        logger.info("BuildListener onStarted called.");
        BitbucketBuildTrigger trigger = BitbucketBuildTrigger.getTrigger(abstractBuild.getProject());
        if (trigger == null) {
            return;
        }
        trigger.getBuilder().getBuilds().onStarted(abstractBuild);
    }

    @Override
    public void onCompleted(AbstractBuild abstractBuild, @Nonnull TaskListener listener) {
        BitbucketBuildTrigger trigger = BitbucketBuildTrigger.getTrigger(abstractBuild.getProject());
        if (trigger == null) {
            return;
        }
        trigger.getBuilder().getBuilds().onCompleted(abstractBuild);
    }
}
