package com.berlin.user

import kotlinx.serialization.Serializable

@Serializable
data class UserCredentials(val email: String, val password: String)

@Serializable
data class LoginUser(val id: Int, val name: String, val age: Int?, val email: String?, var avatar: String?, var token: String)