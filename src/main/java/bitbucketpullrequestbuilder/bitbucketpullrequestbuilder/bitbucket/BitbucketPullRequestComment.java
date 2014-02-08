package bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Comparator;

/**
 * Created by nishio
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BitbucketPullRequestComment implements Comparable<BitbucketPullRequestComment> {
    private Boolean isEntityAuthor;
    private Integer pullRequestId;
    private String contentRendered;
    private Boolean deleted;
    private String UtcLastUpdated;
    private Integer commentId;
    private String content;
    private String UtcCreatedOn;
    private Boolean isSpam;

    @JsonProperty("is_entity_author")
    public Boolean getIsEntityAuthor() {
        return isEntityAuthor;
    }

    @JsonProperty("is_entity_author")
    public void setIsEntityAuthor(Boolean isEntityAuthor) {
        this.isEntityAuthor = isEntityAuthor;
    }

    @JsonProperty("pull_request_id")
    public Integer getPullRequestId() {
        return pullRequestId;
    }

    @JsonProperty("pull_request_id")
    public void setPullRequestId(Integer pullRequestId) {
        this.pullRequestId = pullRequestId;
    }

    @JsonProperty("content_rendered")
    public String getContentRendered() {
        return contentRendered;
    }

    @JsonProperty("content_rendered")
    public void setContentRendered(String contentRendered) {
        this.contentRendered = contentRendered;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    @JsonProperty("utc_last_updated")
    public String getUtcLastUpdated() {
        return UtcLastUpdated;
    }

    @JsonProperty("utc_last_updated")
    public void setUtcLastUpdated(String utcLastUpdated) {
        UtcLastUpdated = utcLastUpdated;
    }

    @JsonProperty("comment_id")
    public Integer getCommentId() {
        return commentId;
    }

    @JsonProperty("comment_id")
    public void setCommentId(Integer commentId) {
        this.commentId = commentId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @JsonProperty("utc_created_on")
    public String getUtcCreatedOn() {
        return UtcCreatedOn;
    }

    @JsonProperty("utc_created_on")
    public void setUtcCreatedOn(String utcCreatedOn) {
        UtcCreatedOn = utcCreatedOn;
    }

    @JsonProperty("is_spam")
    public Boolean getIsSpam() {
        return isSpam;
    }

    @JsonProperty("is_spam")
    public void setIsSpam(Boolean isSpam) {
        this.isSpam = isSpam;
    }

    public int compareTo(BitbucketPullRequestComment target) {
        if (this.getCommentId() > target.getCommentId()) {
            return 1;
        } else if (this.getCommentId().equals(target.getCommentId())) {
            return 0;
        } else {
            return -1;
        }
    }
}
