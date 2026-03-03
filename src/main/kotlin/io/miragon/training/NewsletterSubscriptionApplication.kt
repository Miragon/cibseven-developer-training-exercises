package io.miragon.training

import org.cibseven.bpm.engine.RuntimeService
import org.cibseven.bpm.spring.boot.starter.annotation.EnableProcessApplication
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication
@EnableJpaRepositories
@EnableProcessApplication
class NewsletterSubscriptionApplication

@Autowired
private val runtimeService: RuntimeService? = null

fun main(args: Array<String>) {
    runApplication<NewsletterSubscriptionApplication>(*args)
}
