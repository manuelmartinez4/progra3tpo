package uade.prog3.tpo.algorithm;

import org.junit.jupiter.api.Test;
import uade.prog3.tpo.Fixtures;
import uade.prog3.tpo.service.Grafo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ArbolRecubrimientoTest {

    private final ArbolRecubrimiento arbol = new ArbolRecubrimiento();

    /** Aristas esperadas del MST del dominio, como pares no ordenados. */
    private static final Set<String> MST_DOMINIO = Set.of("A-C", "B-C", "B-D", "D-E", "E-F", "F-H", "G-H");

    @Test
    void primDelDominio() {
        ArbolRecubrimiento.Mst mst = arbol.prim(Fixtures.redDelDominio(), "A");
        assertEquals(7, mst.aristas().size());
        assertEquals(18.0, mst.costoTotal());
        assertEquals(MST_DOMINIO, pares(mst));
        assertArbolValido(Fixtures.redDelDominio(), mst);
    }

    @Test
    void kruskalDelDominio() {
        ArbolRecubrimiento.Mst mst = arbol.kruskal(Fixtures.redDelDominio());
        assertEquals(7, mst.aristas().size());
        assertEquals(18.0, mst.costoTotal());
        assertEquals(MST_DOMINIO, pares(mst));
        assertArbolValido(Fixtures.redDelDominio(), mst);
    }

    @Test
    void kruskalAgregaLasAristasEnOrdenDeCosto() {
        List<ArbolRecubrimiento.AristaMst> aristas = arbol.kruskal(Fixtures.redDelDominio()).aristas();
        for (int i = 1; i < aristas.size(); i++) {
            assertTrue(aristas.get(i - 1).costo() <= aristas.get(i).costo());
        }
    }

    @Test
    void primDaElMismoCostoDesdeCualquierOrigen() {
        Grafo g = Fixtures.redDelDominio();
        for (String origen : g.ids()) {
            assertEquals(18.0, arbol.prim(g, origen).costoTotal(), "origen " + origen);
        }
    }

    @Test
    void conEmpatesSeVerificaCostoYValidez() {
        // cuadrado con diagonal, todo cuesta 1: hay varios MST validos
        Grafo g = new Grafo(false);
        g.agregarArista("W", "X", 1);
        g.agregarArista("X", "Y", 1);
        g.agregarArista("Y", "Z", 1);
        g.agregarArista("Z", "W", 1);
        g.agregarArista("W", "Y", 1);
        for (ArbolRecubrimiento.Mst mst : List.of(arbol.prim(g, "W"), arbol.kruskal(g))) {
            assertEquals(3, mst.aristas().size());
            assertEquals(3.0, mst.costoTotal());
            assertArbolValido(g, mst);
        }
    }

    @Test
    void grafoNoConexoNoTieneArbol() {
        Grafo g = Fixtures.redConLugarAislado();
        assertThrows(IllegalStateException.class, () -> arbol.prim(g, "A"));
        assertThrows(IllegalStateException.class, () -> arbol.kruskal(g));
    }

    @Test
    void grafoDirigidoSeRechaza() {
        Grafo g = new Grafo(true);
        g.agregarArista("X", "Y", 1);
        assertThrows(IllegalArgumentException.class, () -> arbol.prim(g, "X"));
        assertThrows(IllegalArgumentException.class, () -> arbol.kruskal(g));
    }

    @Test
    void unSoloVerticeDaUnArbolVacio() {
        Grafo g = new Grafo(false);
        g.agregarVertice("X");
        assertEquals(0, arbol.prim(g, "X").aristas().size());
        assertEquals(0.0, arbol.kruskal(g).costoTotal());
    }

    @Test
    void origenInexistenteFalla() {
        assertThrows(IllegalArgumentException.class, () -> arbol.prim(Fixtures.redDelDominio(), "Q"));
    }

    @Test
    void unionFindDetectaCiclos() {
        ArbolRecubrimiento.UnionFind uf = new ArbolRecubrimiento.UnionFind(4);
        assertTrue(uf.unir(0, 1));
        assertTrue(uf.unir(2, 3));
        assertNotEquals(uf.find(0), uf.find(2));
        assertTrue(uf.unir(1, 3));
        assertEquals(uf.find(0), uf.find(2));
        assertFalse(uf.unir(0, 2), "0 y 2 ya estan conectados: unirlos formaria un ciclo");
    }

    private static Set<String> pares(ArbolRecubrimiento.Mst mst) {
        Set<String> pares = new HashSet<>();
        for (ArbolRecubrimiento.AristaMst a : mst.aristas()) {
            String x = a.origen();
            String y = a.destino();
            pares.add(x.compareTo(y) < 0 ? x + "-" + y : y + "-" + x);
        }
        return pares;
    }

    /** V-1 aristas que existen en el grafo, sin ciclos y que conectan todos los vertices. */
    private static void assertArbolValido(Grafo g, ArbolRecubrimiento.Mst mst) {
        int n = g.cantidadVertices();
        assertEquals(n - 1, mst.aristas().size());
        ArbolRecubrimiento.UnionFind uf = new ArbolRecubrimiento.UnionFind(n);
        double suma = 0;
        for (ArbolRecubrimiento.AristaMst a : mst.aristas()) {
            int u = g.indiceDe(a.origen());
            int v = g.indiceDe(a.destino());
            assertTrue(g.vecinos(u).stream().anyMatch(x -> x.destino() == v && x.costo() == a.costo()),
                    "la arista " + a + " no existe en el grafo");
            assertTrue(uf.unir(u, v), "la arista " + a + " forma un ciclo");
            suma += a.costo();
        }
        assertEquals(mst.costoTotal(), suma);
    }
}
