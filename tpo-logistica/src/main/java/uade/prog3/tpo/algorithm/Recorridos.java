package uade.prog3.tpo.algorithm;

import org.springframework.stereotype.Component;
import uade.prog3.tpo.service.Grafo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * UNIDAD: Grafos I (clase 4)          PUNTAJE: 2 puntos
 *
 * Recorridos sobre grafos. Es lo primero que conviene implementar:
 * es lo mas simple, vale 2 puntos, y ademas DFS es la base estructural
 * del backtracking que van a necesitar mas adelante.
 *
 * Los vecinos se visitan en el orden de la lista de adyacencia. GrafoService
 * la arma ordenada por id, asi el recorrido es reproducible.
 */
@Component
public class Recorridos {

    /**
     * Recorrido en profundidad desde un vertice, recursivo.
     *
     * Cada vertice entra una sola vez a la recursion (se marca al entrar) y
     * cada lista de adyacencia se recorre una sola vez.
     *
     * Complejidad: O(V + E) con lista de adyacencia.
     * Memoria: O(V) auxiliar (visitados + pila de recursion de hasta V llamadas).
     *
     * @param grafo grafo cargado en memoria
     * @param origenId id del vertice de partida
     * @return lista de ids en orden de visita
     */
    public List<String> dfs(Grafo grafo, String origenId) {
        int origen = grafo.indiceDe(origenId);
        boolean[] visitado = new boolean[grafo.cantidadVertices()];
        List<String> orden = new ArrayList<>();
        dfs(grafo, origen, visitado, orden);
        return orden;
    }

    private void dfs(Grafo grafo, int u, boolean[] visitado, List<String> orden) {
        visitado[u] = true;
        orden.add(grafo.idDe(u));
        for (Grafo.Arista a : grafo.vecinos(u)) {
            if (!visitado[a.destino()]) {
                dfs(grafo, a.destino(), visitado, orden);
            }
        }
    }

    /**
     * Recorrido en anchura desde un vertice, con cola FIFO.
     *
     * Se marca como visitado AL ENCOLAR: asi ningun vertice entra dos veces a
     * la cola aunque sea vecino de varios vertices del mismo nivel.
     *
     * Complejidad: O(V + E). Memoria: O(V) auxiliar (visitados + cola).
     */
    public List<String> bfs(Grafo grafo, String origenId) {
        int origen = grafo.indiceDe(origenId);
        boolean[] visitado = new boolean[grafo.cantidadVertices()];
        List<String> orden = new ArrayList<>();
        Deque<Integer> cola = new ArrayDeque<>();

        visitado[origen] = true;
        cola.add(origen);

        while (!cola.isEmpty()) {
            int u = cola.poll();
            orden.add(grafo.idDe(u));
            for (Grafo.Arista a : grafo.vecinos(u)) {
                if (!visitado[a.destino()]) {
                    visitado[a.destino()] = true;
                    cola.add(a.destino());
                }
            }
        }
        return orden;
    }
}
