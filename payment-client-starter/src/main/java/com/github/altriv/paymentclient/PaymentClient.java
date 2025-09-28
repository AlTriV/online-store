package com.github.altriv.paymentclient;

import com.github.altriv.paymentclient.api.DefaultApi;

public class PaymentClient extends DefaultApi {

    public PaymentClient(String url) {
        super();
        getApiClient().setBasePath(url);
    }

    public PaymentClient setJwtToken(String token) {
        getApiClient().setBearerToken(token);
        return this;
    }
}
