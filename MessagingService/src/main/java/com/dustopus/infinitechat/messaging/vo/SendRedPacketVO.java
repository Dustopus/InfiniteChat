package com.dustopus.infinitechat.messaging.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SendRedPacketVO {

    private Long receiverId;

    private Long groupId;

    private BigDecimal totalAmount;

    private Integer totalCount;

    private String greeting;
}
