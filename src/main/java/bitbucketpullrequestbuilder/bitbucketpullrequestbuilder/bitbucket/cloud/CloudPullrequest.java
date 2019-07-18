package bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.cloud;

import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.AbstractPullrequest;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * POJOs representing the pull-requests extracted from the
 * JSON response of the Bitbucket API V2.
 *
 * @see "https://confluence.atlassian.com/bitbucket/pullrequests-resource-423626332.html#pullrequestsResource-GETaspecificpullrequest"
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudPullrequest extends AbstractPullrequest {

    private String     description;
    private Boolean    closeSourceBranch;
    private String     title;
    private Revision   destination;
    private String     reason;
    private String     closedBy;
    private Revision   source;
    private String     state;
    private String     createdOn;
    private String     updatedOn;
    private String     mergeCommit;
    private String     id;
    private Author     author;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Revision implements AbstractPullrequest.Revision {
        private Repository repository;
        private Branch branch;
        private Commit commit;

        public Repository getRepository() {
            return repository;
        }
        public void setRepository(Repository repository) {
            this.repository = repository;
        }
        public Branch getBranch() {
            return branch;
        }
        public void setBranch(Branch branch) {
            this.branch = branch;
        }
        public Commit getCommit() {
            return commit;
        }
        public void setCommit(Commit commit) {
            this.commit = commit;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Repository implements AbstractPullrequest.Repository {
        private String fullName;
        private String name;
        private String ownerName;
        private String repositoryName;
        private RepositoryLinks links;

        @JsonProperty("full_name")
        public String getFullName() {
            return fullName;
        }
        @JsonProperty("full_name")
        public void setFullName(String fullName) {
            // Also extract owner- and reponame
            if (fullName != null) {
                this.ownerName = fullName.split("/")[0];
                this.repositoryName = fullName.split("/")[1];
            }
            this.fullName = fullName;
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public void setLinks(RepositoryLinks links) {
            this.links = links;
        }
        @Override public String getOwnerName() {
            return ownerName;
        }
        @Override public String getRepositoryName() {
            return repositoryName;
        }
        @Override public RepositoryLinks getLinks() {
            return links;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Branch implements AbstractPullrequest.Branch {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Commit implements AbstractPullrequest.Commit {
        private String hash;

        public String getHash() {
            return hash;
        }

        public void setHash(String hash) {
            this.hash = hash;
        }
    }

    // Was: Approval
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Participant implements AbstractPullrequest.Participant {
        private String role;
        private Boolean approved;

        public String getRole() {
            return role;
        }
        public void setRole(String role) {
            this.role = role;
        }
        public Boolean getApproved() {
            return approved;
        }
        public void setApproved(Boolean approved) {
            this.approved = approved;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Author implements AbstractPullrequest.Author {
        private String username;
        private String display_name;

        public String getUsername() {
            return username;
        }
        public void setUsername(String username) {
            this.username = username;
        }

        @JsonProperty("display_name")
        public String getDisplayName() {
            return display_name;
        }

        @JsonProperty("display_name")
        public void setDisplayName(String display_name) {
            this.display_name = display_name;
        }
        public String getCombinedUsername() {
            return String.format(AUTHOR_COMBINED_NAME, this.getDisplayName(), this.getUsername());
        }
    }

    // https://confluence.atlassian.com/bitbucket/pullrequests-resource-1-0-296095210.html#pullrequestsResource1.0-POSTanewcomment
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Comment implements AbstractPullrequest.Comment {
        private Integer id;
        private Content content;

        public Comment() {
        }

        public Comment(String rawContent) {
            this.content = new Content();
            this.content.setRaw(rawContent);
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Content {
            private String raw;

            public String getRaw() {
                return raw;
            }

            public void setRaw(String rawContent) {
                this.raw = rawContent;
            }
        }

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

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        // This annotation prevents getContent() - the abstract method, from being used in json
        // serialization/deserialization
        @JsonIgnore
        public String getContent() {
            if (content == null) {
                return "";
            }
            return content.getRaw();
        }

        // This annotation is needed so that the serializer will use the actual content
        // for serialization, even though the abstract class assumes getContent() will return a string.
        @JsonProperty("content")
        public Content getContentRaw() {
            return content;
        }

        // And, since the getContent didn't get grabbed by default due to my @JsonIgnore, we need to 
        // tell it setContent is still ok to use.
        @JsonProperty("content")
        public void setContent(Content content) {
            this.content = content;
        }
    }

    //-------------------- only getters and setters follow -----------------

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty("close_source_branch")
    public Boolean getCloseSourceBranch() {
        return closeSourceBranch;
    }

    @JsonProperty("close_source_branch")
    public void setCloseSourceBranch(Boolean closeSourceBranch) {
        this.closeSourceBranch = closeSourceBranch;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Revision getDestination() {
        return destination;
    }

    public void setDestination(Revision destination) {
        this.destination = destination;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @JsonProperty("closed_by")
    public String getClosedBy() {
        return closedBy;
    }

    @JsonProperty("closed_by")
    public void setClosedBy(String closedBy) {
        this.closedBy = closedBy;
    }

    public Revision getSource() {
        return source;
    }

    public void setSource(Revision source) {
        this.source = source;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @JsonProperty("created_on")
    public String getCreatedOn() {
        return createdOn;
    }

    @JsonProperty("created_on")
    public void setCreatedOn(String createdOn) {
        this.createdOn = createdOn;
    }

    @JsonProperty("updated_on")
    public String getUpdatedOn() {
        return updatedOn;
    }

    @JsonProperty("updated_on")
    public void setUpdatedOn(String updatedOn) {
        this.updatedOn = updatedOn;
    }

    @JsonProperty("merge_commit")
    public String getMergeCommit() {
        return mergeCommit;
    }

    @JsonProperty("merge_commit")
    public void setMergeCommit(String mergeCommit) {
        this.mergeCommit = mergeCommit;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Author getAuthor() {
        return this.author;
    }

    public void setAuthor(Author author) {
        this.author = author;
    }

}
