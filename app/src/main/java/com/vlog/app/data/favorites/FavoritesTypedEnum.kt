package com.vlog.app.data.favorites

/**
 * 订阅类型枚举
 */
enum class FavoritesTypedEnum(id: Int, name: String) {
    STORY(10,"STORY"),      // 日志
    VIDEO(11,"VIDEO"),      // 视频
    COMMENT(20,"COMMENT"),    // 评论
    OTHER(0,"OTHER")       // 其他
}