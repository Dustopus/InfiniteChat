package com.dustopus.infinitechat.rtc.handler;

import com.dustopus.infinitechat.common.constant.RedisConstants;
import com.dustopus.infinitechat.common.jwt.JwtUtil;
import com.dustopus.infinitechat.rtc.manager.ChannelManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.QueryStringDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@ChannelHandler.Sharable
@RequiredArgsConstructor
public class WebSocketHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final ChannelManager channelManager;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        // Extract token from query parameter
        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        String token = null;
        if (decoder.parameters().containsKey("token")) {
            token = decoder.parameters().get("token").get(0);
        }

        // Validate token
        if (token == null || !JwtUtil.isTokenValid(token)) {
            ctx.channel().close();
            log.warn("WebSocket connection rejected: invalid token");
            return;
        }

        Long userId = JwtUtil.getUserId(token);

        // Check if token is blacklisted (user logged out)
        String tokenKey = RedisConstants.USER_TOKEN_PREFIX + userId;
        String cachedToken = stringRedisTemplate.opsForValue().get(tokenKey);
        if (cachedToken != null && !cachedToken.equals(token)) {
            ctx.channel().close();
            log.warn("WebSocket connection rejected: token mismatch for user {}", userId);
            return;
        }
        // Store userId in channel attributes
        ctx.channel().attr(ChannelManager.USER_ID_KEY).set(userId);

        // Pass to next handler (WebSocket upgrade)
        ctx.fireChannelRead(request.retain());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // Handle WebSocket handshake completion
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        log.debug("Handler added for channel: {}", ctx.channel().id().asShortText());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        Long userId = channel.attr(ChannelManager.USER_ID_KEY).get();
        if (userId != null) {
            channelManager.unbindUser(userId);
            log.info("User {} disconnected", userId);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("WebSocket handler error", cause);
        ctx.channel().close();
    }
}
