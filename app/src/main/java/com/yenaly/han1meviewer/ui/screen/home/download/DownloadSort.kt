package com.yenaly.han1meviewer.ui.screen.home.download

import androidx.annotation.StringRes
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.entity.download.VideoWithCategories
import java.text.Collator
import java.util.Locale

/**
 * 已下载列表的排序方式。
 *
 * @param labelRes 用于 UI 展示的字符串资源 ID
 */
enum class DownloadSort(@StringRes val labelRes: Int) {
    /** 日期反排序（最新在前） */
    DATE_DESC(R.string.sort_by_date_descending),

    /** 日期正排序（最早在前） */
    DATE_ASC(R.string.sort_by_date_ascending),

    /** 首字母正排序（A-Z，支持英文/中文拼音/日文假名） */
    FIRST_LETTER_ASC(R.string.sort_by_alphabet_ascending),

    /** 首字母反排序（Z-A） */
    FIRST_LETTER_DESC(R.string.sort_by_alphabet_descending),
}

/**
 * 生成对应排序方式的比较器。
 */
fun downloadSortComparator(sort: DownloadSort): Comparator<VideoWithCategories> = when (sort) {
    DownloadSort.DATE_DESC -> compareByDescending { it.video.addDate }
    DownloadSort.DATE_ASC -> compareBy { it.video.addDate }
    DownloadSort.FIRST_LETTER_ASC -> firstLetterComparator()
    DownloadSort.FIRST_LETTER_DESC -> firstLetterComparator().reversed()
}

private fun firstLetterComparator(): Comparator<VideoWithCategories> {
    val collator = Collator.getInstance(Locale.CHINESE).apply {
        strength = Collator.PRIMARY
    }
    return Comparator { a, b ->
        val compared = collator.compare(sortKey(a.video.title), sortKey(b.video.title))
        if (compared != 0) compared else a.video.title.compareTo(b.video.title)
    }
}

/**
 * 生成用于「首字母」排序的归一化字符串：
 * - 全角空格转半角空格
 * - 全角 ASCII 转半角
 * - 日文假名转罗马音
 * - 其余（英文、中文等）保留，交由 [Collator] 处理（中文按拼音排序）
 */
private fun sortKey(title: String): String {
    val sb = StringBuilder(title.length)
    for (ch in title) {
        when {
            ch == '\u3000' -> sb.append(' ')
            ch in '\uFF01'..'\uFF5E' -> sb.append((ch.code - 0xFEE0).toChar())
            else -> sb.append(KANA_TO_ROMAJI[ch] ?: ch)
        }
    }
    return sb.toString()
}

/**
 * 假名（平假名 + 片假名）到罗马音的映射表。
 * 片假名通过平假名码点 + 0x60 推导，无需重复声明。
 */
private val KANA_TO_ROMAJI: Map<Char, String> = buildMap {
    val hiraganaToRomaji = mapOf(
        'あ' to "a", 'い' to "i", 'う' to "u", 'え' to "e", 'お' to "o",
        'か' to "ka", 'き' to "ki", 'く' to "ku", 'け' to "ke", 'こ' to "ko",
        'さ' to "sa", 'し' to "shi", 'す' to "su", 'せ' to "se", 'そ' to "so",
        'た' to "ta", 'ち' to "chi", 'つ' to "tsu", 'て' to "te", 'と' to "to",
        'な' to "na", 'に' to "ni", 'ぬ' to "nu", 'ね' to "ne", 'の' to "no",
        'は' to "ha", 'ひ' to "hi", 'ふ' to "fu", 'へ' to "he", 'ほ' to "ho",
        'ま' to "ma", 'み' to "mi", 'む' to "mu", 'め' to "me", 'も' to "mo",
        'や' to "ya", 'ゆ' to "yu", 'よ' to "yo",
        'ら' to "ra", 'り' to "ri", 'る' to "ru", 'れ' to "re", 'ろ' to "ro",
        'わ' to "wa", 'を' to "wo", 'ん' to "n",
        // 浊音
        'が' to "ga", 'ぎ' to "gi", 'ぐ' to "gu", 'げ' to "ge", 'ご' to "go",
        'ざ' to "za", 'じ' to "ji", 'ず' to "zu", 'ぜ' to "ze", 'ぞ' to "zo",
        'だ' to "da", 'ぢ' to "di", 'づ' to "du", 'で' to "de", 'ど' to "do",
        'ば' to "ba", 'び' to "bi", 'ぶ' to "bu", 'べ' to "be", 'ぼ' to "bo",
        // 半浊音
        'ぱ' to "pa", 'ぴ' to "pi", 'ぷ' to "pu", 'ぺ' to "pe", 'ぽ' to "po",
        // 小假名
        'ぁ' to "a", 'ぃ' to "i", 'ぅ' to "u", 'ぇ' to "e", 'ぉ' to "o",
        'っ' to "tsu", 'ゃ' to "ya", 'ゅ' to "yu", 'ょ' to "yo", 'ゎ' to "wa",
        // ヴ 的平假名
        'ゔ' to "vu",
    )
    for ((hiragana, romaji) in hiraganaToRomaji) {
        put(hiragana, romaji)
        put((hiragana.code + 0x60).toChar(), romaji)
    }
    // 片假名 ヵ / ヶ（无对应平假名）
    put('ヵ', "ka")
    put('ヶ', "ke")
    // 长音记号，忽略
    put('ー', "")
}
