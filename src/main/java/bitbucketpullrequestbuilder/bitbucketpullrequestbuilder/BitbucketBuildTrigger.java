package bitbucketpullrequestbuilder.bitbucketpullrequestbuilder;

import static com.cloudbees.plugins.credentials.CredentialsMatchers.instanceOf;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.transport.URIish;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import antlr.ANTLRException;
import hudson.Extension;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.Executor;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.queue.QueueTaskFuture;
import hudson.plugins.git.RevisionParameterAction;
import hudson.security.ACL;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import net.sf.json.JSONObject;

/**
 * Created by nishio
 */
public class BitbucketBuildTrigger extends Trigger<Job<?, ?>> {
    private static final Logger logger = Logger.getLogger(BitbucketBuildTrigger.class.getName());
    private static final ExecutorService pool = Executors.newFixedThreadPool(5);

    private final String projectPath;
    private final String bitbucketServer;
    private final String cron;
    private final String credentialsId;
    private final String username;
    private final String password;
    private final String repositoryOwner;
    private final String repositoryName;
    private final String branchesFilter;
    private final boolean branchesFilterBySCMIncludes;
    private final String ciKey;
    private final String ciName;
    private final String ciSkipPhrases;
    private final boolean checkDestinationCommit;
    private final boolean approveIfSuccess;
    private final boolean cancelOutdatedJobs;
    private final String commentTrigger;

    transient private BitbucketPullRequestsBuilder bitbucketPullRequestsBuilder;

    public static final BitbucketBuildTriggerDescriptor descriptor = new BitbucketBuildTriggerDescriptor();

    @DataBoundConstructor
    public BitbucketBuildTrigger(
            String projectPath,
            String bitbucketServer,
            String cron,
            String credentialsId,
            String username,
            String password,
            String repositoryOwner,
            String repositoryName,
            String branchesFilter,
            boolean branchesFilterBySCMIncludes,
            String ciKey,
            String ciName,
            String ciSkipPhrases,
            boolean checkDestinationCommit,
            boolean approveIfSuccess,
            boolean cancelOutdatedJobs,
            String commentTrigger
            ) throws ANTLRException {
        super(cron);
        this.projectPath = projectPath;
        this.bitbucketServer = bitbucketServer;
        this.cron = cron;
        this.credentialsId = credentialsId;
        this.username = username;
        this.password = password;
        this.repositoryOwner = repositoryOwner;
        this.repositoryName = repositoryName;
        this.branchesFilter = branchesFilter;
        this.branchesFilterBySCMIncludes = branchesFilterBySCMIncludes;
        this.ciKey = ciKey;
        this.ciName = ciName;
        this.ciSkipPhrases = ciSkipPhrases;
        this.checkDestinationCommit = checkDestinationCommit;
        this.approveIfSuccess = approveIfSuccess;
        this.cancelOutdatedJobs = cancelOutdatedJobs;
        this.commentTrigger = commentTrigger;
    }

    public String getProjectPath() {
        return this.projectPath;
    }

    public String getBitbucketServer() {
        return bitbucketServer;
    }

    public String getCron() {
        return this.cron;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getRepositoryOwner() {
        return repositoryOwner;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public String getBranchesFilter() {
        return branchesFilter;
    }

    public boolean getBranchesFilterBySCMIncludes() {
      return branchesFilterBySCMIncludes;
    }

    public String getCiKey() {
        return ciKey;
    }

    public String getCiName() {
        return ciName;
    }

    public String getCiSkipPhrases() {
        return ciSkipPhrases;
    }

    public boolean getCheckDestinationCommit() {
    	return checkDestinationCommit;
    }

    public boolean getApproveIfSuccess() {
        return approveIfSuccess;
    }

    public boolean getCancelOutdatedJobs() {
        return cancelOutdatedJobs;
    }

    /**
     * @return a phrase that when entered in a comment will trigger a new build
     */
    public String getCommentTrigger() {
        return commentTrigger;
    }

    @Override
    public void start(Job<?, ?> job, boolean newInstance) {
        super.start(job, newInstance);

        try {
            this.bitbucketPullRequestsBuilder = BitbucketPullRequestsBuilder.getBuilder();
            this.bitbucketPullRequestsBuilder.setJob(job);
            this.bitbucketPullRequestsBuilder.setTrigger(this);
            this.bitbucketPullRequestsBuilder.setupBuilder();
        } catch(Exception e) {
            logger.log(Level.SEVERE, "Can't start trigger", e);
            return;
        }
    }

    public static BitbucketBuildTrigger getTrigger(Job<?, ?> job) {
        if (!(job instanceof ParameterizedJobMixIn.ParameterizedJob)) {
            return null;
        }

        ParameterizedJobMixIn.ParameterizedJob pjob = (ParameterizedJobMixIn.ParameterizedJob) job;

        Trigger<?> trigger = pjob.getTriggers().get(descriptor);
        return (BitbucketBuildTrigger)trigger;
    }

    public BitbucketPullRequestsBuilder getBuilder() {
        return this.bitbucketPullRequestsBuilder;
    }

    public QueueTaskFuture<?> startJob(BitbucketCause cause) {
        Map<String, ParameterValue> values = this.getDefaultParameters();

        if (getCancelOutdatedJobs()) {
            cancelPreviousJobsInQueueThatMatch(cause);
            abortRunningJobsThatMatch(cause);
        }

        ParameterizedJobMixIn scheduledJob = new ParameterizedJobMixIn() {
            @Override
            protected Job asJob() {
                return job;
            }
        };

        return scheduledJob.scheduleBuild2(
            this.getInstance().getQuietPeriod(),
            new CauseAction(cause),
            new ParametersAction(new ArrayList<ParameterValue>(values.values())),
            new RevisionParameterAction(cause.getSourceCommitHash())
        );
    }

    private URIish getBitbucketRepoUrl(String repoOwner, String repoName) {
        try{
            return new URIish(String.format("git@bitbucket.org:%s/%s.git", repoOwner, repoName));
        } catch (URISyntaxException e) {
            logger.log(Level.SEVERE, "Unable to create URIish for bitbucket url, checking out the pull request branch may fail.", e);
            return null;
        }
    }

    private void cancelPreviousJobsInQueueThatMatch(@Nonnull BitbucketCause bitbucketCause) {
        logger.fine("Looking for queued jobs that match PR ID: " + bitbucketCause.getPullRequestId());
        Queue queue = getInstance().getQueue();

        for (Queue.Item item : queue.getItems()) {
            if (hasCauseFromTheSamePullRequest(item.getCauses(), bitbucketCause)) {
                logger.fine("Canceling item in queue: " + item);
                queue.cancel(item);
            }
        }
    }

    private Jenkins getInstance() {
        final Jenkins instance = Jenkins.getInstance();
        if (instance == null){
            throw new IllegalStateException("Jenkins instance is NULL!");
        }
        return instance;
    }

    private void abortRunningJobsThatMatch(@Nonnull BitbucketCause bitbucketCause) {
        logger.fine("Looking for running jobs that match PR ID: " + bitbucketCause.getPullRequestId());
        for (Object o : job.getBuilds()) {
            if (o instanceof Run) {
                Run<?,?> build = (Run<?,?>) o;
                if (build.isBuilding() && hasCauseFromTheSamePullRequest(build.getCauses(), bitbucketCause)) {
                    logger.fine("Aborting build: " + build + " since PR is outdated");
                    setBuildDescription(build);
                    final Executor executor = build.getExecutor();
                    if (executor == null){
                        throw new IllegalStateException("Executor can't be NULL");
                    }
                    executor.interrupt(Result.ABORTED);
                }
            }
        }
    }

    private void setBuildDescription(final Run<?,?> build) {
        try {
            build.setDescription("Aborting build by `Bitbucket Pullrequest Builder Plugin`: " + build + " since PR is outdated");
        } catch (IOException e) {
            logger.warning("Can't set up build description due to an IOException: " + e.getMessage());
        }
    }

    private boolean hasCauseFromTheSamePullRequest(@Nullable List<Cause> causes, @Nullable BitbucketCause pullRequestCause) {
        if (causes != null && pullRequestCause != null) {
            for (Cause cause : causes) {
                if (cause instanceof BitbucketCause) {
                    BitbucketCause sc = (BitbucketCause) cause;
                    if (StringUtils.equals(sc.getPullRequestId(), pullRequestCause.getPullRequestId()) &&
                            StringUtils.equals(sc.getRepositoryName(), pullRequestCause.getRepositoryName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private Map<String, ParameterValue> getDefaultParameters() {
        Map<String, ParameterValue> values = new HashMap<String, ParameterValue>();
        ParametersDefinitionProperty definitionProperty = this.job.getProperty(ParametersDefinitionProperty.class);

        if (definitionProperty != null) {
            for (ParameterDefinition definition : definitionProperty.getParameterDefinitions()) {
                values.put(definition.getName(), definition.getDefaultParameterValue());
            }
        }
        return values;
    }

    @Override
    public void run() {
        Job<?,?> job = this.getBuilder().getJob();
        String name = job.getFullName();

        if (!job.isBuildable()) {
            logger.log(Level.FINE, "Build Skip for job - {0}.", name);
        } else {
            logger.log(Level.FINE, "running trigger for the job - {0}", name);

            pool.submit(new TriggerRunnable(this.getBuilder()));
            this.getDescriptor().save();
        }
    }

    @Override
    public void stop() {
        super.stop();
    }

    public boolean isCloud() {
        return StringUtils.isEmpty(bitbucketServer);
    }

    @Extension
    @Symbol("bitbucketpr")
    public static final class BitbucketBuildTriggerDescriptor extends TriggerDescriptor {
        public BitbucketBuildTriggerDescriptor() {
            load();
        }

        @Override
        public boolean isApplicable(Item item) {
            return item instanceof Job && item instanceof ParameterizedJobMixIn.ParameterizedJob;
        }

        @Override
        public String getDisplayName() {
            return "Bitbucket Pull Requests Builder";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            save();
            return super.configure(req, json);
        }

        public ListBoxModel doFillCredentialsIdItems() {
            return new StandardListBoxModel()
                .withEmptySelection()
                .withMatching(
                    instanceOf(UsernamePasswordCredentials.class),
                    CredentialsProvider.lookupCredentials(
                        StandardUsernamePasswordCredentials.class,
                        (Item) null,
                        ACL.SYSTEM,
                        (DomainRequirement) null
                    )
                );
        }
    }

    private static final class TriggerRunnable implements Runnable {
        private final BitbucketPullRequestsBuilder builder;

        TriggerRunnable(BitbucketPullRequestsBuilder builder) {
            this.builder = builder;
        }

        @Override
        public void run() {
            synchronized (this) {
                this.builder.run();
            }
        }
    }
}
