package br.com.carrefour.catalogo.core.config

import org.assertj.core.util.Lists
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import springfox.documentation.builders.ApiInfoBuilder
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.ApiKey
import springfox.documentation.service.AuthorizationScope
import springfox.documentation.service.SecurityReference
import springfox.documentation.service.SecurityScheme
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spi.service.contexts.SecurityContext
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger.web.ApiKeyVehicle
import springfox.documentation.swagger2.annotations.EnableSwagger2


@Configuration
@EnableSwagger2
class SwaggerConfig {

    @Value("\${project.version}")
    private val version: String? = null

    @Bean
    fun api() =
        Docket(DocumentationType.SWAGGER_2).useDefaultResponseMessages(false)
            .select()
            .apis(RequestHandlerSelectors.basePackage("br.com.carrefour.catalogo.vtex"))
            .paths(PathSelectors.any())
            .build()
            .securityContexts(Lists.newArrayList(securityContext()))
            .securitySchemes(apiKey() as List<SecurityScheme>?)
            .forCodeGeneration(true)
            .apiInfo(apiInfo())

    @Bean
    fun securityContext(): SecurityContext =
        SecurityContext.builder()
            .securityReferences(defaultAuth())
//				.forPaths(PathSelectors.any())
            .build()

    private fun apiInfo() =
        ApiInfoBuilder()
            .title("Carrefour - Catalagos")
            .description("API de acesso ao sistema VTEX.")
            .version(this.version)
            .license("Apache License Version 2.0")
            .licenseUrl("https://www.apache.com/licenses/LICENSE-2.0")
            .build()

    private fun defaultAuth(): List<SecurityReference> {
        val authorizationScope = AuthorizationScope("global", "accessEverything")
        val authorizationScopes = arrayOf(AuthorizationScope("global", "accessEverything"))
        authorizationScopes[0] = authorizationScope

        return Lists.newArrayList(SecurityReference("JWT",  authorizationScopes))
    }

    private fun apiKey() =
        Lists.newArrayList(ApiKey("JWT", HttpHeaders.AUTHORIZATION, ApiKeyVehicle.HEADER.value))
}
