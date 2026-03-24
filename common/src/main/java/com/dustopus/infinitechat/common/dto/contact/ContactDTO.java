package com.dustopus.infinitechat.common.dto.contact;

import lombok.Data;
import java.io.Serializable;

@Data
public class ContactDTO implements Serializable {
    private Long contactId;
    private Long userId;
    private Long friendId;
    private String remark;
    private String friendName;
    private String friendAvatar;
}
