package com.dustopus.infinitechat.messaging.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("red_packet_record")
public class RedPacketRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long packetId;

    private Long userId;

    private BigDecimal amount;

    private LocalDateTime createdAt;
}
