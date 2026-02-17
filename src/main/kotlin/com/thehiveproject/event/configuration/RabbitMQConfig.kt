package com.thehiveproject.event.configuration

import org.slf4j.LoggerFactory
import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMQConfig {
    private val logger = LoggerFactory.getLogger(RabbitMQConfig::class.java)

    companion object {
        // 1. Unified Exchange Name
        const val EXCHANGE_NAME = "hive.notifications"

        // 2. Specific Queue for Core
        const val QUEUE_CORE_EMAIL = "q.notification.core.email"

        // 3. Clean Routing Key (Domain.Action)
        const val ROUTING_KEY_CORE_EMAIL = "core.email"
    }

    @Bean
    fun rabbitAdmin(connectionFactory: ConnectionFactory): RabbitAdmin {
        return RabbitAdmin(connectionFactory)
    }

    @Bean
    fun coreEmailQueue(): Queue {
        return Queue(QUEUE_CORE_EMAIL, true)
    }

    @Bean
    fun exchange(): DirectExchange {
        return DirectExchange(EXCHANGE_NAME)
    }

    @Bean
    fun binding(coreEmailQueue: Queue, exchange: DirectExchange): Binding {
        return BindingBuilder.bind(coreEmailQueue)
            .to(exchange)
            .with(ROUTING_KEY_CORE_EMAIL)
    }

    @Bean
    fun messageConverter(): MessageConverter {
        return Jackson2JsonMessageConverter()
    }

    @Bean
    fun rabbitTemplate(connectionFactory: ConnectionFactory, messageConverter: MessageConverter): RabbitTemplate {
        val template = RabbitTemplate(connectionFactory)
        template.messageConverter = messageConverter
        return template
    }

    @Bean
    fun rabbitMqInitializer(admin: RabbitAdmin, coreEmailQueue: Queue, exchange: DirectExchange, binding: Binding): ApplicationRunner {
        return ApplicationRunner {
            try {
                admin.declareQueue(coreEmailQueue)
                admin.declareExchange(exchange)
                admin.declareBinding(binding)
                logger.info("INIT: Core API RabbitMQ declared (Exchange: $EXCHANGE_NAME)")
            } catch (e: Exception) {
                logger.error("INIT FAILED: Core RabbitMQ", e)
            }
        }
    }
}