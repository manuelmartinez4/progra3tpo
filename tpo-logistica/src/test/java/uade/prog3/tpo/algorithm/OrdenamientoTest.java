package uade.prog3.tpo.algorithm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uade.prog3.tpo.Fixtures;
import uade.prog3.tpo.model.Item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Los mismos casos se corren contra QuickSort y MergeSort.
 * No se usa ningun ordenamiento de biblioteca ni siquiera como oraculo:
 * se verifica que la salida este ordenada y tenga los mismos items.
 */
class OrdenamientoTest {

    private static final Ordenamiento ORDENAMIENTO = new Ordenamiento();
    private static final Comparator<Item> POR_PESO = Comparator.comparingDouble(Item::getPeso);
    private static final Comparator<Item> POR_VALOR = Comparator.comparingDouble(Item::getValor);
    private static final Comparator<Item> POR_RATIO = Comparator.comparingDouble(Item::ratioValorPeso);

    static Stream<Arguments> algoritmos() {
        BiFunction<List<Item>, Comparator<Item>, List<Item>> quick = ORDENAMIENTO::quickSort;
        BiFunction<List<Item>, Comparator<Item>, List<Item>> merge = ORDENAMIENTO::mergeSort;
        return Stream.of(Arguments.of("QuickSort", quick), Arguments.of("MergeSort", merge));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("algoritmos")
    void listaVaciaYUnElemento(String nombre, BiFunction<List<Item>, Comparator<Item>, List<Item>> ordenar) {
        assertEquals(List.of(), ordenar.apply(List.of(), POR_PESO));
        Item unico = Fixtures.item("U", 1, 1);
        assertEquals(List.of(unico), ordenar.apply(List.of(unico), POR_PESO));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("algoritmos")
    void ordenaLosPaquetesPorLosTresCriterios(String nombre, BiFunction<List<Item>, Comparator<Item>, List<Item>> ordenar) {
        List<Item> paquetes = Fixtures.paquetes();
        for (Comparator<Item> criterio : List.of(POR_PESO, POR_VALOR, POR_RATIO)) {
            List<Item> salida = ordenar.apply(paquetes, criterio);
            assertOrdenada(salida, criterio);
            assertMismosItems(paquetes, salida);
        }
        // por peso: P5(3) P4(4) {P2, P3}(5, empatados) P1(6)
        List<String> porPeso = ids(ordenar.apply(paquetes, POR_PESO));
        assertEquals(List.of("P5", "P4"), porPeso.subList(0, 2));
        assertEquals(Set.of("P2", "P3"), Set.copyOf(porPeso.subList(2, 4)));
        assertEquals("P1", porPeso.get(4));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("algoritmos")
    void yaOrdenadaEInversa(String nombre, BiFunction<List<Item>, Comparator<Item>, List<Item>> ordenar) {
        List<Item> ascendente = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            ascendente.add(Fixtures.item("I" + i, i, 1));
        }
        List<Item> descendente = new ArrayList<>();
        for (int i = 49; i >= 0; i--) {
            descendente.add(ascendente.get(i));
        }
        assertEquals(ascendente, ordenar.apply(ascendente, POR_PESO));
        assertEquals(ascendente, ordenar.apply(descendente, POR_PESO));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("algoritmos")
    void listasAleatoriasConEmpates(String nombre, BiFunction<List<Item>, Comparator<Item>, List<Item>> ordenar) {
        Random random = new Random(42);
        for (int prueba = 0; prueba < 30; prueba++) {
            List<Item> items = new ArrayList<>();
            int n = random.nextInt(60);
            for (int i = 0; i < n; i++) {
                items.add(Fixtures.item("R" + i, random.nextInt(8), random.nextInt(8)));
            }
            List<Item> salida = ordenar.apply(items, POR_PESO);
            assertOrdenada(salida, POR_PESO);
            assertMismosItems(items, salida);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("algoritmos")
    void noModificaLaEntrada(String nombre, BiFunction<List<Item>, Comparator<Item>, List<Item>> ordenar) {
        List<Item> entrada = new ArrayList<>(Fixtures.paquetes());
        List<Item> copia = new ArrayList<>(entrada);
        List<Item> salida = ordenar.apply(entrada, POR_VALOR);
        assertEquals(copia, entrada, "la lista de entrada no debe cambiar");
        assertNotSame(entrada, salida, "debe devolver una lista nueva");
    }

    @Test
    void mergeSortEsEstable() {
        // mismo peso, ids en orden de llegada: deben salir en ese mismo orden
        List<Item> items = List.of(
                Fixtures.item("x1", 2, 0), Fixtures.item("y1", 1, 0), Fixtures.item("x2", 2, 0),
                Fixtures.item("y2", 1, 0), Fixtures.item("x3", 2, 0), Fixtures.item("y3", 1, 0));
        assertEquals(List.of("y1", "y2", "y3", "x1", "x2", "x3"), ids(ORDENAMIENTO.mergeSort(items, POR_PESO)));
    }

    @Test
    void ordenarPorMezclaEsGenerico() {
        assertEquals(List.of(1, 2, 3, 5, 8), Ordenamiento.ordenarPorMezcla(List.of(5, 3, 8, 1, 2),
                Comparator.naturalOrder()));
    }

    @Test
    void entradaNullFalla() {
        assertThrows(IllegalArgumentException.class, () -> ORDENAMIENTO.quickSort(null, POR_PESO));
        assertThrows(IllegalArgumentException.class, () -> ORDENAMIENTO.mergeSort(null, POR_PESO));
    }

    private static void assertOrdenada(List<Item> lista, Comparator<Item> criterio) {
        for (int i = 1; i < lista.size(); i++) {
            assertTrue(criterio.compare(lista.get(i - 1), lista.get(i)) <= 0, "desordenado en la posicion " + i);
        }
    }

    private static void assertMismosItems(List<Item> esperados, List<Item> obtenidos) {
        assertEquals(contar(esperados), contar(obtenidos), "deben ser los mismos items, sin perder ni duplicar");
    }

    private static Map<String, Integer> contar(List<Item> items) {
        Map<String, Integer> cuenta = new HashMap<>();
        for (Item item : items) {
            cuenta.merge(item.getId(), 1, Integer::sum);
        }
        return cuenta;
    }

    private static List<String> ids(List<Item> items) {
        return items.stream().map(Item::getId).toList();
    }
}
