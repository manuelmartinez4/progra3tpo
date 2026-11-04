package uade.prog3.tpo.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.TransactionSystemException;
import uade.prog3.tpo.Fixtures;
import uade.prog3.tpo.algorithm.ArbolRecubrimiento;
import uade.prog3.tpo.algorithm.Backtracking;
import uade.prog3.tpo.algorithm.CaminosMinimos;
import uade.prog3.tpo.algorithm.Ordenamiento;
import uade.prog3.tpo.algorithm.RamificacionYPoda;
import uade.prog3.tpo.algorithm.Recorridos;
import uade.prog3.tpo.algorithm.Seleccion;
import uade.prog3.tpo.repository.ItemRepository;
import uade.prog3.tpo.service.GrafoService;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de los endpoints sin Neo4j: GrafoService e ItemRepository se
 * reemplazan por mocks que devuelven los datos del dominio en memoria.
 * Verifican contrato JSON, codigos de estado y mensajes de error.
 */
@WebMvcTest
@Import({Recorridos.class, CaminosMinimos.class, ArbolRecubrimiento.class, Backtracking.class,
        Ordenamiento.class, Seleccion.class, RamificacionYPoda.class})
class ApiTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private GrafoService grafoService;

    @MockitoBean
    private ItemRepository itemRepository;

    @BeforeEach
    void datosDelDominio() {
        when(grafoService.cargar(false)).thenAnswer(i -> Fixtures.redDelDominio());
        when(itemRepository.findAllByOrderByIdAsc()).thenAnswer(i -> Fixtures.paquetes());
    }

    private ResultActions pedir(String url) throws Exception {
        return mvc.perform(get(url));
    }

    // ---------------------------------------------------------------- respuestas correctas

    @Test
    void resumenDeLaRed() throws Exception {
        pedir("/api/grafo/resumen")
                .andExpect(status().isOk())
                .andExpect(content().string(
                        "{\"vertices\":8,\"aristas\":12,\"dirigido\":false,\"ids\":[\"A\",\"B\",\"C\",\"D\",\"E\",\"F\",\"G\",\"H\"]}"));
    }

    @Test
    void recorridos() throws Exception {
        pedir("/api/grafo/bfs?origen=A").andExpect(status().isOk())
                .andExpect(jsonPath("$", contains("A", "B", "C", "D", "E", "F", "G", "H")));
        pedir("/api/grafo/dfs?origen=A").andExpect(status().isOk())
                .andExpect(jsonPath("$", contains("A", "B", "C", "D", "E", "F", "H", "G")));
    }

    @Test
    void dijkstra() throws Exception {
        pedir("/api/grafo/dijkstra?origen=A&destino=H")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vertices", contains("A", "C", "B", "D", "E", "F", "H")))
                .andExpect(jsonPath("$.costoTotal").value(17.0));
    }

    @Test
    void arbolesDeRecubrimiento() throws Exception {
        pedir("/api/grafo/prim?origen=A").andExpect(status().isOk())
                .andExpect(jsonPath("$.aristas", hasSize(7)))
                .andExpect(jsonPath("$.costoTotal").value(18.0));
        pedir("/api/grafo/kruskal").andExpect(status().isOk())
                .andExpect(jsonPath("$.aristas", hasSize(7)))
                .andExpect(jsonPath("$.costoTotal").value(18.0));
    }

    @Test
    void floyd() throws Exception {
        pedir("/api/grafo/floyd").andExpect(status().isOk())
                .andExpect(jsonPath("$.['A->H']").value(17.0))
                .andExpect(jsonPath("$.length()").value(64));
    }

    @Test
    void floydConParSinCaminoDevuelveNull() throws Exception {
        when(grafoService.cargar(false)).thenAnswer(i -> Fixtures.redConLugarAislado());
        pedir("/api/grafo/floyd").andExpect(status().isOk())
                .andExpect(jsonPath("$.['A->Z']").value(nullValue()));
    }

    @Test
    void rutas() throws Exception {
        pedir("/api/grafo/rutas?origen=A&destino=H&costoMaximo=20&saltosMaximos=6")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(11)))
                .andExpect(jsonPath("$[0].vertices", contains("A", "B", "D", "E", "F", "H")))
                .andExpect(jsonPath("$[0].costoTotal").value(18.0));
    }

    @Test
    void ordenamientos() throws Exception {
        pedir("/api/seleccion/mergesort?criterio=peso").andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains("P5", "P4", "P2", "P3", "P1")));
        pedir("/api/seleccion/quicksort?criterio=valor").andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("P5"))
                .andExpect(jsonPath("$[4].id").value("P1"));
    }

    @Test
    void greedyFrenteADinamica() throws Exception {
        pedir("/api/seleccion/greedy?capacidad=10").andExpect(status().isOk())
                .andExpect(jsonPath("$.elegidos[*].id", contains("P1", "P4")))
                .andExpect(jsonPath("$.valorTotal").value(16.0))
                .andExpect(jsonPath("$.pesoTotal").value(10.0))
                .andExpect(jsonPath("$.tecnica").value(Seleccion.TECNICA_GREEDY));
        pedir("/api/seleccion/dinamica?capacidad=10").andExpect(status().isOk())
                .andExpect(jsonPath("$.elegidos[*].id", contains("P2", "P3")))
                .andExpect(jsonPath("$.valorTotal").value(18.0));
    }

    @Test
    void repartir() throws Exception {
        pedir("/api/seleccion/repartir?contenedores=3").andExpect(status().isOk())
                .andExpect(jsonPath("$.cargaMaxima").value(9.0))
                .andExpect(jsonPath("$.porContenedor.length()").value(3));
    }

    // ---------------------------------------------------------------- errores

    @Test
    void idInexistenteDa400() throws Exception {
        pedir("/api/grafo/dijkstra?origen=A&destino=Q")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value("ERROR"))
                .andExpect(jsonPath("$.detalle").value("No existe el nodo con id: Q"));
        pedir("/api/grafo/bfs?origen=Q").andExpect(status().isBadRequest());
    }

    @Test
    void parametroAusenteDa400() throws Exception {
        pedir("/api/grafo/dijkstra?origen=A")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalle").value("Falta el parametro obligatorio 'destino' de tipo String"));
    }

    @Test
    void parametroDeTipoIncorrectoDa400() throws Exception {
        pedir("/api/seleccion/greedy?capacidad=diez")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalle", containsString("capacidad")));
        pedir("/api/seleccion/dinamica?capacidad=10.5")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalle")
                        .value("El parametro 'capacidad' tiene un valor invalido: '10.5'. Se esperaba int"));
    }

    @Test
    void valoresFueraDeRangoDan400() throws Exception {
        pedir("/api/seleccion/greedy?capacidad=-1").andExpect(status().isBadRequest());
        pedir("/api/seleccion/greedy?capacidad=NaN")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalle").value("La capacidad debe ser un numero mayor o igual a 0"));
        pedir("/api/seleccion/dinamica?capacidad=-1").andExpect(status().isBadRequest());
        pedir("/api/seleccion/repartir?contenedores=0")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalle").value("La cantidad de contenedores debe ser al menos 1"));
        pedir("/api/grafo/rutas?origen=A&destino=H&costoMaximo=-5").andExpect(status().isBadRequest());
        pedir("/api/grafo/rutas?origen=A&destino=H&costoMaximo=NaN")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalle").value("El costo maximo debe ser un numero mayor o igual a 0"));
        pedir("/api/grafo/rutas?origen=A&destino=H&saltosMaximos=-1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalle").value("Los saltos maximos no pueden ser negativos"));
        pedir("/api/seleccion/quicksort?criterio=color")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalle", containsString("Criterio invalido")));
    }

    @Test
    void valoresMuyGrandesNoAgotanLaMemoria() throws Exception {
        pedir("/api/seleccion/dinamica?capacidad=2147483647")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorTotal").value(37.0));
        pedir("/api/seleccion/repartir?contenedores=101")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalle").value("La cantidad de contenedores no puede superar 100"));
        pedir("/api/seleccion/repartir?contenedores=2147483647").andExpect(status().isBadRequest());
    }

    @Test
    void cuerpoDeErrorConOrdenFijoYSiempreEnJson() throws Exception {
        pedir("/api/grafo/bfs?origen=Q")
                .andExpect(content().string("{\"estado\":\"ERROR\",\"detalle\":\"No existe el nodo con id: Q\"}"));
        mvc.perform(get("/api/grafo/bfs?origen=Q").accept(MediaType.TEXT_HTML))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.detalle").value("No existe el nodo con id: Q"));
    }

    @Test
    void metodoNoPermitidoDa405ConCabeceraAllow() throws Exception {
        mvc.perform(post("/api/grafo/bfs?origen=A"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")))
                .andExpect(jsonPath("$.estado").value("ERROR"))
                .andExpect(jsonPath("$.detalle").value("Request method 'POST' is not supported"));
    }

    @Test
    void sinCaminoOGrafoNoConexoDan422() throws Exception {
        when(grafoService.cargar(false)).thenAnswer(i -> Fixtures.redConLugarAislado());
        pedir("/api/grafo/dijkstra?origen=A&destino=Z")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalle").value("No existe camino de A a Z"));
        pedir("/api/grafo/kruskal")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalle", startsWith("El grafo no es conexo: ")));
        pedir("/api/grafo/prim?origen=A").andExpect(status().isUnprocessableEntity());
    }

    @Test
    void ucsOpcionalDa501() throws Exception {
        pedir("/api/grafo/ucs?origen=A&destino=H")
                .andExpect(status().isNotImplemented())
                .andExpect(content().string("{\"estado\":\"PENDIENTE\","
                        + "\"detalle\":\"Todavia no esta implementado: UCS (opcional, fuera del alcance)\"}"));
    }

    @Test
    void baseNoDisponibleDa503SinStackTrace() throws Exception {
        when(grafoService.cargar(false)).thenThrow(new DataAccessResourceFailureException("conexion rechazada"));
        pedir("/api/grafo/bfs?origen=A")
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detalle", startsWith("No se pudo acceder a Neo4j. ")))
                .andExpect(content().string(not(containsString("Exception"))));
    }

    @Test
    void sinConexionAlAbrirLaTransaccionTambienDa503() throws Exception {
        // es lo que lanza Spring Data Neo4j cuando la base no esta levantada
        when(itemRepository.findAllByOrderByIdAsc())
                .thenThrow(new TransactionSystemException("Could not open a new Neo4j session"));
        pedir("/api/seleccion/greedy?capacidad=10")
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detalle", containsString("Neo4j")));
    }

    @Test
    void errorInesperadoDa500SinStackTrace() throws Exception {
        when(itemRepository.findAllByOrderByIdAsc()).thenThrow(new NullPointerException("detalle interno"));
        pedir("/api/seleccion/greedy?capacidad=10")
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detalle").value("Error interno del servidor"))
                .andExpect(content().string(not(containsString("detalle interno"))));
    }

    @Test
    void rutaInexistenteDa404() throws Exception {
        pedir("/api/grafo/noexiste")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detalle").value("No static resource api/grafo/noexiste."));
    }
}
