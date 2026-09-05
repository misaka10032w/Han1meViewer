package com.yenaly.han1meviewer.util

import android.icu.text.Transliterator
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * 将文本归一化为简体中文，用于让搜索同时匹配简体、繁体与日文汉字。
 *
 * 归一化分两步：
 * 1. 日文新字体（しんじたい）→ 简体中文（纯 Kotlin 映射，所有版本可用）
 * 2. 繁体中文 → 简体中文（依赖 ICU，API 29+）
 *
 * 例如「飛鳥」「飞鸟」均归一化为「飞鸟」；「伝」「傳」「传」均归一化为「传」。
 */
fun String.toSimplified(): String {
    val jpConverted = JapaneseKanjiConverter.toSimplified(this)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return jpConverted
    return try {
        ChineseTransliterator.simplify(jpConverted)
    } catch (_: Throwable) {
        jpConverted
    }
}

/**
 * 日文新字体 → 简体中文映射。
 *
 * 覆盖日文常用汉字中与中文简体不同的日文特有写法（如「伝」「転」「円」「楽」），
 * 参考 OpenCC JPShinjitai 数据。无需 ICU，所有 Android 版本均可使用。
 */
private object JapaneseKanjiConverter {
    private val map: Map<Char, String> = mapOf(
        // あ行
        '亜' to "亚", '悪' to "恶", '圧' to "压", '囲' to "围", '壱' to "一",
        '隠' to "隐", '栄' to "荣", '営' to "营", '桜' to "樱", '円' to "圆",
        '応' to "应", '穏' to "稳",
        // か行
        '仮' to "假", '価' to "价", '絵' to "绘", '壊' to "坏", '懐' to "怀",
        '拡' to "扩", '殻' to "壳", '楽' to "乐", '渇' to "渴", '巻' to "卷",
        '陥' to "陷", '勧' to "劝", '寛' to "宽", '幹' to "干", '漢' to "汉",
        '関' to "关", '歓' to "欢", '観' to "观", '気' to "气", '帰' to "归",
        '亀' to "龟", '偽' to "伪", '戯' to "戏", '犠' to "牺", '拠' to "据",
        '挙' to "举", '郷' to "乡", '響' to "响", '暁' to "晓", '謹' to "谨",
        '駆' to "驱", '勲' to "勋", '薫' to "熏", '恵' to "惠", '掲' to "揭",
        '渓' to "溪", '経' to "经", '蛍' to "萤", '軽' to "轻", '継' to "继",
        '鶏' to "鸡", '芸' to "艺", '撃' to "击", '県' to "县", '倹' to "俭",
        '剣' to "剑", '険' to "险", '圏' to "圈", '検' to "检", '権' to "权",
        '顕' to "显", '験' to "验", '厳' to "严", '広' to "广", '効' to "效",
        '鉱' to "矿", '黒' to "黑", '獄' to "狱", '頃' to "顷",
        // さ行
        '査' to "查", '砕' to "碎", '済' to "济", '災' to "灾", '斎' to "斋",
        '剤' to "剂", '冊' to "册", '殺' to "杀", '雑' to "杂", '桟' to "栈",
        '賛' to "赞", '糸' to "丝", '歯' to "齿", '児' to "儿", '実' to "实",
        '舎' to "舍", '釈' to "释", '収' to "收", '従' to "从", '渋' to "涩",
        '獣' to "兽", '縦' to "纵", '粛' to "肃", '処' to "处", '緒' to "绪",
        '諸' to "诸", '渉' to "涉", '焼' to "烧", '証' to "证", '奨' to "奖",
        '乗' to "乘", '浄' to "净", '剰' to "剩", '畳' to "叠", '縄' to "绳",
        '壌' to "壤", '嬢' to "娘", '譲' to "让", '醸' to "酿", '図' to "图",
        '粋' to "粹", '酔' to "醉", '穂' to "穗", '髄' to "髓", '瀬' to "濑",
        '斉' to "齐", '摂' to "摄", '節' to "节", '専' to "专", '戦' to "战",
        '銭' to "钱", '繊' to "纤", '捜' to "搜", '挿' to "插", '巣' to "巢",
        '痩' to "瘦", '層' to "层", '総' to "总", '騒' to "骚", '増' to "增",
        '蔵' to "藏", '贈' to "赠", '臓' to "脏", '続' to "续",
        // た行
        '対' to "对", '帯' to "带", '滝' to "泷", '択' to "择", '沢' to "泽",
        '単' to "单", '団' to "团", '弾' to "弹", '遅' to "迟", '鋳' to "铸",
        '庁' to "厅", '徴' to "征", '懲' to "惩", '聴' to "听", '鎮' to "镇",
        '陳' to "陈", '賃' to "赁", '塚' to "冢", '漬' to "渍", '訂' to "订",
        '逓' to "递", '鉄' to "铁", '転' to "转", '伝' to "传", '稲' to "稻",
        '闘' to "斗", '徳' to "德", '読' to "读", '難' to "难", '弐' to "贰",
        '認' to "认", '脳' to "脑", '農' to "农",
        // は行
        '覇' to "霸", '拝' to "拜", '売' to "卖", '発' to "发", '髪' to "发",
        '抜' to "拔", '罰' to "罚", '飯' to "饭", '晩' to "晚", '浜' to "滨",
        '賓' to "宾", '頻' to "频", '譜' to "谱", '風' to "风", '払' to "拂",
        '仏' to "佛", '併' to "并", '閉' to "闭", '幣' to "币", '辺' to "边",
        '変' to "变", '弁' to "辩", '舗' to "铺", '豊' to "丰", '縫' to "缝",
        '謀' to "谋",
        // ま行
        '毎' to "每", '満' to "满", '無' to "无", '夢' to "梦", '霧' to "雾",
        '鳴' to "鸣", '綿' to "绵", '網' to "网", '黙' to "默", '問' to "问",
        '紋' to "纹", '門' to "门",
        // や行
        '約' to "约", '訳' to "译", '薬' to "药", '輸' to "输", '諭' to "谕",
        '癒' to "愈", '郵' to "邮", '誘' to "诱", '優' to "优", '預' to "预",
        '様' to "样", '養' to "养",
        // ら行
        '羅' to "罗", '頼' to "赖", '絡' to "络", '欄' to "栏", '濫' to "滥",
        '覧' to "览", '裏' to "里", '離' to "离", '陸' to "陆", '竜' to "龙",
        '慮' to "虑", '涼' to "凉", '猟' to "猎", '領' to "领", '療' to "疗",
        '瞭' to "了", '緑' to "绿", '倫' to "伦", '臨' to "临", '輪' to "轮",
        '隣' to "邻", '塁' to "垒", '涙' to "泪", '類' to "类", '齢' to "龄",
        '霊' to "灵", '歴' to "历", '暦' to "历", '練' to "练", '連' to "连",
        '錬' to "炼", '労' to "劳", '録' to "录", '論' to "论",
        // わ行 等
        '話' to "话", '賄' to "贿", '姉' to "姐", '塩' to "盐", '綺' to "绮",
        '儘' to "尽", '侭' to "尽", '歳' to "岁", '駅' to "驿",
    )

    fun toSimplified(text: String): String {
        if (text.none { it in map }) return text
        return buildString(text.length) {
            for (ch in text) append(map[ch] ?: ch)
        }
    }
}

/**
 * 独立持有 [android.icu.text.Transliterator] 引用，仅在 API 29+ 时被加载，
 * 避免低版本设备因类不存在导致类加载失败。
 */
private object ChineseTransliterator {
    @RequiresApi(Build.VERSION_CODES.Q)
    private val transliterator =
       Transliterator.getInstance("Traditional-Simplified")

    @RequiresApi(Build.VERSION_CODES.Q)
    fun simplify(text: String): String = transliterator.transliterate(text)
}
