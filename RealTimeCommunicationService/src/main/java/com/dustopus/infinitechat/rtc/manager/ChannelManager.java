package com.dustopus.infinitechat.rtc.manager;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ChannelManager {

    public static final AttributeKey<Long> USER_ID_KEY = AttributeKey.valueOf("userId");

    // userId → Channel (one user can have multiple connections)
    private final ConcurrentHashMap<Long, Channel> userChannelMap = new ConcurrentHashMap<>();
    private final ChannelGroup allChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    /**
     * Bind a user to a channel
     */
    public void bindUser(Long userId, Channel channel) {
        // Close previous connection if exists
        Channel oldChannel = userChannelMap.put(userId, channel);
        if (oldChannel != null && oldChannel.isActive()) {
            oldChannel.close();
            log.info("Closed old connection for user {}", userId);
        }
        allChannels.add(channel);
        channel.attr(USER_ID_KEY).set(userId);
        log.debug("User {} bound to channel {}", userId, channel.id().asShortText());
    }

    /**
     * Unbind user from channel
     */
    public void unbindUser(Long userId) {
        Channel removed = userChannelMap.remove(userId);
        if (removed != null) {
            allChannels.remove(removed);
            log.debug("User {} unbound from channel", userId);
        }
    }

    /**
     * Get channel by userId
     */
    public Channel getChannel(Long userId) {
        Channel channel = userChannelMap.get(userId);
        if (channel != null && !channel.isActive()) {
            userChannelMap.remove(userId);
            return null;
        }
        return channel;
    }

    /**
     * Check if user is online
     */
    public boolean isOnline(Long userId) {
        return getChannel(userId) != null;
    }

    /**
     * Send message to a specific user
     */
    public boolean sendToUser(Long userId, String message) {
        Channel channel = getChannel(userId);
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(new io.netty.handler.codec.http.websocketx.TextWebSocketFrame(message));
            return true;
        }
        return false;
    }

    /**
     * Get online user count
     */
    public int getOnlineCount() {
        return userChannelMap.size();
    }

    /**
     * Close all channels
     */
    public void closeAll() {
        allChannels.close();
        userChannelMap.clear();
    }
}
