package com.ecommerce.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentMethodRequest {
    @NotNull(message = "Payment type is required")
    private String paymentType;
    
    @NotBlank(message = "Card number is required")
    private String cardNumber;
    
    @NotBlank(message = "Expiry date is required")
    private String expiryDate;
    
    @NotBlank(message = "Card holder name is required")
    private String cardHolderName;
    
    private boolean isDefault;
}
