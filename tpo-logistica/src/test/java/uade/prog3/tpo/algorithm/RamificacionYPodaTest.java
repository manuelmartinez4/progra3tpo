package uade.prog3.tpo.algorithm;

import org.junit.jupiter.api.Test;
import uade.prog3.tpo.Fixtures;
import uade.prog3.tpo.model.Item;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RamificacionYPodaTest {

    private final RamificacionYPoda bb = new RamificacionYPoda();

    @Test
    void tresCamionesDanCargaMaxima9() {
        // 23 kg / 3 = 7,67 -> la cota inicial es 8, pero 8 es imposible:
        // el paquete de 6 iria solo y quedarian 17 kg para dos camiones de 8.
        RamificacionYPoda.Asignacion a = bb.repartir(Fixtures.paquetes(), 3);
        assertEquals(9.0, a.cargaMaxima());
        assertRepartoValido(Fixtures.paquetes(), 3, a);
    }

    @Test
    void casosBorde() {
        assertEquals(23.0, bb.repartir(Fixtures.paquetes(), 1).cargaMaxima());
        assertEquals(12.0, bb.repartir(Fixtures.paquetes(), 2).cargaMaxima());
        assertEquals(7.0, bb.repartir(Fixtures.paquetes(), 4).cargaMaxima(), "la cota es 6 pero el optimo es 7");
        assertEquals(6.0, bb.repartir(Fixtures.paquetes(), 5).cargaMaxima());
        assertEquals(6.0, bb.repartir(Fixtures.paquetes(), 7).cargaMaxima(), "con mas camiones que paquetes");
    }

    @Test
    void listaVaciaDaCargaCero() {
        RamificacionYPoda.Asignacion a = bb.repartir(List.of(), 3);
        assertEquals(0.0, a.cargaMaxima());
        assertEquals(3, a.porContenedor().size());
        a.porContenedor().values().forEach(l -> assertTrue(l.isEmpty()));
    }

    @Test
    void cantidadDeCamionesFueraDeRangoEsInvalida() {
        assertThrows(IllegalArgumentException.class, () -> bb.repartir(Fixtures.paquetes(), 0));
        assertThrows(IllegalArgumentException.class, () -> bb.repartir(null, 3));
        int tope = RamificacionYPoda.MAX_CONTENEDORES;
        assertEquals(tope, bb.repartir(Fixtures.paquetes(), tope).porContenedor().size());
        assertThrows(IllegalArgumentException.class, () -> bb.repartir(Fixtures.paquetes(), tope + 1));
        assertThrows(IllegalArgumentException.class, () -> bb.repartir(Fixtures.paquetes(), Integer.MAX_VALUE));
    }

    @Test
    void cadaPaqueteApareceExactamenteUnaVez() {
        for (int k = 1; k <= 6; k++) {
            assertRepartoValido(Fixtures.paquetes(), k, bb.repartir(Fixtures.paquetes(), k));
        }
    }

    @Test
    void coincideConLaFuerzaBruta() {
        for (int k = 1; k <= 4; k++) {
            assertEquals(fuerzaBruta(Fixtures.paquetes(), k), bb.repartir(Fixtures.paquetes(), k).cargaMaxima(), "k=" + k);
        }
        Random random = new Random(3);
        for (int prueba = 0; prueba < 30; prueba++) {
            List<Item> items = new ArrayList<>();
            int n = random.nextInt(7);
            for (int i = 0; i < n; i++) {
                items.add(Fixtures.item("R" + i, 1 + random.nextInt(10), 0));
            }
            int k = 1 + random.nextInt(4);
            RamificacionYPoda.Asignacion a = bb.repartir(items, k);
            assertEquals(fuerzaBruta(items, k), a.cargaMaxima(), "items " + items + " k=" + k);
            assertRepartoValido(items, k, a);
        }
    }

    @Test
    void metricasDePodaConTresCamiones() {
        RamificacionYPoda.ResultadoReparto r = bb.repartirConMetricas(Fixtures.paquetes(), 3);
        // el arbol completo tiene 1 + 3 + 9 + 27 + 81 + 243 = 364 nodos
        assertEquals(36, r.nodosExplorados());
        assertEquals(43, r.ramasPodadas());
        assertEquals(10, r.solucionesCompletas());
    }

    @Test
    void funcionaConPesosDecimales() {
        List<Item> items = List.of(Fixtures.item("X", 1.5, 0), Fixtures.item("Y", 1.5, 0), Fixtures.item("Z", 3, 0));
        assertEquals(3.0, bb.repartir(items, 2).cargaMaxima());
    }

    /** Enumera las k^n asignaciones y devuelve la menor carga maxima. */
    private static double fuerzaBruta(List<Item> items, int k) {
        int n = items.size();
        int total = 1;
        for (int i = 0; i < n; i++) {
            total *= k;
        }
        double mejor = Double.POSITIVE_INFINITY;
        for (int codigo = 0; codigo < total; codigo++) {
            double[] cargas = new double[k];
            int resto = codigo;
            for (Item item : items) {
                cargas[resto % k] += item.getPeso();
                resto /= k;
            }
            double max = 0;
            for (double c : cargas) {
                max = Math.max(max, c);
            }
            mejor = Math.min(mejor, max);
        }
        return mejor;
    }

    private static void assertRepartoValido(List<Item> items, int k, RamificacionYPoda.Asignacion a) {
        assertEquals(k, a.porContenedor().size());
        Set<String> vistos = new HashSet<>();
        double maxCarga = 0;
        for (List<Item> carga : a.porContenedor().values()) {
            double suma = 0;
            for (Item item : carga) {
                assertTrue(vistos.add(item.getId()), item.getId() + " aparece dos veces");
                suma += item.getPeso();
            }
            maxCarga = Math.max(maxCarga, suma);
        }
        assertEquals(items.size(), vistos.size(), "todos los paquetes deben repartirse");
        assertEquals(a.cargaMaxima(), maxCarga, "la carga maxima informada debe coincidir con la distribucion");
    }
}
