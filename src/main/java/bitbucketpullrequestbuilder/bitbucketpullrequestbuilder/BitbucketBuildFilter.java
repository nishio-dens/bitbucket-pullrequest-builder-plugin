package bitbucketpullrequestbuilder.bitbucketpullrequestbuilder;

import hudson.ExtensionList;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
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
  public static final String BRANCH_FILTER_RX_PART = "([^\\s$]*)";
  
  abstract public boolean apply(String filter, BitbucketCause cause);
  abstract public boolean check(String filter);
  
  static final Pattern RX_SRC_DST_PARTS = Pattern.compile("(s:)|(d:)");
  public static boolean HasSourceOrDestPartsPredicate(String filter) { return RX_SRC_DST_PARTS.matcher(filter).find(); }
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
    logger.log(Level.INFO, "OnlySourceFlag using filter: {0}", selectedRx);
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
    logger.log(Level.INFO, "OnlyDestFlag using filter: {0}", selectedRx);
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
  
  boolean applyByRx(Pattern rx, Filter usedFilter, String filter, BitbucketCause cause) {    
    Matcher srcMatch = rx.matcher(filter);
    boolean apply = rx.matcher(filter).matches();
    while (srcMatch.find()) {
      String computedFilter = ((srcMatch.group(1) == null ? "" : srcMatch.group(1)) + srcMatch.group(2)).trim();
      logger.log(Level.INFO, "Apply computed filter: {0}", computedFilter);
      apply = apply || (computedFilter.isEmpty() ? true : usedFilter.apply(computedFilter, cause));
    }
    return apply;
  }
  
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
    filters.add(new OnlyDestFlag());
    filters.add(new SourceDestFlag());
    filters.add(new EmptyFilter());    
    AvailableFilters = filters;
  }
  
  public BitbucketBuildFilter(String f) {
    this.filter = (f != null ? f : "").trim();
    this.buildFilter(this.filter);
  }
  
  private void buildFilter(String filter) {
    logger.log(Level.INFO, "Build filter by phrase: {0}", filter);
    for(Filter f : AvailableFilters) {
      if (f.check(filter)) {
        this.currFilter = f;
        logger.log(Level.INFO, "Using filter: {0}", f.getClass().getSimpleName());
        break;
      }
    }  
  }
  
  public boolean approved(BitbucketCause cause) {    
    logger.log(Level.INFO, "Approve cause: {0}", cause.toString());
    return this.currFilter.apply(this.filter, cause);
  }  
  
  public static BitbucketBuildFilter InstanceByString(String filter) {
    logger.log(Level.INFO, "Filter instance by filter string");
    return new BitbucketBuildFilter(filter);
  }    
  
  static public String FilterFromGitSCMSource(AbstractGitSCMSource gitscm, String defaultFilter) {
    if (gitscm == null) {
      logger.log(Level.INFO, "Git SCMSource unavailable. Using default value: {0}", defaultFilter);
      return defaultFilter;
    }

    String filter = defaultFilter;
    final String includes = gitscm.getIncludes();
    if (includes != null && !includes.isEmpty()) {
      for(String part : includes.split("\\s+")) {
        filter += String.format("%s ", part.replaceAll("\\*\\/", "d:"));
      }
    }    
    
    logger.log(Level.INFO, "Git includes transformation to filter result: {1} -> {0}; default: {2}", new Object[]{ filter, includes, defaultFilter });
    return filter.trim();
  }
  
  public static BitbucketBuildFilter InstanceBySCM(ExtensionList<SCMSource> scmSources, String defaultFilter) {      
    logger.log(Level.FINE, "Filter instance by using SCM");
    AbstractGitSCMSource gitscm = null;
    for(SCMSource scm : scmSources) {
      gitscm = (AbstractGitSCMSource)scm;
      if (gitscm != null) break;
    }    
    return new BitbucketBuildFilter(FilterFromGitSCMSource(gitscm, defaultFilter));
  }
}
