package com.fullcontact.rpc.jersey.yaml;

import com.google.api.HttpRule;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;
/**
 * Created by kylehansen @Sypticus on 12/28/16.
 */
@Data
public class YamlHttpRule {
  private String selector;
  private String get;
  private String post;
  private String put;
  private String delete;
  private String body;
  private List<YamlHttpRule> additionalBindings;

  public HttpRule buildHttpRule(){
    HttpRule.Builder builder = HttpRule.newBuilder();
    if(get != null){
      builder.setGet(get);
    }
    if(put != null){
      builder.setPut(put);
    }
    if(delete != null){
      builder.setDelete(delete);
    }
    if(post != null){
      builder.setPost(post);
    }
    if(body != null){
      builder.setBody(body);
    }
    if(additionalBindings != null){
      builder.addAllAdditionalBindings(additionalBindings.stream().map(YamlHttpRule::buildHttpRule).collect(Collectors.toList()));
    }

    return builder.build();

  }


}
