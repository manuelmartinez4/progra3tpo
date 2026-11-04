package uade.prog3.tpo.algorithm;

import org.springframework.stereotype.Component;
import uade.prog3.tpo.model.Item;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * UNIDAD: Branch and Bound (clase 12)       PUNTAJE: 1 punto
 *
 * Es backtracking mas una cota. La diferencia con la clase Backtracking
 * es el criterio de poda: alli se podaba por VIABILIDAD, aca se poda por
 * OPTIMALIDAD.
 *
 * Problema: repartir TODOS los paquetes entre k camiones identicos
 * minimizando la carga (kg) del camion mas cargado.
 */
@Component
public class RamificacionYPoda {

    /**
     * Tope de contenedores. Protege la memoria del servidor: la respuesta
     * lista cada contenedor, aunque quede vacio.
     */
    public static final int MAX_CONTENEDORES = 100;

    /** Asignacion de items a contenedores o responsables. */
    public record Asignacion(Map<String, List<Item>> porContenedor, double cargaMaxima) { }

    /**
     * Resultado interno con metricas de la busqueda.
     *   nodosExplorados: entradas a la funcion recursiva, incluida la raiz
     *   ramasPodadas: hijos descartados porque su cota era >= la mejor solucion
     *   solucionesCompletas: hojas alcanzadas (cada una mejoro la anterior)
     * Una rama podada no equivale a la cantidad de descendientes que se evito explorar.
     */
    public record ResultadoReparto(Asignacion asignacion, long nodosExplorados,
                                   long ramasPodadas, long solucionesCompletas) { }

    /**
     * Branch and Bound en profundidad. Cada nivel decide a que camion va el
     * siguiente paquete (ramificar en k hijos).
     *
     * Cota inferior (optimista) de un nodo:
     *     max( mayor carga actual , techo(peso total / k) )
     * Es optimista porque ninguna carga ya asignada puede bajar al agregar
     * paquetes, y algun camion debe llevar al menos el promedio; con pesos
     * enteros ese promedio se redondea hacia arriba. Si algun peso no es
     * entero se usa el promedio sin redondear.
     *
     * Poda: si la cota de un hijo es >= la mejor carga maxima de una solucion
     * completa ya encontrada, ese hijo no puede mejorarla y se descarta.
     * El mejor valor arranca en infinito.
     *
     * Complejidad: hasta k^n asignaciones completas. Cota conservadora
     * O((n + k) * k^n) contando la copia de cada nueva mejor solucion.
     * Memoria: O(n + k) auxiliar (cargas, asignacion actual y mejor) mas la pila.
     */
    public Asignacion repartir(List<Item> items, int cantidadContenedores) {
        return repartirConMetricas(items, cantidadContenedores).asignacion();
    }

    public ResultadoReparto repartirConMetricas(List<Item> items, int cantidadContenedores) {
        if (items == null) {
            throw new IllegalArgumentException("La lista de items no puede ser null");
        }
        validarContenedores(cantidadContenedores);
        double total = 0;
        boolean todosEnteros = true;
        for (Item item : items) {
            if (item.getPeso() < 0) {
                throw new IllegalArgumentException("El peso no puede ser negativo: " + item.getId());
            }
            total += item.getPeso();
            todosEnteros &= item.getPeso() == Math.floor(item.getPeso());
        }
        double promedio = total / cantidadContenedores;
        double cotaGlobal = todosEnteros ? Math.ceil(promedio) : promedio;

        Busqueda b = new Busqueda(items, cantidadContenedores, cotaGlobal);
        b.explorar(0, 0);
        return new ResultadoReparto(armarAsignacion(items, cantidadContenedores, b.mejorAsignacion, b.mejorCargaMaxima),
                b.nodosExplorados, b.ramasPodadas, b.solucionesCompletas);
    }

    /** @throws IllegalArgumentException si k no esta entre 1 y MAX_CONTENEDORES */
    public static void validarContenedores(int cantidadContenedores) {
        if (cantidadContenedores < 1) {
            throw new IllegalArgumentException("La cantidad de contenedores debe ser al menos 1");
        }
        if (cantidadContenedores > MAX_CONTENEDORES) {
            throw new IllegalArgumentException("La cantidad de contenedores no puede superar " + MAX_CONTENEDORES);
        }
    }

    private Asignacion armarAsignacion(List<Item> items, int k, int[] contenedorDe, double cargaMaxima) {
        List<List<Item>> listas = new ArrayList<>(k);
        for (int c = 0; c < k; c++) {
            listas.add(new ArrayList<>());
        }
        for (int i = 0; i < items.size(); i++) {
            listas.get(contenedorDe[i]).add(items.get(i));
        }
        Map<String, List<Item>> porContenedor = new LinkedHashMap<>();
        for (int c = 0; c < k; c++) {
            porContenedor.put(nombreContenedor(c), listas.get(c));
        }
        return new Asignacion(porContenedor, cargaMaxima);
    }

    static String nombreContenedor(int indice) {
        return "Camion " + (indice + 1);
    }

    /** Estado de UNA busqueda. Se crea uno nuevo por llamada. */
    private static final class Busqueda {
        private final List<Item> items;
        private final int k;
        private final double cotaGlobal;

        private final double[] cargas;
        private final int[] asignacion;
        private int[] mejorAsignacion;
        private double mejorCargaMaxima = Double.POSITIVE_INFINITY;

        private long nodosExplorados;
        private long ramasPodadas;
        private long solucionesCompletas;

        Busqueda(List<Item> items, int k, double cotaGlobal) {
            this.items = items;
            this.k = k;
            this.cotaGlobal = cotaGlobal;
            this.cargas = new double[k];
            this.asignacion = new int[items.size()];
        }

        /**
         * @param i indice del proximo paquete a asignar
         * @param cargaMaximaActual mayor carga entre los camiones en este nodo
         */
        void explorar(int i, double cargaMaximaActual) {
            nodosExplorados++;
            if (i == items.size()) {
                // solo se llega aca si la cota era < mejor, asi que siempre mejora
                solucionesCompletas++;
                mejorCargaMaxima = cargaMaximaActual;
                mejorAsignacion = asignacion.clone(); // copia, no referencia al arreglo que sigue cambiando
                return;
            }
            double peso = items.get(i).getPeso();
            for (int c = 0; c < k; c++) {
                double cargaMaximaHijo = Math.max(cargaMaximaActual, cargas[c] + peso);
                double cota = Math.max(cargaMaximaHijo, cotaGlobal);
                if (cota >= mejorCargaMaxima) {
                    ramasPodadas++;
                    continue;
                }
                cargas[c] += peso;
                asignacion[i] = c;
                explorar(i + 1, cargaMaximaHijo);
                cargas[c] -= peso;
            }
        }
    }
}
