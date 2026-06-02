package com.yenaly.han1meviewer.util

import com.yenaly.han1meviewer.logic.model.SearchOption

object TagLocalizer {

    private data class TagMappings(
        val labels: Map<String, String>,
        val searchKeys: Map<String, String>,
    )

    private val tagMappings: TagMappings by lazy {
        buildTagMappings(
            loadAssetAs<Map<String, List<SearchOption>>>("search_options/tags.json")
                .orEmpty()
                .values
                .flatten() + loadAssetAs<List<SearchOption>>("search_options/genre.json").orEmpty()
        )
    }

    fun localizeTags(tags: List<String>): List<String> {
        if (tags.isEmpty()) return tags
        return tags.map(::localizeTag)
    }

    fun localizeTag(tag: String): String = tagMappings.labels[tag] ?: tag

    fun resolveSearchKey(tag: String): String = tagMappings.searchKeys[tag] ?: tag

    private fun buildTagMappings(options: List<SearchOption>): TagMappings {
        val labels = mutableMapOf<String, String>()
        val searchKeys = mutableMapOf<String, String>()
        options.forEach { option ->
            val label = option.value.takeIf { it.isNotBlank() } ?: return@forEach
            val searchKey = option.searchKey?.takeIf { it.isNotBlank() } ?: return@forEach
            listOfNotNull(
                option.searchKey,
                option.name,
                option.lang?.zhrCN,
                option.lang?.zhrTW,
                option.lang?.en,
                option.lang?.ja,
            ).forEach { rawTag ->
                labels.putIfAbsent(rawTag, label)
                searchKeys.putIfAbsent(rawTag, searchKey)
            }
        }
        return TagMappings(labels = labels, searchKeys = searchKeys)
    }
}
