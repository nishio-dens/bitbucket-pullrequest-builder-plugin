import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.ApiClient;
import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.cloud.CloudApiClient;
import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.cloud.CloudPullrequest;

import java.util.logging.Logger;
import java.util.List;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.codehaus.jackson.type.TypeReference;

import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;

import org.easymock.*;
import org.junit.Test;
import org.junit.Rule;
import static org.junit.Assert.*;

/**
 * Tests
 */
public class BitbucketCloudTest {

  private static Logger log = Logger.getLogger(BitbucketCloudTest.class.getName());

  @Rule
  public JenkinsRule jRule = new JenkinsRule();

  private <R> R parse(String json, TypeReference<R> ref) throws java.io.IOException {
      return new ObjectMapper().readValue(json, ref);
  }

  @Test
  @WithoutJenkins
  public void simpleTest() throws java.io.IOException {
      CloudPullrequest.Comment comment = new CloudPullrequest.Comment("This is a comment");

      log.info("I'm starting");
      String jsonStr = ApiClient.serializeObject(comment);
      log.info("Got thhis json: " + jsonStr + "\n");
      assertEquals("{\"content\":{\"raw\":\"This is a comment\"}}", jsonStr);
  }

  @Test
  @WithoutJenkins
  // Simple test of the json parser, since the cloud api's abstraction is, urm,
  // contrived . . .
  public void cloudCommentsFromJsonTest() throws java.io.IOException {

    final CloudApiClient cloudApi = EasyMock.createNiceMock(CloudApiClient.class);

    CloudPullrequest.Comment comment = null;
    String commentStr = null;

    // Test 1 - extra fields
    commentStr = "{\"content\": {" +
              "\"html\": \"<p>rebuild please</p>\"," +
              "\"raw\": \"rebuild please\"" +
              "}," +
              "\"created_on\": \"2019-01-17T18:59:22.173394+00:00\"," +
              "\"id\": 88463806}";

    comment = parse(commentStr, new TypeReference<CloudPullrequest.Comment>() {});
    assertEquals("Comment Mismatch", "rebuild please", comment.getContent());
    assertTrue("Id Mismatch", comment.getId() == 88463806);

    // Test 2 - a TPP
    commentStr = "{\"content\": {" +
      "\"raw\": \"TTP build flag ```[bid: #jenkins-7d061ab3f0531c7c7514cabf6cb81be5]```\"}," +
      "\"id\": 88464241}";
    comment = parse(commentStr, new TypeReference<CloudPullrequest.Comment>() {});
    assertTrue("Comment Mismatch", comment.getContent().contains("jenkins-7d06"));
    assertTrue("Id Mismatch", comment.getId() == 88464241);

    // Test 3 - Minimal with trigger phrase
    commentStr = "{\"content\": {\"raw\": \"rebuild please\"},\"id\": 88469113}";
    comment = parse(commentStr, new TypeReference<CloudPullrequest.Comment>() {});
    assertEquals("Comment Mismatch", "rebuild please", comment.getContent());
    assertTrue("Id Mismatch", comment.getId() == 88469113);
  }
}
