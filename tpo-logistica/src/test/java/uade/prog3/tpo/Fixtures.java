package uade.prog3.tpo;

import uade.prog3.tpo.model.Item;
import uade.prog3.tpo.seed.DatosDominio;
import uade.prog3.tpo.service.Grafo;

import java.util.List;

/**
 * Fixtures en memoria: los tests de algoritmos corren sin Neo4j.
 *
 * La red del dominio se arma con los mismos datos que carga CargaInicial.
 * Los caminos estan ordenados por (origen, destino) y van del id menor al
 * mayor, asi que la lista de vecinos de cada lugar queda en orden alfabetico,
 * igual que la arma GrafoService.
 */
public final class Fixtures {

    private Fixtures() {
    }

    /** Red logistica del dominio: 8 lugares, 12 caminos bidireccionales. */
    public static Grafo redDelDominio() {
        Grafo g = new Grafo(false);
        for (DatosDominio.Lugar l : DatosDominio.LUGARES) {
            g.agregarVertice(l.id());
        }
        for (DatosDominio.Camino c : DatosDominio.CAMINOS) {
            g.agregarArista(c.origen(), c.destino(), c.km());
        }
        return g;
    }

    /** Red del dominio mas un lugar Z sin caminos. */
    public static Grafo redConLugarAislado() {
        Grafo g = redDelDominio();
        g.agregarVertice("Z");
        return g;
    }

    /** Los 5 paquetes del dominio, en orden de id. */
    public static List<Item> paquetes() {
        return DatosDominio.paquetes();
    }

    public static Item item(String id, double peso, double valor) {
        return new Item(id, "Item " + id, peso, valor, null);
    }
}
