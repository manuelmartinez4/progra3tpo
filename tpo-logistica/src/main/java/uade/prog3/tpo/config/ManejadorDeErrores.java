package uade.prog3.tpo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import uade.prog3.tpo.algorithm.PendienteDeImplementar;

/**
 * Traduce excepciones a respuestas HTTP con un cuerpo entendible.
 *
 * Lo importante para el trabajo: un algoritmo sin implementar devuelve
 * 501 Not Implemented con un mensaje claro, en vez de un stack trace de 500.
 * Asi la aplicacion arranca y es demostrable desde el primer dia.
 *
 * Politica de errores del dominio:
 *   400  id inexistente, parametro ausente, de tipo incorrecto o fuera de rango
 *   422  los datos no admiten una respuesta: no hay camino, grafo no conexo,
 *        costos negativos donde no corresponden, ciclo negativo
 *   501  algoritmo opcional sin implementar (UCS)
 *   503  no se puede acceder a Neo4j
 * Ninguna respuesta incluye stack traces.
 *
 * El cuerpo es siempre JSON {"estado", "detalle"}, aunque el cliente pida
 * otro formato: con el Content-Type fijado, Spring no negocia con Accept.
 */
@RestControllerAdvice
public class ManejadorDeErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorDeErrores.class);

    /** Cuerpo de error. Al ser un record, el JSON respeta el orden: estado, detalle. */
    public record CuerpoError(String estado, String detalle) { }

    @ExceptionHandler(PendienteDeImplementar.class)
    public ResponseEntity<CuerpoError> pendiente(PendienteDeImplementar e) {
        return responder(HttpStatus.NOT_IMPLEMENTED, null, "PENDIENTE", e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CuerpoError> datosInvalidos(IllegalArgumentException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<CuerpoError> estadoInvalido(IllegalStateException e) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<CuerpoError> parametroAusente(MissingServletRequestParameterException e) {
        return error(HttpStatus.BAD_REQUEST, "Falta el parametro obligatorio '"
                + e.getParameterName() + "' de tipo " + e.getParameterType());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<CuerpoError> tipoIncorrecto(MethodArgumentTypeMismatchException e) {
        String tipo = e.getRequiredType() == null ? "valido" : e.getRequiredType().getSimpleName();
        return error(HttpStatus.BAD_REQUEST, "El parametro '" + e.getName()
                + "' tiene un valor invalido: '" + e.getValue() + "'. Se esperaba " + tipo);
    }

    /**
     * Con la base caida, Spring Data Neo4j falla al abrir la transaccion
     * (TransactionSystemException) o al consultar (DataAccessException).
     * Se registra una linea por peticion; la traza completa, solo en DEBUG.
     */
    @ExceptionHandler({DataAccessException.class, TransactionException.class})
    public ResponseEntity<CuerpoError> baseNoDisponible(RuntimeException e) {
        // el mensaje de primer nivel incluye host y puerto ("Unable to connect to ...")
        log.warn("Error de acceso a Neo4j: {}", e.getMessage());
        log.debug("Traza del error de acceso a Neo4j", e);
        return error(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo acceder a Neo4j. Verificar que la base "
                + "este levantada y las variables NEO4J_URI, NEO4J_USER y NEO4J_PASSWORD");
    }

    /**
     * Cualquier otro error. Los errores propios de Spring (404, 405...) conservan
     * su estado y sus cabeceras, por ejemplo Allow en un 405.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CuerpoError> inesperado(Exception e) {
        HttpStatusCode estado = HttpStatus.INTERNAL_SERVER_ERROR;
        HttpHeaders cabeceras = null;
        if (e instanceof ErrorResponse er) {
            estado = er.getStatusCode();
            cabeceras = er.getHeaders();
        }
        if (estado.is5xxServerError()) {
            log.error("Error inesperado", e);
            return responder(estado, cabeceras, "ERROR", "Error interno del servidor");
        }
        String detalle = e.getMessage() == null ? estado.toString() : e.getMessage();
        return responder(estado, cabeceras, "ERROR", detalle);
    }

    private static ResponseEntity<CuerpoError> error(HttpStatusCode estado, String detalle) {
        return responder(estado, null, "ERROR", detalle);
    }

    private static ResponseEntity<CuerpoError> responder(HttpStatusCode estado, HttpHeaders cabeceras,
                                                         String tipo, String detalle) {
        ResponseEntity.BodyBuilder respuesta = ResponseEntity.status(estado);
        if (cabeceras != null) {
            respuesta.headers(cabeceras);
        }
        return respuesta.contentType(MediaType.APPLICATION_JSON).body(new CuerpoError(tipo, detalle));
    }
}
