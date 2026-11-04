package uade.prog3.tpo.seed;

import org.neo4j.driver.exceptions.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import uade.prog3.tpo.model.Item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Sincroniza la base con los datos del dominio ({@link DatosDominio}).
 *
 * Es REPETIBLE: se puede ejecutar en cada arranque sin duplicar nada.
 *   - lugares y paquetes se crean o actualizan por id (MERGE)
 *   - los caminos que tocan lugares del dominio se reemplazan por los 12
 *     del dominio, una sola relacion CONECTA por camino fisico
 * Todo ocurre en una unica transaccion.
 *
 * Ademas crea restricciones de unicidad sobre Nodo.id e Item.id. Sin ellas,
 * dos cargas simultaneas sobre una base vacia (por ejemplo, la aplicacion y
 * la prueba de integracion) pueden crear el mismo id dos veces. Si ya hay ids
 * del dominio repetidos, la carga deja uno solo.
 *
 * Corre al terminar de crear los beans, ANTES de que el servidor web acepte
 * peticiones: ningun endpoint ve la base a medio sincronizar. (Un
 * CommandLineRunner correria despues de abrir el puerto.)
 *
 * Los nodos e items ajenos al dominio (por ejemplo, los de la semilla
 * generica del scaffold) se borran, porque alterarian los resultados. Con
 * app.seed.eliminar-ajenos=false solo se informan en el log. Pensado para
 * una base de desarrollo dedicada.
 *
 * Se desactiva con app.seed.enabled=false.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class CargaInicial implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(CargaInicial.class);

    private static final List<String> ETIQUETAS = List.of("Nodo", "Item");

    private final Neo4jClient neo4jClient;
    private final TransactionTemplate transaccion;
    private final boolean eliminarAjenos;

    public CargaInicial(Neo4jClient neo4jClient,
                        PlatformTransactionManager transactionManager,
                        @Value("${app.seed.eliminar-ajenos:true}") boolean eliminarAjenos) {
        this.neo4jClient = neo4jClient;
        this.transaccion = new TransactionTemplate(transactionManager);
        this.eliminarAjenos = eliminarAjenos;
    }

    @Override
    public void afterSingletonsInstantiated() {
        sincronizar();
    }

    public void sincronizar() {
        // Neo4j no admite cambios de esquema y de datos en la misma transaccion.
        // Si ya hay ids repetidos, la restriccion no se puede crear antes: se
        // reintenta despues de la carga, que los unifica.
        boolean conRestricciones = crearRestriccionesDeUnicidad(false);
        transaccion.executeWithoutResult(estado -> sincronizarEnTransaccion());
        if (!conRestricciones) {
            crearRestriccionesDeUnicidad(true);
        }
    }

    private boolean crearRestriccionesDeUnicidad(boolean avisarSiFalla) {
        boolean todas = true;
        for (String etiqueta : ETIQUETAS) {
            String nombre = etiqueta.toLowerCase(Locale.ROOT) + "_id_unico";
            try {
                // se consulta antes para no llenar el log con avisos de "ya existe"
                boolean existe = neo4jClient
                        .query("SHOW CONSTRAINTS YIELD name WHERE name = '" + nombre + "' RETURN count(*) > 0")
                        .fetchAs(Boolean.class).one().orElse(false);
                if (existe) {
                    continue;
                }
                // con ids repetidos Neo4j rechaza la restriccion y lo registra como error
                Collection<String> repetidos = idsRepetidos(etiqueta);
                if (!repetidos.isEmpty()) {
                    todas = false;
                    if (avisarSiFalla) {
                        log.warn("No se creo la restriccion de unicidad de {}.id porque hay ids repetidos: {}",
                                etiqueta, repetidos);
                    }
                    continue;
                }
                neo4jClient.query("CREATE CONSTRAINT " + nombre + " IF NOT EXISTS "
                        + "FOR (x:" + etiqueta + ") REQUIRE x.id IS UNIQUE").run();
            } catch (RuntimeException e) {
                if (esFallaDeConexion(e)) {
                    throw e; // sin base no tiene sentido seguir: la aplicacion no arranca
                }
                todas = false;
                if (avisarSiFalla) {
                    log.warn("No se pudo crear la restriccion de unicidad de {}.id: {}", etiqueta,
                            String.valueOf(e.getMessage()).replaceAll("\\s+", " "));
                }
            }
        }
        return todas;
    }

    /**
     * Ids que aparecen en mas de un nodo de la etiqueta. En una base nueva la
     * etiqueta o la propiedad id todavia no existen: no puede haber repetidos y
     * no se consulta, porque Neo4j avisaria que no las conoce.
     */
    private Collection<String> idsRepetidos(String etiqueta) {
        boolean hayEtiqueta = neo4jClient
                .query("CALL db.labels() YIELD label WHERE label = '" + etiqueta + "' RETURN count(*) > 0")
                .fetchAs(Boolean.class).one().orElse(false);
        boolean hayPropiedad = neo4jClient
                .query("CALL db.propertyKeys() YIELD propertyKey WHERE propertyKey = 'id' RETURN count(*) > 0")
                .fetchAs(Boolean.class).one().orElse(false);
        if (!hayEtiqueta || !hayPropiedad) {
            return List.of();
        }
        return neo4jClient.query("MATCH (x:" + etiqueta + ") WHERE x.id IS NOT NULL "
                        + "WITH x.id AS id, count(*) AS copias WHERE copias > 1 "
                        + "RETURN coalesce(toStringOrNull(id), '(otro tipo)') AS id")
                .fetchAs(String.class)
                .all();
    }

    private static boolean esFallaDeConexion(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof ServiceUnavailableException) {
                return true;
            }
        }
        return false;
    }

    private void sincronizarEnTransaccion() {
        List<String> idsLugares = new ArrayList<>();
        List<Map<String, Object>> lugares = new ArrayList<>();
        for (DatosDominio.Lugar l : DatosDominio.LUGARES) {
            idsLugares.add(l.id());
            lugares.add(Map.of("id", l.id(), "nombre", l.nombre(), "tipo", l.tipo()));
        }
        unificarRepetidos("Nodo", idsLugares);
        neo4jClient.query("""
                        UNWIND $lugares AS l
                        MERGE (n:Nodo {id: l.id})
                        SET n.nombre = l.nombre, n.tipo = l.tipo, n.valor = 0.0
                        """)
                .bindAll(Map.of("lugares", lugares))
                .run();

        List<Map<String, Object>> caminos = new ArrayList<>();
        for (DatosDominio.Camino c : DatosDominio.CAMINOS) {
            caminos.add(Map.of("origen", c.origen(), "destino", c.destino(), "km", c.km()));
        }
        neo4jClient.query("""
                        MATCH (a:Nodo)-[r:CONECTA]->(b:Nodo)
                        WHERE a.id IN $ids OR b.id IN $ids
                        DELETE r
                        """)
                .bindAll(Map.of("ids", idsLugares))
                .run();
        neo4jClient.query("""
                        UNWIND $caminos AS c
                        MATCH (a:Nodo {id: c.origen})
                        MATCH (b:Nodo {id: c.destino})
                        CREATE (a)-[:CONECTA {costo: c.km}]->(b)
                        """)
                .bindAll(Map.of("caminos", caminos))
                .run();

        List<String> idsPaquetes = new ArrayList<>();
        List<Map<String, Object>> paquetes = new ArrayList<>();
        for (Item p : DatosDominio.paquetes()) {
            idsPaquetes.add(p.getId());
            paquetes.add(Map.of("id", p.getId(), "nombre", p.getNombre(), "peso", p.getPeso(),
                    "valor", p.getValor(), "nodoId", p.getNodoId()));
        }
        unificarRepetidos("Item", idsPaquetes);
        neo4jClient.query("""
                        UNWIND $paquetes AS p
                        MERGE (i:Item {id: p.id})
                        SET i.nombre = p.nombre, i.peso = p.peso, i.valor = p.valor, i.nodoId = p.nodoId
                        """)
                .bindAll(Map.of("paquetes", paquetes))
                .run();

        revisarAjenos("Nodo", idsLugares, "Item", idsPaquetes);
        revisarAjenos("Item", idsPaquetes, "Nodo", idsLugares);

        log.info("Datos del dominio sincronizados: {} lugares, {} caminos, {} paquetes.",
                idsLugares.size(), caminos.size(), idsPaquetes.size());
    }

    /** Deja un solo nodo por cada id del dominio que aparezca repetido. */
    private void unificarRepetidos(String etiqueta, List<String> idsDelDominio) {
        Collection<String> repetidos = neo4jClient.query("MATCH (x:" + etiqueta + ") WHERE x.id IN $ids "
                        + "WITH x.id AS id, collect(x) AS copias WHERE size(copias) > 1 "
                        + "UNWIND tail(copias) AS sobrante DETACH DELETE sobrante RETURN DISTINCT id")
                .bindAll(Map.of("ids", idsDelDominio))
                .fetchAs(String.class)
                .all();
        if (!repetidos.isEmpty()) {
            log.warn("Habia nodos {} repetidos; se dejo uno por id: {}", etiqueta, repetidos);
        }
    }

    /**
     * Ajeno = cualquier nodo de la etiqueta cuyo id no es del dominio, incluidos
     * los que no tienen id o lo tienen de otro tipo (un numero, por ejemplo).
     *
     * Un lugar o paquete del dominio que ademas lleva la otra etiqueta (por
     * ejemplo, el lugar C marcado tambien como Item) no se borra: solo pierde
     * la etiqueta que le sobra.
     */
    private void revisarAjenos(String etiqueta, List<String> idsDelDominio,
                               String otraEtiqueta, List<String> idsDeLaOtra) {
        String filtro = "MATCH (x:" + etiqueta + ") WHERE (x.id IS NULL OR NOT x.id IN $ids) ";
        String idLegible = "coalesce(toStringOrNull(x.id), '(sin id)')";
        Map<String, Object> parametros = Map.of("ids", idsDelDominio, "idsOtra", idsDeLaOtra);
        if (!eliminarAjenos) {
            Collection<String> ajenos = neo4jClient.query(filtro + "RETURN " + idLegible + " AS id")
                    .bindAll(parametros)
                    .fetchAs(String.class)
                    .all();
            if (!ajenos.isEmpty()) {
                log.warn("La base tiene {} nodos {} ajenos al dominio: {}. Alteran los resultados de los "
                        + "algoritmos. Para borrarlos: app.seed.eliminar-ajenos=true", ajenos.size(), etiqueta, ajenos);
            }
            return;
        }
        Collection<String> corregidos = neo4jClient
                .query(filtro + "AND x:" + otraEtiqueta + " AND x.id IN $idsOtra "
                        + "REMOVE x:" + etiqueta + " RETURN x.id AS id")
                .bindAll(parametros)
                .fetchAs(String.class)
                .all();
        if (!corregidos.isEmpty()) {
            log.warn("Se quito la etiqueta {} a {} nodos del dominio que la tenian de mas: {}",
                    etiqueta, corregidos.size(), corregidos);
        }
        Collection<String> borrados = neo4jClient
                .query(filtro + "WITH x, " + idLegible + " AS id DETACH DELETE x RETURN id")
                .bindAll(parametros)
                .fetchAs(String.class)
                .all();
        if (!borrados.isEmpty()) {
            log.info("Se eliminaron {} nodos {} ajenos al dominio: {}", borrados.size(), etiqueta, borrados);
        }
    }
}
