package bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.server;

import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.AbstractPullrequest;
import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.BuildState;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Map;
import java.util.Objects;

/**
 * POJOs representing the pull-requests extracted from the
 * JSON response of the Bitbucket Server API V1.
 *
 * https://docs.atlassian.com/bitbucket-server/rest/5.9.0/bitbucket-rest.html
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServerPullrequest extends AbstractPullrequest {
    private String id;
    private String title;

    @JsonProperty("toRef")
    private Revision toRef;

    @JsonProperty("fromRef")
    private Revision fromRef;

    private String state;
    private Author author;

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public Revision getDestination() {
        return toRef;
    }

    @Override
    public Revision getSource() {
        return fromRef;
    }

    @Override
    public String getState() {
        return state;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Author getAuthor() {
        return author;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Revision implements AbstractPullrequest.Revision {
        private String id;
        private String latestCommit;
        private Repository repository;

        @Override
        public Repository getRepository() {
            return repository;
        }

        @Override
        public Branch getBranch() {
            return new Branch(id);
        }

        @Override
        public Commit getCommit() {
            return new Commit(latestCommit);
        }

        public void setId(String id) {
            this.id = StringUtils.remove(id, "refs/heads/");
        }

        public void setLatestCommit(String latestCommit) {
            this.latestCommit = latestCommit;
        }

        public void setRepository(Repository repository) {
            this.repository = repository;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Repository implements AbstractPullrequest.Repository {
        private String name;
        private String slug;
        private String ownerName;
        private RepositoryLinks links;

        @JsonProperty("project")
        private void unpackProject(Map<String, Object> project) {
            this.ownerName = project.get("key").toString();
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setSlug(String slug) {
            this.slug = slug;
        }

        public void setLinks(RepositoryLinks links) {
            this.links = links;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOwnerName() {
            return ownerName;
        }

        @Override
        public String getRepositoryName() {
            return slug;
        }

        @Override
        public RepositoryLinks getLinks() {
            return links;
        }
    }

    public static class Branch implements AbstractPullrequest.Branch {
        private String name;

        public Branch(final String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public static class Commit implements AbstractPullrequest.Commit {
        private String hash;

        public Commit(final String hash) {
            this.hash = hash;
        }

        @Override
        public String getHash() {
            return hash;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Author implements AbstractPullrequest.Author{

        private String username;
        private String displayName;

        @JsonProperty("user")
        private void unpackUser(Map<String, Object> user) {
            this.username = user.get("name").toString();
            this.displayName = user.get("displayName").toString();
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String getCombinedUsername() {
            return String.format(AUTHOR_COMBINED_NAME, this.getDisplayName(), this.getUsername());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Comment implements AbstractPullrequest.Comment {

        private Integer id;
        private String  content;

        @Override
        public int compareTo(AbstractPullrequest.Comment target) {
            if (target == null){
                return -1;
            } else if (this.getId() > target.getId()) {
                return 1;
            } else if (this.getId().equals(target.getId())) {
                return 0;
            } else {
                return -1;
            }
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final Comment comment = (Comment) o;

            return getId() != null ? getId().equals(comment.getId()) : comment.getId() == null;
        }

        @Override
        public int hashCode() {
            return getId() != null ? getId().hashCode() : 0;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public void setContent(String content) {
            this.content = content;
        }

        @Override
        public Integer getId() {
            return id;
        }

        @Override
        @JsonProperty("text")
        public String getContent() {
            return content;
        }

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Activity {
        private String action;
        private Integer id;
        private String text;
        private String path;

        @JsonProperty("comment")
        private void unpackComment(Map<String, Object> comment) {
            this.id = Integer.valueOf(comment.get("id").toString());
            this.text = comment.get("text").toString();
        }

        @JsonProperty("commentAnchor")
        private void unpackAnchor(Map<String, Object> anchor) {
            this.path = anchor.get("path").toString();
        }

        public boolean isComment() {
            return "COMMENTED".equals(this.action);
        }

        public Comment toComment() {
            final Comment comment = new Comment();
            comment.setId(id);
            comment.setContent(text);
            return comment;
        }

        public String getAction() {
            return action;
        }

        @JsonProperty("action")
        public void setAction(String action) {
            this.action = action;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CommitBuildState {
        private BuildState state;
        private String key;
        private String url;

        public BuildState getState() {
            return state;
        }

        public void setState(BuildState state) {
            this.state = state;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Approver {
        private boolean approved;
        private String status;
        private User user;

        public boolean isApproved() {
            return approved;
        }

        public void setApproved(boolean approved) {
            this.approved = approved;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public User getUser() {
            return user;
        }

        public void setUser(User user) {
            this.user = user;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class User {
        private String name;

        public User(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

}