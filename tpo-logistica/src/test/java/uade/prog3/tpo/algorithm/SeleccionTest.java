package uade.prog3.tpo.algorithm;

import org.junit.jupiter.api.Test;
import uade.prog3.tpo.Fixtures;
import uade.prog3.tpo.model.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class SeleccionTest {

    private final Seleccion seleccion = new Seleccion();

    @Test
    void greedyEligeP1YP4ConCapacidad10() {
        Seleccion.Resultado r = seleccion.greedy(Fixtures.paquetes(), 10);
        assertEquals(List.of("P1", "P4"), ids(r.elegidos()));
        assertEquals(16.0, r.valorTotal());
        assertEquals(10.0, r.pesoTotal());
        assertEquals(Seleccion.TECNICA_GREEDY, r.tecnica());
    }

    @Test
    void dinamicaEligeP2YP3ConCapacidad10() {
        Seleccion.Resultado r = seleccion.programacionDinamica(Fixtures.paquetes(), 10);
        assertEquals(List.of("P2", "P3"), ids(r.elegidos()));
        assertEquals(18.0, r.valorTotal());
        assertEquals(10.0, r.pesoTotal());
        assertEquals(Seleccion.TECNICA_DINAMICA, r.tecnica());
    }

    @Test
    void contraejemploGreedyNoEsOptimo() {
        double greedy = seleccion.greedy(Fixtures.paquetes(), 10).valorTotal();
        double optimo = seleccion.programacionDinamica(Fixtures.paquetes(), 10).valorTotal();
        assertTrue(greedy < optimo, "con capacidad 10 greedy obtiene 16 y el optimo es 18");
    }

    @Test
    void tablaDpDelEjemplo() {
        double[][] dp = seleccion.tablaDp(Fixtures.paquetes(), 10);
        assertEquals(6, dp.length);
        assertEquals(11, dp[0].length);
        assertArrayEquals(new double[]{0, 0, 0, 0, 0, 0, 12, 12, 12, 12, 12}, dp[1]);
        assertArrayEquals(new double[]{0, 0, 0, 0, 0, 9, 12, 12, 12, 12, 12}, dp[2]);
        assertArrayEquals(new double[]{0, 0, 0, 0, 0, 9, 12, 12, 12, 12, 18}, dp[3]);
        assertArrayEquals(new double[]{0, 0, 0, 0, 4, 9, 12, 12, 12, 13, 18}, dp[4]);
        assertArrayEquals(new double[]{0, 0, 0, 3, 4, 9, 12, 12, 12, 15, 18}, dp[5]);
    }

    @Test
    void greedyEsReproducibleAunqueCambieElOrdenDeEntrada() {
        List<Item> invertidos = new ArrayList<>();
        List<Item> paquetes = Fixtures.paquetes();
        for (int i = paquetes.size() - 1; i >= 0; i--) {
            invertidos.add(paquetes.get(i));
        }
        // P4 y P5 empatan con ratio 1: el desempate por id elige siempre P4 primero
        assertEquals(List.of("P1", "P4"), ids(seleccion.greedy(invertidos, 10).elegidos()));
        assertEquals(List.of("P1", "P2", "P4"), ids(seleccion.greedy(invertidos, 15).elegidos()));
    }

    @Test
    void capacidadCero() {
        assertTrue(seleccion.greedy(Fixtures.paquetes(), 0).elegidos().isEmpty());
        assertTrue(seleccion.programacionDinamica(Fixtures.paquetes(), 0).elegidos().isEmpty());
    }

    @Test
    void ningunPaqueteEntra() {
        assertEquals(0.0, seleccion.greedy(Fixtures.paquetes(), 2).valorTotal());
        assertEquals(0.0, seleccion.programacionDinamica(Fixtures.paquetes(), 2).valorTotal());
    }

    @Test
    void entranTodos() {
        assertEquals(5, seleccion.greedy(Fixtures.paquetes(), 23).elegidos().size());
        Seleccion.Resultado r = seleccion.programacionDinamica(Fixtures.paquetes(), 30);
        assertEquals(5, r.elegidos().size());
        assertEquals(37.0, r.valorTotal());
        assertEquals(23.0, r.pesoTotal());
    }

    @Test
    void listaVacia() {
        assertTrue(seleccion.greedy(List.of(), 10).elegidos().isEmpty());
        assertEquals(0.0, seleccion.programacionDinamica(List.of(), 10).valorTotal());
    }

    @Test
    void capacidadMuchoMayorQueLaSumaDePesosNoArmaUnaTablaEnorme() {
        // la tabla usa min(capacidad, 23): sin eso, capacidad + 1 desborda o agota la memoria
        Seleccion.Resultado r = seleccion.programacionDinamica(Fixtures.paquetes(), Integer.MAX_VALUE);
        assertEquals(List.of("P1", "P2", "P3", "P4", "P5"), r.elegidos().stream().map(Item::getId).toList());
        assertEquals(37.0, r.valorTotal());
        assertEquals(23.0, r.pesoTotal());
    }

    @Test
    void capacidadNegativaFalla() {
        assertThrows(IllegalArgumentException.class, () -> seleccion.greedy(Fixtures.paquetes(), -1));
        assertThrows(IllegalArgumentException.class, () -> seleccion.programacionDinamica(Fixtures.paquetes(), -1));
    }

    @Test
    void capacidadNaNFalla() {
        // NaN < 0 es falso: sin validarlo aparte, greedy devolveria una seleccion vacia
        assertThrows(IllegalArgumentException.class, () -> seleccion.greedy(Fixtures.paquetes(), Double.NaN));
    }

    @Test
    void dinamicaRechazaPesosNoEnteros() {
        List<Item> items = List.of(Fixtures.item("X", 2.5, 10));
        assertThrows(IllegalArgumentException.class, () -> seleccion.programacionDinamica(items, 10));
    }

    @Test
    void dinamicaCoincideConLaEnumeracionDeSubconjuntos() {
        List<Item> paquetes = Fixtures.paquetes();
        for (int capacidad = 0; capacidad <= 25; capacidad++) {
            assertSeleccionOptima(paquetes, capacidad);
        }
        Random random = new Random(7);
        for (int prueba = 0; prueba < 40; prueba++) {
            List<Item> items = new ArrayList<>();
            int n = 1 + random.nextInt(8);
            for (int i = 0; i < n; i++) {
                items.add(Fixtures.item("R" + i, 1 + random.nextInt(9), random.nextInt(20)));
            }
            assertSeleccionOptima(items, random.nextInt(25));
        }
    }

    @Test
    void greedyNuncaSuperaAlOptimo() {
        Random random = new Random(11);
        for (int prueba = 0; prueba < 40; prueba++) {
            List<Item> items = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                items.add(Fixtures.item("R" + i, 1 + random.nextInt(9), random.nextInt(20)));
            }
            int capacidad = random.nextInt(25);
            Seleccion.Resultado g = seleccion.greedy(items, capacidad);
            assertTrue(g.pesoTotal() <= capacidad);
            assertTrue(g.valorTotal() <= seleccion.programacionDinamica(items, capacidad).valorTotal());
        }
    }

    /** Compara la mochila con la fuerza bruta (2^n subconjuntos) y valida la recuperacion. */
    private void assertSeleccionOptima(List<Item> items, int capacidad) {
        Seleccion.Resultado r = seleccion.programacionDinamica(items, capacidad);
        double mejor = 0;
        for (int mascara = 0; mascara < (1 << items.size()); mascara++) {
            double peso = 0;
            double valor = 0;
            for (int i = 0; i < items.size(); i++) {
                if ((mascara & (1 << i)) != 0) {
                    peso += items.get(i).getPeso();
                    valor += items.get(i).getValor();
                }
            }
            if (peso <= capacidad && valor > mejor) {
                mejor = valor;
            }
        }
        assertEquals(mejor, r.valorTotal(), "capacidad " + capacidad + " items " + items);
        double peso = 0;
        double valor = 0;
        for (Item item : r.elegidos()) {
            peso += item.getPeso();
            valor += item.getValor();
        }
        assertEquals(r.valorTotal(), valor, "los elegidos deben sumar el valor informado");
        assertEquals(r.pesoTotal(), peso);
        assertTrue(peso <= capacidad);
        assertEquals(r.elegidos().size(), r.elegidos().stream().distinct().count(), "sin repetir items");
    }

    private static List<String> ids(List<Item> items) {
        return items.stream().map(Item::getId).toList();
    }
}
