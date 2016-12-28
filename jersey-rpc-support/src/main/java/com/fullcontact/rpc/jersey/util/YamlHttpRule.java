package com.fullcontact.rpc.jersey.util;

import com.google.api.HttpRule;

import java.util.List;
import java.util.stream.Collectors;
/**
 * Created by kylehansen @Sypticus on 12/28/16.
 */
public class YamlHttpRule {
  private String selector;
  private String get;
  private String post;
  private String put;
  private String delete;
  private String body;
  private List<YamlHttpRule> additionalBindings;


  public String getSelector() {
    return selector;
  }

  public void setSelector(String selector) {
    this.selector = selector;
  }

  public String getGet() {
    return get;
  }

  public void setGet(String get) {
    this.get = get;
  }

  public String getPost() {
    return post;
  }

  public void setPost(String post) {
    this.post = post;
  }

  public String getPut() {
    return put;
  }

  public void setPut(String put) {
    this.put = put;
  }

  public String getDelete() {
    return delete;
  }

  public void setDelete(String delete) {
    this.delete = delete;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public List<YamlHttpRule> getAdditionalBindings() {
    return additionalBindings;
  }

  public void setAdditionalBindings(List<YamlHttpRule> additionalBindings) {
    this.additionalBindings = additionalBindings;
  }


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

  @Override
  public String toString() {
    return "Rule{" +
        "selector='" + selector + '\'' +
        ", get='" + get + '\'' +
        ", post='" + post + '\'' +
        ", put='" + put + '\'' +
        ", delete='" + delete + '\'' +
        ", body='" + body + '\'' +
        ", additionalBindings=" + additionalBindings +
        '}';
  }



}
