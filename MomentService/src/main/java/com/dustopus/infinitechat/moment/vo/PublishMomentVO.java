package com.dustopus.infinitechat.moment.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class PublishMomentVO {
    private String content;
    private List<String> images;
}
