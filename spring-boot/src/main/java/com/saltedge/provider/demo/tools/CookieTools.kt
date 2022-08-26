/*
 * This file is part of the Salt Edge Authenticator distribution
 * (https://github.com/saltedge/sca-identity-service-example).
 * Copyright (c) 2020 Salt Edge Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 or later.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * For the additional permissions granted for Salt Edge Authenticator
 * under Section 7 of the GNU General Public License see THIRD_PARTY_NOTICES.md
 */
package com.saltedge.provider.demo.tools

import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletResponse

const val COOKIE_AUTHENTICATION_ACTION = "KEY_AUTHENTICATION_ACTION"
const val COOKIE_PAYMENT_ACTION = "COOKIE_PAYMENT_ACTION"

fun clearActionCookie(key: String, response: HttpServletResponse) {
    val cookie = Cookie(key, "");
    cookie.maxAge = 0
    response.addCookie(cookie);
}

fun saveActionCookie(key: String, data: String, response: HttpServletResponse) {
    val cookie = Cookie(key, data);
    cookie.maxAge = 5 * 60
    response.addCookie(cookie);
}