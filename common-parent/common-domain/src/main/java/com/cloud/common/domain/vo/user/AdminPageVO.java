package com.cloud.common.domain.vo.user;

import com.cloud.common.domain.dto.user.AdminDTO;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPageVO implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  private Long current;

  private Long size;

  private Long total;

  private Long pages;

  private List<AdminDTO> records;

  private Boolean hasPrevious;

  private Boolean hasNext;
}
