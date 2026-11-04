package uade.prog3.tpo.algorithm;

import org.junit.jupiter.api.Test;
import uade.prog3.tpo.Fixtures;
import uade.prog3.tpo.service.Grafo;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RecorridosTest {

    private final Recorridos recorridos = new Recorridos();

    @Test
    void bfsDelDominioRecorrePorNiveles() {
        assertEquals(List.of("A", "B", "C", "D", "E", "F", "G", "H"),
                recorridos.bfs(Fixtures.redDelDominio(), "A"));
    }

    @Test
    void dfsDelDominioProfundizaAntesDeRetroceder() {
        assertEquals(List.of("A", "B", "C", "D", "E", "F", "H", "G"),
                recorridos.dfs(Fixtures.redDelDominio(), "A"));
    }

    @Test
    void bfsVisitaEnNivelesNoDecrecientes() {
        // saltos minimos desde A, calculados a mano sobre la red
        Map<String, Integer> nivel = Map.of("A", 0, "B", 1, "C", 1, "D", 2,
                "E", 2, "F", 3, "G", 3, "H", 4);
        List<String> orden = recorridos.bfs(Fixtures.redDelDominio(), "A");
        for (int i = 1; i < orden.size(); i++) {
            assertTrue(nivel.get(orden.get(i - 1)) <= nivel.get(orden.get(i)),
                    "BFS no puede volver a un nivel anterior: " + orden);
        }
    }

    @Test
    void cadaNodoAlcanzableApareceUnaSolaVezDesdeCualquierOrigen() {
        Grafo g = Fixtures.redDelDominio();
        for (String origen : g.ids()) {
            List<String> bfs = recorridos.bfs(g, origen);
            List<String> dfs = recorridos.dfs(g, origen);
            assertEquals(8, bfs.size());
            assertEquals(8, new HashSet<>(bfs).size());
            assertEquals(8, dfs.size());
            assertEquals(8, new HashSet<>(dfs).size());
            assertEquals(origen, bfs.get(0));
            assertEquals(origen, dfs.get(0));
        }
    }

    @Test
    void grafoConCicloNoEntraEnBucle() {
        Grafo triangulo = new Grafo(false);
        triangulo.agregarArista("X", "Y", 1);
        triangulo.agregarArista("Y", "Z", 1);
        triangulo.agregarArista("Z", "X", 1);
        assertEquals(List.of("X", "Y", "Z"), recorridos.bfs(triangulo, "X"));
        assertEquals(List.of("X", "Y", "Z"), recorridos.dfs(triangulo, "X"));
    }

    @Test
    void nodoAisladoSoloSeVisitaASiMismo() {
        Grafo g = Fixtures.redConLugarAislado();
        assertEquals(List.of("Z"), recorridos.bfs(g, "Z"));
        assertEquals(List.of("Z"), recorridos.dfs(g, "Z"));
        assertFalse(recorridos.bfs(g, "A").contains("Z"));
        assertFalse(recorridos.dfs(g, "A").contains("Z"));
    }

    @Test
    void origenInexistenteFalla() {
        Grafo g = Fixtures.redDelDominio();
        assertThrows(IllegalArgumentException.class, () -> recorridos.bfs(g, "Q"));
        assertThrows(IllegalArgumentException.class, () -> recorridos.dfs(g, "Q"));
    }
}
