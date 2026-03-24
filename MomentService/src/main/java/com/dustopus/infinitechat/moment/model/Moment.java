package com.dustopus.infinitechat.moment.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("moment")
public class Moment {
    @TableId(type = IdType.INPUT)
    private Long momentId;
    private Long userId;
    private String content;
    private String images;
    private Integer likeCount;
    private Integer commentCount;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
