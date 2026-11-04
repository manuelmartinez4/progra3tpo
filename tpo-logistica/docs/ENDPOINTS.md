# Documentación de endpoints

Una ficha por algoritmo. Todas las respuestas de ejemplo son **reales**: se obtuvieron
ejecutando la API sobre los datos del dominio (8 lugares, 12 caminos, 5 paquetes; ver
README) y se volvieron a comprobar contra un Neo4j 5 real. Suponen una base con solo
los datos del dominio, que es lo que deja la carga con la configuración por defecto. Las
unidades son kilómetros ficticios para los caminos, kilogramos para el peso y puntos de
beneficio para el valor.

Notación: V vértices, E caminos, n paquetes, W capacidad entera, k camiones,
L suma de longitudes de las rutas devueltas.

Errores comunes a todos los endpoints, con cuerpo JSON `{"estado":"ERROR","detalle":"..."}`
en ese orden, aunque el cliente pida otro formato:

| Estado | Causa | Ejemplo de `detalle` |
|---|---|---|
| 400 | id inexistente | `No existe el nodo con id: Q` |
| 400 | parámetro obligatorio ausente | `Falta el parametro obligatorio 'destino' de tipo String` |
| 400 | parámetro de tipo incorrecto | `El parametro 'capacidad' tiene un valor invalido: '10.5'. Se esperaba int` |
| 404 | ruta inexistente | `No static resource api/grafo/noexiste.` |
| 405 | método no admitido, por ejemplo POST o PUT (con cabecera `Allow: GET`) | `Request method 'POST' is not supported` |
| 503 | Neo4j no disponible | `No se pudo acceder a Neo4j. Verificar que la base este levantada...` |
| 500 | error inesperado (por ejemplo, nodos sin id que la carga no borró; ver README) | `Error interno del servidor` |

Todos los endpoints de grafos cargan la red **no dirigida**: cada camino se guarda una
vez en Neo4j y `Grafo` agrega el sentido inverso en memoria. `GrafoService` ordena
lugares y caminos por id, así que la lista de vecinos de cada lugar queda en orden
alfabético y los recorridos son reproducibles.

---

### GET /api/grafo/resumen

Verificación de la conexión y de los datos cargados.

**Parámetros:** `dirigido` (opcional, default `false`).

**Ejemplo:** `GET /api/grafo/resumen`

**Respuesta:**
```json
{"vertices":8,"aristas":12,"dirigido":false,"ids":["A","B","C","D","E","F","G","H"]}
```

**Complejidad:** O(V log V + E log E). Traer lugares y caminos y armar el grafo es
O(V + E); lo que domina es ordenarlos por id con MergeSort para que el resultado sea
reproducible. Memoria O(V + E), por el grafo en memoria.

**Estructura usada:** `Grafo`, con lista de adyacencia. Con `dirigido=false` agrega en
memoria el sentido inverso de cada camino, pero `aristas` sigue contando cada camino una
sola vez.

---

### GET /api/grafo/bfs

Recorrido en anchura desde un lugar: visita primero todos los lugares a un camino de
distancia, después los que están a dos, y así sucesivamente.

**Parámetros:** `origen` (id del lugar).

**Ejemplo:** `GET /api/grafo/bfs?origen=A`

**Respuesta:**
```json
["A","B","C","D","E","F","G","H"]
```
Niveles desde A: A (0); B, C (1); D, E (2); F, G (3); H (4).

**Errores:** 400 si el origen no existe.

**Complejidad:** O(V + E). Cada vértice entra una sola vez a la cola porque se marca
visitado **al encolar**, y la lista de adyacencia de cada vértice se recorre una vez
al desencolarlo. Memoria auxiliar O(V).

**Estructura usada:** cola FIFO (`ArrayDeque`) + arreglo `visitado[]` + lista de adyacencia.

BFS da el camino mínimo en **cantidad de caminos**, no en kilómetros: de A a H visita
4 saltos, pero ignora los costos.

---

### GET /api/grafo/dfs

Recorrido en profundidad desde un lugar: avanza lo más lejos posible antes de
retroceder.

**Parámetros:** `origen` (id del lugar).

**Ejemplo:** `GET /api/grafo/dfs?origen=A`

**Respuesta:**
```json
["A","B","C","D","E","F","H","G"]
```
A → B → C (A ya visitado) → D → E → F → H → G; G no tiene vecinos sin visitar y se
retrocede hasta terminar.

**Errores:** 400 si el origen no existe.

**Complejidad:** O(V + E). Cada vértice entra una vez a la recursión y cada lista de
adyacencia se recorre una vez. Memoria auxiliar O(V): `visitado[]` y la pila de
recursión, que puede llegar a V llamadas.

**Estructura usada:** recursión (pila de llamadas) + arreglo `visitado[]`.

---

### GET /api/grafo/dijkstra

Camino de menor costo en km entre dos lugares.

**Parámetros:** `origen`, `destino` (ids).

**Ejemplo:** `GET /api/grafo/dijkstra?origen=A&destino=H`

**Respuesta:**
```json
{"vertices":["A","C","B","D","E","F","H"],"costoTotal":17.0}
```

Con `destino=B` devuelve `{"vertices":["A","C","B"],"costoTotal":3.0}`: el camino
directo A-B cuesta 4, pero la relajación encuentra A-C-B por 2 + 1 = 3.

**Errores:**
- 400 si un id no existe.
- 422 `No existe camino de A a Z` si el destino no es alcanzable.
- 422 si algún camino tiene costo negativo: Dijkstra no lo admite.

**Complejidad:** O((V + E) log V). Cada mejora de una distancia agrega una entrada a
la cola (hasta O(E) entradas, y log E = O(log V) en un grafo simple); las entradas
obsoletas se descartan al extraerlas. Memoria auxiliar O(V + E) por esas entradas
repetidas.

**Estructura usada:** `PriorityQueue` (heap binario) de entradas inmutables
(distancia, vértice) + arreglos `dist[]`, `predecesor[]` y `finalizado[]`. El camino
se reconstruye siguiendo `predecesor[]` desde el destino.

Es greedy: finaliza en cada paso el vértice no finalizado de menor distancia y no lo
revisa más. Con costos negativos una arista posterior podría abaratar un vértice ya
finalizado; por eso se rechazan.

---

### GET /api/grafo/prim

Árbol de recubrimiento mínimo creciendo desde un lugar: la red de caminos de menor
costo total que mantiene conectados los 8 lugares. **No es una ruta de reparto.**

**Parámetros:** `origen` (id del lugar inicial).

**Ejemplo:** `GET /api/grafo/prim?origen=A`

**Respuesta:**
```json
{"aristas":[{"origen":"A","destino":"C","costo":2.0},{"origen":"C","destino":"B","costo":1.0},
 {"origen":"B","destino":"D","costo":5.0},{"origen":"D","destino":"E","costo":2.0},
 {"origen":"E","destino":"F","costo":3.0},{"origen":"F","destino":"H","costo":4.0},
 {"origen":"H","destino":"G","costo":1.0}],"costoTotal":18.0}
```
Las aristas aparecen en el orden en que Prim las agrega.

**Errores:** 400 si el origen no existe; 422 si el grafo no es conexo
(`El grafo no es conexo: ... (Z no es alcanzable desde A)`).

**Complejidad:** O(V² + E) = O(V²) en grafo simple. Se eligió la variante con
**búsqueda lineal** del siguiente vértice: V iteraciones que buscan el mínimo en O(V)
y recorren los vecinos del vértice agregado. Con 8 lugares es más simple que un heap y
no pierde nada; en un grafo denso (E ≈ V²) incluso es la mejor opción. Memoria
auxiliar O(V).

**Estructura usada:** arreglos `key[]`, `parent[]` e `inMST[]`.

---

### GET /api/grafo/kruskal

Árbol de recubrimiento mínimo agregando caminos de menor a mayor costo sin formar
ciclos.

**Parámetros:** ninguno.

**Ejemplo:** `GET /api/grafo/kruskal`

**Respuesta:**
```json
{"aristas":[{"origen":"B","destino":"C","costo":1.0},{"origen":"G","destino":"H","costo":1.0},
 {"origen":"A","destino":"C","costo":2.0},{"origen":"D","destino":"E","costo":2.0},
 {"origen":"E","destino":"F","costo":3.0},{"origen":"F","destino":"H","costo":4.0},
 {"origen":"B","destino":"D","costo":5.0}],"costoTotal":18.0}
```
Mismo conjunto de aristas que Prim, en orden de costo. A-B (4) se descarta porque A y
B ya están unidos por A-C-B.

**Errores:** 422 si el grafo no es conexo (se obtendría un bosque).

**Complejidad:** O(E log E + V): ordenar las E aristas con MergeSort cuesta
O(E log E), las E operaciones de Union-Find cuestan O(E·α(V)) y la inicialización
O(V). Memoria O(V + E). Conviene en grafos ralos.

**Estructura usada:** lista con una sola copia de cada arista (la que va del índice
menor al mayor), MergeSort propio y **Union-Find propio** con compresión de caminos y
unión por rango. Una arista forma ciclo si `find(u) == find(v)`.

---

### GET /api/grafo/floyd

Distancia mínima en km entre **todos** los pares de lugares.

**Parámetros:** ninguno.

**Ejemplo:** `GET /api/grafo/floyd`

**Respuesta** (64 pares; se muestra la fila de A):
```json
{"A->A":0.0,"A->B":3.0,"A->C":2.0,"A->D":8.0,"A->E":10.0,"A->F":13.0,"A->G":17.0,"A->H":17.0, ...}
```
Un par sin camino se devuelve con `null`, porque JSON no tiene infinito.

**Errores:** 422 si hay un ciclo de costo negativo (la diagonal queda negativa).

**Complejidad:** O(V³) en tiempo (tres bucles anidados de V) y O(V²) en memoria (una
sola matriz que se actualiza en el lugar).

**Estructura usada:** matriz de adyacencia (`grafo.matrizDeAdyacencia()`) con diagonal
0 e infinito donde no hay camino. El bucle del intermedio `k` va **por fuera** de `i`
y `j`. La comparación con Dijkstra está en `docs/VERIFICACIONES.md`.

---

### GET /api/seleccion/quicksort

Paquetes ordenados de menor a mayor según un criterio.

**Parámetros:** `criterio` = `valor` | `peso` | `ratio` (default `ratio`). Orden ascendente.

**Ejemplo:** `GET /api/seleccion/quicksort?criterio=ratio`

**Respuesta:**
```json
[{"id":"P5","nombre":"Paquete 5","peso":3.0,"valor":3.0,"nodoId":"H"},
 {"id":"P4","nombre":"Paquete 4","peso":4.0,"valor":4.0,"nodoId":"E"},
 {"id":"P3","nombre":"Paquete 3","peso":5.0,"valor":9.0,"nodoId":"D"},
 {"id":"P2","nombre":"Paquete 2","peso":5.0,"valor":9.0,"nodoId":"C"},
 {"id":"P1","nombre":"Paquete 1","peso":6.0,"valor":12.0,"nodoId":"B"}]
```
QuickSort **no es estable**: P3 quedó antes que P2 aunque empatan y entraron en orden P2, P3.

**Errores:** 400 `Criterio invalido: color. Use valor, peso o ratio.`

**Complejidad:** promedio O(n log n), peor caso O(n²). Memoria O(n) por la copia más
la pila de recursión, O(log n) en promedio y O(n) en el peor caso.

**Estructura usada:** copia en `ArrayList` y partición de Lomuto propia. **Pivote:
mediana de tres** (primero, medio y último): evita el peor caso con listas ya
ordenadas o invertidas. El peor caso sigue existiendo, y no solo para entradas armadas
a propósito: Lomuto deja de un mismo lado todos los elementos iguales al pivote. Si
todas las claves son iguales (por ejemplo, miles de paquetes con el mismo peso), cada
partición separa un solo elemento: O(n²) con pila O(n); con muchas claves repetidas se
acerca a ese caso. Una partición en
tres vías (menores, iguales, mayores) lo evitaría. Con los 5 paquetes del dominio no
influye. No modifica la lista de entrada.

---

### GET /api/seleccion/mergesort

Paquetes ordenados de menor a mayor según un criterio, de forma estable.

**Parámetros:** `criterio` = `valor` | `peso` | `ratio` (default `ratio`). Orden ascendente.

**Ejemplo:** `GET /api/seleccion/mergesort?criterio=peso`

**Respuesta:**
```json
[{"id":"P5","nombre":"Paquete 5","peso":3.0,"valor":3.0,"nodoId":"H"},
 {"id":"P4","nombre":"Paquete 4","peso":4.0,"valor":4.0,"nodoId":"E"},
 {"id":"P2","nombre":"Paquete 2","peso":5.0,"valor":9.0,"nodoId":"C"},
 {"id":"P3","nombre":"Paquete 3","peso":5.0,"valor":9.0,"nodoId":"D"},
 {"id":"P1","nombre":"Paquete 1","peso":6.0,"valor":12.0,"nodoId":"B"}]
```
P2 y P3 empatan en peso y conservan su orden de entrada: MergeSort es **estable**.

**Errores:** 400 si el criterio es inválido.

**Complejidad:** O(n log n) en todos los casos: T(n) = 2T(n/2) + O(n). Memoria
auxiliar O(n).

**Estructura usada:** copia de la lista + lista auxiliar para la mezcla propia. Ante
empate se toma primero el elemento de la mitad izquierda. El mismo núcleo genérico
(`Ordenamiento.ordenarPorMezcla`) ordena los paquetes en greedy, las aristas en Kruskal
y los lugares al cargar el grafo.

---

### GET /api/seleccion/greedy

Selección de paquetes para un vehículo con heurística greedy.

**Función objetivo:** maximizar el valor total de los paquetes elegidos sin que su peso
total supere la capacidad. Los paquetes son indivisibles.

**Parámetros:** `capacidad` (kg, número ≥ 0).

**Ejemplo:** `GET /api/seleccion/greedy?capacidad=10`

**Respuesta:**
```json
{"elegidos":[{"id":"P1","nombre":"Paquete 1","peso":6.0,"valor":12.0,"nodoId":"B"},
 {"id":"P4","nombre":"Paquete 4","peso":4.0,"valor":4.0,"nodoId":"E"}],
 "valorTotal":16.0,"pesoTotal":10.0,"tecnica":"Greedy por ratio valor/peso"}
```

**Caso donde greedy NO da el óptimo:** con capacidad 10, greedy toma P1 (ratio 2,0,
6 kg), descarta P2 y P3 porque ya no entran (6 + 5 = 11) y completa con P4: valor 16.
El óptimo es P2 + P3 = 10 kg y valor **18** (`/api/seleccion/dinamica`).

**Errores:** 400 si la capacidad es negativa o no es un número (incluido `NaN`).

**Complejidad:** O(n log n), dominada por el ordenamiento; la pasada que elige es O(n).
Memoria O(n).

**Estructura usada:** MergeSort propio por ratio valor/peso **descendente**, con
desempate por id ascendente (P4 antes que P5, ambos con ratio 1), para que el
resultado no dependa del orden en que la base devuelva los paquetes. Si un paquete no
entra, se sigue revisando los siguientes.

---

### GET /api/seleccion/dinamica

Selección óptima de paquetes (mochila 0/1) con programación dinámica.

**Parámetros:** `capacidad` (kg, entero ≥ 0).

**Ejemplo:** `GET /api/seleccion/dinamica?capacidad=10`

**Respuesta:**
```json
{"elegidos":[{"id":"P2","nombre":"Paquete 2","peso":5.0,"valor":9.0,"nodoId":"C"},
 {"id":"P3","nombre":"Paquete 3","peso":5.0,"valor":9.0,"nodoId":"D"}],
 "valorTotal":18.0,"pesoTotal":10.0,"tecnica":"Programacion dinamica (mochila 0/1)"}
```

**Tabla dp del ejemplo** (fila i = primeros i paquetes, columna w = capacidad):

| i \ w | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 0 (ninguno) | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| 1 (P1: 6 kg, 12) | 0 | 0 | 0 | 0 | 0 | 0 | 12 | 12 | 12 | 12 | 12 |
| 2 (P2: 5 kg, 9) | 0 | 0 | 0 | 0 | 0 | 9 | 12 | 12 | 12 | 12 | 12 |
| 3 (P3: 5 kg, 9) | 0 | 0 | 0 | 0 | 0 | 9 | 12 | 12 | 12 | 12 | **18** |
| 4 (P4: 4 kg, 4) | 0 | 0 | 0 | 0 | 4 | 9 | 12 | 12 | 12 | 13 | **18** |
| 5 (P5: 3 kg, 3) | 0 | 0 | 0 | 3 | 4 | 9 | 12 | 12 | 12 | 15 | **18** |

Recuperación desde dp[5][10] = 18: dp[5][10] = dp[4][10], P5 no se toma;
dp[4][10] = dp[3][10], P4 no se toma; dp[3][10] = 18 ≠ dp[2][10] = 12, **P3 se toma** y
quedan 5 kg; dp[2][5] = 9 ≠ dp[1][5] = 0, **P2 se toma** y quedan 0 kg; dp[1][0] = dp[0][0],
P1 no se toma.

**Errores:** 400 si la capacidad es negativa o no es entera; 400 si algún paquete
tiene peso no entero (no se truncan decimales). Una capacidad muy grande no es un
error: `capacidad=2147483647` devuelve los 5 paquetes, valor 37.

**Complejidad:** O(n·W) en tiempo y memoria. Es **pseudopolinómica**: depende del
valor numérico de W y no del tamaño de su representación; duplicar los bits de W
eleva al cuadrado el trabajo.

**Estructura usada:** tabla `double[n+1][W+1]` llenada bottom-up con
dp[i][w] = max(dp[i-1][w], dp[i-1][w - peso_i] + valor_i) cuando el paquete entra.
W es el mínimo entre la capacidad pedida y la suma de los pesos (23 kg): con más
capacidad entran todos y la respuesta no cambia, así que no se reserva una tabla
gigante.

---

### GET /api/grafo/rutas

Todas las rutas **simples** (sin repetir lugares) entre dos lugares que no superan un
costo máximo ni una cantidad máxima de saltos. Un salto es un camino recorrido: una
ruta de 4 lugares tiene 3 saltos.

**Parámetros:** `origen`, `destino` (ids), `costoMaximo` (km, default 1000),
`saltosMaximos` (default 10). Los límites son inclusivos.

**Ejemplo:** `GET /api/grafo/rutas?origen=A&destino=H&costoMaximo=18&saltosMaximos=6`

**Respuesta:**
```json
[{"vertices":["A","B","D","E","F","H"],"costoTotal":18.0},
 {"vertices":["A","C","B","D","E","F","H"],"costoTotal":17.0},
 {"vertices":["A","C","B","D","E","G","H"],"costoTotal":18.0},
 {"vertices":["A","C","B","D","F","H"],"costoTotal":18.0}]
```

**Exploración con y sin poda** (A → H; nodos = entradas a la función recursiva,
incluida la raíz):

| Límites | Rutas | Nodos con poda | Descartadas por costo | Descartadas por saltos | Nodos sin poda |
|---|---|---|---|---|---|
| costo 20, saltos 6 | 11 | 49 | 16 | 0 | 78 |
| costo 18, saltos 5 | 2 | 34 | 23 | 2 | 78 |

Ambas variantes devuelven exactamente las mismas rutas. Detalle en `docs/VERIFICACIONES.md`.

**Errores:** 400 si un id no existe o un límite es negativo o `NaN`; 422 si hay costos
negativos (la poda por costo parcial solo es válida con costos no negativos). Si no
hay rutas, devuelve `[]`. Con origen igual a destino devuelve la ruta `[origen]` de
costo 0.

**Complejidad:** exponencial. En un grafo completo hay del orden de (V−2)! rutas
simples entre dos lugares y cada estado hace trabajo O(V): cota O(V·V!). La poda
reduce la exploración, pero no el peor caso. Memoria O(V + L): camino, visitados y
pila, más las rutas devueltas.

**Estructura usada:** camino actual + `visitado[]`. Hacer: agregar el vecino y
marcarlo; explorar; **deshacer**: desmarcarlo y quitarlo del camino. Se poda en cuanto
agregar un camino excede el costo o los saltos máximos.

---

### GET /api/seleccion/repartir

Reparto de **todos** los paquetes entre k camiones idénticos minimizando la carga del
camión más cargado.

**Parámetros:** `contenedores` (cantidad de camiones, de 1 a 100).

**Ejemplo:** `GET /api/seleccion/repartir?contenedores=3`

**Respuesta:**
```json
{"porContenedor":{
  "Camion 1":[{"id":"P1","nombre":"Paquete 1","peso":6.0,"valor":12.0,"nodoId":"B"},
              {"id":"P5","nombre":"Paquete 5","peso":3.0,"valor":3.0,"nodoId":"H"}],
  "Camion 2":[{"id":"P2","nombre":"Paquete 2","peso":5.0,"valor":9.0,"nodoId":"C"},
              {"id":"P4","nombre":"Paquete 4","peso":4.0,"valor":4.0,"nodoId":"E"}],
  "Camion 3":[{"id":"P3","nombre":"Paquete 3","peso":5.0,"valor":9.0,"nodoId":"D"}]},
 "cargaMaxima":9.0}
```
Cargas 9, 9 y 5. Puede haber otros repartos óptimos (por ejemplo 6, 9 y 8).

**Cota y por qué es optimista:** cota inferior de un nodo =
max(mayor carga actual, ⌈peso total / k⌉). Ninguna carga ya asignada puede bajar al
agregar paquetes, y algún camión tiene que llevar al menos el promedio, que con pesos
enteros se redondea hacia arriba. Nunca sobreestima lo alcanzable desde el nodo, así
que podar no pierde la solución óptima.

**Poda:** si la cota de un hijo es ≥ la mejor carga máxima de una solución completa ya
encontrada, se descarta. Con 3 camiones se exploraron **36 nodos y se podaron 43
ramas**, frente a los 364 nodos del árbol completo (1 + 3 + 9 + 27 + 81 + 243).

**Errores:** 400 si `contenedores` < 1 (`La cantidad de contenedores debe ser al menos 1`)
o > 100 (`La cantidad de contenedores no puede superar 100`). El tope protege la memoria
del servidor: la respuesta lista cada camión, aunque quede vacío. Con más camiones que
paquetes los sobrantes quedan vacíos. Una lista vacía da carga máxima 0.

**Complejidad:** hasta k^n asignaciones completas; cota conservadora O((n + k)·k^n)
contando la copia de cada nueva mejor solución. Memoria O(n + k) auxiliar más la pila.

**Estructura usada:** búsqueda en profundidad con arreglo de cargas, asignación
parcial y **copia** de la mejor asignación (no una referencia al arreglo que se sigue
modificando).

---

### GET /api/grafo/ucs

**Opcional, no implementado.** Responde 501
`{"estado":"PENDIENTE","detalle":"Todavia no esta implementado: UCS (opcional, fuera del alcance)"}`.
No pertenece a los rubros puntuados y queda fuera de la demostración.
