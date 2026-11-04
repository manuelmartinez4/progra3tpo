package uade.prog3.tpo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.http.ResponseEntity;
import uade.prog3.tpo.model.Nodo;
import uade.prog3.tpo.repository.ItemRepository;
import uade.prog3.tpo.repository.NodoRepository;
import uade.prog3.tpo.seed.CargaInicial;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba de integracion contra una base Neo4j REAL. Comprueba la carga del
 * dominio y algunos endpoints de punta a punta.
 *
 * Esta desactivada por defecto para que ./mvnw test corra sin base. Para
 * ejecutarla, con Neo4j levantado y las variables NEO4J_* configuradas:
 *     NEO4J_INTEGRACION=true ./mvnw test
 *
 * ATENCION: la carga sincroniza la base con el dominio y borra los datos
 * ajenos (por ejemplo, la semilla generica del scaffold). Usar una base de
 * desarrollo dedicada.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named = "NEO4J_INTEGRACION", matches = "true")
class IntegracionNeo4jTest {

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private CargaInicial cargaInicial;

    @Autowired
    private NodoRepository nodoRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private Neo4jClient neo4jClient;

    @Test
    void laCargaEsRepetibleYNoDuplicaDatos() {
        cargaInicial.sincronizar();
        cargaInicial.sincronizar();
        assertBaseDelDominio();
    }

    @Test
    void laCargaBorraLosDatosAjenos() {
        // como la semilla generica: un lugar extra conectado a A, un camino inverso
        // con otro costo y un paquete extra; ademas, nodos sin id o con id numerico
        neo4jClient.query("""
                MATCH (a:Nodo {id: 'A'})
                MATCH (b:Nodo {id: 'B'})
                CREATE (z:Nodo {id: 'Z', nombre: 'Ajeno'})-[:CONECTA {costo: 1.0}]->(a)
                CREATE (b)-[:CONECTA {costo: 99.0}]->(a)
                CREATE (:Item {id: 'X9', nombre: 'Ajeno', peso: 1.0, valor: 50.0})
                CREATE (:Nodo {nombre: 'sin id'})
                CREATE (:Nodo {id: 42})
                CREATE (:Item {nombre: 'sin id', peso: 1.0, valor: 1.0})
                """).run();
        // un lugar y un paquete del dominio con la otra etiqueta de mas: no se borran
        neo4jClient.query("""
                MATCH (c:Nodo {id: 'C'})
                MATCH (p:Item {id: 'P2'})
                SET c:Item, p:Nodo
                """).run();
        cargaInicial.sincronizar();
        assertBaseDelDominio();
        assertTrue(neo4jClient.query("MATCH (x {id: 'C'}) RETURN labels(x) = ['Nodo']")
                .fetchAs(Boolean.class).one().orElseThrow(), "C conserva solo la etiqueta Nodo");
        assertTrue(neo4jClient.query("MATCH (x {id: 'P2'}) RETURN labels(x) = ['Item']")
                .fetchAs(Boolean.class).one().orElseThrow(), "P2 conserva solo la etiqueta Item");
        assertEquals(16.0, http.getForObject("/api/seleccion/greedy?capacidad=10", Map.class).get("valorTotal"));
    }

    @Test
    void guardarUnLugarActualizaElCostoDeSuCamino() {
        // Conexion guarda el elementId de la relacion: con el dialecto de Neo4j 5
        // (config/Neo4jConfig) un save actualiza la relacion existente
        Nodo a = nodoRepository.findById("A").orElseThrow();
        a.getConexiones().stream()
                .filter(c -> c.getDestino().getId().equals("B"))
                .findFirst().orElseThrow()
                .setCosto(4.5);
        try {
            nodoRepository.save(a);
            assertEquals(List.of(4.5), neo4jClient
                    .query("MATCH (:Nodo {id: 'A'})-[r:CONECTA]->(:Nodo {id: 'B'}) RETURN r.costo")
                    .fetchAs(Double.class).all());
            assertBaseDelDominio();
        } finally {
            cargaInicial.sincronizar(); // vuelve el costo a 4
        }
    }

    @Test
    void laCargaUnificaIdsRepetidosYCreaRestriccionesDeUnicidad() {
        // sin las restricciones, dos cargas simultaneas sobre una base vacia pueden repetir ids
        neo4jClient.query("DROP CONSTRAINT nodo_id_unico IF EXISTS").run();
        neo4jClient.query("DROP CONSTRAINT item_id_unico IF EXISTS").run();
        neo4jClient.query("""
                CREATE (:Nodo {id: 'A', nombre: 'Copia'})
                CREATE (:Item {id: 'P1', nombre: 'Copia', peso: 6.0, valor: 12.0})
                """).run();
        cargaInicial.sincronizar();
        assertBaseDelDominio();
        long restricciones = neo4jClient.query("""
                        SHOW CONSTRAINTS YIELD name, type
                        WHERE name IN ['nodo_id_unico', 'item_id_unico'] AND type = 'UNIQUENESS'
                        RETURN count(*)
                        """)
                .fetchAs(Long.class).one().orElseThrow();
        assertEquals(2, restricciones);
        assertEquals(List.of("A", "C", "B"),
                http.getForObject("/api/grafo/dijkstra?origen=A&destino=B", Map.class).get("vertices"));
    }

    private void assertBaseDelDominio() {
        assertEquals(8, nodoRepository.count());
        assertEquals(5, itemRepository.count());
        long caminos = neo4jClient.query("MATCH (:Nodo)-[r:CONECTA]->(:Nodo) RETURN count(r)")
                .fetchAs(Long.class).one().orElseThrow();
        assertEquals(12, caminos, "una sola relacion por camino fisico");
    }

    @Test
    @SuppressWarnings("unchecked")
    void resumenDeLaRedCargada() {
        Map<String, Object> resumen = http.getForObject("/api/grafo/resumen", Map.class);
        assertEquals(8, resumen.get("vertices"));
        assertEquals(12, resumen.get("aristas"));
        assertEquals(false, resumen.get("dirigido"));
        assertEquals(List.of("A", "B", "C", "D", "E", "F", "G", "H"), resumen.get("ids"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void endpointsSobreLaBaseReal() {
        assertEquals(List.of("A", "B", "C", "D", "E", "F", "G", "H"), http.getForObject("/api/grafo/bfs?origen=A", List.class));
        assertEquals(List.of("A", "B", "C", "D", "E", "F", "H", "G"), http.getForObject("/api/grafo/dfs?origen=A", List.class));
        assertEquals(17.0, http.getForObject("/api/grafo/dijkstra?origen=A&destino=H", Map.class).get("costoTotal"));
        assertEquals(18.0, http.getForObject("/api/grafo/kruskal", Map.class).get("costoTotal"));
        assertEquals(16.0, http.getForObject("/api/seleccion/greedy?capacidad=10", Map.class).get("valorTotal"));
        assertEquals(18.0, http.getForObject("/api/seleccion/dinamica?capacidad=10", Map.class).get("valorTotal"));
        assertEquals(9.0, http.getForObject("/api/seleccion/repartir?contenedores=3", Map.class).get("cargaMaxima"));

        ResponseEntity<Map> error = http.getForEntity("/api/grafo/bfs?origen=Q", Map.class);
        assertEquals(400, error.getStatusCode().value());
        assertFalse(error.getBody().containsKey("trace"));
    }
}
