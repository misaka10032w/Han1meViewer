package com.yenaly.han1meviewer.ui.adapter

import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter4.BaseQuickAdapter
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2024/04/05 005 20:44
 */
abstract class BaseSingleDifferAdapter<T : Any, VH : RecyclerView.ViewHolder> :
    BaseQuickAdapter<T, VH> {

    private constructor(items: List<T>, config: AsyncDifferConfig<T>) : super(items, config)

    constructor(diffCallback: DiffUtil.ItemCallback<T>) : this(
        emptyList(),
        AsyncDifferConfig.Builder(diffCallback).build()
    )

    constructor(diffCallback: DiffUtil.ItemCallback<T>, item: T) : this(
        listOf(item),
        AsyncDifferConfig.Builder(diffCallback).build()
    )

    constructor(config: AsyncDifferConfig<T>) : this(emptyList(), config)

    constructor(config: AsyncDifferConfig<T>, item: T) : this(listOf(item), config)

    var item: T?
        get() = items.firstOrNull()
        set(value) {
            items = if (value != null) listOf(value) else emptyList()
        }

    suspend fun submit(item: T?) = suspendCoroutine { cont ->
        val list = item?.let(::listOf).orEmpty()
        submitList(list) {
            cont.resume(Unit)
        }
    }

    protected abstract fun onBindViewHolder(holder: VH, item: T?)

    open fun onBindViewHolder(holder: VH, item: T?, payloads: List<Any>) {
        onBindViewHolder(holder, item)
    }

    final override fun onBindViewHolder(holder: VH, position: Int, item: T?) {
        onBindViewHolder(holder, item)
    }

    final override fun onBindViewHolder(holder: VH, position: Int, item: T?, payloads: List<Any>) {
        onBindViewHolder(holder, item, payloads)
    }

    override fun add(data: T, commitCallback: Runnable?) {
        throw RuntimeException("Please use setItem()")
    }

    override fun add(position: Int, data: T, commitCallback: Runnable?) {
        throw RuntimeException("Please use setItem()")
    }

    override fun addAll(collection: Collection<T>, commitCallback: Runnable?) {
        throw RuntimeException("Please use setItem()")
    }

    override fun addAll(position: Int, collection: Collection<T>, commitCallback: Runnable?) {
        throw RuntimeException("Please use setItem()")
    }

    override fun remove(data: T, commitCallback: Runnable?) {
        throw RuntimeException("Please use setItem()")
    }

    override fun removeAtRange(range: IntRange, commitCallback: Runnable?) {
        throw RuntimeException("Please use setItem()")
    }

    override fun removeAt(position: Int, commitCallback: Runnable?) {
        throw RuntimeException("Please use setItem()")
    }

    override fun set(position: Int, data: T) {
        throw RuntimeException("Please use setItem()")
    }
}