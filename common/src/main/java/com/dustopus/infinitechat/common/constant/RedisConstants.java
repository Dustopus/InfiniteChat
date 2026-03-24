package com.dustopus.infinitechat.common.constant;

public class RedisConstants {
    public static final String USER_TOKEN_PREFIX = "user:token:";
    public static final String USER_ONLINE_PREFIX = "user:online:";
    public static final String USER_SESSION_PREFIX = "user:session:";
    public static final String CONTACT_LIST_PREFIX = "contact:list:";
    public static final String GROUP_MEMBER_PREFIX = "group:members:";
    public static final String VERIFY_CODE_PREFIX = "verify:code:";
    public static final String RED_PACKET_PREFIX = "redpacket:";
    public static final String RED_PACKET_RECORD_PREFIX = "redpacket:record:";
    public static final String USER_BALANCE_PREFIX = "user:balance:";
    public static final String MOMENT_TIMELINE_PREFIX = "moment:timeline:";
    public static final String MOMENT_LIKE_PREFIX = "moment:like:";
    public static final String OFFLINE_MSG_COUNT_PREFIX = "offline:count:";
    public static final String RATE_LIMIT_PREFIX = "ratelimit:";
    public static final long VERIFY_CODE_EXPIRE = 300L;
    public static final long TOKEN_EXPIRE = 604800L;
    public static final long RED_PACKET_EXPIRE = 86400L;
    private RedisConstants() {}
}
