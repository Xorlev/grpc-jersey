package com.fullcontact.rpc.jersey.util;

import java.util.List;
import java.util.Map;

/**
 * Created by kylehansen @Sypticus on 12/28/16.
 */
public class YamlHttpConfig {
  public Map<String, List<YamlHttpRule>> http;
  public List<YamlHttpRule> getRules(){
   return http.get("rules");
  }
}
