package com.cloud.order.service;

import org.springframework.messaging.MessageChannel;

public interface OrderMessageProducer {
    MessageChannel orderLogOutput();

    MessageChannel paymentCreateOutput();

    MessageChannel stockFreezeOutput();
}