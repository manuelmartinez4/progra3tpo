# Complejidades

Notación: V cantidad de vértices (lugares), E cantidad de aristas (caminos), n cantidad
de paquetes, W capacidad entera, k cantidad de camiones, L suma de las longitudes de
las rutas devueltas.

**Supuestos.** El grafo se representa con listas de adyacencia y ya está cargado en
memoria: la carga desde Neo4j cuesta O(V + E) más el ordenamiento por id de lugares y
caminos, O(V log V + E log E), y no se cuenta dentro de los algoritmos. Las memorias
auxiliares de los algoritmos de grafos excluyen el grafo. La red es simple (sin
aristas paralelas), así que E = O(V²) y log E = O(log V).

## Resumen

| Algoritmo | Recurrencia | Tiempo | Memoria | Estructura que la sostiene |
|---|---|---|---|---|
| BFS | no corresponde (iterativo) | O(V + E) | O(V) auxiliar | lista de adyacencia, cola `ArrayDeque`, `visitado[]` |
| DFS | T(u) = O(1 + grado(u)) + Σ T(v), con v vecino sin visitar | O(V + E) | O(V) auxiliar (pila de recursión) | lista de adyacencia, `visitado[]` |
| Dijkstra | no corresponde (iterativo) | O((V + E) log V) | O(V + E) auxiliar | lista de adyacencia, `PriorityQueue` con entradas repetidas, arreglos de distancia, predecesor y finalizado |
| Prim | no corresponde (iterativo) | O(V² + E) = O(V²) | O(V) auxiliar | lista de adyacencia, arreglos `key[]`, `parent[]` e `inMST[]` con búsqueda lineal |
| Kruskal | no corresponde (iterativo) | O(E log E + V) | O(V + E) | lista de aristas ordenada con MergeSort, Union-Find con `padre[]` y `rango[]` |
| QuickSort | T(n) = T(q) + T(n − q − 1) + O(n) | promedio O(n log n), peor O(n²) | O(n) total (copia + pila) | copia en `ArrayList`, partición de Lomuto, pivote mediana de tres |
| MergeSort | T(n) = 2T(n/2) + O(n) | O(n log n) | O(n) auxiliar | copia de la lista y lista auxiliar de mezcla |
| Greedy | no corresponde (iterativo) | O(n log n) | O(n) | lista ordenada por ratio con MergeSort |
| Mochila 0/1 | dp[i][w] = max(dp[i−1][w], dp[i−1][w − peso_i] + valor_i) si el paquete entra; si no, dp[i−1][w] | O(n·W), con W = min(capacidad, Σ pesos) | O(n·W) | tabla `double[n+1][W+1]` |
| Floyd-Warshall | d_k[i][j] = min(d_{k−1}[i][j], d_{k−1}[i][k] + d_{k−1}[k][j]) | O(V³) | O(V²) | una matriz V×V actualizada en el lugar |
| Rutas simples (backtracking) | T(m) = m·T(m − 1) + O(V), con m vértices libres | exponencial; cota O(V·V!) | O(V + L) incluyendo rutas | lista de adyacencia, camino actual, `visitado[]` |
| Reparto (Branch & Bound) | T(i) = k·T(i + 1) + O(k), con T(n) = O(n) | hasta k^n hojas; cota O((n + k)·k^n) | O(n + k) auxiliar y solución | arreglo de cargas, asignación parcial y copia de la mejor asignación |

## Recorridos

**BFS.** Cada vértice se encola una sola vez, porque se marca visitado al encolar. Al
desencolarlo se recorre su lista de adyacencia: la suma de los grados es 2E en un grafo
no dirigido. Total O(V + E). Memoria: `visitado[]` y la cola, O(V).

**DFS.** Cada vértice entra una sola vez a la recursión y recorre su lista de
adyacencia una vez: O(V + E). Si se escribe como recurrencia sobre los vértices
todavía no visitados, cada llamada hace O(1 + grado(u)) y llama a lo sumo una vez por
vecino no visitado, y la suma de esos trabajos es O(V + E). La profundidad de la
recursión puede llegar a V (si el grafo es un camino), así que la pila es O(V).

## Caminos mínimos

**Dijkstra.** Se usa una `PriorityQueue` (heap binario) con entradas inmutables
(distancia, vértice). En vez de disminuir la clave, cada mejora de `dist[v]` agrega una
entrada nueva y las obsoletas se descartan al extraerlas. Hay a lo sumo una entrada por
mejora, es decir O(E) entradas; cada inserción y extracción cuesta O(log E) = O(log V).
Cada vértice se finaliza una vez y cada arista se relaja una vez por sentido. Total
O((V + E) log V). Memoria: la cola puede tener **más de V** entradas, hasta O(E), más
los arreglos `dist`, `predecesor` y `finalizado`: O(V + E).

**Floyd-Warshall.** Tres bucles anidados de V iteraciones con trabajo O(1): O(V³). Se
actualiza una sola matriz V×V en el lugar: O(V²). La recurrencia de programación
dinámica es d_k[i][j] = min(d_{k−1}[i][j], d_{k−1}[i][k] + d_{k−1}[k][j]); usar una
sola matriz es correcto porque en la iteración k las filas y columnas de k no cambian
(d[k][k] = 0 si no hay ciclos negativos).

**Comparación.** Para todos los pares, V ejecuciones de Dijkstra cuestan
O(V·(V + E) log V): en un grafo ralo es mejor que O(V³); en uno denso (E ≈ V²) llega a
O(V³ log V) y Floyd gana. Floyd además admite costos negativos sin ciclos negativos.

## Árboles de recubrimiento mínimo

**Prim (búsqueda lineal).** V iteraciones; en cada una se busca el vértice fuera del
árbol de menor `key` en O(V) y se recorren sus vecinos. Total O(V² + E) = O(V²) porque
E ≤ V². Memoria `key[]`, `parent[]`, `inMST[]`: O(V). Con heap sería O(E log V), mejor
en grafos ralos; con 8 lugares se priorizó la simplicidad.

**Kruskal.** Extraer una copia de cada arista: O(V + E). Ordenarlas con MergeSort:
O(E log E). Recorrerlas con Union-Find con compresión de caminos y unión por rango:
O(E·α(V)), con α prácticamente constante. Total O(E log E + V). Memoria: la lista de
aristas, su copia ordenada y los arreglos `padre` y `rango`: O(V + E).

**Prim o Kruskal.** Prim con arreglos conviene en grafos densos; Kruskal en grafos
ralos, donde ordenar E aristas es barato.

## Ordenamientos

**MergeSort.** Divide en dos mitades, ordena cada una y mezcla en O(n):

    T(n) = 2T(n/2) + O(n)

Regla práctica con a = 2, b = 2, k = 1: como a = b^k, T(n) = O(n^k log n) = O(n log n),
en todos los casos. Memoria: la copia y la lista auxiliar de mezcla, O(n), más la pila
O(log n).

**QuickSort.** La partición de Lomuto recorre el subarreglo en O(n) y deja el pivote en
su posición final:

    T(n) = T(q) + T(n − q − 1) + O(n)

- Caso promedio (particiones balanceadas): T(n) = 2T(n/2) + O(n) = O(n log n).
- Peor caso (el pivote es siempre un extremo): T(n) = T(n − 1) + O(n) = O(n²).

Con pivote **mediana de tres** las listas ordenadas o invertidas ya no son peor caso,
pero existen entradas que lo provocan. La más común son las **claves repetidas**:
Lomuto deja todos los elementos iguales al pivote del mismo lado, así que si todas las
claves son iguales cada partición separa un solo elemento y T(n) = T(n − 1) + O(n) =
O(n²), con pila O(n). Una partición en tres vías (menores, iguales, mayores) lo evita. Memoria: como devuelve una
copia, **no es O(1)**: O(n) por la copia, más pila O(log n) en promedio y O(n) en el
peor caso.

## Selección

**Greedy.** Ordenar por ratio valor/peso con MergeSort: O(n log n). Recorrer la lista
eligiendo: O(n). Total O(n log n). Memoria O(n). No garantiza el óptimo con paquetes
indivisibles.

**Mochila 0/1.** La tabla tiene (n + 1)·(W + 1) celdas y cada una se calcula en O(1):
O(n·W) en tiempo y memoria. La recuperación recorre las n filas hacia atrás: O(n).
W es el mínimo entre la capacidad pedida y la suma de los pesos, porque con más
capacidad entran todos los paquetes y la respuesta es la misma: así la tabla nunca
supera (n + 1)·(Σ pesos + 1) celdas, aunque se pida una capacidad enorme.
Es **pseudopolinómica**: W es un número, y su tamaño de entrada es log W bits; el
tiempo es exponencial en ese tamaño. Duplicar la cantidad de bits de W eleva W al
cuadrado.

## Búsquedas exponenciales

**Backtracking de rutas simples.** El árbol de búsqueda tiene un nodo por cada camino
simple que empieza en el origen. En un grafo completo hay del orden de (V − 2)! caminos
simples entre dos vértices fijos, y cada nodo recorre la lista de vecinos, O(V):
cota O(V·V!). Recurrencia del peor caso, con m vértices todavía libres:

    T(m) = m·T(m − 1) + O(V)   →   O(V·m!)

La poda por costo y saltos corta ramas en cuanto se excede un límite, pero no cambia
el peor caso (por ejemplo, con límites muy altos no poda nada). Memoria: camino y
`visitado[]`, O(V), pila de profundidad O(V), más las rutas guardadas, O(L).

**Branch & Bound de reparto.** Cada paquete puede ir a cualquiera de los k camiones:
el árbol tiene k^n hojas y (k^(n+1) − 1)/(k − 1) nodos. Cada nodo interno evalúa la
cota de sus k hijos en O(1) (la carga máxima se lleva como parámetro); cada hoja que
mejora la solución copia la asignación en O(n). Cota conservadora O((n + k)·k^n).
Recurrencia del peor caso:

    T(i) = k·T(i + 1) + O(k),   T(n) = O(n)   →   O((n + k)·k^n)

La poda por cota reduce mucho la exploración en la práctica (36 nodos en lugar de 364
con 5 paquetes y 3 camiones) pero no elimina el peor caso exponencial. Memoria:
cargas O(k), asignación actual y mejor O(n), pila O(n).
