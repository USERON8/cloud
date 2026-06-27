package com.cloud.common.messaging.event;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class CanalFlatMessage implements Serializable {

  private static final long serialVersionUID = 1L;

  private Long id;
  private String database;
  private String table;
  private List<String> pkNames;
  private Boolean isDdl;
  private String type;
  private Long es;
  private Long ts;
  private String sql;
  private Map<String, Integer> sqlType;
  private Map<String, String> mysqlType;
  private List<Map<String, Object>> data;
  private List<Map<String, Object>> old;
}
