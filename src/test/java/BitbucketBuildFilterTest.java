/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.BitbucketBuildFilter;
import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.BitbucketCause;
import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.BitbucketPullRequestsBuilder;
import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.BitbucketRepository;
import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.ApiClient;
import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.Pullrequest;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import jenkins.plugins.git.AbstractGitSCMSource;
import org.easymock.*;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;

/**
 *
 * @author maxvodo
 */
public class BitbucketBuildFilterTest {
  
  @Rule
  public JenkinsRule jRule = new JenkinsRule();  
  
  @Test
  @WithoutJenkins
  public void mockTest() {
    BitbucketCause cause = EasyMock.createMock(BitbucketCause.class);
    EasyMock.expect(cause.getTargetBranch()).andReturn("mock").anyTimes();    
    EasyMock.replay(cause);
    for(Integer i : new Integer[] {1, 2, 3, 4, 5}) assertEquals("mock", cause.getTargetBranch());
  }
    
  @Test
  @WithoutJenkins
  public void anyFilter() {
    BitbucketCause cause = EasyMock.createMock(BitbucketCause.class);
    EasyMock.expect(cause.getTargetBranch()).andReturn("master").anyTimes(); 
    EasyMock.replay(cause);
      
    for(String f : new String[] {"", "*", "any"}) {
      BitbucketBuildFilter filter = BitbucketBuildFilter.InstanceByString(f);      
      assertTrue(filter.approved(cause));
    }
    
    for(String f : new String[] {"foo", "bar", " baz "}) {
      BitbucketBuildFilter filter = BitbucketBuildFilter.InstanceByString(f);      
      assertFalse(filter.approved(cause));
    }
  }
  
  @Test
  @WithoutJenkins
  public void onlyDestinationFilter() {
    BitbucketCause cause = EasyMock.createMock(BitbucketCause.class);
    EasyMock.expect(cause.getTargetBranch()).andReturn("master-branch").anyTimes();
    EasyMock.replay(cause);
    
    for(String f : new String[] {"master-branch", "r:^master", "r:branch$", " master-branch "}) {
      BitbucketBuildFilter filter = BitbucketBuildFilter.InstanceByString(f);      
      assertTrue(filter.approved(cause));
    }
    
    for(String f : new String[] {"develop", "feature-good-thing", "r:develop$"}) {
      BitbucketBuildFilter filter = BitbucketBuildFilter.InstanceByString(f);      
      assertFalse(filter.approved(cause));
    }
  }
  
  @Test
  @WithoutJenkins
  public void rxSourceDestCheck() {    
    for(String f : new String[] {"", "master", "r:master", "*"})      
      assertFalse(Pattern.compile("(s:)|(d:)").matcher(f).find());
    
    for(String f : new String[] {"s:master d:feature-master", "s:master d:r:^feature", "s:r:^master d:r:^feature"})
      assertTrue(Pattern.compile("(s:)|(d:)").matcher(f).find());
  }
  
  @Test
  @WithoutJenkins
  public void sourceAndDestFilter() {
    BitbucketCause cause = EasyMock.createMock(BitbucketCause.class);
    EasyMock.expect(cause.getTargetBranch()).andReturn("master").anyTimes();
    EasyMock.expect(cause.getSourceBranch()).andReturn("feature-for-master").anyTimes();
    EasyMock.replay(cause);
    
    for(String f : new String[] {"s:feature-for-master d:master", "s:r:^feature d:master", "s:feature-for-master d:r:^m", "s:r:^feature d:r:^master"}) {
      BitbucketBuildFilter filter = BitbucketBuildFilter.InstanceByString(f);      
      assertTrue(filter.approved(cause));
    }
    
    for(String f : new String[] {"s:feature-for-master d:foo", "s:bar d:master", "s:foo d:bar"}) {
      BitbucketBuildFilter filter = BitbucketBuildFilter.InstanceByString(f);      
      assertFalse(filter.approved(cause));
    }
  }
  
  @Test
  @WithoutJenkins
  public void multipleSrcDestFilter() {
    BitbucketCause cause = EasyMock.createMock(BitbucketCause.class);
    EasyMock.expect(cause.getTargetBranch()).andReturn("master").anyTimes();
    EasyMock.expect(cause.getSourceBranch()).andReturn("feature-master").anyTimes();
    EasyMock.replay(cause);
    
    for(String f : new String[] {"s: d:", "s:r:^feature s:good-branch d:r:.*", "s:good-branch s:feature-master d:r:.*", "s: d:r:.*", "d:master d:foo d:bar s:"}) {
      BitbucketBuildFilter filter = BitbucketBuildFilter.InstanceByString(f);      
      assertTrue(filter.approved(cause));
    }
    
    for(String f : new String[] {"d:ggg d:ooo d:333 s:feature-master", "s:111 s:222 s:333 d:master"}) {
      BitbucketBuildFilter filter = BitbucketBuildFilter.InstanceByString(f);      
      assertFalse(filter.approved(cause));
    }
  }
  
  @Test
  @WithoutJenkins
  public void sourceAndDestPartiallyFilter() {
    BitbucketCause cause = EasyMock.createMock(BitbucketCause.class);
    EasyMock.expect(cause.getTargetBranch()).andReturn("master").anyTimes();
    EasyMock.expect(cause.getSourceBranch()).andReturn("feature-master").anyTimes();
    EasyMock.replay(cause);
    
    for(String f : new String[] {"s:feature-master d:", "d:master s:"}) {
      BitbucketBuildFilter filter = BitbucketBuildFilter.InstanceByString(f);      
      assertTrue(filter.approved(cause));
    }
    
    for(String f : new String[] {"s:feature-master", "d:master"}) {
      BitbucketBuildFilter filter = BitbucketBuildFilter.InstanceByString(f);      
      assertFalse(filter.approved(cause));
    }
  }
  
  @Test
  @WithoutJenkins
  public void emptyGitSCMFilter() {
    BitbucketCause cause = EasyMock.createMock(BitbucketCause.class);
    EasyMock.expect(cause.getTargetBranch()).andReturn("master").anyTimes();
    EasyMock.replay(cause);
    
    assertTrue(BitbucketBuildFilter.FilterFromGitSCMSource(null, "").isEmpty());
    assertEquals("default", BitbucketBuildFilter.FilterFromGitSCMSource(null, "default"));
    
    assertTrue(BitbucketBuildFilter.InstanceByString(
      BitbucketBuildFilter.FilterFromGitSCMSource(null, "")).approved(cause)
    );
  }
  
  @Test
  @WithoutJenkins
  public void fromGitSCMFilter() {
    AbstractGitSCMSource git = EasyMock.createMock(AbstractGitSCMSource.class);
    EasyMock.expect(git.getIncludes())
      .andReturn("").times(1)
      .andReturn("").times(1)      
      .andReturn("*/master */feature-branch").times(1)      
      .andReturn("*/master").anyTimes();
    EasyMock.replay(git);
    
    assertTrue(git.getIncludes().isEmpty());    
    assertEquals("", BitbucketBuildFilter.FilterFromGitSCMSource(git, ""));
    assertEquals("d:master d:feature-branch", BitbucketBuildFilter.FilterFromGitSCMSource(git, ""));
    assertEquals("d:master", BitbucketBuildFilter.FilterFromGitSCMSource(git, ""));
  }
  
  @Test
  @WithoutJenkins
  public void filterPRComments() {
    BitbucketPullRequestsBuilder builder = EasyMock.createMock(BitbucketPullRequestsBuilder.class); 
    EasyMock.expect(builder.getTrigger()).andReturn(null).anyTimes();
    EasyMock.replay(builder);
    
    List<Pullrequest.Comment> comments = new LinkedList<Pullrequest.Comment>();
    for(String commentContent : new String[] { 
      "check",
      "",      
      "Hello from mock",
      "Jenkins: test this please",
      "TTP build flag [bid: #jenkins-902f259e962ff16100843123480a0970]",
      "check",
      "",      
      "Hello from mock",
      "Jenkins: test this please",
      "TTP build flag [bid: #jenkins-902f259e962ff16100843123480a0970]",
      "TTP build flag [bid: #jenkins-902f259e962ff16100843123480a0970 #jenkins-foo]",
      "TTP build flag [bid: #jenkins-902f259e962ff16100843123480a0970 #jenkins-foo #jenkins-bar]",
    }) {
      Pullrequest.Comment comment = EasyMock.createNiceMock(Pullrequest.Comment.class);
      EasyMock.expect(comment.getContent()).andReturn(commentContent).anyTimes();
      EasyMock.expect(comment.getId()).andReturn(new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()).getNanos()).anyTimes();
      EasyMock.replay(comment);
      comments.add(comment);
    }
    
    // Check twice
    assertEquals("check", comments.get(0).getContent());
    assertEquals("check", comments.get(0).getContent());
    
    assertEquals("Hello from mock", comments.get(2).getContent());
    
    BitbucketRepository repo = new BitbucketRepository("", builder);
    repo.init(EasyMock.createNiceMock(ApiClient.class));    
    
    List<Pullrequest.Comment> filteredComments = repo.filterPullRequestComments(comments);
        
    assertTrue(filteredComments.size() == 4);
    assertEquals("Jenkins: test this please", filteredComments.get(filteredComments.size() - 1).getContent());
  }
  
  @Test
  @WithoutJenkins
  public void checkHashMyBuildTagTrue() {
    BitbucketPullRequestsBuilder builder = EasyMock.createMock(BitbucketPullRequestsBuilder.class); 
    EasyMock.expect(builder.getTrigger()).andReturn(null).anyTimes();
    EasyMock.replay(builder);
    
    IMockBuilder<BitbucketRepository> repoBuilder = EasyMock.partialMockBuilder(BitbucketRepository.class);    
    repoBuilder.addMockedMethod("getMyBuildTag");
    BitbucketRepository repo = repoBuilder.createMock();       
    EasyMock.expect(repo.getMyBuildTag(EasyMock.anyString())).andReturn("#jenkins-902f259e962ff16100843123480a0970").anyTimes();   
    EasyMock.replay(repo);
    
    List<Pullrequest.Comment> comments = new LinkedList<Pullrequest.Comment>();
    for(String commentContent : new String[] { 
      "TTP build flag [bid: #jenkins-902f259e962ff16100843123480a0970]",
      "TTP build flag [bid: #jenkins-902f259e962ff16100843123480a0970 #jenkins-foo]",
      "TTP build flag [bid: #jenkins-902f259e962ff16100843123480a0970 #jenkins-foo #jenkins-bar]",
      "TTP build flag ```[bid: #jenkins-902f259e962ff16100843123480a0970 #jenkins-foo #jenkins-bar]```",
    }) {
      Pullrequest.Comment comment = EasyMock.createNiceMock(Pullrequest.Comment.class);
      EasyMock.expect(comment.getContent()).andReturn(commentContent).anyTimes();
      EasyMock.expect(comment.getId()).andReturn(new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()).getNanos()).anyTimes();
      EasyMock.replay(comment);
      comments.add(comment);
    }
    
    String myBuildKey = "902f259e962ff16100843123480a0970";
    for(Pullrequest.Comment comment : comments)
      assertTrue(repo.hasMyBuildTagInTTPComment(comment.getContent(), myBuildKey));
  }
  
  @Test
  @WithoutJenkins
  public void checkHashMyBuildTagFalse() {
    BitbucketPullRequestsBuilder builder = EasyMock.createMock(BitbucketPullRequestsBuilder.class); 
    EasyMock.expect(builder.getTrigger()).andReturn(null).anyTimes();
    EasyMock.replay(builder);
    
    IMockBuilder<BitbucketRepository> repoBuilder = EasyMock.partialMockBuilder(BitbucketRepository.class);    
    repoBuilder.addMockedMethod("getMyBuildTag");
    BitbucketRepository repo = repoBuilder.createMock();       
    EasyMock.expect(repo.getMyBuildTag(EasyMock.anyString())).andReturn("#jenkins-902f259e962ff16100843123480a0970").anyTimes();   
    EasyMock.replay(repo);
    
    List<Pullrequest.Comment> comments = new LinkedList<Pullrequest.Comment>();
    for(String commentContent : new String[] { 
      "check",
      "",      
      "Hello from mock",
      "Jenkins: test this please",
      "TTP build flag [bid: #jenkins]",
      "TTP build flag [bid: #jenkins-foo]",
      "TTP build flag [bid: #jenkins-foo #jenkins-bar]",
      "TTP build flag ```[bid: #jenkins-foo #jenkins-bar]```",
    }) {
      Pullrequest.Comment comment = EasyMock.createNiceMock(Pullrequest.Comment.class);
      EasyMock.expect(comment.getContent()).andReturn(commentContent).anyTimes();
      EasyMock.expect(comment.getId()).andReturn(new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()).getNanos()).anyTimes();
      EasyMock.replay(comment);
      comments.add(comment);
    }
    
    String myBuildKey = "902f259e962ff16100843123480a0970";
    for(Pullrequest.Comment comment : comments)
      assertFalse(repo.hasMyBuildTagInTTPComment(comment.getContent(), myBuildKey));
  }
  
  //@Test
  @WithoutJenkins
  public void ttpCommentTest() {    
    ApiClient client = EasyMock.createNiceMock(ApiClient.class);
    Collection<List<Pullrequest>> prs = new LinkedList<List<Pullrequest>>();
    
    prs.add(Arrays.asList(new Pullrequest[] {
      new Pullrequest()
    }));
    
    for(List<Pullrequest> pr : prs) EasyMock.expect(client.getPullRequests()).andReturn(pr).times(1);
    BitbucketPullRequestsBuilder builder = EasyMock.createMock(BitbucketPullRequestsBuilder.class);
    EasyMock.replay(client, builder);
    
    BitbucketRepository repo = new BitbucketRepository("", builder);
    repo.init(client);
    
    Collection<Pullrequest> targetPRs = repo.getTargetPullRequests();
  }
}
