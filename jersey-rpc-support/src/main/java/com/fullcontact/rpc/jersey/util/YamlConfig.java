package com.fullcontact.rpc.jersey.util;

import java.util.List;
import java.util.Map;

/**
 * Created by kylehansen on 12/28/16.
 */
public class YamlConfig {
  public Map<String, List<Rule>> http;
  public List<Rule> getRules(){
   return http.get("rules");
  }
}
