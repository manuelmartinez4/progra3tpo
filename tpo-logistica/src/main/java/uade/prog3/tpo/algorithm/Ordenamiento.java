package uade.prog3.tpo.algorithm;

import org.springframework.stereotype.Component;
import uade.prog3.tpo.model.Item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * UNIDAD: Divide y Conquista (clase 2)     PUNTAJE: 1 punto
 *
 * IMPORTANTE: no vale llamar a Collections.sort ni a Arrays.sort.
 * El punto se asigna por implementar el algoritmo, no por usarlo.
 *
 * Ninguno de los dos metodos modifica la lista de entrada: ambos trabajan
 * sobre una copia y devuelven una lista nueva con los mismos elementos.
 */
@Component
public class Ordenamiento {

    /**
     * QuickSort sobre una lista de items.
     *
     * Pivote: mediana de tres (primero, medio y ultimo del subarreglo).
     * Evita el peor caso O(n^2) en listas ya ordenadas o en orden inverso,
     * que son frecuentes cuando se reordena una lista por otro criterio.
     * El peor caso sigue existiendo (por ejemplo, entradas armadas para que
     * la mediana de tres caiga siempre cerca de un extremo).
     *
     * Particion de Lomuto: el pivote se lleva al final, se recorre el
     * subarreglo dejando a la izquierda los menores y al final se ubica el
     * pivote en su posicion definitiva.
     *
     * Recurrencia: T(n) = T(k) + T(n-k-1) + O(n).
     *   promedio  T(n) = 2T(n/2) + O(n)  ->  O(n log n)
     *   peor caso T(n) = T(n-1) + O(n)   ->  O(n^2)
     * Memoria: O(n) por la copia + pila de recursion O(log n) promedio, O(n) peor.
     * No es estable.
     */
    public List<Item> quickSort(List<Item> items, Comparator<Item> criterio) {
        validar(items, criterio);
        List<Item> copia = new ArrayList<>(items);
        quickSort(copia, 0, copia.size() - 1, criterio);
        return copia;
    }

    private <T> void quickSort(List<T> lista, int desde, int hasta, Comparator<? super T> criterio) {
        if (desde >= hasta) {
            return;
        }
        int posicionPivote = particionar(lista, desde, hasta, criterio);
        quickSort(lista, desde, posicionPivote - 1, criterio);
        quickSort(lista, posicionPivote + 1, hasta, criterio);
    }

    /** Particion de Lomuto con pivote mediana de tres. Devuelve la posicion final del pivote. */
    private <T> int particionar(List<T> lista, int desde, int hasta, Comparator<? super T> criterio) {
        int medio = desde + (hasta - desde) / 2;
        int indicePivote = medianaDeTres(lista, desde, medio, hasta, criterio);
        intercambiar(lista, indicePivote, hasta);
        T pivote = lista.get(hasta);

        int frontera = desde; // lista[desde .. frontera-1] son menores que el pivote
        for (int i = desde; i < hasta; i++) {
            if (criterio.compare(lista.get(i), pivote) < 0) {
                intercambiar(lista, i, frontera);
                frontera++;
            }
        }
        intercambiar(lista, frontera, hasta);
        return frontera;
    }

    private <T> int medianaDeTres(List<T> lista, int a, int b, int c, Comparator<? super T> criterio) {
        T x = lista.get(a);
        T y = lista.get(b);
        T z = lista.get(c);
        if (criterio.compare(x, y) <= 0) {
            if (criterio.compare(y, z) <= 0) return b;      // x <= y <= z
            return criterio.compare(x, z) <= 0 ? c : a;     // x <= z < y  |  z < x <= y
        }
        if (criterio.compare(x, z) <= 0) return a;          // y < x <= z
        return criterio.compare(y, z) <= 0 ? c : b;         // y <= z < x  |  z < y < x
    }

    private <T> void intercambiar(List<T> lista, int i, int j) {
        T aux = lista.get(i);
        lista.set(i, lista.get(j));
        lista.set(j, aux);
    }

    /**
     * MergeSort sobre una lista de items. Delega en el nucleo generico
     * {@link #ordenarPorMezcla(List, Comparator)}, que tambien usan greedy,
     * Kruskal y la carga del grafo.
     */
    public List<Item> mergeSort(List<Item> items, Comparator<Item> criterio) {
        return ordenarPorMezcla(items, criterio);
    }

    /**
     * Nucleo de MergeSort generico y ESTABLE: ante empate del comparador se
     * toma primero el elemento de la mitad izquierda, asi los iguales
     * conservan su orden relativo de entrada.
     *
     * Recurrencia: T(n) = 2T(n/2) + O(n). Regla practica con a=2, b=2, k=1:
     * a = b^k, entonces T(n) = O(n^k log n) = O(n log n) en todos los casos.
     * Memoria: O(n) auxiliar (copia + arreglo de mezcla) y pila O(log n).
     */
    public static <T> List<T> ordenarPorMezcla(List<T> elementos, Comparator<? super T> criterio) {
        validar(elementos, criterio);
        List<T> copia = new ArrayList<>(elementos);
        List<T> auxiliar = new ArrayList<>(elementos);
        mergeSort(copia, auxiliar, 0, copia.size() - 1, criterio);
        return copia;
    }

    private static <T> void mergeSort(List<T> lista, List<T> auxiliar, int desde, int hasta,
                                      Comparator<? super T> criterio) {
        if (desde >= hasta) {
            return;
        }
        int medio = desde + (hasta - desde) / 2;
        mergeSort(lista, auxiliar, desde, medio, criterio);
        mergeSort(lista, auxiliar, medio + 1, hasta, criterio);
        mezclar(lista, auxiliar, desde, medio, hasta, criterio);
    }

    /** Mezcla lista[desde..medio] y lista[medio+1..hasta], ambas ya ordenadas. */
    private static <T> void mezclar(List<T> lista, List<T> auxiliar, int desde, int medio, int hasta,
                                    Comparator<? super T> criterio) {
        for (int i = desde; i <= hasta; i++) {
            auxiliar.set(i, lista.get(i));
        }
        int izq = desde;
        int der = medio + 1;
        int destino = desde;
        while (izq <= medio && der <= hasta) {
            // <= 0 y no < 0: el empate favorece a la izquierda, eso da la estabilidad
            if (criterio.compare(auxiliar.get(izq), auxiliar.get(der)) <= 0) {
                lista.set(destino++, auxiliar.get(izq++));
            } else {
                lista.set(destino++, auxiliar.get(der++));
            }
        }
        while (izq <= medio) {
            lista.set(destino++, auxiliar.get(izq++));
        }
        // lo que queda a la derecha ya esta en su lugar
    }

    private static void validar(List<?> elementos, Comparator<?> criterio) {
        if (elementos == null) {
            throw new IllegalArgumentException("La lista a ordenar no puede ser null");
        }
        if (criterio == null) {
            throw new IllegalArgumentException("El criterio de ordenamiento no puede ser null");
        }
    }
}
