package com.cloud.search.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

  private static final List<DateTimeFormatter> SUPPORTED_FORMATTERS =
      List.of(
          DateTimeFormatter.ISO_LOCAL_DATE_TIME,
          DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

  @Override
  public LocalDateTime deserialize(JsonParser parser, DeserializationContext context)
      throws IOException {
    String text = parser.getValueAsString();
    if (text == null) {
      return null;
    }

    String normalized = text.trim();
    if (normalized.isEmpty()) {
      return null;
    }

    for (DateTimeFormatter formatter : SUPPORTED_FORMATTERS) {
      try {
        return LocalDateTime.parse(normalized, formatter);
      } catch (DateTimeParseException ignored) {
      }
    }

    throw InvalidFormatException.from(
        parser, "Unsupported LocalDateTime value", normalized, LocalDateTime.class);
  }
}
