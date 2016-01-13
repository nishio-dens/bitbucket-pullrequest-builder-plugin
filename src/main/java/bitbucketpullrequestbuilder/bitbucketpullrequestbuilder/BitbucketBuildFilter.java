package bitbucketpullrequestbuilder.bitbucketpullrequestbuilder;

import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

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
  
  abstract public boolean apply(String filter, BitbucketCause cause);
  abstract public boolean check(String filter);
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

class OnlyDestFlag extends Filter {  
  @Override
  public boolean apply(String filter, BitbucketCause cause) { 
    return Pattern.compile(
      filter.startsWith(RX_FILTER_FLAG_SINGLE) ? filter.substring(RX_FILTER_FLAG_SINGLE.length()) : Pattern.quote(filter),
    Pattern.CASE_INSENSITIVE).matcher(cause.getTargetBranch()).find();
  } 
  @Override
  public boolean check(String filter) { 
    return !(Pattern.matches("(s:)|(d:)", filter));
  }
}

class SourceDestFlag extends Filter {  
  @Override
  public boolean apply(String filter, BitbucketCause cause) { 
    return false;
  } 
  @Override
  public boolean check(String filter) { 
    return Pattern.matches("(s:)|(d:)", filter);
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
    filters.add(new EmptyFilter());
    AvailableFilters = filters;
  }
  
  public BitbucketBuildFilter(String f) {
    this.filter = f;
    this.buildFilter(this.filter);
  }
  
  private void buildFilter(String filter) {
    for(Filter f : AvailableFilters) {
      if (f.check(filter)) {
        this.currFilter = f;
        logger.log(Level.INFO, "Using filter: {0}", f.getClass().getSimpleName());
        break;
      }
    }
    logger.warning("No available filters to use ...");  
  }
  
  public boolean approved(BitbucketCause cause) {    
    return this.currFilter.apply(this.filter, cause);
  }  
}
