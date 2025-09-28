package com.github.altriv.store.model;

import com.github.altriv.paymentclient.domain.PurchaseRequest;
import com.github.altriv.paymentclient.domain.PurchaseResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Purchase {

    private boolean success;
    @NonNull
    private Cart cart;
    private Order paidOrder;
    private String errorMessage;
    @NonNull
    private String username;

    public PurchaseRequest generatePurchaseRequest() {
        return new PurchaseRequest()
                .requestId(UUID.randomUUID())
                .price((long) cart.getTotalPrice())
                .username(username);
    }

    public Purchase processPurchaseResponse(@NonNull PurchaseResponse purchaseRs) {
        this.setSuccess(purchaseRs.getPurchaseResult());
        this.setErrorMessage(purchaseRs.getErrorMessage());
        return this;
    }

    public Purchase addPaidOrder(@NonNull Order order) {
        this.paidOrder = order;
        return this;
    }

    public Purchase addErrorMessage(@NonNull String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }
}
