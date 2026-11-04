package uade.prog3.tpo.algorithm;

import org.junit.jupiter.api.Test;
import uade.prog3.tpo.Fixtures;
import uade.prog3.tpo.service.Grafo;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CaminosMinimosTest {

    private final CaminosMinimos caminos = new CaminosMinimos();

    // ---------------------------------------------------------------- Dijkstra

    @Test
    void relajaElCaminoDirecto() {
        // A-B directo cuesta 4, pero A-C-B cuesta 2+1 = 3
        CaminosMinimos.Camino c = caminos.dijkstra(Fixtures.redDelDominio(), "A", "B");
        assertEquals(List.of("A", "C", "B"), c.vertices());
        assertEquals(3.0, c.costoTotal());
    }

    @Test
    void caminoDelDepositoALaTerminalDos() {
        // 2 + 1 + 5 + 2 + 3 + 4 = 17
        CaminosMinimos.Camino c = caminos.dijkstra(Fixtures.redDelDominio(), "A", "H");
        assertEquals(List.of("A", "C", "B", "D", "E", "F", "H"), c.vertices());
        assertEquals(17.0, c.costoTotal());
    }

    @Test
    void origenIgualADestino() {
        CaminosMinimos.Camino c = caminos.dijkstra(Fixtures.redDelDominio(), "D", "D");
        assertEquals(List.of("D"), c.vertices());
        assertEquals(0.0, c.costoTotal());
    }

    @Test
    void destinoInalcanzableEsErrorDeDominio() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> caminos.dijkstra(Fixtures.redConLugarAislado(), "A", "Z"));
        assertTrue(e.getMessage().contains("No existe camino"));
    }

    @Test
    void rechazaCostosNegativos() {
        Grafo g = Fixtures.redDelDominio();
        g.agregarArista("G", "H", -1);
        assertThrows(IllegalStateException.class, () -> caminos.dijkstra(g, "A", "H"));
    }

    @Test
    void idInexistenteFalla() {
        assertThrows(IllegalArgumentException.class, () -> caminos.dijkstra(Fixtures.redDelDominio(), "A", "Q"));
        assertThrows(IllegalArgumentException.class, () -> caminos.dijkstra(Fixtures.redDelDominio(), "Q", "A"));
    }

    @Test
    void contadoresDeDijkstraDesdeA() {
        Grafo g = Fixtures.redDelDominio();
        CaminosMinimos.DistanciasDesde d = caminos.dijkstraDesde(g, g.indiceDe("A"));
        assertEquals(8, d.verticesFinalizados());
        assertEquals(24, d.relajaciones(), "cada arista se relaja una vez por sentido: 2E");
        assertEquals(11, d.mejoras());
    }

    // ---------------------------------------------------------------- Floyd-Warshall

    @Test
    void floydCoincideConOchoEjecucionesDeDijkstra() {
        Grafo g = Fixtures.redDelDominio();
        CaminosMinimos.ResultadoFloyd floyd = caminos.floydWarshallConMetricas(g);
        int finalizados = 0;
        int relajaciones = 0;
        int[] mejoras = new int[g.cantidadVertices()];
        for (int origen = 0; origen < g.cantidadVertices(); origen++) {
            CaminosMinimos.DistanciasDesde d = caminos.dijkstraDesde(g, origen);
            assertArrayEquals(d.distancias(), floyd.distancias()[origen], "fila de " + g.idDe(origen));
            finalizados += d.verticesFinalizados();
            relajaciones += d.relajaciones();
            mejoras[origen] = d.mejoras();
        }
        // metricas de cada tecnica, que NO miden lo mismo
        assertEquals(64, finalizados);
        assertEquals(192, relajaciones);
        assertArrayEquals(new int[] {11, 10, 10, 10, 9, 11, 10, 11}, mejoras, "mejoras desde A..H");
        assertEquals(82, Arrays.stream(mejoras).sum());
        assertEquals(8, floyd.verticesIntermedios());
        assertEquals(512, floyd.ternasEvaluadas());
    }

    @Test
    void floydEsSimetricoConDiagonalCero() {
        Grafo g = Fixtures.redDelDominio();
        double[][] d = caminos.floydWarshallConMetricas(g).distancias();
        for (int i = 0; i < d.length; i++) {
            assertEquals(0.0, d[i][i]);
            for (int j = 0; j < d.length; j++) {
                assertEquals(d[i][j], d[j][i], "los caminos son bidireccionales");
            }
        }
    }

    @Test
    void floydDevuelveUnMapaOrigenDestino() {
        Map<String, Double> m = caminos.floydWarshall(Fixtures.redDelDominio());
        assertEquals(64, m.size());
        assertEquals(17.0, m.get("A->H"));
        assertEquals(3.0, m.get("A->B"));
        assertEquals(0.0, m.get("C->C"));
        assertEquals(1.0, m.get("H->G"));
    }

    @Test
    void floydRepresentaLosParesSinCaminoConNull() {
        Map<String, Double> m = caminos.floydWarshall(Fixtures.redConLugarAislado());
        assertTrue(m.containsKey("A->Z"));
        assertNull(m.get("A->Z"));
        assertNull(m.get("Z->A"));
        assertEquals(0.0, m.get("Z->Z"));
    }

    @Test
    void floydDetectaCicloNegativo() {
        // grafo DIRIGIDO sintetico: X -> Y -> Z -> X suma -1
        Grafo g = new Grafo(true);
        g.agregarArista("X", "Y", 2);
        g.agregarArista("Y", "Z", -4);
        g.agregarArista("Z", "X", 1);
        assertThrows(IllegalStateException.class, () -> caminos.floydWarshall(g));
    }

    @Test
    void floydAdmiteAristasNegativasSinCicloNegativo() {
        Grafo g = new Grafo(true);
        g.agregarArista("X", "Y", 4);
        g.agregarArista("X", "Z", 1);
        g.agregarArista("Z", "Y", -2);
        Map<String, Double> m = caminos.floydWarshall(g);
        assertEquals(-1.0, m.get("X->Y"));
        assertNull(m.get("Y->X"));
    }

    @Test
    void ucsEsOpcionalYQuedaSinImplementar() {
        assertThrows(PendienteDeImplementar.class, () -> caminos.ucs(Fixtures.redDelDominio(), "A", "H"));
    }
}
