package com.cloud.payment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloud.common.annotation.DistributedLock;
import com.cloud.common.domain.dto.payment.PaymentDTO;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.exception.ResourceNotFoundException;
import com.cloud.common.result.PageResult;
import com.cloud.common.result.Result;
import com.cloud.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;






@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "鏀粯鏈嶅姟", description = "鏀粯璧勬簮鐨凴ESTful API鎺ュ彛")
public class PaymentController {

    private final PaymentService paymentService;

    


    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SCOPE_payment:read')")
    @Operation(summary = "鑾峰彇鏀粯鍒楄〃", description = "鑾峰彇鏀粯鍒楄〃锛屾敮鎸佸垎椤靛拰鏌ヨ鍙傛暟")
    public Result<PageResult<PaymentDTO>> getPayments(
            @Parameter(description = "椤电爜") @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "椤电爜蹇呴』澶т簬0") Integer page,

            @Parameter(description = "姣忛〉鏁伴噺") @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "姣忛〉鏁伴噺蹇呴』澶т簬0")
            @Max(value = 100, message = "姣忛〉鏁伴噺涓嶈兘瓒呰繃100") Integer size,

            @Parameter(description = "鐢ㄦ埛ID") @RequestParam(required = false)
            @Positive(message = "鐢ㄦ埛ID蹇呴』涓烘鏁存暟") Long userId,

            @Parameter(description = "鏀粯鐘舵€?) @RequestParam(required = false)
            @Min(value = 0, message = "鏀粯鐘舵€佸€奸敊璇?)
            @Max(value = 9, message = "鏀粯鐘舵€佸€奸敊璇?) Integer status,

            @Parameter(description = "鏀粯娓犻亾") @RequestParam(required = false)
            @Min(value = 0, message = "鏀粯娓犻亾鍊奸敊璇?)
            @Max(value = 9, message = "鏀粯娓犻亾鍊奸敊璇?) Integer channel,

            Authentication authentication) {

        Page<PaymentDTO> pageResult = paymentService.getPaymentsPage(page, size, userId, status, channel);

        PageResult<PaymentDTO> result = PageResult.of(
                pageResult.getCurrent(),
                pageResult.getSize(),
                pageResult.getTotal(),
                pageResult.getRecords()
        );

        
        return Result.success(result);
    }

    


    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SCOPE_payment:read')")
    @Operation(summary = "鑾峰彇鏀粯璇︽儏", description = "鏍规嵁鏀粯ID鑾峰彇鏀粯璇︾粏淇℃伅")
    public Result<PaymentDTO> getPaymentById(
            @Parameter(description = "鏀粯ID") @PathVariable
            @NotNull(message = "鏀粯ID涓嶈兘涓虹┖")
            @Positive(message = "鏀粯ID蹇呴』涓烘鏁存暟") Long id,
            Authentication authentication) {

        PaymentDTO payment = paymentService.getPaymentById(id);
        if (payment == null) {
            log.warn("鏀粯璁板綍涓嶅瓨鍦? id={}", id);
            throw new ResourceNotFoundException("Payment", String.valueOf(id));
        }
        
        return Result.success("鏌ヨ鎴愬姛", payment);
    }

    


    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "鍒涘缓鏀粯璁板綍", description = "鍒涘缓鏂扮殑鏀粯璁板綍")
    public Result<Long> createPayment(
            @Parameter(description = "鏀粯淇℃伅") @RequestBody
            @Valid @NotNull(message = "鏀粯淇℃伅涓嶈兘涓虹┖") PaymentDTO paymentDTO) {

        Long paymentId = paymentService.createPayment(paymentDTO);
        
        return Result.success("鏀粯璁板綍鍒涘缓鎴愬姛", paymentId);
    }

    


    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "鏇存柊鏀粯璁板綍", description = "鏇存柊鏀粯璁板綍淇℃伅")
    public Result<Boolean> updatePayment(
            @Parameter(description = "鏀粯ID") @PathVariable Long id,
            @Parameter(description = "鏀粯淇℃伅") @RequestBody
            @Valid @NotNull(message = "鏀粯淇℃伅涓嶈兘涓虹┖") PaymentDTO paymentDTO,
            Authentication authentication) {

        
        Boolean result = paymentService.updatePayment(paymentDTO);
        
        return Result.success("鏀粯璁板綍鏇存柊鎴愬姛", result);
    }

    


    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "鍒犻櫎鏀粯璁板綍", description = "鍒犻櫎鏀粯璁板綍")
    public Result<Boolean> deletePayment(
            @Parameter(description = "鏀粯ID") @PathVariable
            @NotNull(message = "鏀粯ID涓嶈兘涓虹┖")
            @Positive(message = "鏀粯ID蹇呴』涓烘鏁存暟") Long id) {

        Boolean result = paymentService.deletePayment(id);
        
        return Result.success("鏀粯璁板綍鍒犻櫎鎴愬姛", result);
    }

    


    @PostMapping("/{id}/success")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SCOPE_payment:write')")
    @DistributedLock(
            key = "'payment:success:' + #id",
            waitTime = 5,
            leaseTime = 30,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "鏀粯澶勭悊涓紝璇峰嬁閲嶅鎻愪氦"
    )
    @Operation(summary = "鏀粯鎴愬姛", description = "澶勭悊鏀粯鎴愬姛鐘舵€佸彉鏇?)
    public Result<Boolean> paymentSuccess(
            @Parameter(description = "鏀粯ID") @PathVariable Long id,
            Authentication authentication) {

        
        Boolean result = paymentService.processPaymentSuccess(id);

        if (!result) {
            log.warn("鈿狅笍 鏀粯鎴愬姛澶勭悊澶辫触 - 鏀粯ID: {}", id);
            throw new BusinessException("鏀粯鎴愬姛澶勭悊澶辫触锛岃妫€鏌ユ敮浠樼姸鎬?);
        }
        
        return Result.success("鏀粯鎴愬姛澶勭悊瀹屾垚", result);
    }


    


    @PostMapping("/{id}/fail")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SCOPE_payment:write')")
    @DistributedLock(
            key = "'payment:fail:' + #id",
            waitTime = 5,
            leaseTime = 30,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "鏀粯澶勭悊涓紝璇峰嬁閲嶅鎻愪氦"
    )
    @Operation(summary = "鏀粯澶辫触", description = "澶勭悊鏀粯澶辫触鐘舵€佸彉鏇?)
    public Result<Boolean> paymentFail(
            @Parameter(description = "鏀粯ID") @PathVariable Long id,
            @Parameter(description = "澶辫触鍘熷洜") @RequestParam(required = false) String failReason,
            Authentication authentication) {

        
        Boolean result = paymentService.processPaymentFailed(id, failReason);

        if (!result) {
            log.warn("鈿狅笍 鏀粯澶辫触澶勭悊澶辫触 - 鏀粯ID: {}", id);
            throw new BusinessException("鏀粯澶辫触澶勭悊澶辫触锛岃妫€鏌ユ敮浠樼姸鎬?);
        }
        
        return Result.success("鏀粯澶辫触澶勭悊瀹屾垚", result);
    }

    

    @PostMapping("/{id}/refund")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SCOPE_payment:write')")
    @DistributedLock(
            key = "'payment:refund:' + #id",
            waitTime = 3,
            leaseTime = 20,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "閫€娆惧鐞嗕腑锛岃鍕块噸澶嶆彁浜?
    )
    @Operation(summary = "鏀粯閫€娆?, description = "澶勭悊鏀粯閫€娆?)
    public Result<Boolean> refundPayment(
            @Parameter(description = "鏀粯ID") @PathVariable Long id,
            @Parameter(description = "閫€娆鹃噾棰?) @RequestParam BigDecimal refundAmount,
            @Parameter(description = "閫€娆惧師鍥?) @RequestParam(required = false) String refundReason,
            Authentication authentication) {

        
        Boolean result = paymentService.processRefund(id, refundAmount, refundReason);

        if (!result) {
            log.warn("鈿狅笍 閫€娆惧鐞嗗け璐?- 鏀粯ID: {}", id);
            throw new BusinessException("閫€娆惧鐞嗗け璐ワ紝璇锋鏌ユ敮浠樼姸鎬?);
        }
        
        return Result.success("閫€娆惧鐞嗗畬鎴?, result);
    }

    


    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SCOPE_payment:read')")
    @Operation(summary = "鏍规嵁璁㈠崟ID鏌ヨ鏀粯淇℃伅", description = "鏍规嵁璁㈠崟ID鑾峰彇鏀粯淇℃伅")
    public Result<PaymentDTO> getPaymentByOrderId(
            @Parameter(description = "璁㈠崟ID") @PathVariable Long orderId,
            Authentication authentication) {

        PaymentDTO payment = paymentService.getPaymentByOrderId(orderId);
        if (payment == null) {
            log.warn("鈿狅笍 鏈壘鍒拌璁㈠崟鐨勬敮浠樿褰?- 璁㈠崟ID: {}", orderId);
            throw new ResourceNotFoundException("Payment for Order", String.valueOf(orderId));
        }
        
        return Result.success("鏌ヨ鎴愬姛", payment);
    }

    

    @PostMapping("/risk-check")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SCOPE_payment:read')")
    @DistributedLock(
            key = "'payment:risk:user:' + #userId",
            waitTime = 0,
            leaseTime = 3,
            timeUnit = TimeUnit.SECONDS,
            failMessage = "椋庢帶妫€鏌ョ郴缁熺箒蹇欙紝璇风◢鍚庡啀璇?
    )
    @Operation(summary = "鏀粯椋庢帶妫€鏌?, description = "鎵ц鏀粯椋庢帶妫€鏌?)
    public Result<Boolean> riskCheck(
            @Parameter(description = "鐢ㄦ埛ID") @RequestParam Long userId,
            @Parameter(description = "鏀粯閲戦") @RequestParam BigDecimal amount,
            @Parameter(description = "鏀粯鏂瑰紡") @RequestParam String paymentMethod,
            Authentication authentication) {

        
        Boolean riskPassed = paymentService.riskCheck(userId, amount, paymentMethod);

        if (riskPassed) {
            
        } else {
            log.warn("鈿狅笍 椋庢帶妫€鏌ヤ笉閫氳繃 - 鐢ㄦ埛ID: {}, 閲戦: {}", userId, amount);
        }
        return Result.success(riskPassed ? "椋庢帶妫€鏌ラ€氳繃" : "椋庢帶妫€鏌ヤ笉閫氳繃", riskPassed);
    }
}
