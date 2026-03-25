package com.dustopus.infinitechat.common.result;

public enum ErrorCode {
    SUCCESS(200, "成功"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户已存在"),
    PASSWORD_ERROR(1003, "密码错误"),
    PHONE_ALREADY_REGISTERED(1004, "手机号已注册"),
    VERIFICATION_CODE_ERROR(1005, "验证码错误"),
    FRIEND_REQUEST_ALREADY_SENT(2001, "好友请求已发送"),
    ALREADY_FRIENDS(2002, "已经是好友了"),
    FRIEND_NOT_FOUND(2003, "好友不存在"),
    GROUP_NOT_FOUND(3001, "群组不存在"),
    NOT_GROUP_MEMBER(3002, "非群成员"),
    MESSAGE_SEND_FAILED(4001, "消息发送失败"),
    RED_PACKET_EMPTY(5001, "红包已被抢完"),
    RED_PACKET_EXPIRED(5002, "红包已过期"),
    BALANCE_INSUFFICIENT(5003, "余额不足"),
    SERVER_ERROR(500, "服务器内部错误");

    private final int code;
    private final String msg;

    ErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() { return code; }
    public String getMsg() { return msg; }
}
