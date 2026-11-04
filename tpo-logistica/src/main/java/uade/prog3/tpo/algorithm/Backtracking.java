package uade.prog3.tpo.algorithm;

import org.springframework.stereotype.Component;
import uade.prog3.tpo.service.Grafo;

import java.util.ArrayList;
import java.util.List;

/**
 * UNIDAD: Backtracking (clases 9 y 10)      PUNTAJE: 1 punto
 *
 * Problema: enumerar todas las rutas simples (sin repetir lugares) entre un
 * origen y un destino que no superen un costo maximo en km ni una cantidad
 * maxima de saltos. Un salto es una arista recorrida: una ruta de 4 nodos
 * tiene 3 saltos.
 *
 * Cada busqueda usa su propio estado (camino, visitados, contadores): el
 * componente no guarda estado mutable entre peticiones.
 */
@Component
public class Backtracking {

    /** Un camino simple encontrado, con su costo acumulado. */
    public record Ruta(List<String> vertices, double costoTotal) { }

    /**
     * Resultado interno con metricas de exploracion.
     *   nodosExplorados: entradas a la funcion recursiva, incluida la raiz
     *   descartadasPorCosto: ramas no abiertas porque la arista excedia el costo maximo
     *   descartadasPorSaltos: ramas no abiertas porque la arista excedia los saltos maximos
     * Una rama descartada se cuenta en la primera condicion violada (se evalua primero el costo).
     */
    public record ResultadoRutas(List<Ruta> rutas, long nodosExplorados,
                                 long descartadasPorCosto, long descartadasPorSaltos) { }

    /**
     * Enumera TODOS los caminos simples de origen a destino que no superen
     * un costo maximo ni una cantidad maxima de saltos.
     *
     * Complejidad: exponencial en el peor caso. En un grafo completo hay del
     * orden de (V-2)! caminos simples entre dos vertices, y cada estado hace
     * trabajo O(V): cota O(V * V!). La poda reduce la exploracion en la
     * practica pero no cambia el peor caso.
     * Memoria: O(V) para camino, visitados y pila + O(L) para las rutas
     * devueltas, con L la suma de sus longitudes.
     */
    public List<Ruta> rutasSimples(Grafo grafo, String origenId, String destinoId,
                                   double costoMaximo, int saltosMaximos) {
        return buscarRutas(grafo, origenId, destinoId, costoMaximo, saltosMaximos, true).rutas();
    }

    /**
     * Busqueda con metricas. Con conPoda=false es la variante de referencia:
     * enumera todos los caminos simples y aplica los limites recien al llegar
     * al destino. Ambas variantes devuelven el mismo conjunto de rutas.
     *
     * @throws IllegalArgumentException si un id no existe o un limite es negativo
     * @throws IllegalStateException si hay costos negativos (la poda por costo
     *         parcial solo es valida con costos no negativos)
     */
    public ResultadoRutas buscarRutas(Grafo grafo, String origenId, String destinoId,
                                      double costoMaximo, int saltosMaximos, boolean conPoda) {
        int origen = grafo.indiceDe(origenId);
        int destino = grafo.indiceDe(destinoId);
        if (Double.isNaN(costoMaximo) || costoMaximo < 0) {
            throw new IllegalArgumentException("El costo maximo debe ser un numero mayor o igual a 0");
        }
        if (saltosMaximos < 0) {
            throw new IllegalArgumentException("Los saltos maximos no pueden ser negativos");
        }
        validarCostosNoNegativos(grafo);

        Busqueda b = new Busqueda(grafo, destino, costoMaximo, saltosMaximos, conPoda);
        b.camino.add(origen);
        b.visitado[origen] = true;
        b.explorar(origen, 0);
        return new ResultadoRutas(b.rutas, b.nodosExplorados, b.descartadasPorCosto, b.descartadasPorSaltos);
    }

    /** Estado de UNA busqueda. Se crea uno nuevo por llamada. */
    private static final class Busqueda {
        private final Grafo grafo;
        private final int destino;
        private final double costoMaximo;
        private final int saltosMaximos;
        private final boolean conPoda;

        private final List<Integer> camino = new ArrayList<>();
        private final boolean[] visitado;
        private final List<Ruta> rutas = new ArrayList<>();
        private long nodosExplorados;
        private long descartadasPorCosto;
        private long descartadasPorSaltos;

        Busqueda(Grafo grafo, int destino, double costoMaximo, int saltosMaximos, boolean conPoda) {
            this.grafo = grafo;
            this.destino = destino;
            this.costoMaximo = costoMaximo;
            this.saltosMaximos = saltosMaximos;
            this.conPoda = conPoda;
            this.visitado = new boolean[grafo.cantidadVertices()];
        }

        /** u es el ultimo vertice del camino actual; costo es el costo acumulado. */
        void explorar(int u, double costo) {
            nodosExplorados++;
            int saltos = camino.size() - 1;

            if (u == destino) {
                // con poda los limites ya se respetan; sin poda se verifican recien aca
                if (costo <= costoMaximo && saltos <= saltosMaximos) {
                    rutas.add(new Ruta(idsDelCamino(), costo));
                }
                return; // seguir desde el destino obligaria a repetirlo: no hay mas rutas simples
            }

            for (Grafo.Arista a : grafo.vecinos(u)) {
                int v = a.destino();
                if (visitado[v]) {
                    continue; // restriccion del problema: camino simple
                }
                double nuevoCosto = costo + a.costo();
                if (conPoda) {
                    if (nuevoCosto > costoMaximo) {
                        descartadasPorCosto++;
                        continue;
                    }
                    if (saltos + 1 > saltosMaximos) {
                        descartadasPorSaltos++;
                        continue;
                    }
                }
                // hacer
                camino.add(v);
                visitado[v] = true;
                explorar(v, nuevoCosto);
                // deshacer: este es el retroceso
                visitado[v] = false;
                camino.remove(camino.size() - 1);
            }
        }

        private List<String> idsDelCamino() {
            List<String> ids = new ArrayList<>(camino.size());
            for (int v : camino) {
                ids.add(grafo.idDe(v));
            }
            return ids;
        }
    }

    private void validarCostosNoNegativos(Grafo grafo) {
        for (int u = 0; u < grafo.cantidadVertices(); u++) {
            for (Grafo.Arista a : grafo.vecinos(u)) {
                if (a.costo() < 0) {
                    throw new IllegalStateException("El backtracking de rutas requiere costos no negativos: "
                            + grafo.idDe(u) + "-" + grafo.idDe(a.destino()) + " cuesta " + a.costo());
                }
            }
        }
    }
}
