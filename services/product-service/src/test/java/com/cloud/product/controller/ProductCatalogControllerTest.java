package com.cloud.product.controller;

import com.cloud.common.domain.dto.product.SkuDTO;
import com.cloud.common.domain.dto.product.SpuCreateRequestDTO;
import com.cloud.common.domain.dto.product.SpuDTO;
import com.cloud.common.domain.vo.product.SpuDetailVO;
import com.cloud.product.service.ProductCatalogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductCatalogControllerTest {

    @Mock
    private ProductCatalogService productCatalogService;

    @Test
    void createSpuShouldRejectForeignMerchantForMerchantUser() {
        ProductCatalogController controller = new ProductCatalogController(productCatalogService);

        var result = controller.createSpu(requestWithMerchant(200L), authentication("100", "ROLE_MERCHANT"));

        assertThat(result.getCode()).isEqualTo(403);
    }

    @Test
    void updateSpuShouldRejectForeignMerchantOwnership() {
        SpuDetailVO existing = new SpuDetailVO();
        existing.setMerchantId(200L);
        when(productCatalogService.getSpuById(11L)).thenReturn(existing);

        ProductCatalogController controller = new ProductCatalogController(productCatalogService);
        var result = controller.updateSpu(11L, requestWithMerchant(100L), authentication("100", "ROLE_MERCHANT"));

        assertThat(result.getCode()).isEqualTo(403);
    }

    @Test
    void updateSpuShouldPreserveMerchantIdForMerchantUser() {
        SpuDetailVO existing = new SpuDetailVO();
        existing.setMerchantId(100L);
        when(productCatalogService.getSpuById(11L)).thenReturn(existing);
        when(productCatalogService.updateSpu(any(), any())).thenReturn(true);

        ProductCatalogController controller = new ProductCatalogController(productCatalogService);
        SpuCreateRequestDTO request = requestWithMerchant(999L);
        var result = controller.updateSpu(11L, request, authentication("100", "ROLE_MERCHANT"));

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(request.getSpu().getMerchantId()).isEqualTo(100L);
        verify(productCatalogService).updateSpu(11L, request);
    }

    @Test
    void updateSpuStatusShouldAllowMerchantOwner() {
        SpuDetailVO existing = new SpuDetailVO();
        existing.setMerchantId(100L);
        when(productCatalogService.getSpuById(11L)).thenReturn(existing);
        when(productCatalogService.updateSpuStatus(11L, 1)).thenReturn(true);

        ProductCatalogController controller = new ProductCatalogController(productCatalogService);
        var result = controller.updateSpuStatus(11L, 1, authentication("100", "ROLE_MERCHANT"));

        assertThat(result.getCode()).isEqualTo(200);
        verify(productCatalogService).updateSpuStatus(11L, 1);
    }

    private SpuCreateRequestDTO requestWithMerchant(Long merchantId) {
        SpuDTO spu = new SpuDTO();
        spu.setSpuName("demo");
        spu.setCategoryId(1L);
        spu.setMerchantId(merchantId);

        SkuDTO sku = new SkuDTO();
        sku.setSkuCode("SKU-1");
        sku.setSkuName("sku");
        sku.setSalePrice(BigDecimal.TEN);
        sku.setStatus(1);

        SpuCreateRequestDTO request = new SpuCreateRequestDTO();
        request.setSpu(spu);
        request.setSkus(List.of(sku));
        return request;
    }

    private Authentication authentication(String userId, String... authorities) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("subject")
                .claim("user_id", userId)
                .claim("username", "merchant-" + userId)
                .build();
        return new JwtAuthenticationToken(
                jwt,
                Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList(),
                "merchant-" + userId
        );
    }
}
