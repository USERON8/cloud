package com.cloud.stock.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.common.domain.dto.stock.StockDTO;
import com.cloud.common.domain.vo.stock.StockVO;
import com.cloud.common.result.PageResult;
import com.cloud.stock.module.dto.StockPageDTO;
import com.cloud.stock.module.entity.Stock;

import java.util.Collection;
import java.util.List;






public interface StockService extends IService<Stock> {


    





    StockDTO getStockById(Long id);

    





    StockDTO createStock(StockDTO stockDTO);

    





    StockDTO getStockByProductId(Long productId);

    





    boolean updateStock(StockDTO stockDTO);

    





    List<StockDTO> getStocksByProductIds(Collection<Long> productIds);

    





    PageResult<StockVO> pageQuery(StockPageDTO pageDTO);


    





    boolean deleteStock(Long id);

    





    boolean deleteStocksByIds(Collection<Long> ids);

    







    boolean stockIn(Long productId, Integer quantity, String remark);

    









    boolean stockOut(Long productId, Integer quantity, Long orderId, String orderNo, String remark);

    






    boolean reserveStock(Long productId, Integer quantity);

    






    boolean releaseReservedStock(Long productId, Integer quantity);

    







    boolean confirmReservedStockOut(Long productId, Integer quantity, Long orderId, String orderNo, String remark);

    






    boolean checkStockSufficient(Long productId, Integer quantity);

    





    boolean isStockDeducted(Long orderId);

    





    boolean isStockFrozen(Long orderId);

    





    boolean isStockReserved(Long orderId);

    





    boolean isStockConfirmed(Long orderId);

    





    boolean isStockRolledBack(Long orderId);

    

    





    Integer batchCreateStocks(List<StockDTO> stockDTOList);

    





    Integer batchUpdateStocks(List<StockDTO> stockDTOList);

    





    Integer batchStockIn(List<StockAdjustmentRequest> requests);

    





    Integer batchStockOut(List<StockAdjustmentRequest> requests);

    





    Integer batchReserveStock(List<StockAdjustmentRequest> requests);

    


    class StockAdjustmentRequest {
        private Long productId;
        private Integer quantity;
        private Long orderId;
        private String orderNo;
        private String remark;

        
        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public Long getOrderId() {
            return orderId;
        }

        public void setOrderId(Long orderId) {
            this.orderId = orderId;
        }

        public String getOrderNo() {
            return orderNo;
        }

        public void setOrderNo(String orderNo) {
            this.orderNo = orderNo;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }

}

