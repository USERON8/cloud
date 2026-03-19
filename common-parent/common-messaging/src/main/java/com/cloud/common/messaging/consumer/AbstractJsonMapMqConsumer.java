package com.cloud.common.messaging.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Map;

public abstract class AbstractJsonMapMqConsumer
    extends AbstractJsonMqConsumer<Map<String, Object>> {

  private static final TypeReference<Map<String, Object>> MAP_TYPE =
      new TypeReference<Map<String, Object>>() {};

  @Override
  protected final TypeReference<Map<String, Object>> payloadTypeReference() {
    return MAP_TYPE;
  }
}
