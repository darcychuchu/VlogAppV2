package com.vlog.app.di

object Constants {

    // AndroidManifest.xml application set  android:usesCleartextTraffic="true"
    //const val VLOG_APP = "http://192.168.43.239:8199"
    const val VLOG_APP = "https://66log.com"
    const val API_BASE_URL = "${VLOG_APP}/api/json/v2/"

    const val APP_VERSION = "2.0.7"

    // update
    const val ENDPOINT_CHECK_UPDATE = "app/version"

    // users
    const val ENDPOINT_USER_CHECK_NAME = "users/stated-name"
    const val ENDPOINT_USER_CHECK_NICKNAME = "users/stated-nickname"
    const val ENDPOINT_USER_LOGIN = "users/login"
    const val ENDPOINT_USER_REGISTER = "users/register"
    const val ENDPOINT_USER_INFO = "users/stated-me/{name}/{token}"
    const val ENDPOINT_USER_UPDATE = "users/updated/{name}/{token}"

    // videos
    const val ENDPOINT_VIDEO_COMMENTS_POST = "videos/comments-created/{videoId}"
    const val ENDPOINT_VIDEO_COMMENTS = "videos/comments/{videoId}"
    const val ENDPOINT_VIDEO_DETAIL = "videos/detail/{videoId}"
    const val ENDPOINT_VIDEO_GATHER = "videos/detail/gathers/{videoId}"
    const val ENDPOINT_VIDEO_FILTER = "videos/filter"
    const val ENDPOINT_VIDEO_CATEGORIES = "videos/categories"
    const val ENDPOINT_VIDEO_SEARCH = "videos/search"

    // favorites
    const val ENDPOINT_FAVORITES_LIST = "videos/favorites/{username}"
    const val ENDPOINT_FAVORITES_CREATE = "videos/favorites-created/{videoId}"
    const val ENDPOINT_FAVORITES_REMOVE = "videos/favorites-removed/{videoId}"
    const val ENDPOINT_FAVORITES_UPDATE = "videos/favorites-videos-sync/{username}"


    // stories
    const val ENDPOINT_STORY_COMMENTS_POST = "{name}/stories/comments-created/{storyId}"
    const val ENDPOINT_STORY_COMMENTS = "{name}/stories/comments/{storyId}"
    const val ENDPOINT_STORY_DETAIL = "{name}/stories/detail/{storyId}"
    const val ENDPOINT_STORY_LIST = "{name}/stories/list"
    const val ENDPOINT_STORY_CREATE = "{name}/stories-created"


    // Global stories
    const val ENDPOINT_GLOBAL_STORY_LIST = "stories/list"
}