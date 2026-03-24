package com.dustopus.infinitechat.common.dto.moment;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class MomentDTO implements Serializable {
    private Long momentId;
    private Long userId;
    private String content;
    private List<String> images;
    private Integer likeCount;
    private Integer commentCount;
    private Boolean liked;
    private LocalDateTime createdAt;
    private String userName;
    private String userAvatar;
}
