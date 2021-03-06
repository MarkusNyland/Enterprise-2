package org.tsdes.spring.microservice.gateway.service

import org.junit.ClassRule
import org.springframework.boot.test.util.EnvironmentTestUtils
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.containers.GenericContainer

/*
    Here, I want to run the same tests, but on a real database.
    However, I cannot use the same settings as in production code, as
    those require Docker Compose and running whole system.
    So here I just start a Postgres with Docker using TestContainer.
    However, I need to change the settings to connect to such database,
    and I do so by using and activating a specific profile with its
    own configuration file.
 */

@ActiveProfiles("pg")
@ContextConfiguration(initializers = arrayOf(ServiceApplicationPgTest.Companion.Initializer::class))
class ServiceApplicationPgTest  : ServiceApplicationTest(){

    companion object {

        class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)

        /*
           See: https://hub.docker.com/_/postgres/
         */

        @ClassRule
        @JvmField
        val postgres = KGenericContainer("postgres:10").withExposedPorts(5432)

        class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
            override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {

                val host = postgres.containerIpAddress
                val port = postgres.getMappedPort(5432)

                EnvironmentTestUtils.addEnvironment(
                        "testcontainers",
                        configurableApplicationContext.environment,
                        "spring.datasource.url=jdbc:postgresql://$host:$port/postgres"
                )
            }
        }
    }
}