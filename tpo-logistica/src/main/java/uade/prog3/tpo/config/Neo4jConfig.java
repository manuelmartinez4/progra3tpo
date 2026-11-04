package uade.prog3.tpo.config;

import org.neo4j.cypherdsl.core.renderer.Dialect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Data Neo4j genera el Cypher para el dialecto de Neo4j 4 si no se le
 * indica otro: identifica las relaciones con id(), el id interno numerico,
 * que esta deprecado. El proyecto usa Neo4j 5, asi que se elige su dialecto,
 * que usa elementId(). Es lo que permite que Conexion guarde el id de la
 * relacion como String, tanto al leer como al guardar.
 */
@Configuration
public class Neo4jConfig {

    @Bean
    public org.neo4j.cypherdsl.core.renderer.Configuration cypherDslConfiguration() {
        return org.neo4j.cypherdsl.core.renderer.Configuration.newConfig()
                .withDialect(Dialect.NEO4J_5)
                .build();
    }
}
