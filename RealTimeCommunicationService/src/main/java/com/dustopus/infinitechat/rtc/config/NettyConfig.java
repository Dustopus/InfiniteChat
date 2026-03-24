package com.dustopus.infinitechat.rtc.config;

import com.dustopus.infinitechat.rtc.handler.WebSocketHandler;
import com.dustopus.infinitechat.rtc.handler.WebSocketFrameHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class NettyConfig {

    @Value("${netty.port:9090}")
    private int port;

    @Value("${netty.path:/ws}")
    private String wsPath;

    @Bean
    public EventLoopGroup bossGroup() {
        return new NioEventLoopGroup(1);
    }

    @Bean
    public EventLoopGroup workerGroup() {
        return new NioEventLoopGroup();
    }

    @Bean
    public ServerBootstrap serverBootstrap(EventLoopGroup bossGroup, EventLoopGroup workerGroup,
                                           WebSocketHandler webSocketHandler,
                                           WebSocketFrameHandler webSocketFrameHandler) {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        // HTTP codec
                        pipeline.addLast(new HttpServerCodec());
                        pipeline.addLast(new ChunkedWriteHandler());
                        pipeline.addLast(new HttpObjectAggregator(65536));
                        // Heartbeat: 60s no read → idle
                        pipeline.addLast(new IdleStateHandler(60, 0, 0, TimeUnit.SECONDS));
                        // WebSocket protocol handler
                        pipeline.addLast(new WebSocketServerProtocolHandler(wsPath, null, true, 65536));
                        // Custom handlers
                        pipeline.addLast(webSocketHandler);
                        pipeline.addLast(webSocketFrameHandler);
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true);

        return bootstrap;
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public NettyServer nettyServer(ServerBootstrap bootstrap) {
        return new NettyServer(bootstrap, port);
    }

    public static class NettyServer {
        private final ServerBootstrap bootstrap;
        private final int port;
        private Channel serverChannel;

        public NettyServer(ServerBootstrap bootstrap, int port) {
            this.bootstrap = bootstrap;
            this.port = port;
        }

        public void start() throws InterruptedException {
            serverChannel = bootstrap.bind(port).sync().channel();
            log.info("WebSocket server started on port {}", port);
        }

        public void stop() {
            if (serverChannel != null) {
                serverChannel.close();
            }
        }
    }
}
