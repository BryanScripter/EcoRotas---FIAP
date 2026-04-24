package com.example.ecorotas.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

// Guarda usuários, sessão e histórico de rotas no SharedPreferences (JSON em string).
class Storage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "EcoRotasStorage"
        private const val PREFS_NAME = "ecorotas_prefs"
        private const val USERS_KEY = "ecorota_users"
        private const val SESSION_KEY = "ecorota_session"
        private const val ROUTES_KEY = "ecorota_routes"
    }

    // usuários
    fun getUsers(): List<User> {
        val json = prefs.getString(USERS_KEY, null)
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            val arr = JSONArray(json)
            List(arr.length()) { i ->
                val obj = arr.getJSONObject(i)
                User(
                    name = obj.getString("name"),
                    email = obj.getString("email"),
                    password = obj.getString("password")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao ler usuários: ${e.message}")
            emptyList()
        }
    }

    fun findUser(email: String): User? = getUsers().find { it.email.equals(email, ignoreCase = true) }

    fun registerUser(user: User): Boolean {
        val users = getUsers().toMutableList()
        if (users.any { it.email.equals(user.email, ignoreCase = true) }) {
            Log.w(TAG, "Usuário já existe: ${user.email}")
            return false
        }
        users.add(user)
        val arr = JSONArray()
        users.forEach { u ->
            arr.put(JSONObject().apply {
                put("name", u.name)
                put("email", u.email)
                put("password", u.password)
            })
        }
        val success = prefs.edit().putString(USERS_KEY, arr.toString()).commit()
        Log.d(TAG, "Usuário registrado: ${user.email}, Sucesso: $success")
        return success
    }

    fun validateLogin(email: String, password: String): User? {
        val user = findUser(email.trim())
        return if (user != null && user.password == password) user else null
    }

    // sessão (quem está logado)
    fun setSession(user: User) {
        val json = JSONObject().apply {
            put("name", user.name)
            put("email", user.email)
            put("password", user.password)
        }.toString()
        prefs.edit().putString(SESSION_KEY, json).commit()
        Log.d(TAG, "Sessão iniciada para: ${user.email}")
    }

    fun getSession(): User? {
        val json = prefs.getString(SESSION_KEY, null) ?: return null
        return try {
            val obj = JSONObject(json)
            User(
                name = obj.getString("name"),
                email = obj.getString("email"),
                password = obj.getString("password")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao ler sessão: ${e.message}")
            null
        }
    }

    fun clearSession() {
        prefs.edit().remove(SESSION_KEY).commit()
        Log.d(TAG, "Sessão encerrada")
    }

    // histórico de rotas
    fun getRoutes(): List<RouteRecord> {
        val json = prefs.getString(ROUTES_KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            List(arr.length()) { i ->
                val obj = arr.getJSONObject(i)
                RouteRecord(
                    id = obj.getString("id"),
                    origin = obj.getString("origin"),
                    destination = obj.getString("destination"),
                    distance = obj.getDouble("distance"),
                    bestMode = obj.getString("bestMode"),
                    co2Saved = obj.getDouble("co2Saved"),
                    date = obj.getString("date")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao ler rotas: ${e.message}")
            emptyList()
        }
    }

    fun saveRoute(origin: String, destination: String, distance: Double, bestMode: String, co2Saved: Double): RouteRecord {
        val route = RouteRecord(
            id = UUID.randomUUID().toString(),
            origin = origin,
            destination = destination,
            distance = distance,
            bestMode = bestMode,
            co2Saved = co2Saved,
            date = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date())
        )
        val routes = getRoutes().toMutableList()
        routes.add(0, route)
        val arr = JSONArray()
        routes.forEach { r ->
            arr.put(JSONObject().apply {
                put("id", r.id)
                put("origin", r.origin)
                put("destination", r.destination)
                put("distance", r.distance)
                put("bestMode", r.bestMode)
                put("co2Saved", r.co2Saved)
                put("date", r.date)
            })
        }
        prefs.edit().putString(ROUTES_KEY, arr.toString()).commit()
        return route
    }

    fun getTotalCO2Saved(): Double {
        return (getRoutes().sumOf { it.co2Saved } * 1000).toLong() / 1000.0
    }
}
