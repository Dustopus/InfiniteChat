package com.dustopus.infinitechat.common.constant;

public class MessageConstants {
    public static final String SINGLE_CHAT = "single";
    public static final String GROUP_CHAT = "group";
    public static final int MSG_TYPE_TEXT = 1;
    public static final int MSG_TYPE_IMAGE = 2;
    public static final int MSG_TYPE_FILE = 3;
    public static final int MSG_TYPE_VOICE = 4;
    public static final int MSG_TYPE_VIDEO = 5;
    public static final int MSG_TYPE_RED_PACKET = 6;
    public static final int MSG_TYPE_SYSTEM = 7;
    public static final String KAFKA_TOPIC_MESSAGE = "im-message-topic";
    public static final String KAFKA_TOPIC_OFFLINE = "im-offline-topic";
    public static final String KAFKA_TOPIC_NOTIFY = "im-notify-topic";
    public static final String REDIS_CHANNEL_MESSAGE = "im:message:broadcast";
    public static final long HEARTBEAT_TIMEOUT = 60000L;
    private MessageConstants() {}
}
