package com.fullcontact.rpc.jersey.yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Created by kylehansen @Sypticus on 12/28/16.
 */
public class YamlHttpConfig {
  public Map<String, List<YamlHttpRule>> http;
  public List<YamlHttpRule> getRules(){
   return http.get("rules");
  }

  public static Optional<YamlHttpConfig> getFromOptions(Set<String> options){
    Optional<String> yamlOption = options.stream().filter(option -> option.startsWith("yaml=")).findFirst();
    if(yamlOption.isPresent()) {
      String yamlPath = yamlOption.get().split("=")[1];
        try {
          File yamlFile = new File(yamlPath);
          if(!yamlFile.exists()){
            throw new RuntimeException("YAMLs file does not exist: "+ yamlFile.getAbsolutePath());
          }
          InputStream yamlStream = new FileInputStream(yamlFile);

          ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
          return Optional.of(mapper.readValue(yamlStream, YamlHttpConfig.class));
        } catch (IOException e) {
          throw new RuntimeException("Failed to parse YAML", e);
        }
      }
    return Optional.empty();
  }
}
