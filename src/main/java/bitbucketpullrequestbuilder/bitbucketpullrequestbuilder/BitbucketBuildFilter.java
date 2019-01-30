package bitbucketpullrequestbuilder.bitbucketpullrequestbuilder;

import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;

import bitbucketpullrequestbuilder.bitbucketpullrequestbuilder.bitbucket.cloud.CloudBitbucketCause;
import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.scm.api.SCMSource;

/**
 * Mutable wrapper
 */
class Mutable<T> {
  private T value;
  public Mutable() { this.value = null; }
  public Mutable(T value) { this.value = value; }
  T get() { return this.value; }
  void set(T value) { this.value = value; }
}

abstract class Filter {
  protected static final Logger logger = Logger.getLogger(BitbucketBuildTrigger.class.getName());
  
  public static final String RX_FILTER_FLAG = "r";
  public static final String RX_FILTER_FLAG_SINGLE = RX_FILTER_FLAG + ":";
  
  public static final String SRC_RX = "s:(" + RX_FILTER_FLAG_SINGLE + ")?";
  public static final String DST_RX = "d:(" + RX_FILTER_FLAG_SINGLE + ")?";
  public static final String AUTHOR_RX = "a:(" + RX_FILTER_FLAG_SINGLE + ")?";
  public static final String BRANCH_FILTER_RX_PART = "([^\\s$]*)";
  
  abstract public boolean apply(String filter, BitbucketCause cause);
  abstract public boolean check(String filter);
  
  static final Pattern RX_SRC_DST_PARTS = Pattern.compile("(s:)|(d:)");
  public static boolean HasSourceOrDestPartsPredicate(String filter) { return RX_SRC_DST_PARTS.matcher(filter).find(); }
  
  static final Pattern RX_AUTHOR_PARTS = Pattern.compile("(a:)");
  public static boolean HasAuthorPartsPredicate(String filter) { return RX_AUTHOR_PARTS.matcher(filter).find(); }
  
  protected boolean applyByRx(Pattern rx, Filter usedFilter, String filter, BitbucketCause cause) {
    Matcher srcMatch = rx.matcher(filter);
    boolean apply = false;
    while (srcMatch.find()) {
      String computedFilter = ((srcMatch.group(1) == null ? "" : srcMatch.group(1)) + srcMatch.group(2)).trim();
      logger.log(Level.FINE, "Apply computed filter: {0}", computedFilter);
      apply = apply || (computedFilter.isEmpty() ? true : usedFilter.apply(computedFilter, cause));
    }
    return apply;
  }
}

class EmptyFilter extends Filter {
  @Override
  public boolean apply(String filter, BitbucketCause cause) { return true; }
  @Override
  public boolean check(String filter) { return true; }
}

class AnyFlag extends Filter {  
  @Override
  public boolean apply(String filter, BitbucketCause cause) { return true; }
  @Override
  public boolean check(String filter) { return filter.isEmpty() || filter.contains("*") || filter.toLowerCase().contains("any"); }
}

class OnlySourceFlag extends Filter {  
  @Override
  public boolean apply(String filter, BitbucketCause cause) {
    String selectedRx = filter.startsWith(RX_FILTER_FLAG_SINGLE) ? filter.substring(RX_FILTER_FLAG_SINGLE.length()) : Pattern.quote(filter);    
    logger.log(Level.FINE, "OnlySourceFlag using filter: {0}", selectedRx);
    Matcher matcher = Pattern.compile(selectedRx, Pattern.CASE_INSENSITIVE).matcher(cause.getSourceBranch());
    return filter.startsWith(RX_FILTER_FLAG_SINGLE) ? matcher.find() : matcher.matches();
  } 
  @Override
  public boolean check(String filter) { 
    return false;
  }
}

class OnlyDestFlag extends Filter {  
  @Override
  public boolean apply(String filter, BitbucketCause cause) {
    String selectedRx = filter.startsWith(RX_FILTER_FLAG_SINGLE) ? filter.substring(RX_FILTER_FLAG_SINGLE.length()) : Pattern.quote(filter);
    logger.log(Level.FINE, "OnlyDestFlag using filter: {0}", selectedRx);
    Matcher matcher = Pattern.compile(selectedRx, Pattern.CASE_INSENSITIVE).matcher(cause.getTargetBranch());
    return filter.startsWith(RX_FILTER_FLAG_SINGLE) ? matcher.find() : matcher.matches();
  } 
  @Override
  public boolean check(String filter) { 
    return !HasSourceOrDestPartsPredicate(filter);
  }
}

class SourceDestFlag extends Filter {    
  static final Pattern SRC_MATCHER_RX = Pattern.compile(SRC_RX + BRANCH_FILTER_RX_PART, Pattern.CASE_INSENSITIVE | Pattern.CANON_EQ);
  static final Pattern DST_MATCHER_RX = Pattern.compile(DST_RX + BRANCH_FILTER_RX_PART, Pattern.CASE_INSENSITIVE | Pattern.CANON_EQ);
  
  @Override
  public boolean apply(String filter, BitbucketCause cause) {
    return this.applyByRx(SRC_MATCHER_RX, new OnlySourceFlag(), filter, cause) &&
           this.applyByRx(DST_MATCHER_RX, new OnlyDestFlag(), filter, cause);
  } 
  @Override
  public boolean check(String filter) { 
    return HasSourceOrDestPartsPredicate(filter);
  }
}

class AuthorFlag extends Filter {
  static final Pattern AUTHOR_MATCHER_RX = Pattern.compile(AUTHOR_RX + BRANCH_FILTER_RX_PART, Pattern.CASE_INSENSITIVE | Pattern.CANON_EQ);
  
  static class AuthorFlagImpl extends Filter {
    @Override
    public boolean apply(String filter, BitbucketCause cause) {
      String selectedRx = filter.startsWith(RX_FILTER_FLAG_SINGLE) ? filter.substring(RX_FILTER_FLAG_SINGLE.length()) : Pattern.quote(filter);
      logger.log(Level.FINE, "AuthorFlagImpl using filter: {0}", selectedRx);
      Matcher matcher = Pattern.compile(selectedRx, Pattern.CASE_INSENSITIVE).matcher(cause.getPullRequestAuthor());
      return filter.startsWith(RX_FILTER_FLAG_SINGLE) ? matcher.find() : matcher.matches();
    } 
    @Override
    public boolean check(String filter) {  return false; }
  }
  
  @Override
  public boolean apply(String filter, BitbucketCause cause) {
    return this.applyByRx(AUTHOR_MATCHER_RX, new AuthorFlagImpl(), filter, cause);
  } 
  @Override
  public boolean check(String filter) { 
    return HasAuthorPartsPredicate(filter);
  }
}

class CombinedFlags extends Filter {
  private final Filter[] _filters;
  public CombinedFlags(Filter[] filters) {
    _filters = filters;
  }
  
  @Override
  public boolean apply(String filter, BitbucketCause cause) {
    boolean applied = true;
    for(Filter f: _filters)
      if (f.check(filter)) 
        applied = applied && f.apply(filter, cause);
    return applied;
  } 
  @Override
  public boolean check(String filter) { 
    for(Filter f: _filters) 
      if (f.check(filter)) 
        return true;
    return false;
  }
}

/**
 * Created by maxvodo
 */
public class BitbucketBuildFilter {  
  private static final Logger logger = Logger.getLogger(BitbucketBuildTrigger.class.getName());
  
  private final String filter;
  private Filter currFilter = null;
  private static final List<Filter> AvailableFilters;
  
  static {
    ArrayList<Filter> filters = new ArrayList<Filter>();
    
    filters.add(new AnyFlag());        
    filters.add(new CombinedFlags(new Filter[] {
      new SourceDestFlag(),
      new AuthorFlag()
    }));    
    filters.add(new OnlyDestFlag());
    filters.add(new EmptyFilter()); 
    
    AvailableFilters = filters;
  }
  
  public BitbucketBuildFilter(String f) {
    this.filter = (f != null ? f : "").trim();
    this.buildFilter(this.filter);
  }
  
  private void buildFilter(String filter) {
    logger.log(Level.FINE, "Build filter by phrase: {0}", filter);
    for(Filter f : AvailableFilters) {
      if (f.check(filter)) {
        this.currFilter = f;
        logger.log(Level.FINE, "Using filter: {0}", f.getClass().getSimpleName());
        break;
      }
    }  
  }
  
  public boolean approved(BitbucketCause cause) {
    logger.log(Level.FINE, "Approve cause: {0}", cause.toString());
    return this.currFilter.apply(this.filter, cause);
  }  
  
  public static BitbucketBuildFilter instanceByString(String filter) {
    logger.log(Level.FINE, "Filter instance by filter string");
    return new BitbucketBuildFilter(filter);
  }    
  
  static public String filterFromGitSCMSource(AbstractGitSCMSource gitscm, String defaultFilter) {
    if (gitscm == null) {
      logger.log(Level.FINE, "Git SCMSource unavailable. Using default value: {0}", defaultFilter);
      return defaultFilter;
    }

    StringBuffer filter = new StringBuffer(defaultFilter);
    final String includes = gitscm.getIncludes();
    if (includes != null && !includes.isEmpty()) {
      for(String part : includes.split("\\s+")) {
        filter.append(String.format("%s ", part.replaceAll("\\*\\/", "d:")));
      }
    }    
    
    logger.log(Level.FINE, "Git includes transformation to filter result: {1} -> {0}; default: {2}", new Object[]{ filter, includes, defaultFilter });
    return filter.toString().trim();
  }
  
  public static BitbucketBuildFilter instanceBySCM(Collection<SCMSource> scmSources, String defaultFilter) {
    logger.log(Level.FINE, "Filter instance by using SCMSources list with {0} items", scmSources.size());
    AbstractGitSCMSource gitscm = null;
    for(SCMSource scm : scmSources) {
      logger.log(Level.FINE, "Check {0} SCMSource ", scm.getClass());
      if (scm instanceof AbstractGitSCMSource) {
        gitscm = (AbstractGitSCMSource)scm;
        break;
      }
    }    
    return new BitbucketBuildFilter(filterFromGitSCMSource(gitscm, defaultFilter));
  }
}
