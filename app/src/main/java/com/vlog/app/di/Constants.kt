package com.vlog.app.di

object Constants {

    const val VLOG_APP = "https://api.66log.com"
    const val API_BASE_URL = "${VLOG_APP}/api/json/v2/"

    const val APP_VERSION = "1.0.0"

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
    const val ENDPOINT_COMMENTS_POST = "videos/comments-created/{id}"
    const val ENDPOINT_COMMENTS = "videos/comments/{id}"
    const val ENDPOINT_VIDEO_DETAIL = "videos/detail/{id}"
    const val ENDPOINT_FILTER_LIST = "videos/list"
    const val ENDPOINT_CATEGORIES = "videos/categories"
    const val ENDPOINT_SEARCH = "videos/search"
}