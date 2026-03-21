package com.cloud.search.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.cloud.common.exception.ResourceNotFoundException;
import com.cloud.search.document.ShopDocument;
import com.cloud.search.service.ShopSearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShopSearchControllerTest {

  @Mock private ShopSearchService shopSearchService;

  @Test
  void getShopByIdShouldReturnActiveShop() {
    ShopDocument shop = new ShopDocument();
    shop.setShopId(11L);
    shop.setStatus(1);
    when(shopSearchService.findByShopId(11L)).thenReturn(shop);

    ShopSearchController controller = new ShopSearchController(shopSearchService);

    var result = controller.getShopById(11L);

    assertThat(result.getData()).isSameAs(shop);
  }

  @Test
  void getShopByIdShouldHideInactiveShop() {
    ShopDocument shop = new ShopDocument();
    shop.setShopId(12L);
    shop.setStatus(0);
    when(shopSearchService.findByShopId(12L)).thenReturn(shop);

    ShopSearchController controller = new ShopSearchController(shopSearchService);

    assertThrows(ResourceNotFoundException.class, () -> controller.getShopById(12L));
  }
}
