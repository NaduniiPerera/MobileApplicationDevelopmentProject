package com.example.imilipocket.ui.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.imilipocket.R
import com.example.imilipocket.data.Transaction
import com.example.imilipocket.databinding.FragmentTransactionsBinding
import com.example.imilipocket.data.PreferenceManager
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.*

class TransactionsFragment : Fragment() {

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TransactionsViewModel by viewModels {
        TransactionsViewModelFactory(PreferenceManager(requireContext()), requireContext())
    }
    private val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupLineCharts()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        if (isAdded && !isDetached) {
            viewModel.loadTransactions()
        }
    }

    private fun setupLineCharts() {
        val commonSetup: com.github.mikephil.charting.charts.LineChart.() -> Unit = {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            setPinchZoom(true)
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return try {
                        dateFormat.format(Date(value.toLong()))
                    } catch (e: Exception) {
                        ""
                    }
                }
            }
            axisLeft.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return formatCurrency(value.toDouble())
                }
            }
            axisRight.isEnabled = false
            setNoDataText("No data available")
        }

        binding.incomeChart.apply(commonSetup)
        binding.expenseChart.apply(commonSetup)
    }

    private fun observeViewModel() {
        viewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            updateLineCharts(transactions)
        }
    }

    private fun updateLineCharts(transactions: List<Transaction>) {
        try {
            val incomeEntries = transactions
                .filter { it.type == Transaction.Type.INCOME }
                .groupBy { it.date }
                .map { (date, transactions) ->
                    Entry(date.toFloat(), transactions.sumOf { it.amount }.toFloat())
                }
                .sortedBy { it.x }

            val expenseEntries = transactions
                .filter { it.type == Transaction.Type.EXPENSE }
                .groupBy { it.date }
                .map { (date, transactions) ->
                    Entry(date.toFloat(), transactions.sumOf { it.amount }.toFloat())
                }
                .sortedBy { it.x }

            // Update Income Chart
            if (incomeEntries.isNotEmpty()) {
                val incomeDataSet = LineDataSet(incomeEntries, "Income").apply {
                    color = ContextCompat.getColor(requireContext(), R.color.green_500)
                    setDrawCircles(true)
                    setDrawValues(true)
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return formatCurrency(value.toDouble())
                        }
                    }
                }
                binding.incomeChart.data = LineData(incomeDataSet)
            } else {
                binding.incomeChart.data = null
                binding.incomeChart.setNoDataText("No income transactions yet")
            }
            binding.incomeChart.invalidate()

            // Update Expense Chart
            if (expenseEntries.isNotEmpty()) {
                val expenseDataSet = LineDataSet(expenseEntries, "Expenses").apply {
                    color = ContextCompat.getColor(requireContext(), R.color.red_500)
                    setDrawCircles(true)
                    setDrawValues(true)
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return formatCurrency(value.toDouble())
                        }
                    }
                }
                binding.expenseChart.data = LineData(expenseDataSet)
            } else {
                binding.expenseChart.data = null
                binding.expenseChart.setNoDataText("No expense transactions yet")
            }
            binding.expenseChart.invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
            binding.incomeChart.setNoDataText("Error loading data")
            binding.expenseChart.setNoDataText("Error loading data")
            binding.incomeChart.invalidate()
            binding.expenseChart.invalidate()
        }
    }

    private fun formatCurrency(amount: Double): String {
        return try {
            java.text.NumberFormat.getCurrencyInstance().format(amount)
        } catch (e: Exception) {
            e.printStackTrace()
            "$0.00"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 