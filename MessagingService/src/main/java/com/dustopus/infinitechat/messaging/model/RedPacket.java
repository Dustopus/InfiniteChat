package com.dustopus.infinitechat.messaging.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("red_packet")
public class RedPacket {

    @TableId
    private Long packetId;

    private Long senderId;

    private Long groupId;

    private BigDecimal totalAmount;

    private Integer totalCount;

    private Integer grabbedCount;

    private String greeting;

    private Integer status;

    private LocalDateTime expireAt;

    private LocalDateTime createdAt;
}
