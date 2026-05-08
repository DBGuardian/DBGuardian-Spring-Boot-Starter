package com.erp.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置类
 * 
 * @author ERP System
 * @date 2025-11-27
 */
@Configuration
public class RabbitMQConfig {

    // 消息通知相关队列和交换机
    public static final String NOTIFICATION_EXCHANGE = "erp.notification.exchange";
    public static final String NOTIFICATION_QUEUE = "erp.notification.queue";
    public static final String NOTIFICATION_ROUTING_KEY = "notification";

    // 预警消息相关队列和交换机
    public static final String ALERT_EXCHANGE = "erp.alert.exchange";
    public static final String ALERT_QUEUE = "erp.alert.queue";
    public static final String ALERT_ROUTING_KEY = "alert";

    // 业务通知相关队列和交换机
    public static final String BUSINESS_EXCHANGE = "erp.business.exchange";
    public static final String BUSINESS_QUEUE = "erp.business.queue";
    public static final String BUSINESS_ROUTING_KEY = "business";

    // 系统消息相关队列和交换机
    public static final String SYSTEM_EXCHANGE = "erp.system.exchange";
    public static final String SYSTEM_QUEUE = "erp.system.queue";
    public static final String SYSTEM_ROUTING_KEY = "system";

    /**
     * 配置RabbitTemplate，使用JSON序列化
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        return template;
    }

    /**
     * 配置消息监听容器工厂，使用JSON反序列化
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(new Jackson2JsonMessageConverter());
        return factory;
    }

    // ========== 消息通知相关配置 ==========

    /**
     * 消息通知交换机
     */
    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(NOTIFICATION_EXCHANGE, true, false);
    }

    /**
     * 消息通知队列
     */
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE).build();
    }

    /**
     * 绑定消息通知队列到交换机
     */
    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue())
                .to(notificationExchange())
                .with(NOTIFICATION_ROUTING_KEY);
    }

    // ========== 预警消息相关配置 ==========

    /**
     * 预警消息交换机
     */
    @Bean
    public DirectExchange alertExchange() {
        return new DirectExchange(ALERT_EXCHANGE, true, false);
    }

    /**
     * 预警消息队列
     */
    @Bean
    public Queue alertQueue() {
        return QueueBuilder.durable(ALERT_QUEUE).build();
    }

    /**
     * 绑定预警消息队列到交换机
     */
    @Bean
    public Binding alertBinding() {
        return BindingBuilder.bind(alertQueue())
                .to(alertExchange())
                .with(ALERT_ROUTING_KEY);
    }

    // ========== 业务通知相关配置 ==========

    /**
     * 业务通知交换机
     */
    @Bean
    public DirectExchange businessExchange() {
        return new DirectExchange(BUSINESS_EXCHANGE, true, false);
    }

    /**
     * 业务通知队列
     */
    @Bean
    public Queue businessQueue() {
        return QueueBuilder.durable(BUSINESS_QUEUE).build();
    }

    /**
     * 绑定业务通知队列到交换机
     */
    @Bean
    public Binding businessBinding() {
        return BindingBuilder.bind(businessQueue())
                .to(businessExchange())
                .with(BUSINESS_ROUTING_KEY);
    }

    // ========== 系统消息相关配置 ==========

    /**
     * 系统消息交换机
     */
    @Bean
    public DirectExchange systemExchange() {
        return new DirectExchange(SYSTEM_EXCHANGE, true, false);
    }

    /**
     * 系统消息队列
     */
    @Bean
    public Queue systemQueue() {
        return QueueBuilder.durable(SYSTEM_QUEUE).build();
    }

    /**
     * 绑定系统消息队列到交换机
     */
    @Bean
    public Binding systemBinding() {
        return BindingBuilder.bind(systemQueue())
                .to(systemExchange())
                .with(SYSTEM_ROUTING_KEY);
    }
}


































