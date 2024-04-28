package br.com.carrefour.catalogo.vtex

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.jms.annotation.EnableJms
import org.springframework.jms.config.DefaultJmsListenerContainerFactory
import org.springframework.jms.config.JmsListenerContainerFactory
import javax.jms.ConnectionFactory


@SpringBootApplication(scanBasePackages = ["br.com.carrefour.catalogo"])
@EnableMongoRepositories(basePackages = ["br.com.carrefour.catalogo"])
@EnableJms
class VtexApplication {
    @Bean
    fun defaultFactory(
        connectionFactory: ConnectionFactory?,
        configurer: DefaultJmsListenerContainerFactoryConfigurer
    ): JmsListenerContainerFactory<*>? {
        val factory = DefaultJmsListenerContainerFactory()
        configurer.configure(factory, connectionFactory)
        return factory
    }
}

fun main(args: Array<String>) {
    runApplication<VtexApplication>(*args)
}
