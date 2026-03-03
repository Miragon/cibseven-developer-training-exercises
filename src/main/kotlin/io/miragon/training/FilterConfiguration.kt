package io.miragon.training

import org.cibseven.bpm.engine.rest.security.auth.ProcessEngineAuthenticationFilter
import org.cibseven.bpm.engine.rest.security.auth.impl.CompositeAuthenticationProvider
import org.springframework.boot.autoconfigure.web.servlet.JerseyApplicationPath
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FilterConfiguration {
    @Bean // Composite Authentication Filter with Jwt Token and Http Basic
    fun AuthenticationFilter(applicationPath: JerseyApplicationPath): FilterRegistrationBean<ProcessEngineAuthenticationFilter?> {
        val registrationBean = FilterRegistrationBean<ProcessEngineAuthenticationFilter?>()
        registrationBean.setName("cibseven-composite-auth")
        registrationBean.setFilter(ProcessEngineAuthenticationFilter())
        registrationBean.setOrder(10) // Order of execution if multiple filters

        val restApiPathPattern = applicationPath.getPath()

        // Apply to all URLs - whitelist logic in the filter handlers exclusions
        val urlPatterns: Array<String> = arrayOf(addUrl(restApiPathPattern, "/*"))

        // Enable async support
        registrationBean.setAsyncSupported(true)
        // Init parameters
        registrationBean.addInitParameter(
            "authentication-provider",
            CompositeAuthenticationProvider::class.java.getName()
        )

        registrationBean.addUrlPatterns(*urlPatterns)
        return registrationBean
    }

    private fun addUrl(base: String?, extend: String?): String {
        return (base + extend).replaceFirst("^(\\/+|([^/]))".toRegex(), "/$2")
    }
}