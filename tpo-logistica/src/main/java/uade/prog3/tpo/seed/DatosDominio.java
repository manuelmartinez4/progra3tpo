package uade.prog3.tpo.seed;

import uade.prog3.tpo.model.Item;

import java.util.List;

/**
 * Datos del dominio logistico: una red chica de 8 lugares y 12 caminos y 5
 * paquetes. Es lo bastante chica para verificar cada resultado a mano.
 *
 * Unidades:
 *   costo de un camino  -> kilometros ficticios (enteros positivos)
 *   peso de un paquete  -> kilogramos (enteros positivos)
 *   valor de un paquete -> puntos de beneficio por entregarlo
 *
 * Cada camino es BIDIRECCIONAL y se guarda una sola vez, del id menor al
 * mayor. Los tests usan estas mismas listas para armar sus fixtures.
 */
public final class DatosDominio {

    public record Lugar(String id, String nombre, String tipo) { }

    public record Camino(String origen, String destino, double km) { }

    public static final List<Lugar> LUGARES = List.of(
            new Lugar("A", "Deposito central", "DEPOSITO"),
            new Lugar("B", "Punto Norte", "PUNTO_ENTREGA"),
            new Lugar("C", "Punto Este", "PUNTO_ENTREGA"),
            new Lugar("D", "Punto Sur", "PUNTO_ENTREGA"),
            new Lugar("E", "Punto Oeste", "PUNTO_ENTREGA"),
            new Lugar("F", "Centro de distribucion secundario", "CENTRO_DISTRIBUCION"),
            new Lugar("G", "Terminal uno", "TERMINAL"),
            new Lugar("H", "Terminal dos", "TERMINAL"));

    /**
     * El camino directo A-B cuesta 4, pero A-C-B cuesta 2+1 = 3: sirve para
     * verificar la relajacion de Dijkstra.
     */
    public static final List<Camino> CAMINOS = List.of(
            new Camino("A", "B", 4),
            new Camino("A", "C", 2),
            new Camino("B", "C", 1),
            new Camino("B", "D", 5),
            new Camino("C", "D", 8),
            new Camino("C", "E", 10),
            new Camino("D", "E", 2),
            new Camino("D", "F", 6),
            new Camino("E", "F", 3),
            new Camino("E", "G", 7),
            new Camino("F", "H", 4),
            new Camino("G", "H", 1));

    private DatosDominio() {
    }

    /**
     * Paquetes, nuevas instancias en cada llamada (Item es mutable).
     *
     * Con capacidad 10, greedy por ratio elige P1 y P4 (valor 16) y la
     * mochila 0/1 elige P2 y P3 (valor 18). Con 3 camiones el reparto optimo
     * tiene carga maxima 9.
     */
    public static List<Item> paquetes() {
        return List.of(
                new Item("P1", "Paquete 1", 6, 12, "B"),
                new Item("P2", "Paquete 2", 5, 9, "C"),
                new Item("P3", "Paquete 3", 5, 9, "D"),
                new Item("P4", "Paquete 4", 4, 4, "E"),
                new Item("P5", "Paquete 5", 3, 3, "H"));
    }
}
