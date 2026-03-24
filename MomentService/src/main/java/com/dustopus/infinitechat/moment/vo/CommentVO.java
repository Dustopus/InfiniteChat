package com.dustopus.infinitechat.moment.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentVO {
    @NotNull(message = "朋友圈ID不能为空")
    private Long momentId;
    @NotBlank(message = "评论内容不能为空")
    private String content;
    private Long replyToId;
}
