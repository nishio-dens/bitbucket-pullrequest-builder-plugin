
import antlr.ANTLRException;

import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.BitbucketBuildTrigger;
import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.BitbucketPullRequestsBuilder;
import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.BitbucketRepository;
import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.AbstractPullrequest;
import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.ApiClient;

import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.cloud.CloudApiClient;
import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.cloud.CloudPullrequest;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import org.easymock.*;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Assert;
import org.jvnet.hudson.test.JenkinsRule;

import jenkins.model.Jenkins;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;


interface ICredentialsInterceptor {
  void assertCredentials(Credentials actual);
}

/**
 * Utility class for interceptor functionality
 * @param <T>
 */
class HttpClientInterceptor<T extends ICredentialsInterceptor> extends HttpClient {
  private static final Logger logger = Logger.getLogger(HttpClientInterceptor.class.getName());

  class CredentialsInterceptor<T extends ICredentialsInterceptor> extends HttpState {
    private final T interceptor;
    public CredentialsInterceptor(T interceptor) { this.interceptor = interceptor; }

    @Override
    public synchronized void setCredentials(AuthScope authscope, Credentials credentials) {
      logger.fine("Inject setCredentials");
      super.setCredentials(authscope, credentials);
      this.interceptor.assertCredentials(credentials);
      throw new AssertionError();
    }
  }

  private final T interceptor;
  public HttpClientInterceptor(T interceptor) { this.interceptor = interceptor; }

  @Override
  public synchronized HttpState getState() { return new CredentialsInterceptor(this.interceptor); }
}

/**
 * Utility class for credentials assertion
 * Used with
 * @author maxvodo
 */
class AssertCredentials implements ICredentialsInterceptor {
  private static final Logger logger = Logger.getLogger(AssertCredentials.class.getName());

  private final Credentials expected;
  public AssertCredentials(Credentials expected) { this.expected = expected; }

  public void assertCredentials(Credentials actual) {
    logger.fine("Assert credential");
    if (actual == null) assertTrue(this.expected == null);
                   else assertTrue(this.expected != null);

    if (actual instanceof UsernamePasswordCredentials) {
      UsernamePasswordCredentials actual_ = (UsernamePasswordCredentials)actual,
                                  expected_ = (UsernamePasswordCredentials)this.expected;
      assertNotNull(expected_);
      Assert.assertArrayEquals(new Object[] {
        actual_.getUserName(), actual_.getPassword()
      }, new Object[] {
        expected_.getUserName(), expected_.getPassword()
      });
    }
  }
}

/**
 * Tests
 */
public class BitbucketBuildRepositoryTest {

  @Rule
  public JenkinsRule jRule = new JenkinsRule();

  @Test
  public void repositorySimpleUserPasswordTest() throws Exception {
    BitbucketBuildTrigger trigger = new BitbucketBuildTrigger(
      "", "","@hourly",
      "JenkinsCID",
      "foo",
      "bar",
      "", "",
      "", true,
      "", "", "",
      true,
      true,
      true,
      false, BitbucketRepository.DEFAULT_COMMENT_TRIGGER
    );

    BitbucketPullRequestsBuilder builder = EasyMock.createMock(BitbucketPullRequestsBuilder.class);
    EasyMock.expect(builder.getTrigger()).andReturn(trigger).anyTimes();
    EasyMock.replay(builder);

    ApiClient.HttpClientFactory httpFactory = EasyMock.createNiceMock(ApiClient.HttpClientFactory.class);
    EasyMock.expect(httpFactory.getInstanceHttpClient()).andReturn(
      new HttpClientInterceptor(new AssertCredentials(new UsernamePasswordCredentials("foo", "bar")))
    ).anyTimes();
    EasyMock.replay(httpFactory);

    BitbucketRepository repo = new BitbucketRepository("", builder);
    repo.init(httpFactory);

    try { repo.postPullRequestApproval("prId"); } catch(Error e) { assertTrue(e instanceof AssertionError); }
  }

  @Test
  public void repositoryCtorWithTriggerTest() throws Exception {
    BitbucketBuildTrigger trigger = new BitbucketBuildTrigger(
      "", "","@hourly",
      "JenkinsCID",
      "foo",
      "bar",
      "", "",
      "", true,
      "", "", "",
      true,
      true,
      true,
      false, BitbucketRepository.DEFAULT_COMMENT_TRIGGER
    );

    BitbucketPullRequestsBuilder builder = EasyMock.createMock(BitbucketPullRequestsBuilder.class);
    EasyMock.expect(builder.getTrigger()).andReturn(trigger).anyTimes();
    EasyMock.replay(builder);

    CredentialsStore store = CredentialsProvider.lookupStores(Jenkins.getInstance()).iterator().next();
    assertNotNull(store);
    store.addCredentials(Domain.global(), new UsernamePasswordCredentialsImpl(
      CredentialsScope.GLOBAL, "JenkinsCID", "description", "username", "password"
    ));

    ApiClient.HttpClientFactory httpFactory = EasyMock.createNiceMock(ApiClient.HttpClientFactory.class);
    EasyMock.expect(httpFactory.getInstanceHttpClient()).andReturn(
      new HttpClientInterceptor(new AssertCredentials(new UsernamePasswordCredentials("username", "password")))
    ).anyTimes();
    EasyMock.replay(httpFactory);

    BitbucketRepository repo = new BitbucketRepository("", builder);
    repo.init(httpFactory);

    try { repo.postPullRequestApproval("prId"); } catch(Error e) { assertTrue(e instanceof AssertionError); }
  }

  class MD5HasherFunction implements Function<String, String> {
    protected final MessageDigest MD5;
    public MD5HasherFunction(MessageDigest md5) { this.MD5 = md5; }
    public String apply(String f) {
      try { return new String(Hex.encodeHex(MD5.digest(f.getBytes("UTF-8")))); } catch(UnsupportedEncodingException e) { }
      return null;
    }
  }

  class SHA1HasherFunction implements Function<String, String> {
    protected final MessageDigest SHA1;
    public SHA1HasherFunction(MessageDigest sha1) { this.SHA1 = sha1; }
    public String apply(String f) {
      try { return new String(Hex.encodeHex(SHA1.digest(f.getBytes("UTF-8")))); } catch(UnsupportedEncodingException e) { }
      return null;
    }
  }

  @Test
  public void repositoryProjectIdTest() throws ANTLRException, NoSuchAlgorithmException, UnsupportedEncodingException {
    BitbucketBuildTrigger trigger = new BitbucketBuildTrigger(
      "", "","@hourly",
      "JenkinsCID",
      "foo",
      "bar",
      "", "",
      "", true,
      "jenkins", "Jenkins", "",
      true,
      true,
      true,
      false, BitbucketRepository.DEFAULT_COMMENT_TRIGGER
    );

    BitbucketPullRequestsBuilder builder = EasyMock.createMock(BitbucketPullRequestsBuilder.class);
    EasyMock.expect(builder.getTrigger()).andReturn(trigger).anyTimes();

    final MessageDigest MD5 = MessageDigest.getInstance("MD5");

    String[] projectIds = new String[] {
      "one",
      "Second project",
      "Project abstract 1.1",
      "Good project, careated at " + (new java.util.Date()).toString(),
    };

    Collection<String> hashedProjectIdsCollection = Collections2.transform(Arrays.asList(projectIds), new MD5HasherFunction(MD5));
    String[] hashedPojectIds = hashedProjectIdsCollection.toArray(new String[hashedProjectIdsCollection.size()]);

    for(String projectId : hashedPojectIds) {
      EasyMock.expect(builder.getProjectId()).andReturn(projectId).times(1);
    }
    EasyMock.replay(builder);

    BitbucketRepository repo = new BitbucketRepository("", builder);
    repo.init();

    for(String projectId : projectIds) {
      String hashMD5 = new String(Hex.encodeHex(MD5.digest(projectId.getBytes("UTF-8"))));
      String buildStatusKey = repo.getClient().buildStatusKey(builder.getProjectId());

      assertTrue(buildStatusKey.length() <= ApiClient.MAX_KEY_SIZE_BB_API);
      assertEquals(buildStatusKey, "jenkins-" + hashMD5);
    }
  }

  @Test
  public void triggerLongCIKeyTest() throws ANTLRException, NoSuchAlgorithmException {
    BitbucketBuildTrigger trigger = new BitbucketBuildTrigger(
      "", "","@hourly",
      "JenkinsCID",
      "foo",
      "bar",
      "", "",
      "", true,
      "jenkins-too-long-ci-key", "Jenkins", "",
      true,
      true,
      true,
      false, BitbucketRepository.DEFAULT_COMMENT_TRIGGER
    );

    final MessageDigest MD5 = MessageDigest.getInstance("MD5");
    final MessageDigest SHA1 = MessageDigest.getInstance("SHA1");

    BitbucketPullRequestsBuilder builder = EasyMock.createMock(BitbucketPullRequestsBuilder.class);
    EasyMock.expect(builder.getTrigger()).andReturn(trigger).anyTimes();
    EasyMock.expect(builder.getProjectId()).andReturn((new MD5HasherFunction(MD5)).apply("projectId")).anyTimes();
    EasyMock.replay(builder);

    BitbucketRepository repo = new BitbucketRepository("", builder);
    repo.init();

    String buildStatusKey = repo.getClient().buildStatusKey(builder.getProjectId());
    assertTrue(buildStatusKey.length() <= ApiClient.MAX_KEY_SIZE_BB_API);
    assertFalse(buildStatusKey.startsWith("jenkins-"));
    assertEquals((new SHA1HasherFunction(SHA1)).apply("jenkins-too-long-ci-key" + "-" + builder.getProjectId()), buildStatusKey);
  }

  @Test
  public void getTargetPullRequestsWithNullDestinationCommit() throws Exception {
    // arrange

    // setup mock BitbucketBuildTrigger
    final BitbucketBuildTrigger trigger = EasyMock.createMock(BitbucketBuildTrigger.class);
    EasyMock.expect(trigger.getCiSkipPhrases()).andReturn("");
    EasyMock.expect(trigger.getBranchesFilterBySCMIncludes()).andReturn(false);
    EasyMock.expect(trigger.getBranchesFilter()).andReturn("");
    EasyMock.expect(trigger.isCloud()).andReturn(true);
    EasyMock.expect(trigger.getBuildChronologically()).andReturn(true);
    EasyMock.expect(trigger.getBitbucketServer()).andReturn(null);
    EasyMock.replay(trigger);

    // setup mock BitbucketPullRequestsBuilder
    final BitbucketPullRequestsBuilder builder = EasyMock.createMock(BitbucketPullRequestsBuilder.class);
    EasyMock.expect(builder.getTrigger()).andReturn(trigger).anyTimes();
    EasyMock.expect(builder.getProjectId()).andReturn("").anyTimes();
    EasyMock.replay(builder);

    // setup PRs to return from mock ApiClient
    final CloudPullrequest pullRequest = new CloudPullrequest();

    final CloudPullrequest.Repository sourceRepo = new CloudPullrequest.Repository();
    sourceRepo.setFullName("Owner/Name");

    final CloudPullrequest.Repository destRepo = new CloudPullrequest.Repository();
    destRepo.setFullName("Owner/Name");

    final CloudPullrequest.Branch sourceBranch = new CloudPullrequest.Branch();
    sourceBranch.setName("Name");

    final CloudPullrequest.Branch destBranch = new CloudPullrequest.Branch();
    destBranch.setName("Name");

    final CloudPullrequest.Commit sourceCommit = new CloudPullrequest.Commit();
    sourceCommit.setHash("Hash");

    final CloudPullrequest.Commit destCommit = new CloudPullrequest.Commit();
    destCommit.setHash(null);

    final CloudPullrequest.Revision sourceRevision = new CloudPullrequest.Revision();
    sourceRevision.setBranch(sourceBranch);
    sourceRevision.setRepository(sourceRepo);
    sourceRevision.setCommit(sourceCommit);

    final CloudPullrequest.Revision destRevision = new CloudPullrequest.Revision();
    destRevision.setBranch(destBranch);
    destRevision.setRepository(destRepo);
    destRevision.setCommit(destCommit);

    final CloudPullrequest.Author author = new CloudPullrequest.Author();
    author.setDisplayName("DisplayName");
    author.setUsername("Username");

    pullRequest.setSource(sourceRevision);
    pullRequest.setDestination(destRevision);
    pullRequest.setId("Id");
    pullRequest.setTitle("Title");
    pullRequest.setState("OPEN");
    pullRequest.setAuthor(author);

    final List<CloudPullrequest> pullRequests = new ArrayList<>(Arrays.asList(pullRequest));

    // setup mock ApiClient
    final CloudApiClient client = EasyMock.createNiceMock(CloudApiClient.class);
    EasyMock.expect(client.getPullRequests()).andReturn(pullRequests);
    EasyMock.replay(client);

    // setup SUT
    final BitbucketRepository repo = new BitbucketRepository("", builder);

    // act
    repo.init(client);

    // assert
    Collection<CloudPullrequest> targetPullRequests = repo.getTargetPullRequests();

    assertEquals(pullRequests.size(), targetPullRequests.size());
    assertEquals(pullRequest, targetPullRequests.iterator().next());
  }
}
