package bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BitbucketPullRequestApproval {
    private String role;
    private Boolean approved;

    @JsonProperty("role")
    public String getRole() {
        return role;
    }

    @JsonProperty("approved")
    public Boolean getApproved() {
        return approved;
    }
}
