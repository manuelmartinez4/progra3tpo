package uade.prog3.tpo.algorithm;

import org.springframework.stereotype.Component;
import uade.prog3.tpo.service.Grafo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * UNIDAD: Grafos II (clase 5) y PD sobre grafos (clase 8)
 * PUNTAJE: forma parte de los 3 puntos de "Dijkstra, Prim, Kruskal"
 *          y de 1 punto de programacion dinamica si implementan Floyd.
 */
@Component
public class CaminosMinimos {

    /** Resultado de una consulta de camino minimo. */
    public record Camino(List<String> vertices, double costoTotal) { }

    /**
     * Resultado interno de Dijkstra desde un origen hacia todos los vertices.
     * Los contadores se usan en los tests y en docs/VERIFICACIONES.md:
     *   verticesFinalizados: vertices extraidos de la cola por primera vez
     *   relajaciones: comparaciones dist[u] + costo < dist[v] realizadas
     *   mejoras: relajaciones que efectivamente bajaron dist[v]
     */
    public record DistanciasDesde(int origen, double[] distancias, int[] predecesores,
                                  int verticesFinalizados, int relajaciones, int mejoras) { }

    /**
     * Resultado interno de Floyd-Warshall.
     *   verticesIntermedios: iteraciones del bucle externo k
     *   ternasEvaluadas: comparaciones d[i][k] + d[k][j] < d[i][j]
     */
    public record ResultadoFloyd(double[][] distancias, int verticesIntermedios, long ternasEvaluadas) { }

    /** Entrada inmutable de la cola de prioridad. Puede quedar obsoleta. */
    private record Entrada(double distancia, int vertice) { }

    /**
     * Dijkstra: camino de costo minimo desde un origen hasta un destino.
     *
     * Usa el nucleo {@link #dijkstraDesde(Grafo, int)}, que calcula las
     * distancias a todos los vertices, y reconstruye el camino con el arreglo
     * de predecesores.
     *
     * Complejidad: O((V + E) log V) en grafo simple. La cola admite entradas
     * repetidas (hasta una por mejora, O(E)) y log E = O(log V).
     * Memoria: O(V + E) auxiliar por la cola con entradas obsoletas.
     *
     * Es greedy: en cada paso finaliza el vertice no finalizado de menor
     * distancia y no lo vuelve a revisar. Con costos negativos esa decision
     * podria ser incorrecta, por eso se rechazan.
     *
     * @throws IllegalArgumentException si origen o destino no existen
     * @throws IllegalStateException si hay costos negativos o no hay camino
     */
    public Camino dijkstra(Grafo grafo, String origenId, String destinoId) {
        int origen = grafo.indiceDe(origenId);
        int destino = grafo.indiceDe(destinoId);
        DistanciasDesde resultado = dijkstraDesde(grafo, origen);
        double costo = resultado.distancias()[destino];
        if (costo == Grafo.INFINITO) {
            throw new IllegalStateException("No existe camino de " + origenId + " a " + destinoId);
        }
        return new Camino(reconstruirCamino(grafo, resultado.predecesores(), destino), costo);
    }

    /** Nucleo de Dijkstra: distancias minimas desde un origen a todos los vertices. */
    public DistanciasDesde dijkstraDesde(Grafo grafo, int origen) {
        validarCostosNoNegativos(grafo);
        int n = grafo.cantidadVertices();
        double[] dist = new double[n];
        int[] predecesor = new int[n];
        boolean[] finalizado = new boolean[n];
        Arrays.fill(dist, Grafo.INFINITO);
        Arrays.fill(predecesor, -1);

        PriorityQueue<Entrada> cola = new PriorityQueue<>(Comparator.comparingDouble(Entrada::distancia));
        dist[origen] = 0;
        cola.add(new Entrada(0, origen));

        int finalizados = 0;
        int relajaciones = 0;
        int mejoras = 0;
        while (!cola.isEmpty()) {
            Entrada actual = cola.poll();
            int u = actual.vertice();
            if (finalizado[u]) {
                continue; // entrada obsoleta: u ya salio antes con una distancia menor
            }
            finalizado[u] = true;
            finalizados++;

            for (Grafo.Arista a : grafo.vecinos(u)) {
                int v = a.destino();
                relajaciones++;
                double candidata = dist[u] + a.costo();
                if (candidata < dist[v]) {
                    dist[v] = candidata;
                    predecesor[v] = u;
                    cola.add(new Entrada(candidata, v));
                    mejoras++;
                }
            }
        }
        return new DistanciasDesde(origen, dist, predecesor, finalizados, relajaciones, mejoras);
    }

    private List<String> reconstruirCamino(Grafo grafo, int[] predecesor, int destino) {
        Deque<String> pila = new ArrayDeque<>();
        for (int v = destino; v != -1; v = predecesor[v]) {
            pila.push(grafo.idDe(v));
        }
        return new ArrayList<>(pila);
    }

    private void validarCostosNoNegativos(Grafo grafo) {
        for (int u = 0; u < grafo.cantidadVertices(); u++) {
            for (Grafo.Arista a : grafo.vecinos(u)) {
                if (a.costo() < 0) {
                    throw new IllegalStateException("Dijkstra no admite costos negativos: la arista "
                            + grafo.idDe(u) + "-" + grafo.idDe(a.destino()) + " cuesta " + a.costo());
                }
            }
        }
    }

    /**
     * Floyd-Warshall: costo minimo entre TODOS los pares de vertices.
     *
     * Devuelve un mapa "origen->destino" con el costo minimo de cada par.
     * Los pares sin camino se devuelven con valor null (JSON no tiene
     * infinito). El mapa respeta el orden de los ids del grafo.
     *
     * @throws IllegalStateException si hay un ciclo de costo negativo
     */
    public Map<String, Double> floydWarshall(Grafo grafo) {
        double[][] d = floydWarshallConMetricas(grafo).distancias();
        Map<String, Double> salida = new LinkedHashMap<>();
        for (int i = 0; i < d.length; i++) {
            for (int j = 0; j < d.length; j++) {
                Double costo = d[i][j] == Grafo.INFINITO ? null : d[i][j];
                salida.put(grafo.idDe(i) + "->" + grafo.idDe(j), costo);
            }
        }
        return salida;
    }

    /**
     * Nucleo de Floyd-Warshall sobre la matriz de adyacencia.
     *
     * Subproblema: d_k[i][j] = costo minimo de i a j usando como intermedios
     * solo los vertices 0..k. Recurrencia:
     *     d_k[i][j] = min( d_{k-1}[i][j] , d_{k-1}[i][k] + d_{k-1}[k][j] )
     * Por eso k va SIEMPRE por fuera: cuando se usa d[i][k] y d[k][j], esas
     * entradas ya consideran todos los intermedios anteriores a k.
     *
     * Complejidad: O(V^3) en tiempo, O(V^2) en memoria (una sola matriz).
     */
    public ResultadoFloyd floydWarshallConMetricas(Grafo grafo) {
        double[][] d = grafo.matrizDeAdyacencia();
        int n = d.length;
        long ternas = 0;
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    ternas++;
                    if (d[i][k] + d[k][j] < d[i][j]) {
                        d[i][j] = d[i][k] + d[k][j];
                    }
                }
            }
        }
        for (int i = 0; i < n; i++) {
            if (d[i][i] < 0) {
                throw new IllegalStateException(
                        "El grafo tiene un ciclo de costo negativo que pasa por " + grafo.idDe(i));
            }
        }
        return new ResultadoFloyd(d, n, ternas);
    }

    /**
     * UCS (Uniform Cost Search): igual que Dijkstra pero con parada temprana
     * al expandir el destino.
     *
     * OPCIONAL y NO IMPLEMENTADO: no pertenece a los rubros puntuados de la
     * rubrica. Queda fuera de la demostracion y del inventario de algoritmos
     * implementados. El endpoint responde 501.
     */
    public Camino ucs(Grafo grafo, String origenId, String destinoId) {
        throw new PendienteDeImplementar("UCS (opcional, fuera del alcance)");
    }
}
