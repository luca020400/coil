/**
 * Copyright 2022 Savvas Dalkitsis
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package coil.sample

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Modified from: https://github.com/savvasdalkitsis/lazy-staggered-grid */
@Composable
fun LazyStaggeredGrid(
    columnCount: Int,
    states: List<LazyListState> = List(columnCount) { rememberLazyListState() },
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: LazyStaggeredGridScope.() -> Unit,
) {
    check(columnCount == states.size) {
        "Invalid number of lazy list states. Expected: $columnCount. Actual: ${states.size}"
    }

    val scope = rememberCoroutineScope { Dispatchers.Main.immediate }
    val scrollableState = rememberScrollableState { delta ->
        scope.launch { states.forEach { it.scrollBy(-delta) } }
        delta
    }
    val gridScope = RealLazyStaggeredGridScope(columnCount).apply(content)

    Row(Modifier.scrollable(scrollableState, Orientation.Vertical)) {
        for (index in 0 until columnCount) {
            LazyColumn(
                contentPadding = contentPadding,
                state = states[index],
                userScrollEnabled = false,
                modifier = Modifier.weight(1f)
            ) {
                for ((key, itemContent) in gridScope.items(index)) {
                    item(key) { itemContent() }
                }
            }
        }
    }
}

/** Receiver scope which is used by [LazyStaggeredGrid]. */
interface LazyStaggeredGridScope {

    /** Adds a single item. */
    fun item(
        key: Any? = null,
        content: @Composable () -> Unit
    )
}

/** Adds a [count] of items. */
inline fun LazyStaggeredGridScope.items(
    count: Int,
    crossinline itemContent: @Composable (index: Int) -> Unit
) {
    for (index in 0 until count) {
        item { itemContent(index) }
    }
}

/** Adds a [count] of items. */
inline fun LazyStaggeredGridScope.items(
    count: Int,
    key: (index: Int) -> Any,
    crossinline itemContent: @Composable (index: Int) -> Unit
) {
    for (index in 0 until count) {
        item(key(index)) { itemContent(index) }
    }
}

/** Adds a list of items. */
inline fun <T> LazyStaggeredGridScope.items(
    items: List<T>,
    crossinline itemContent: @Composable (item: T) -> Unit
) = items(
    count = items.size,
    itemContent = { itemContent(items[it]) }
)

/** Adds a list of items. */
inline fun <T> LazyStaggeredGridScope.items(
    items: List<T>,
    key: (item: T) -> Any,
    crossinline itemContent: @Composable (item: T) -> Unit
) = items(
    count = items.size,
    key = { key(items[it]) },
    itemContent = { itemContent(items[it]) }
)

private class RealLazyStaggeredGridScope(private val columnCount: Int) : LazyStaggeredGridScope {

    private val items = Array(columnCount) { ArrayList<Pair<Any?, @Composable () -> Unit>>() }
    private var currentIndex = 0

    override fun item(key: Any?, content: @Composable () -> Unit) {
        items[currentIndex % columnCount] += key to content
        currentIndex += 1
    }

    fun items(index: Int): List<Pair<Any?, @Composable () -> Unit>> = items[index]
}