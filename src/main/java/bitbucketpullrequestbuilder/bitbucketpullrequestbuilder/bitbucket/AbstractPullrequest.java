package bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractPullrequest {

    protected static final String AUTHOR_COMBINED_NAME = "%s <@%s>";

    public interface Revision {
        Repository getRepository();

        Branch getBranch();

        Commit getCommit();
    }

    public interface Repository {
        String getName();

        String getOwnerName();

        String getRepositoryName();

        RepositoryLinks getLinks();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RepositoryLinks {
        private List<RepositoryLink> clone = new ArrayList<>();

        public void setClone(List<RepositoryLink> clone) {
            this.clone = clone;
        }
        public List<RepositoryLink> getClone() {
            return clone;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RepositoryLink {
        private String name;
        private String href;

        public void setName(String name) {
            this.name = name;
        }
        public void setHref(String href) {
            this.href = href;
        }
        public String getName() {
            return name;
        }
        public String getHref() {
            return href;
        }
    }

    public interface Branch {
        String getName();
    }

    public interface Commit {
        String getHash();
    }

    public interface Author {
        String getUsername();

        String getDisplayName();

        String getCombinedUsername();
    }

    public interface Participant {
        String getRole() ;

        Boolean getApproved();
    }

    public interface Comment extends Comparable<Comment> {
        Integer getId();

        String getContent();
    }

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

    public abstract String getTitle();

    public abstract Revision getDestination();

    public abstract Revision getSource();

    public abstract String getState();

    public abstract String getId();

    public abstract Author getAuthor();
}