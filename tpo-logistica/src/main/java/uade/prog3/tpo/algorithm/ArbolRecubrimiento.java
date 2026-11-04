package uade.prog3.tpo.algorithm;

import org.springframework.stereotype.Component;
import uade.prog3.tpo.service.Grafo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * UNIDAD: Grafos II (clase 5)
 * PUNTAJE: forma parte de los 3 puntos de "Dijkstra, Prim, Kruskal"
 *
 * ATENCION: el arbol de recubrimiento minimo solo tiene sentido sobre un
 * grafo NO DIRIGIDO y CONEXO. Al cargar el grafo hay que pedirlo con
 * grafoService.cargar(false).
 *
 * En el dominio logistico el MST es la red de caminos de menor costo total
 * que mantiene conectados todos los lugares. NO es una ruta de reparto que
 * visite los puntos en orden.
 */
@Component
public class ArbolRecubrimiento {

    /** Una arista del arbol resultante, con ids de negocio. */
    public record AristaMst(String origen, String destino, double costo) { }

    /** Arbol de recubrimiento minimo completo. */
    public record Mst(List<AristaMst> aristas, double costoTotal) { }

    /** Arista no dirigida con indices, para ordenar en Kruskal. */
    private record AristaIndices(int u, int v, double costo) { }

    /**
     * Prim, variante con arreglos key[], parent[] e inMST[] y busqueda lineal
     * del siguiente vertice.
     *
     * Se eligio la busqueda lineal porque la red es chica y el codigo queda
     * mas simple que con heap. Cada una de las V iteraciones busca el minimo
     * en O(V) y recorre los vecinos del vertice agregado.
     *
     * Complejidad: O(V^2 + E) = O(V^2) en grafo simple. Memoria: O(V) auxiliar.
     * Conviene en grafos densos (E cercano a V^2), donde el heap no aporta.
     *
     * @throws IllegalArgumentException si el grafo es dirigido o el origen no existe
     * @throws IllegalStateException si el grafo no es conexo
     */
    public Mst prim(Grafo grafo, String origenId) {
        validarNoDirigido(grafo);
        int origen = grafo.indiceDe(origenId);
        int n = grafo.cantidadVertices();
        double[] key = new double[n];
        int[] parent = new int[n];
        boolean[] inMST = new boolean[n];
        for (int v = 0; v < n; v++) {
            key[v] = Grafo.INFINITO;
            parent[v] = -1;
        }
        key[origen] = 0;

        List<AristaMst> aristas = new ArrayList<>();
        double costoTotal = 0;
        for (int paso = 0; paso < n; paso++) {
            int u = -1;
            for (int v = 0; v < n; v++) {
                if (!inMST[v] && (u == -1 || key[v] < key[u])) {
                    u = v;
                }
            }
            if (key[u] == Grafo.INFINITO) {
                throw new IllegalStateException("El grafo no es conexo: no existe un arbol que abarque "
                        + "todos los nodos (" + grafo.idDe(u) + " no es alcanzable desde " + origenId + ")");
            }
            inMST[u] = true;
            if (parent[u] != -1) {
                aristas.add(new AristaMst(grafo.idDe(parent[u]), grafo.idDe(u), key[u]));
                costoTotal += key[u];
            }
            for (Grafo.Arista a : grafo.vecinos(u)) {
                int v = a.destino();
                if (!inMST[v] && a.costo() < key[v]) {
                    key[v] = a.costo();
                    parent[v] = u;
                }
            }
        }
        return new Mst(aristas, costoTotal);
    }

    /**
     * Kruskal: ordena TODAS las aristas por costo y las agrega salteando las
     * que formarian ciclo.
     *
     * Se extrae una sola copia de cada arista de la adyacencia bidireccional
     * (la que va del indice menor al mayor), se ordenan con el MergeSort
     * propio y se usa un Union-Find propio con compresion de caminos y union
     * por rango. Una arista forma ciclo si find(u) == find(v).
     *
     * Complejidad: O(E log E) por el ordenamiento + O(E alfa(V)) de Union-Find
     * + O(V) de inicializacion = O(E log E + V). Memoria: O(V + E).
     * Conviene en grafos ralos (E del orden de V).
     *
     * @throws IllegalArgumentException si el grafo es dirigido
     * @throws IllegalStateException si el grafo no es conexo
     */
    public Mst kruskal(Grafo grafo) {
        validarNoDirigido(grafo);
        int n = grafo.cantidadVertices();

        List<AristaIndices> todas = new ArrayList<>();
        for (int u = 0; u < n; u++) {
            for (Grafo.Arista a : grafo.vecinos(u)) {
                if (u < a.destino()) {
                    todas.add(new AristaIndices(u, a.destino(), a.costo()));
                }
            }
        }
        List<AristaIndices> ordenadas = Ordenamiento.ordenarPorMezcla(todas,
                Comparator.comparingDouble(AristaIndices::costo));

        UnionFind conjuntos = new UnionFind(n);
        List<AristaMst> aristas = new ArrayList<>();
        double costoTotal = 0;
        for (AristaIndices a : ordenadas) {
            if (aristas.size() == n - 1) {
                break;
            }
            if (conjuntos.unir(a.u(), a.v())) {
                aristas.add(new AristaMst(grafo.idDe(a.u()), grafo.idDe(a.v()), a.costo()));
                costoTotal += a.costo();
            }
        }
        if (n > 0 && aristas.size() < n - 1) {
            throw new IllegalStateException("El grafo no es conexo: no existe un arbol que abarque "
                    + "todos los nodos (se obtuvo un bosque de " + aristas.size() + " aristas, "
                    + "se necesitan " + (n - 1) + ")");
        }
        return new Mst(aristas, costoTotal);
    }

    private void validarNoDirigido(Grafo grafo) {
        if (grafo.esDirigido()) {
            throw new IllegalArgumentException("El arbol de recubrimiento minimo requiere un grafo no dirigido");
        }
    }

    /**
     * Union-Find (conjuntos disjuntos) con compresion de caminos y union por
     * rango. Con ambas mejoras cada operacion cuesta O(alfa(V)), practicamente
     * constante.
     */
    static final class UnionFind {
        private final int[] padre;
        private final int[] rango;

        UnionFind(int n) {
            padre = new int[n];
            rango = new int[n];
            for (int i = 0; i < n; i++) {
                padre[i] = i;
            }
        }

        /** Representante del conjunto de x. Comprime el camino recorrido. */
        int find(int x) {
            if (padre[x] != x) {
                padre[x] = find(padre[x]);
            }
            return padre[x];
        }

        /** Une los conjuntos de a y b. Devuelve false si ya estaban juntos (formaria ciclo). */
        boolean unir(int a, int b) {
            int raizA = find(a);
            int raizB = find(b);
            if (raizA == raizB) {
                return false;
            }
            if (rango[raizA] < rango[raizB]) {
                padre[raizA] = raizB;
            } else if (rango[raizA] > rango[raizB]) {
                padre[raizB] = raizA;
            } else {
                padre[raizB] = raizA;
                rango[raizA]++;
            }
            return true;
        }
    }
}
