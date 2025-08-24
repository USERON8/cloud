package com.cloud.product.controller;

import com.cloud.common.domain.Result;
import com.cloud.common.domain.dto.product.ProductDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.product.converter.ProductConverter;
import com.cloud.product.module.entity.Product;
import com.cloud.product.service.ProductService;
import com.cloud.product.service.impl.ProductLogMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 商品管理控制器
 * 提供商品的增删改等管理功能
 */
@Slf4j
@RestController
@RequestMapping("/product/manage")
@RequiredArgsConstructor
@Tag(name = "商品管理", description = "商品管理接口")
public class ProductManageController {

    private final ProductService productService;
    private final ProductConverter productConverter = ProductConverter.INSTANCE;
    private final ProductLogMessageService productLogMessageService;

    /**
     * 创建商品
     *
     * @param productDTO 商品信息
     * @return 创建结果
     */
    @PostMapping("/create")
    @Operation(summary = "创建商品", description = "创建新的商品")
    @ApiResponse(responseCode = "200", description = "创建成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<ProductDTO> createProduct(
            @Parameter(description = "商品信息") @RequestBody ProductDTO productDTO) {
        try {
            log.info("创建商品: {}", productDTO.getName());

            Product product = productConverter.toEntity(productDTO);
            boolean saved = productService.save(product);

            if (saved) {
                Product savedProduct = productService.getById(product.getId());
                ProductDTO savedProductDTO = productConverter.toDTO(savedProduct);
                
                // 异步发送商品变更消息到日志服务
                productLogMessageService.sendProductChangeMessage(
                        savedProduct.getId(),
                        savedProduct.getName(),
                        0, // 创建商品前数量为0
                        1, // 创建商品变更数量为1
                        1, // 创建商品后数量为1
                        1, // 1表示创建商品
                        "system" // 操作人，实际项目中应该从上下文中获取
                );
                
                log.info("创建商品成功，商品ID: {}", savedProduct.getId());
                return Result.success(savedProductDTO);
            } else {
                log.error("创建商品失败: {}", productDTO.getName());
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "创建商品失败");
            }
        } catch (Exception e) {
            log.error("创建商品失败", e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "创建商品失败: " + e.getMessage());
        }
    }

    /**
     * 更新商品
     *
     * @param id         商品ID
     * @param productDTO 商品信息
     * @return 更新结果
     */
    @PutMapping("/update/{id}")
    @Operation(summary = "更新商品", description = "更新商品信息")
    @ApiResponse(responseCode = "200", description = "更新成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<ProductDTO> updateProduct(
            @Parameter(description = "商品ID") @PathVariable Long id,
            @Parameter(description = "商品信息") @RequestBody ProductDTO productDTO) {
        try {
            log.info("更新商品，商品ID: {}", id);

            Product existingProduct = productService.getById(id);
            if (existingProduct == null) {
                log.warn("商品不存在，商品ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "商品不存在");
            }

            Product product = productConverter.toEntity(productDTO);
            product.setId(id); // 确保ID一致
            boolean updated = productService.updateById(product);

            if (updated) {
                Product updatedProduct = productService.getById(id);
                ProductDTO updatedProductDTO = productConverter.toDTO(updatedProduct);
                
                // 异步发送商品变更消息到日志服务
                productLogMessageService.sendProductChangeMessage(
                        updatedProduct.getId(),
                        updatedProduct.getName(),
                        1, // 更新前商品存在
                        0, // 更新操作本身不改变数量
                        1, // 更新后商品仍存在
                        2, // 2表示更新商品
                        "system" // 操作人，实际项目中应该从上下文中获取
                );
                
                log.info("更新商品成功，商品ID: {}", id);
                return Result.success(updatedProductDTO);
            } else {
                log.error("更新商品失败，商品ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "更新商品失败");
            }
        } catch (Exception e) {
            log.error("更新商品失败，商品ID: {}", id, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "更新商品失败: " + e.getMessage());
        }
    }

    /**
     * 删除商品
     *
     * @param id 商品ID
     * @return 删除结果
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除商品", description = "删除商品")
    @ApiResponse(responseCode = "200", description = "删除成功",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Result.class)))
    public Result<Void> deleteProduct(
            @Parameter(description = "商品ID") @PathVariable Long id) {
        try {
            log.info("删除商品，商品ID: {}", id);

            Product existingProduct = productService.getById(id);
            if (existingProduct == null) {
                log.warn("商品不存在，商品ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "商品不存在");
            }

            boolean removed = productService.removeById(id);
            if (removed) {
                // 异步发送商品变更消息到日志服务
                productLogMessageService.sendProductChangeMessage(
                        existingProduct.getId(),
                        existingProduct.getName(),
                        1, // 删除前商品存在
                        -1, // 删除操作减少一个商品
                        0, // 删除后商品不存在
                        3, // 3表示删除商品
                        "system" // 操作人，实际项目中应该从上下文中获取
                );
                
                log.info("删除商品成功，商品ID: {}", id);
                return Result.success();
            } else {
                log.error("删除商品失败，商品ID: {}", id);
                return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "删除商品失败");
            }
        } catch (Exception e) {
            log.error("删除商品失败，商品ID: {}", id, e);
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "删除商品失败: " + e.getMessage());
        }
    }
}