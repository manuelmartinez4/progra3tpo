package uade.prog3.tpo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uade.prog3.tpo.algorithm.Ordenamiento;
import uade.prog3.tpo.model.Conexion;
import uade.prog3.tpo.model.Nodo;
import uade.prog3.tpo.repository.NodoRepository;

import java.util.Comparator;
import java.util.List;

/**
 * Carga el grafo desde Neo4j a memoria.
 *
 * INFRAESTRUCTURA: no forma parte de lo que hay que implementar.
 *
 * Decision de diseno importante para el trabajo: los algoritmos NO consultan
 * la base. Se carga el grafo una vez, se opera en memoria, y se devuelve el
 * resultado. Asi la complejidad temporal que se mide y se justifica en el
 * coloquio es la del algoritmo, no la de la red ni la del motor de base.
 */
@Service
public class GrafoService {

    private static final Logger log = LoggerFactory.getLogger(GrafoService.class);

    private final NodoRepository nodoRepository;

    public GrafoService(NodoRepository nodoRepository) {
        this.nodoRepository = nodoRepository;
    }

    /**
     * @param dirigido true si las relaciones CONECTA tienen sentido unico.
     *                 Para el arbol de recubrimiento minimo (Prim y Kruskal)
     *                 el grafo DEBE cargarse como NO dirigido.
     */
    public Grafo cargar(boolean dirigido) {
        Grafo grafo = new Grafo(dirigido);
        // Neo4j no garantiza el orden de los nodos ni de sus relaciones.
        // Se ordenan por id (MergeSort propio) para que los indices y el orden
        // de los vecinos sean siempre los mismos: asi BFS, DFS, backtracking y
        // los desempates de Prim y Kruskal dan resultados reproducibles.
        // Costo de la carga: O(V log V + E log E), fuera de los algoritmos.
        List<Nodo> nodos = Ordenamiento.ordenarPorMezcla(nodoRepository.findAll(),
                Comparator.comparing(Nodo::getId));

        // 1) primero todos los vertices, para que queden en el grafo
        //    incluso los que no tienen ninguna arista
        for (Nodo n : nodos) {
            grafo.agregarVertice(n.getId());
        }

        // 2) despues las aristas, con su costo. Cada camino fisico esta
        //    guardado una sola vez; si el grafo es no dirigido, Grafo agrega
        //    la inversa.
        for (Nodo n : nodos) {
            List<Conexion> conexiones = Ordenamiento.ordenarPorMezcla(n.getConexiones(),
                    Comparator.comparing((Conexion c) -> c.getDestino().getId()));
            for (Conexion c : conexiones) {
                grafo.agregarArista(n.getId(), c.getDestino().getId(), c.getCosto());
            }
        }

        log.info("Grafo cargado: {} vertices, {} aristas, dirigido={}",
                grafo.cantidadVertices(), grafo.cantidadAristas(), dirigido);
        return grafo;
    }
}
