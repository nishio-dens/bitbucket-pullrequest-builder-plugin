package bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket;

import java.util.List;
import java.util.Comparator;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * POJOs representing the pull-requests extracted from the
 * JSON response of the Bitbucket API V2.
 *
 * @see https://confluence.atlassian.com/bitbucket/pullrequests-resource-423626332.html#pullrequestsResource-GETaspecificpullrequest
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class Pullrequest {

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
    public static class Response<T> {
        private int pageLength;
        private List<T> values;
        private int page;
        private int size;
        private String next;

        @JsonProperty("pagelen")
        public int getPageLength() {
            return pageLength;
        }
        @JsonProperty("pagelen")
        public void setPageLength(int pageLength) {
            this.pageLength = pageLength;
        }
        public List<T> getValues() {
            return values;
        }
        public void setValues(List<T> values) {
            this.values = values;
        }
        public int getPage() {
            return page;
        }
        public void setPage(int page) {
            this.page = page;
        }
        public int getSize() {
            return size;
        }
        public void setSize(int size) {
            this.size = size;
        }
        public String getNext() {
            return next;
        }
        public void setNext(String next) {
            this.next = next;
        }
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Revision {
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
    public static class Repository {
        private String fullName;
        private String name;
        private String ownerName;
        private String repositoryName;

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
        public String getOwnerName() {
            return ownerName;
        }
        public String getRepositoryName() {
            return repositoryName;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Branch {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Commit {
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
    public static class Participant {
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

    // https://confluence.atlassian.com/bitbucket/pullrequests-resource-1-0-296095210.html#pullrequestsResource1.0-POSTanewcomment
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Comment implements Comparable<Comment> {
        private Integer id;
        private String  filename;
        private String  content;
        private String  updatedOn;
        private String  createdOn;

        @Override
        public int compareTo(Comment target) {
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

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public String getContent() {
            return content;
        }

        public void setContent(Object content) {
            if (content instanceof String) {
                this.content = (String)content;
            } else if (content instanceof Map){
                this.content = (String)((Map)content).get("raw");
            }
            return;
        }
        @JsonProperty("utc_last_updated")
        public String getUpdatedOn() {
            return updatedOn;
        }
        @JsonProperty("utc_last_updated")
        public void setUpdatedOn(String updatedOn) {
            this.updatedOn = updatedOn;
        }
        @JsonProperty("utc_created_on")
        public String getCreatedOn() {
            return createdOn;
        }
        @JsonProperty("utc_created_on")
        public void setCreatedOn(String createdOn) {
            this.createdOn = createdOn;
        }
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Author {
      private String username;
      private String display_name;
      public static final String COMBINED_NAME = "%s <@%s>";
      
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
        return String.format(COMBINED_NAME, this.getDisplayName(), this.getUsername());
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
    
    public void setAutohor(Author author) {
      this.author = author;
    }

}
