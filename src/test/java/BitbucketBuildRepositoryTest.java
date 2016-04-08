
import antlr.ANTLRException;

import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.BitbucketBuildTrigger;
import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.BitbucketPullRequestsBuilder;
import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.BitbucketRepository;
import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.ApiClient;

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
import java.util.Arrays;
import java.util.Collection;
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
      logger.info("Inject setCredentials");
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
    logger.info("Assert credential");
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
      "", "@hourly",
      "JenkinsCID",
      "foo",
      "bar",
      "", "",
      "", true,
      "", "", "",
      true, 
      true
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
      "", "@hourly",
      "JenkinsCID",
      "foo",
      "bar",
      "", "",
      "", true,
      "", "", "",
      true, 
      true
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
      "", "@hourly",
      "JenkinsCID",
      "foo",
      "bar",
      "", "",
      "", true,
      "jenkins", "Jenkins", "",
      true, 
      true
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

    BitbucketRepository repo = new BitbucketRepository("", builder);
    repo.init();       
    
    for(String projectId : projectIds) {
      String hashMD5 = new String(Hex.encodeHex(MD5.digest(projectId.getBytes("UTF-8"))));
      String buildStatusKey = repo.getClient().buildStatusKey(repo.getKeyPart());
      
      assertTrue(buildStatusKey.length() <= ApiClient.MAX_KEY_SIZE_BB_API);
      assertEquals(buildStatusKey, "jenkins-" + hashMD5);
    }
  }
}
