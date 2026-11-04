# Verificaciones

Cálculos a mano y mediciones sobre los datos del dominio. Las secciones 1 a 9 salen de
ejecutar el código sobre fixtures en memoria idénticas a la carga de Neo4j
(`Fixtures.redDelDominio()` y `Fixtures.paquetes()`, construidas con
`seed/DatosDominio`); la sección 10, de ejecutarlo contra Neo4j real. Los números
marcados con **(test)** están fijados en un test, así que si el código cambia y dejan
de valer, `./mvnw test` falla. Los marcados con **(test de integración)** solo se
comprueban con Neo4j levantado y `NEO4J_INTEGRACION=true`.

Orden de exploración: la lista de vecinos de cada lugar está en orden alfabético
(`GrafoService` ordena lugares y caminos por id) y los paquetes se procesan en orden
de id.

## 1. Red

8 lugares (A a H) y 12 caminos: A-B 4, A-C 2, B-C 1, B-D 5, C-D 8, C-E 10, D-E 2,
D-F 6, E-F 3, E-G 7, F-H 4, G-H 1. Cargada como grafo no dirigido: 8 vértices,
12 aristas y matriz de adyacencia simétrica **(test: GrafoTest)**.

Vecinos en orden: A: B, C · B: A, C, D · C: A, B, D, E · D: B, C, E, F ·
E: C, D, F, G · F: D, E, H · G: E, H · H: F, G.

## 2. Recorridos desde A

**BFS** **(test)**: cola [A] → saca A, encola B, C → saca B, encola D → saca C, encola E
→ saca D, encola F → saca E, encola G → saca F, encola H → saca G → saca H.
Orden: **A B C D E F G H**. Niveles 0, 1, 1, 2, 2, 3, 3, 4: no decrecientes.

**DFS** **(test)**: A → B (primer vecino) → C (A visitado) → D (A, B visitados) → E →
F → H → G; desde G no quedan vecinos sin visitar y se retrocede hasta A.
Orden: **A B C D E F H G**.

Los dos recorridos difieren desde la séptima posición: BFS visita G (nivel 3) antes que
H (nivel 4); DFS llega a H desde F y recién desde H baja a G.

## 3. Dijkstra desde A

| Paso | Finaliza | Relajaciones que mejoran |
|---|---|---|
| 1 | A (0) | B = 4 (A), C = 2 (A) |
| 2 | C (2) | B = 2 + 1 = **3** (C), D = 10 (C), E = 12 (C) |
| 3 | B (3) | D = 3 + 5 = **8** (B) |
| — | B (4) | entrada obsoleta, se descarta |
| 4 | D (8) | E = 8 + 2 = **10** (D), F = 14 (D) |
| — | D (10) | obsoleta |
| 5 | E (10) | F = 10 + 3 = **13** (E), G = 17 (E) |
| — | E (12) | obsoleta |
| 6 | F (13) | H = 13 + 4 = **17** (F) |
| — | F (14) | obsoleta |
| 7 y 8 | G (17), H (17) | ninguna (G-H daría 18) |

- **A → B = 3** por A-C-B, aunque el camino directo cuesta 4 **(test)**.
- **A → H = 17** por A-C-B-D-E-F-H = 2 + 1 + 5 + 2 + 3 + 4 **(test)**.

Contadores desde A **(test)**: 8 vértices finalizados, 24 relajaciones (cada arista
una vez por sentido, 2E) y 11 mejoras (las distancias nuevas de la tabla). La cola
recibió 12 entradas (la inicial más las 11 mejoras) para 8 vértices: 4 quedaron
obsoletas. Por eso la memoria de la cola es O(V + E) y no O(V).

## 4. Árbol de recubrimiento mínimo

**Kruskal.** Aristas ordenadas por costo (MergeSort estable):

| Arista | Costo | ¿find(u) = find(v)? | Decisión |
|---|---|---|---|
| B-C | 1 | no | se agrega |
| G-H | 1 | no | se agrega |
| A-C | 2 | no | se agrega |
| D-E | 2 | no | se agrega |
| E-F | 3 | no | se agrega |
| A-B | 4 | **sí** (A-C-B) | se descarta: formaría ciclo |
| F-H | 4 | no | se agrega |
| B-D | 5 | no | se agrega; ya hay V − 1 = 7 aristas, termina |

**Prim desde A** (búsqueda lineal): A → C (2) → B (1, por C) → D (5, por B) →
E (2, por D) → F (3, por E) → H (4, por F) → G (1, por H).

Ambos dan **7 aristas, costo 1 + 1 + 2 + 2 + 3 + 4 + 5 = 18**, con el conjunto
{A-C, B-C, B-D, D-E, E-F, F-H, G-H} **(test)**. Prim da 18 desde los 8 orígenes posibles
**(test)**. Con la red más un lugar aislado, ambos responden 422 en lugar de un árbol
incompleto **(test)**.

## 5. Greedy frente a mochila 0/1 (capacidad 10)

Ratios: P1 2,0 · P2 1,8 · P3 1,8 · P4 1,0 · P5 1,0. Orden greedy: P1, P2, P3, P4, P5
(empates por id).

| Paquete | Peso acumulado si se toma | ¿Entra en 10? | Greedy |
|---|---|---|---|
| P1 (6 kg, 12) | 6 | sí | se toma |
| P2 (5 kg, 9) | 11 | no | se saltea |
| P3 (5 kg, 9) | 11 | no | se saltea |
| P4 (4 kg, 4) | 10 | sí | se toma |
| P5 (3 kg, 3) | 13 | no | se saltea |

- Greedy: **P1 + P4, 10 kg, valor 16** **(test)**.
- Mochila 0/1: **P2 + P3, 10 kg, valor 18** **(test)**.

**Contraejemplo:** greedy se queda con el paquete de mejor ratio (P1), y los 4 kg que
sobran solo alcanzan para P4. Dos paquetes de ratio algo menor (P2 y P3) llenan la
capacidad exacta con más valor. Con paquetes indivisibles, el mejor ratio individual no
garantiza el mejor conjunto.

Tabla dp completa y recuperación: ver `docs/ENDPOINTS.md`, ficha de
`/api/seleccion/dinamica` **(test: tablaDpDelEjemplo)**.

Validación adicional **(test)**: la mochila coincide con la enumeración de los 2^n
subconjuntos para las capacidades 0 a 25 y para 40 instancias aleatorias de hasta 8
paquetes; greedy nunca supera al óptimo.

## 6. Floyd-Warshall comparado con Dijkstra

Matriz de distancias de Floyd (km):

| | A | B | C | D | E | F | G | H |
|---|---|---|---|---|---|---|---|---|
| **A** | 0 | 3 | 2 | 8 | 10 | 13 | 17 | 17 |
| **B** | 3 | 0 | 1 | 5 | 7 | 10 | 14 | 14 |
| **C** | 2 | 1 | 0 | 6 | 8 | 11 | 15 | 15 |
| **D** | 8 | 5 | 6 | 0 | 2 | 5 | 9 | 9 |
| **E** | 10 | 7 | 8 | 2 | 0 | 3 | 7 | 7 |
| **F** | 13 | 10 | 11 | 5 | 3 | 0 | 5 | 4 |
| **G** | 17 | 14 | 15 | 9 | 7 | 5 | 0 | 1 |
| **H** | 17 | 14 | 15 | 9 | 7 | 4 | 1 | 0 |

Simétrica (caminos bidireccionales) y con diagonal 0 **(test)**.

**Nodos expandidos: Floyd contra Dijkstra V veces (test).** Se ejecutó el núcleo de
Dijkstra una vez desde cada uno de los 8 orígenes (8 ejecuciones completas, no 64
consultas entre pares) y cada fila coincide exactamente con la de Floyd.

| Medida | Dijkstra × 8 | Floyd |
|---|---|---|
| Nodos expandidos | 64: extracciones válidas de la cola, 8 por origen | 8: iteraciones del bucle k, cada vértice es intermedio una vez |
| Comparaciones de distancia | 192: dist[u] + costo < dist[v] | 512 = 8³: d[i][k] + d[k][j] < d[i][j] |
| Mejoras de distancia | 82: A 11, B 10, C 10, D 10, E 9, F 11, G 10, H 11 | no se mide |

Lectura:

- **Resultado:** idéntico. Las dos técnicas dan la misma matriz de distancias.
- **Nodos expandidos:** Dijkstra expande los 8 vértices una vez por cada origen, 64 en
  total. Floyd no tiene frontera: cada vértice se expande una sola vez, pero como
  intermedio de los 64 pares a la vez. Por eso el 8 y el 64 no representan el mismo
  trabajo, y el costo se compara mejor con las comparaciones de distancia.
- **Costo:** en esta red, con pocos caminos (E = 12 frente a V² = 64), Dijkstra × 8 hace
  192 comparaciones y Floyd 512. Dijkstra además paga las operaciones de la cola de
  prioridad, que Floyd no usa. Ver la comparación teórica en `docs/COMPLEJIDADES.md`.

**Ciclo negativo (test).** En un grafo dirigido sintético X → Y (2), Y → Z (−4),
Z → X (1) el ciclo suma −1; al terminar, la diagonal queda negativa y Floyd responde
con error. Con X → Y (4), X → Z (1), Z → Y (−2) no hay ciclo negativo y Floyd da
X → Y = −1. Los datos del dominio no se modifican: los km son positivos.

**Pares sin camino (test).** Con un lugar aislado Z, `A->Z` y `Z->A` valen `null` en
el JSON y `Z->Z` vale 0.

## 7. Backtracking de rutas simples (A → H)

Hay **24 rutas simples** de A a H **(test)**. Contadores:

- *nodos explorados*: entradas a la función recursiva, incluida la raíz;
- *descartadas por costo / por saltos*: ramas no abiertas porque agregar el camino
  excedía el límite (si exceden ambos, cuentan en costo, que se evalúa primero).

La variante **sin poda** enumera todos los caminos simples desde A (hasta llegar a H)
y aplica los límites recién al llegar: explora siempre 78 nodos.

| Costo máx. | Saltos máx. | Rutas | Nodos con poda | Por costo | Por saltos | Nodos sin poda |
|---|---|---|---|---|---|---|
| 17 | 6 | 1 | 29 | 24 | 0 | 78 |
| 18 | 6 | 4 | 36 | 23 | 0 | 78 |
| 18 | 5 | 2 | 34 | 23 | 2 | 78 **(test)** |
| 20 | 6 | 11 | 49 | 16 | 0 | 78 **(test)** |
| 22 | 7 | 13 | 56 | 13 | 0 | 78 |
| 25 | 7 | 18 | 65 | 7 | 0 | 78 |
| 30 | 6 | 20 | 73 | 2 | 2 | 78 |
| 1000 | 10 | 24 | 78 | 0 | 0 | 78 |

En los 8 casos y para 9 combinaciones de origen y destino, la variante con poda y la
de referencia devuelven exactamente las mismas rutas en el mismo orden **(test)**.

Lectura: con límites ajustados (costo 17) la poda evita el 63 % de los nodos; con
límites altos (costo 1000) no poda nada y explora lo mismo que la variante de
referencia. Una rama descartada **no** equivale a un solo nodo evitado: puede cortar
todo un subárbol de rutas.

Casos límite **(test)**: los límites son inclusivos (costo 17 y 6 saltos devuelve la
ruta de costo 17 y 6 saltos; con 5 saltos no hay ninguna); origen igual a destino
devuelve `[origen]` con costo 0; ninguna ruta devuelve `[]`; ninguna ruta repite lugares.

## 8. Branch & Bound de reparto

Peso total 23 kg. Cota inicial = ⌈23 / k⌉.

**Tres camiones (test).** La cota inicial es ⌈7,67⌉ = 8, pero 8 es imposible: el
paquete de 6 kg no puede compartir camión con ningún otro (6 + 3 = 9 > 8), así que va
solo; quedan 17 kg para dos camiones de a lo sumo 8, que cargan como máximo 16. El
reparto P1 + P5 (9) · P2 + P4 (9) · P3 (5) alcanza 9, así que **el óptimo es 9**. Otro
óptimo es P1 (6) · P2 + P4 (9) · P3 + P5 (8).

| Camiones k | Cota inicial | Óptimo | Nodos explorados | Ramas podadas | Mejoras de solución | Nodos del árbol completo |
|---|---|---|---|---|---|---|
| 1 | 23 | 23 | 6 | 0 | 1 | 6 |
| 2 | 12 | 12 | 16 | 3 | 7 | 63 |
| 3 | 8 | 9 | 36 | 43 | 10 | 364 |
| 4 | 6 | 7 | 87 | 214 | 12 | 1 365 |
| 5 | 5 | 6 | 28 | 48 | 13 | 3 906 |

La fila de k = 3 está fijada en un test; los óptimos de todas las filas también
**(test)**. El árbol completo tiene (k^(n+1) − 1)/(k − 1) nodos con n = 5.

- k = 2: la cota 12 se alcanza (P1 + P2 = 11 · P3 + P4 + P5 = 12); al encontrarla,
  todas las ramas restantes tienen cota ≥ 12 y se podan.
- k = 4: la cota es 6 pero 6 es imposible (P1 solo; los dos paquetes de 5 kg no pueden
  sumar a nadie sin pasar de 6; P4 y P5 quedan juntos en 7). Óptimo 7.
- k = 5: un paquete por camión; la cota ⌈4,6⌉ = 5 es menor que el paquete más pesado.

**Contra fuerza bruta (test):** para k = 1 a 4 sobre el dominio y 30 instancias
aleatorias de hasta 6 paquetes, el valor coincide con la enumeración de las k^n
asignaciones y cada paquete aparece exactamente una vez.

## 9. Errores HTTP

Verificados con MockMvc **(test: ApiTest)** y contra la aplicación real con Neo4j (§10).
El 503 se comprobó con la aplicación arrancada con `APP_SEED_ENABLED=false` y Neo4j
detenido: con la carga activa la aplicación no arranca sin base, porque la carga falla.

| Petición | Estado | `detalle` |
|---|---|---|
| `/api/grafo/dijkstra?origen=A&destino=Q` | 400 | `No existe el nodo con id: Q` |
| `/api/grafo/dijkstra?origen=A` | 400 | `Falta el parametro obligatorio 'destino' de tipo String` |
| `/api/seleccion/dinamica?capacidad=10.5` | 400 | `El parametro 'capacidad' tiene un valor invalido: '10.5'. Se esperaba int` |
| `/api/seleccion/greedy?capacidad=NaN` | 400 | `La capacidad debe ser un numero mayor o igual a 0` |
| `/api/seleccion/repartir?contenedores=0` | 400 | `La cantidad de contenedores debe ser al menos 1` |
| `/api/seleccion/repartir?contenedores=101` | 400 | `La cantidad de contenedores no puede superar 100` |
| `/api/grafo/rutas?origen=A&destino=H&saltosMaximos=-1` | 400 | `Los saltos maximos no pueden ser negativos` |
| `/api/grafo/rutas?origen=A&destino=H&costoMaximo=NaN` | 400 | `El costo maximo debe ser un numero mayor o igual a 0` |
| `/api/grafo/dijkstra?origen=A&destino=Z` (Z aislado) | 422 | `No existe camino de A a Z` |
| `/api/grafo/kruskal` (grafo no conexo) | 422 | `El grafo no es conexo: ...` |
| `/api/grafo/ucs?origen=A&destino=H` | 501 | `Todavia no esta implementado: UCS (opcional, fuera del alcance)` |
| `POST /api/grafo/bfs?origen=A` | 405 | `Request method 'POST' is not supported`, con cabecera `Allow: GET` |
| `/api/grafo/noexiste` | 404 | `No static resource api/grafo/noexiste.` |
| cualquier endpoint con Neo4j caído | 503 | `No se pudo acceder a Neo4j. ...` |

`/api/seleccion/dinamica?capacidad=2147483647` **no** es un error: devuelve 200 con los
5 paquetes y valor 37, porque la tabla usa W = min(capacidad, 23) **(test)**.

Los errores de los endpoints `/api` tienen siempre cuerpo JSON `{"estado","detalle"}` en
ese orden, aunque se pida `Accept: text/html` **(test)**. Ninguna respuesta incluye stack
traces ni mensajes internos. Quedan fuera de esta regla las peticiones que Tomcat
rechaza antes de llegar a Spring (por ejemplo, una URL con el carácter `|` sin
codificar, que recibe una página HTML de Tomcat) y la ruta `/error` de Spring Boot.

## 10. Verificación con la base real

Las secciones anteriores se calcularon en memoria. Esta se hizo contra Neo4j 5.26 en
Docker, con la aplicación empaquetada (`java -jar target/tpo-2026.1.jar`).

**Prueba de integración.** `NEO4J_INTEGRACION=true ./mvnw test`: 110 tests, 0 fallos
(104 sin base y 6 de integración). Comprueba la carga repetida (8 lugares, 5 paquetes,
12 caminos), el borrado de datos ajenos, la unificación de ids repetidos con las
restricciones de unicidad, que guardar un lugar actualiza su camino y los endpoints
principales.

**Base vacía.** Sobre un Neo4j recién creado, la carga deja 8 `Nodo`, 5 `Item` y 12
`CONECTA`, crea `nodo_id_unico` e `item_id_unico`, y el log no tiene ningún WARN de la
aplicación ni de Neo4j. En el debug.log de Neo4j no queda ningún error.

**Semilla ajena usada en los escenarios.** Lugares A–H con valor (B = 12) y los 12
caminos del dominio con costo 50, un camino inverso B→A de costo 99, un lugar Z
conectado a A, paquetes I1–I5 con (peso, valor) = (2, 40), (3, 30), (4, 20), (1, 25) y
(5, 10), un nodo sin id, un nodo con id numérico 42 y un item sin id.

**Arranque con la semilla ajena y la configuración por defecto.**

1. El log informa `Se eliminaron 3 nodos Nodo ajenos al dominio: [Z, (sin id), 42]` y
   `Se eliminaron 6 nodos Item ajenos al dominio: [I1, I2, I3, I4, I5, (sin id)]` (el
   orden de la lista puede variar). La base queda con 8 `Nodo`, 5 `Item` y 12
   `CONECTA`, sin relaciones duplicadas ni inversas, con los 12 costos del dominio y
   B con valor 0.
2. Se consultó `/api/grafo/resumen` cada ~100 ms desde que se lanzó el proceso: 111
   consultas, 44 rechazadas (puerto todavía cerrado) y 67 respuestas 200, **todas**
   iguales a `{"vertices":8,"aristas":12,"dirigido":false,"ids":[...]}`. Ninguna
   respuesta con datos a medio sincronizar. En el log, `Datos del dominio
   sincronizados` (00:58:42.233) aparece antes de `Tomcat started` (00:58:42.289).
3. Las 23 peticiones de `docs/api.http` devuelven el código y el cuerpo esperados,
   incluido el orden de las claves. Las 11 rutas de backtracking coinciden con una
   enumeración escrita aparte, y los 64 valores de Floyd con un cálculo independiente.
4. Segundo arranque: la carga vuelve a ejecutarse, la base queda idéntica y las 23
   peticiones repiten los resultados.

**Otras configuraciones.**

| Configuración | Resultado |
|---|---|
| `APP_SEED_ELIMINAR_AJENOS=false` | WARN con los mismos nodos e items; no se borran. Los 12 caminos se reemplazan igual (desaparecen B→A y Z→A) y A–H toman los datos del dominio. `resumen` y `kruskal` responden 500 `Error interno del servidor` por el nodo sin id y el de id 42, y `greedy` también, por el item sin id (su ratio 1 empata con P4 y P5, y el desempate por id falla). Sin esos tres: `resumen` da 9 vértices (Z aislado), Kruskal 422 (no conexo) y greedy 115 (I4, I1, I2, I3) |
| `APP_SEED_ENABLED=false` | arranca sin tocar la base: un paquete agregado a mano sigue ahí y greedy lo usa |
| Neo4j detenido, carga activa | la aplicación no arranca: unos 5 s después de lanzarla termina con código 1 y `Application run failed` (no se pudo conectar) |
| Neo4j detenido, `APP_SEED_ENABLED=false` | arranca; los endpoints responden 503 con el cuerpo documentado y sin stack trace; `repartir?contenedores=0` responde 400 sin ir a la base. Al volver a levantar Neo4j, sin reiniciar la aplicación, `resumen` pasa a 200 con 8 y 12 |
| dos instancias que arrancan a la vez sobre una base vacía (3 veces) | arrancan las dos; la base queda en 8/5/12, sin ids ni caminos repetidos, con las dos restricciones |

**Ids repetidos (test de integración).** Con un segundo `Nodo` A y un segundo `Item` P1
en la base, la carga deja uno de cada uno, la base vuelve a 8/5/12 y quedan creadas las
restricciones `nodo_id_unico` e `item_id_unico`. En la ejecución real el log informa
`Habia nodos Nodo repetidos; se dejo uno por id: [A]` y lo mismo para `[P1]`. Si los
repetidos son ajenos y no se borran (`APP_SEED_ELIMINAR_AJENOS=false`), la restricción
no se crea y se avisa: `No se creo la restriccion de unicidad de Nodo.id porque hay ids
repetidos: [Z]`.

**Etiqueta de más (test de integración).** Si el lugar C tiene además la etiqueta
`Item`, o el paquete P2 la etiqueta `Nodo`, la carga no los borra: les quita solo la
etiqueta que sobra.

**Corrección del arranque.** La primera verificación mostró que la carga, escrita como
`CommandLineRunner`, corría **después** de que Tomcat aceptara peticiones: durante unos
3 s `/api/grafo/resumen` respondió 200 con 0 vértices sobre una base vacía, o con los 9
vértices de la base vieja. Ahora la carga corre al terminar de crear los beans
(`SmartInitializingSingleton`), antes de abrir el puerto, y el punto 2 lo confirma.
