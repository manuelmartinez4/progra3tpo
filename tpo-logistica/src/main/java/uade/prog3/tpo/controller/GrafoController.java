package uade.prog3.tpo.controller;

import org.springframework.web.bind.annotation.*;
import uade.prog3.tpo.algorithm.ArbolRecubrimiento;
import uade.prog3.tpo.algorithm.Backtracking;
import uade.prog3.tpo.algorithm.CaminosMinimos;
import uade.prog3.tpo.algorithm.Recorridos;
import uade.prog3.tpo.service.Grafo;
import uade.prog3.tpo.service.GrafoService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Endpoints de los algoritmos sobre grafos.
 *
 * El cableado ya esta hecho: cada endpoint carga el grafo y delega en la
 * clase de algoritmo correspondiente. Mientras el algoritmo no este
 * implementado, la respuesta es 501 con un mensaje claro.
 *
 * Lo unico que hay que tocar para que un endpoint funcione es el metodo
 * del paquete algorithm. Los controllers NO deben contener logica algoritmica.
 *
 * Dominio: cada camino de la red logistica es bidireccional y se guarda en
 * Neo4j con UNA sola relacion. Por eso todos los algoritmos cargan el grafo
 * como NO dirigido: Grafo(false) agrega la arista inversa en memoria.
 */
@RestController
@RequestMapping("/api/grafo")
public class GrafoController {

    private final GrafoService grafoService;
    private final Recorridos recorridos;
    private final CaminosMinimos caminosMinimos;
    private final ArbolRecubrimiento arbolRecubrimiento;
    private final Backtracking backtracking;

    public GrafoController(GrafoService grafoService,
                           Recorridos recorridos,
                           CaminosMinimos caminosMinimos,
                           ArbolRecubrimiento arbolRecubrimiento,
                           Backtracking backtracking) {
        this.grafoService = grafoService;
        this.recorridos = recorridos;
        this.caminosMinimos = caminosMinimos;
        this.arbolRecubrimiento = arbolRecubrimiento;
        this.backtracking = backtracking;
    }

    /**
     * Metadatos del grafo cargado. Sirve para verificar la conexion a la base.
     * LinkedHashMap y no Map.of: asi el JSON sale siempre en el mismo orden.
     */
    @GetMapping("/resumen")
    public Map<String, Object> resumen(@RequestParam(defaultValue = "false") boolean dirigido) {
        Grafo g = grafoService.cargar(dirigido);
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("vertices", g.cantidadVertices());
        resumen.put("aristas", g.cantidadAristas());
        resumen.put("dirigido", g.esDirigido());
        resumen.put("ids", g.ids());
        return resumen;
    }

    /** GET /api/grafo/dfs?origen=A */
    @GetMapping("/dfs")
    public List<String> dfs(@RequestParam String origen) {
        return recorridos.dfs(grafoService.cargar(false), origen);
    }

    /** GET /api/grafo/bfs?origen=A */
    @GetMapping("/bfs")
    public List<String> bfs(@RequestParam String origen) {
        return recorridos.bfs(grafoService.cargar(false), origen);
    }

    /** GET /api/grafo/dijkstra?origen=A&destino=H */
    @GetMapping("/dijkstra")
    public CaminosMinimos.Camino dijkstra(@RequestParam String origen, @RequestParam String destino) {
        return caminosMinimos.dijkstra(grafoService.cargar(false), origen, destino);
    }

    /** GET /api/grafo/floyd */
    @GetMapping("/floyd")
    public Map<String, Double> floyd() {
        return caminosMinimos.floydWarshall(grafoService.cargar(false));
    }

    /** GET /api/grafo/ucs?origen=A&destino=H   OPCIONAL, no implementado: responde 501 */
    @GetMapping("/ucs")
    public CaminosMinimos.Camino ucs(@RequestParam String origen, @RequestParam String destino) {
        return caminosMinimos.ucs(grafoService.cargar(false), origen, destino);
    }

    /** GET /api/grafo/prim?origen=A   (el MST exige grafo NO dirigido) */
    @GetMapping("/prim")
    public ArbolRecubrimiento.Mst prim(@RequestParam String origen) {
        return arbolRecubrimiento.prim(grafoService.cargar(false), origen);
    }

    /** GET /api/grafo/kruskal */
    @GetMapping("/kruskal")
    public ArbolRecubrimiento.Mst kruskal() {
        return arbolRecubrimiento.kruskal(grafoService.cargar(false));
    }

    /** GET /api/grafo/rutas?origen=A&destino=H&costoMaximo=30&saltosMaximos=5 */
    @GetMapping("/rutas")
    public List<Backtracking.Ruta> rutas(@RequestParam String origen,
                                         @RequestParam String destino,
                                         @RequestParam(defaultValue = "1000") double costoMaximo,
                                         @RequestParam(defaultValue = "10") int saltosMaximos) {
        return backtracking.rutasSimples(grafoService.cargar(false), origen, destino,
                costoMaximo, saltosMaximos);
    }
}
