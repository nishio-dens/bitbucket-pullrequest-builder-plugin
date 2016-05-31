package bitbucketpullrequestbuilder.bitbucketpullrequestbuilder;

import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.Pullrequest;
import hudson.model.AbstractProject;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Hex;

/**
 * Created by nishio
 */
public class BitbucketPullRequestsBuilder {
    private static final Logger logger = Logger.getLogger(BitbucketBuildTrigger.class.getName());
    private AbstractProject<?, ?> project;
    private BitbucketBuildTrigger trigger;
    private BitbucketRepository repository;
    private BitbucketBuilds builds;

    public static BitbucketPullRequestsBuilder getBuilder() {
        return new BitbucketPullRequestsBuilder();
    }

    public void stop() {
        // TODO?
    }

    public void run() {
        logger.info("Build Start.");
        this.repository.init();
        Collection<Pullrequest> targetPullRequests = this.repository.getTargetPullRequests();
        this.repository.addFutureBuildTasks(targetPullRequests);
    }

    public BitbucketPullRequestsBuilder setupBuilder() {
        if (this.project == null || this.trigger == null) {
            throw new IllegalStateException();
        }
        this.repository = new BitbucketRepository(this.trigger.getProjectPath(), this);
        this.builds = new BitbucketBuilds(this.trigger, this.repository);
        return this;
    }

    public void setProject(AbstractProject<?, ?> project) {
        this.project = project;
    }

    public void setTrigger(BitbucketBuildTrigger trigger) {
        this.trigger = trigger;
    }

    public AbstractProject<?, ?> getProject() {
        return this.project;
    }

    /**
     * Return MD5 hashed full project name or full project name, if MD5 hash provider inaccessible
     * @return unique project id
     */
    public String getProjectId() {
      try {
        final MessageDigest MD5 = MessageDigest.getInstance("MD5");
        return new String(Hex.encodeHex(MD5.digest(this.project.getFullName().getBytes("UTF-8"))));
      } catch (NoSuchAlgorithmException exc) {
        logger.log(Level.WARNING, "Failed to produce hash", exc);
        exc.printStackTrace();
      } catch (UnsupportedEncodingException exc) {
        logger.log(Level.WARNING, "Failed to produce hash", exc);
        exc.printStackTrace();
      }
      return this.project.getFullName();
    }

    public BitbucketBuildTrigger getTrigger() {
        return this.trigger;
    }

    public BitbucketBuilds getBuilds() {
        return this.builds;
    }
}
