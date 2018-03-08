package com.fullcontact.rpc.jersey.yaml;

import com.google.api.HttpRule;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Value;

/**
 * HTTPRules defined in the .yml will be parsed into a YamlHTTPRule, from which a com.google.api.HttpRule can be
 * generated.
 *
 * @author Kyle Hansen (sypticus)
 */
@Value
public class YamlHttpRule {
    String selector;
    String get;
    String post;
    String put;
    String delete;
    String body;
    List<YamlHttpRule> additionalBindings;

    public HttpRule buildHttpRule() {
        HttpRule.Builder builder = HttpRule.newBuilder();
        if (get != null) {
            builder.setGet(get);
        }
        if (put != null) {
            builder.setPut(put);
        }
        if (delete != null) {
            builder.setDelete(delete);
        }
        if (post != null) {
            builder.setPost(post);
        }
        if (body != null) {
            builder.setBody(body);
        }
        if (additionalBindings != null) {
            builder.addAllAdditionalBindings(
                    additionalBindings.stream().map(YamlHttpRule::buildHttpRule).collect(Collectors.toList()));
        }

        return builder.build();
    }


}
