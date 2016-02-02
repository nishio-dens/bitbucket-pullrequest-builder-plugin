
import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.BitbucketBuildTrigger;
import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.BitbucketPullRequestsBuilder;
import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.BitbucketRepository;
import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.ApiClient;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import org.easymock.*;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;
import jenkins.model.Jenkins;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.junit.Assert;


interface ICredentialsInterceptor {
  void assertCredentials(Credentials actual);
}

/**
 * Utility class for interceptor functionality
 * @param <T> 
 */
class HttpClientInterceptor<T extends ICredentialsInterceptor> extends HttpClient {  
  
  class CredentialsInterceptor<T extends ICredentialsInterceptor> extends HttpState {
    private final T interceptor;
    public CredentialsInterceptor(T interceptor) { this.interceptor = interceptor; }
    
    @Override
    public synchronized void setCredentials(AuthScope authscope, Credentials credentials) {      
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
  private final Credentials expected;
  public AssertCredentials(Credentials expected) { this.expected = expected; }

  public void assertCredentials(Credentials actual) {
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
    
    ApiClient.HttpClientFactory httpFactory = EasyMock.createMock(ApiClient.HttpClientFactory.class);
    EasyMock.expect(httpFactory.getInstanceHttpClient()).andReturn(
      new HttpClientInterceptor(new AssertCredentials(new UsernamePasswordCredentials("foo", "bar")))
    ).anyTimes();
    EasyMock.replay(httpFactory);            
    
    BitbucketRepository repo = new BitbucketRepository("", builder);
    repo.init(httpFactory.getClass());
    
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
    
    ApiClient.HttpClientFactory httpFactory = EasyMock.createMock(ApiClient.HttpClientFactory.class);
    EasyMock.expect(httpFactory.getInstanceHttpClient()).andReturn(
      new HttpClientInterceptor(new AssertCredentials(new UsernamePasswordCredentials("username", "password")))
    ).anyTimes();
    EasyMock.replay(httpFactory);  
    
    BitbucketRepository repo = new BitbucketRepository("", builder);
    repo.init(httpFactory.getClass());        
    
    try { repo.postPullRequestApproval("prId"); } catch(Error e) { assertTrue(e instanceof AssertionError); }                
  }
}
