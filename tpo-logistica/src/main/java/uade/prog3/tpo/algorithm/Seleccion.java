package uade.prog3.tpo.algorithm;

import org.springframework.stereotype.Component;
import uade.prog3.tpo.model.Item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * UNIDAD: Greedy (clase 3) y Programacion Dinamica (clase 6)
 * PUNTAJE: 1 punto greedy + 1 punto programacion dinamica
 *
 * Los dos metodos resuelven EL MISMO problema con tecnicas distintas:
 * elegir que paquetes cargar en un vehiculo de capacidad dada (kg).
 *
 * FUNCION OBJETIVO: maximizar la suma de los valores de los paquetes
 * elegidos, sujeto a que la suma de sus pesos no supere la capacidad.
 * Los paquetes son indivisibles: se llevan completos o no se llevan.
 *
 * Contraejemplo con los datos del dominio y capacidad 10:
 *   greedy elige P1 y P4 -> peso 10, valor 16
 *   dinamica elige P2 y P3 -> peso 10, valor 18 (optimo)
 */
@Component
public class Seleccion {

    public static final String TECNICA_GREEDY = "Greedy por ratio valor/peso";
    public static final String TECNICA_DINAMICA = "Programacion dinamica (mochila 0/1)";

    /** Resultado de una seleccion de items bajo restriccion de capacidad. */
    public record Resultado(List<Item> elegidos, double valorTotal, double pesoTotal, String tecnica) { }

    /** Orden greedy: ratio valor/peso descendente; ante empate, id ascendente. */
    static final Comparator<Item> RATIO_DESCENDENTE = Comparator
            .comparingDouble(Item::ratioValorPeso).reversed()
            .thenComparing(Item::getId);

    /**
     * GREEDY: ordena por ratio valor/peso descendente (MergeSort propio) y
     * toma cada paquete completo que todavia entre. Si uno no entra, sigue
     * revisando los siguientes.
     *
     * El desempate por id hace que el resultado sea reproducible aunque la
     * base devuelva los items en otro orden (P4 y P5 tienen ratio 1).
     *
     * Complejidad: O(n log n), dominada por el ordenamiento; la pasada de
     * seleccion es O(n). Memoria: O(n).
     *
     * Es una HEURISTICA: sobre paquetes indivisibles no garantiza el optimo.
     */
    public Resultado greedy(List<Item> items, double capacidad) {
        validarItems(items);
        if (Double.isNaN(capacidad) || capacidad < 0) {
            throw new IllegalArgumentException("La capacidad debe ser un numero mayor o igual a 0");
        }
        List<Item> ordenados = Ordenamiento.ordenarPorMezcla(items, RATIO_DESCENDENTE);

        List<Item> elegidos = new ArrayList<>();
        double peso = 0;
        double valor = 0;
        for (Item item : ordenados) {
            if (peso + item.getPeso() <= capacidad) {
                elegidos.add(item);
                peso += item.getPeso();
                valor += item.getValor();
            }
        }
        return new Resultado(elegidos, valor, peso, TECNICA_GREEDY);
    }

    /**
     * PROGRAMACION DINAMICA: mochila 0/1 con tabla dp bottom-up.
     *
     * Subproblema: dp[i][w] = mayor valor posible usando solo los primeros i
     * paquetes con capacidad w. Recurrencia:
     *   dp[0][w] = 0
     *   dp[i][w] = dp[i-1][w]                                  si peso_i > w
     *   dp[i][w] = max(dp[i-1][w], dp[i-1][w - peso_i] + valor_i)  si no
     *
     * Recuperacion: desde dp[n][W] hacia atras, si dp[i][w] != dp[i-1][w]
     * el paquete i fue tomado y se descuenta su peso.
     *
     * Los pesos deben ser enteros positivos porque indexan la tabla: no se
     * truncan decimales. La tabla guarda double para respetar el tipo de valor.
     *
     * Una capacidad mayor que la suma de los pesos no cambia la respuesta
     * (entran todos), asi que la tabla usa W = min(capacidad, suma de pesos).
     * Eso evita reservar una tabla enorme para una capacidad muy grande.
     *
     * Complejidad: O(n * W) en tiempo y en memoria. Es PSEUDOPOLINOMICA:
     * depende del valor numerico de W, que es exponencial en la cantidad de
     * bits con que se escribe W.
     */
    public Resultado programacionDinamica(List<Item> items, int capacidad) {
        validarItems(items);
        if (capacidad < 0) {
            throw new IllegalArgumentException("La capacidad no puede ser negativa");
        }
        int[] pesos = pesosEnteros(items);
        long sumaPesos = 0;
        for (int p : pesos) {
            sumaPesos += p;
        }
        int capacidadUtil = (int) Math.min(capacidad, sumaPesos);
        double[][] dp = tablaDp(items, capacidadUtil);
        int n = items.size();

        List<Item> elegidosAlReves = new ArrayList<>();
        int w = capacidadUtil;
        for (int i = n; i >= 1; i--) {
            if (dp[i][w] != dp[i - 1][w]) {
                elegidosAlReves.add(items.get(i - 1));
                w -= pesos[i - 1];
            }
        }
        List<Item> elegidos = new ArrayList<>();
        double peso = 0;
        for (int i = elegidosAlReves.size() - 1; i >= 0; i--) {
            Item item = elegidosAlReves.get(i);
            elegidos.add(item);
            peso += item.getPeso();
        }
        return new Resultado(elegidos, dp[n][capacidadUtil], peso, TECNICA_DINAMICA);
    }

    /**
     * Construye la tabla dp completa, de (n+1) filas por (W+1) columnas.
     * Es publica para poder mostrar la tabla en las verificaciones.
     */
    public double[][] tablaDp(List<Item> items, int capacidad) {
        validarItems(items);
        if (capacidad < 0) {
            throw new IllegalArgumentException("La capacidad no puede ser negativa");
        }
        int n = items.size();
        int[] pesos = pesosEnteros(items);
        double[][] dp = new double[n + 1][capacidad + 1];
        for (int i = 1; i <= n; i++) {
            int pesoI = pesos[i - 1];
            double valorI = items.get(i - 1).getValor();
            for (int w = 0; w <= capacidad; w++) {
                dp[i][w] = dp[i - 1][w];                       // no tomar el paquete i
                if (pesoI <= w && dp[i - 1][w - pesoI] + valorI > dp[i][w]) {
                    dp[i][w] = dp[i - 1][w - pesoI] + valorI;  // tomarlo
                }
            }
        }
        return dp;
    }

    private int[] pesosEnteros(List<Item> items) {
        int[] pesos = new int[items.size()];
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            double peso = item.getPeso();
            if (peso <= 0 || peso != Math.floor(peso) || peso > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("La mochila 0/1 requiere pesos enteros positivos: "
                        + item.getId() + " pesa " + peso);
            }
            pesos[i] = (int) peso;
        }
        return pesos;
    }

    private void validarItems(List<Item> items) {
        if (items == null) {
            throw new IllegalArgumentException("La lista de items no puede ser null");
        }
        for (Item item : items) {
            if (item.getPeso() < 0 || item.getValor() < 0) {
                throw new IllegalArgumentException("Peso y valor no pueden ser negativos: " + item.getId());
            }
        }
    }
}
