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

    /**
     * Represents various URIs connected to the repository (e.g. git clone URIs, various BitBucket repository web
     * URIs, etc.).
     *
     * Used to provide more specific target git revision information (git repository URI) when scheduling
     * Jenkins builds.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RepositoryLinks {
        /**
         * List of URIs that can be used for the <code>git clone</code> operation.
         */
        private List<RepositoryLink> cloneLinks = new ArrayList<>();

        @JsonProperty("clone")
        public void setCloneLinks(List<RepositoryLink> cloneLinks) {
            this.cloneLinks = cloneLinks;
        }

        @JsonProperty("clone")
        public List<RepositoryLink> getCloneLinks() {
            return cloneLinks;
        }
    }

    /**
     * Repository URI (e.g. a git clone URI, BitBucket repository web URI, etc.)
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RepositoryLink {
        /**
         * Used to distinguish between various git clone URIs (e.g. https or ssh). Mostly unused otherwise.
         */
        private String name;

        /**
         * Actual URI value.
         */
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