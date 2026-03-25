package com.dustopus.infinitechat.common.config;

import com.dustopus.infinitechat.common.constant.MessageConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Kafka Topic 自动创建配置
 * 确保所有微服务需要的 Topic 在应用启动时自动创建
 */
@Slf4j
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic messageTopic() {
        log.info("Creating Kafka topic: {}", MessageConstants.KAFKA_TOPIC_MESSAGE);
        return new NewTopic(MessageConstants.KAFKA_TOPIC_MESSAGE, 3, (short) 1);
    }

    @Bean
    public NewTopic offlineTopic() {
        log.info("Creating Kafka topic: {}", MessageConstants.KAFKA_TOPIC_OFFLINE);
        return new NewTopic(MessageConstants.KAFKA_TOPIC_OFFLINE, 3, (short) 1);
    }

    @Bean
    public NewTopic notifyTopic() {
        log.info("Creating Kafka topic: {}", MessageConstants.KAFKA_TOPIC_NOTIFY);
        return new NewTopic(MessageConstants.KAFKA_TOPIC_NOTIFY, 3, (short) 1);
    }
}
