package com.srbd.testrc

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private const val PREFS_NAME = "expenses_prefs"
private const val EXPENSES_KEY = "expenses_json"
private const val THEME_KEY = "selected_theme"

data class Expense(
    val id: String,
    val amount: Double,
    val category: ExpenseCategory,
    val note: String,
    val date: String,
)

enum class ExpenseCategory {
    Food,
    Travel,
    Other,
}

enum class AppThemeChoice(val label: String) {
    Default("Default"),
    Dark("Dark"),
    Blue("Blue"),
    Green("Green"),
}

class ExpenseRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getExpenses(): List<Expense> {
        val raw = prefs.getString(EXPENSES_KEY, "[]") ?: "[]"
        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    Expense(
                        id = item.optString("id"),
                        amount = item.optDouble("amount"),
                        category = runCatching { ExpenseCategory.valueOf(item.optString("category")) }
                            .getOrDefault(ExpenseCategory.Other),
                        note = item.optString("note"),
                        date = item.optString("date"),
                    )
                )
            }
        }.sortedByDescending { it.date }
    }

    fun addExpense(amount: Double, category: ExpenseCategory, note: String, date: String): List<Expense> {
        val updated = listOf(
            Expense(
                id = UUID.randomUUID().toString(),
                amount = amount,
                category = category,
                note = note.trim(),
                date = date.ifBlank { today() },
            )
        ) + getExpenses()
        saveExpenses(updated)
        return updated
    }

    fun selectedTheme(): AppThemeChoice {
        return runCatching { AppThemeChoice.valueOf(prefs.getString(THEME_KEY, AppThemeChoice.Default.name)!!) }
            .getOrDefault(AppThemeChoice.Default)
    }

    fun saveSelectedTheme(theme: AppThemeChoice) {
        prefs.edit().putString(THEME_KEY, theme.name).apply()
    }

    private fun saveExpenses(expenses: List<Expense>) {
        val array = JSONArray()
        expenses.forEach { expense ->
            array.put(
                JSONObject()
                    .put("id", expense.id)
                    .put("amount", expense.amount)
                    .put("category", expense.category.name)
                    .put("note", expense.note)
                    .put("date", expense.date)
            )
        }
        prefs.edit().putString(EXPENSES_KEY, array.toString()).apply()
    }

    companion object {
        fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
}
