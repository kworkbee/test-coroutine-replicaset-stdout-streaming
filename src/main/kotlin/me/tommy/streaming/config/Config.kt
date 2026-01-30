package me.tommy.streaming.config

import io.fabric8.kubernetes.client.ConfigBuilder
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class Config {
    @Bean
    fun kubernetesClient(): KubernetesClient {
        val config =
            ConfigBuilder()
                .withWatchReconnectInterval(1000)
                .withWatchReconnectLimit(-1)
                .build()

        return KubernetesClientBuilder()
            .withConfig(config)
            .build()
    }
}
