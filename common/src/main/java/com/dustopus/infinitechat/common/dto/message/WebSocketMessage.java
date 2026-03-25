package com.dustopus.infinitechat.common.dto.message;

import lombok.Data;
import java.io.Serializable;

@Data
public class WebSocketMessage implements Serializable {
    private String type;
    private Object data;
    private Long timestamp;

    public static WebSocketMessage of(String type, Object data) {
        WebSocketMessage msg = new WebSocketMessage();
        msg.setType(type);
        msg.setData(data);
        msg.setTimestamp(System.currentTimeMillis());
        return msg;
    }
}
