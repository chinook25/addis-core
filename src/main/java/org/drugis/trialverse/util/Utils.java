package org.drugis.trialverse.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;


/**
 * Created by connor on 6-11-14.
 */
public class Utils {
  public static String createJson(Object o) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.writeValueAsString(o);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    return null;
  }

  public static String loadResource(Class clazz, String filename) throws IOException {
      InputStream stream = clazz.getResourceAsStream(filename);
      return IOUtils.toString(stream, "UTF-8");
  }

  public static Map<String, Double> readMu(Integer treatmentId, JsonNode results) {
    JsonNode node =  results.get("multivariateSummary").get(treatmentId.toString()).get("mu");
    ObjectMapper mapper = new ObjectMapper();
    Map<String, Double> result = mapper.convertValue(node, Map.class);
    return result;
//    return result.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> Double.parseDouble(e.getValue())));
  }

}
