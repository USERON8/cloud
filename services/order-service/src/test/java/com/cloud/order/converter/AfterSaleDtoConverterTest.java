package com.cloud.order.converter;

import static org.assertj.core.api.Assertions.assertThat;

import com.cloud.order.dto.AfterSaleDTO;
import com.cloud.order.entity.AfterSale;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class AfterSaleDtoConverterTest {

  private final AfterSaleDtoConverter converter = Mappers.getMapper(AfterSaleDtoConverter.class);

  @Test
  void shouldRoundTripAllFields() {
    AfterSaleDTO dto = new AfterSaleDTO();
    dto.setId(1L);
    dto.setAfterSaleNo("AS-1");
    dto.setMainOrderId(2L);
    dto.setSubOrderId(3L);
    dto.setUserId(4L);
    dto.setMerchantId(5L);
    dto.setAfterSaleType("REFUND");
    dto.setStatus("APPLIED");
    dto.setReason("broken");
    dto.setDescription("screen cracked");
    dto.setApplyAmount(BigDecimal.valueOf(88.5));
    dto.setApprovedAmount(BigDecimal.valueOf(80));
    dto.setReturnLogisticsCompany("SF");
    dto.setReturnLogisticsNo("T100");
    dto.setRefundChannel("ALIPAY");
    dto.setRefundedAt(LocalDateTime.now().minusDays(1));
    dto.setClosedAt(LocalDateTime.now());
    dto.setCloseReason("done");
    dto.setCreatedAt(LocalDateTime.now().minusDays(2));
    dto.setUpdatedAt(LocalDateTime.now().minusHours(1));
    dto.setDeleted(0);
    dto.setVersion(2);

    AfterSale entity = converter.toEntity(dto);
    AfterSaleDTO roundTrip = converter.toDto(entity);

    assertThat(roundTrip).usingRecursiveComparison().isEqualTo(dto);
  }
}
