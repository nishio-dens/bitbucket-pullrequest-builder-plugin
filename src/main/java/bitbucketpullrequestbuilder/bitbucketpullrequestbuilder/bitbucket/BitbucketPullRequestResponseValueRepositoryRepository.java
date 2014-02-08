package bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BitbucketPullRequestResponseValueRepositoryRepository {
    private String fullName;
    private String name;

    @JsonProperty("full_name")
    public String getFullName() {
        return fullName;
    }

    @JsonProperty("full_name")
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwnerName() {
        if (this.fullName != null) {
            return this.fullName.split("/")[0];
        }
        return null;
    }

    public String getRepositoryName() {
        if (this.fullName != null) {
            return this.fullName.split("/")[1];
        }
        return null;
    }
}

