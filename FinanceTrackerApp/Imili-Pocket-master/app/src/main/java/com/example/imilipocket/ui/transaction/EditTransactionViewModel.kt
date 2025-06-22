package com.example.imilipocket.ui.transaction

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.imilipocket.data.PreferenceManager
import com.example.imilipocket.data.Result
import com.example.imilipocket.data.Transaction
import com.example.imilipocket.util.NotificationHelper

class EditTransactionViewModel(
    private val preferenceManager: PreferenceManager,
    private val context: Context
) : ViewModel() {
    private val _updateResult = MutableLiveData<Result<Unit>>()
    val updateResult: LiveData<Result<Unit>> = _updateResult
    private val notificationHelper = NotificationHelper(context)

    fun getCategories(): List<String> {
        return preferenceManager.getCategories()
    }

    fun updateTransaction(transaction: Transaction) {
        try {
            preferenceManager.updateTransaction(transaction)
            _updateResult.value = Result.Success(Unit)

            // Check budget alerts after updating transaction
            if (transaction.type == Transaction.Type.EXPENSE) {
                val monthlyBudget = preferenceManager.getMonthlyBudget()
                val monthlyExpenses = preferenceManager.getMonthlyExpenses()
                notificationHelper.showBudgetAlert(monthlyBudget, monthlyExpenses)
            }
        } catch (e: Exception) {
            _updateResult.value = Result.Error(e)
        }
    }
}

class EditTransactionViewModelFactory(
    private val preferenceManager: PreferenceManager,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditTransactionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditTransactionViewModel(preferenceManager, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 