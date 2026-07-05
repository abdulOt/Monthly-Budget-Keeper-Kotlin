package com.talent.monthlybudgetkeeper

import android.content.Context
import android.widget.Toast

class AppState(private val context: Context) {
    fun showMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
