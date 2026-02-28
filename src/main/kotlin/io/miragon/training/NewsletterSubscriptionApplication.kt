package io.miragon.training

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication
@EnableJpaRepositories
class NewsletterSubscriptionApplication

fun main(args: Array<String>) {
    runApplication<NewsletterSubscriptionApplication>(*args)
}
