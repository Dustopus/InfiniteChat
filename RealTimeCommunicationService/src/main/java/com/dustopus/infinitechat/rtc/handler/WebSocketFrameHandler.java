package com.dustopus.infinitechat.rtc.handler;

import com.dustopus.infinitechat.common.constant.MessageConstants;
import com.dustopus.infinitechat.common.constant.RedisConstants;
import com.dustopus.infinitechat.common.dto.message.WebSocketMessage;
import com.dustopus.infinitechat.rtc.manager.ChannelManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@ChannelHandler.Sharable
@RequiredArgsConstructor
public class WebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private final ChannelManager channelManager;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        Long userId = channel.attr(ChannelManager.USER_ID_KEY).get();
        if (userId != null) {
            channelManager.bindUser(userId, channel);
            // Mark user as online in Redis
            stringRedisTemplate.opsForValue().set(
                    RedisConstants.USER_ONLINE_PREFIX + userId,
                    "1",
                    RedisConstants.TOKEN_EXPIRE,
                    TimeUnit.SECONDS
            );
            log.info("User {} connected via WebSocket", userId);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) {
        if (frame instanceof TextWebSocketFrame) {
            String text = ((TextWebSocketFrame) frame).text();
            handleTextMessage(ctx, text);
        } else if (frame instanceof PingWebSocketFrame) {
            ctx.channel().writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
        } else if (frame instanceof CloseWebSocketFrame) {
            ctx.channel().close();
        }
    }

    private void handleTextMessage(ChannelHandlerContext ctx, String text) {
        try {
            WebSocketMessage message = objectMapper.readValue(text, WebSocketMessage.class);
            String type = message.getType();
            Long userId = ctx.channel().attr(ChannelManager.USER_ID_KEY).get();

            if ("heartbeat".equals(type)) {
                // Respond to heartbeat
                WebSocketMessage pong = WebSocketMessage.of("heartbeat_ack", null);
                ctx.channel().writeAndFlush(new TextWebSocketFrame(objectMapper.writeValueAsString(pong)));
                // Refresh online status
                stringRedisTemplate.expire(
                        RedisConstants.USER_ONLINE_PREFIX + userId,
                        RedisConstants.TOKEN_EXPIRE,
                        TimeUnit.SECONDS
                );
            } else if ("login".equals(type)) {
                // Bind user to channel on login message
                channelManager.bindUser(userId, ctx.channel());
                log.info("User {} login confirmed on WebSocket", userId);
            }
        } catch (Exception e) {
            log.error("Failed to handle WebSocket message: {}", text, e);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                Long userId = ctx.channel().attr(ChannelManager.USER_ID_KEY).get();
                log.warn("User {} heartbeat timeout, closing connection", userId);
                ctx.channel().close();
            }
        } else {
            // Handle WebSocket handshake completion — bind user
            if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
                Channel channel = ctx.channel();
                Long userId = channel.attr(ChannelManager.USER_ID_KEY).get();
                if (userId != null) {
                    channelManager.bindUser(userId, channel);
                    stringRedisTemplate.opsForValue().set(
                            RedisConstants.USER_ONLINE_PREFIX + userId,
                            "1",
                            RedisConstants.TOKEN_EXPIRE,
                            TimeUnit.SECONDS
                    );
                    log.info("User {} WebSocket handshake complete", userId);
                }
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("WebSocket frame handler error", cause);
        ctx.channel().close();
    }
}
