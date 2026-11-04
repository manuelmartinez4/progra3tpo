package uade.prog3.tpo.algorithm;

import org.junit.jupiter.api.Test;
import uade.prog3.tpo.Fixtures;
import uade.prog3.tpo.service.Grafo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BacktrackingTest {

    private final Backtracking backtracking = new Backtracking();

    @Test
    void variasRutasDelDepositoALaTerminalDos() {
        List<Backtracking.Ruta> rutas = backtracking.rutasSimples(Fixtures.redDelDominio(), "A", "H", 20, 6);
        assertEquals(11, rutas.size());
        assertTrue(rutas.contains(new Backtracking.Ruta(List.of("A", "C", "B", "D", "E", "F", "H"), 17)));
        assertTrue(rutas.contains(new Backtracking.Ruta(List.of("A", "B", "D", "F", "H"), 19)));
        for (Backtracking.Ruta r : rutas) {
            assertRutaValida(Fixtures.redDelDominio(), r, "A", "H", 20, 6);
        }
    }

    @Test
    void limitesExactosSeIncluyen() {
        // la unica ruta de costo 17 tiene exactamente 6 saltos
        List<Backtracking.Ruta> rutas = backtracking.rutasSimples(Fixtures.redDelDominio(), "A", "H", 17, 6);
        assertEquals(List.of(new Backtracking.Ruta(List.of("A", "C", "B", "D", "E", "F", "H"), 17)), rutas);
        assertTrue(backtracking.rutasSimples(Fixtures.redDelDominio(), "A", "H", 17, 5).isEmpty(),
                "con 5 saltos no alcanza");
    }

    @Test
    void ningunaRuta() {
        assertTrue(backtracking.rutasSimples(Fixtures.redDelDominio(), "A", "H", 10, 10).isEmpty());
        assertTrue(backtracking.rutasSimples(Fixtures.redConLugarAislado(), "A", "Z", 1000, 10).isEmpty());
    }

    @Test
    void origenIgualADestino() {
        List<Backtracking.Ruta> rutas = backtracking.rutasSimples(Fixtures.redDelDominio(), "C", "C", 0, 0);
        assertEquals(List.of(new Backtracking.Ruta(List.of("C"), 0)), rutas);
    }

    @Test
    void sinLimitesEnumeraTodasLasRutasSimplesSinRepetirVertices() {
        // la red tiene ciclos: ninguna ruta debe repetir un lugar
        List<Backtracking.Ruta> rutas = backtracking.rutasSimples(Fixtures.redDelDominio(), "A", "H", 1000, 10);
        assertEquals(24, rutas.size());
        Set<List<String>> distintas = new HashSet<>();
        for (Backtracking.Ruta r : rutas) {
            assertRutaValida(Fixtures.redDelDominio(), r, "A", "H", 1000, 10);
            distintas.add(r.vertices());
        }
        assertEquals(24, distintas.size(), "no debe haber rutas repetidas");
    }

    @Test
    void conYSinPodaDevuelvenLasMismasRutas() {
        Grafo g = Fixtures.redDelDominio();
        int[][] limites = {{17, 6}, {18, 6}, {18, 5}, {20, 6}, {22, 7}, {25, 7}, {30, 6}, {1000, 10}, {12, 3}, {0, 0}};
        for (String origen : List.of("A", "C", "G")) {
            for (String destino : List.of("H", "B", "E")) {
                for (int[] l : limites) {
                    Backtracking.ResultadoRutas con = backtracking.buscarRutas(g, origen, destino, l[0], l[1], true);
                    Backtracking.ResultadoRutas sin = backtracking.buscarRutas(g, origen, destino, l[0], l[1], false);
                    assertEquals(sin.rutas(), con.rutas(), origen + "->" + destino + " " + l[0] + "/" + l[1]);
                    assertTrue(con.nodosExplorados() <= sin.nodosExplorados());
                }
            }
        }
    }

    @Test
    void metricasDeExploracionConYSinPoda() {
        Grafo g = Fixtures.redDelDominio();

        Backtracking.ResultadoRutas con = backtracking.buscarRutas(g, "A", "H", 20, 6, true);
        Backtracking.ResultadoRutas sin = backtracking.buscarRutas(g, "A", "H", 20, 6, false);
        assertEquals(49, con.nodosExplorados());
        assertEquals(16, con.descartadasPorCosto());
        assertEquals(0, con.descartadasPorSaltos());
        assertEquals(78, sin.nodosExplorados());
        assertEquals(0, sin.descartadasPorCosto() + sin.descartadasPorSaltos());

        Backtracking.ResultadoRutas ajustado = backtracking.buscarRutas(g, "A", "H", 18, 5, true);
        assertEquals(2, ajustado.rutas().size());
        assertEquals(34, ajustado.nodosExplorados());
        assertEquals(23, ajustado.descartadasPorCosto());
        assertEquals(2, ajustado.descartadasPorSaltos());
    }

    @Test
    void parametrosInvalidos() {
        Grafo g = Fixtures.redDelDominio();
        assertThrows(IllegalArgumentException.class, () -> backtracking.rutasSimples(g, "Q", "H", 10, 3));
        assertThrows(IllegalArgumentException.class, () -> backtracking.rutasSimples(g, "A", "Q", 10, 3));
        assertThrows(IllegalArgumentException.class, () -> backtracking.rutasSimples(g, "A", "H", -1, 3));
        assertThrows(IllegalArgumentException.class, () -> backtracking.rutasSimples(g, "A", "H", Double.NaN, 3));
        assertThrows(IllegalArgumentException.class, () -> backtracking.rutasSimples(g, "A", "H", 10, -1));
    }

    @Test
    void rechazaCostosNegativos() {
        Grafo g = Fixtures.redDelDominio();
        g.agregarArista("A", "H", -5);
        assertThrows(IllegalStateException.class, () -> backtracking.rutasSimples(g, "A", "H", 10, 3));
    }

    private static void assertRutaValida(Grafo g, Backtracking.Ruta r, String origen, String destino,
                                         double costoMaximo, int saltosMaximos) {
        List<String> v = r.vertices();
        assertEquals(origen, v.get(0));
        assertEquals(destino, v.get(v.size() - 1));
        assertEquals(v.size(), new HashSet<>(v).size(), "ruta con lugares repetidos: " + v);
        assertTrue(v.size() - 1 <= saltosMaximos);
        assertTrue(r.costoTotal() <= costoMaximo);
        double costo = 0;
        for (int i = 1; i < v.size(); i++) {
            int u = g.indiceDe(v.get(i - 1));
            int w = g.indiceDe(v.get(i));
            costo += g.vecinos(u).stream().filter(a -> a.destino() == w).findFirst()
                    .orElseThrow(() -> new AssertionError("no existe el camino " + v)).costo();
        }
        assertEquals(r.costoTotal(), costo);
    }
}
