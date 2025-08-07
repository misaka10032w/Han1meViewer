package com.yenaly.han1meviewer.ui.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yenaly.han1meviewer.logic.dao.CheckInRecordDatabase
import com.yenaly.han1meviewer.logic.entity.CheckInRecordEntity
import com.yenaly.yenaly_libs.utils.application
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class CheckInCalendarViewModel : ViewModel() {

    // 当前月份
    private val _currentMonth = mutableStateOf(YearMonth.now())
    val currentMonth: State<YearMonth> = _currentMonth
    //记录数
    private val _records = mutableStateMapOf<LocalDate, Int>()
    val records: SnapshotStateMap<LocalDate, Int> = _records
    //每月计数
    private val _checkedDays = mutableIntStateOf(0)
    val checkedDays: State<Int> get() = _checkedDays
    //每月累计
    private val _monthTotal = mutableIntStateOf(0)
    val monthlyTotal: State<Int> get() = _monthTotal

    private val dao = CheckInRecordDatabase.getDatabase(application).checkInDao()

    init {
        loadMonthRecords(_currentMonth.value)
        updateCheckedDays()
        updateMonthlyTotalCheck()
    }

    fun previousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
        loadMonthRecords(_currentMonth.value)
        updateCheckedDays()
        updateMonthlyTotalCheck()
    }

    fun nextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
        loadMonthRecords(_currentMonth.value)
        updateCheckedDays()
        updateMonthlyTotalCheck()
    }

    fun incrementCheckIn(date: LocalDate) {
        viewModelScope.launch {
            val currentCount = dao.getRecord(date.toString())?.count ?: 0
            val newCount = currentCount + 1
            dao.insertOrUpdate(CheckInRecordEntity(date.toString(), newCount))
            _records[date] = newCount
            updateCheckedDays()
            updateMonthlyTotalCheck()
        }
    }

    fun clearCheckIn(date: LocalDate) {
        viewModelScope.launch {
            dao.insertOrUpdate(CheckInRecordEntity(date.toString(), 0))
            _records[date] = 0
            updateCheckedDays()
            updateMonthlyTotalCheck()
        }
    }

    fun updateCheckedDays() {
        viewModelScope.launch {
            val count = dao.getMonthlyCheckedDays(
                currentMonth.value.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            )
            _checkedDays.intValue = count
        }
    }

    fun updateMonthlyTotalCheck() {
        viewModelScope.launch {
            val totalCheckIns = dao.getMonthlyCheckInTotal(
                currentMonth.value.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            ) ?: 0
            _monthTotal.intValue = totalCheckIns
        }
    }

    fun loadMonthRecords(month: YearMonth) {
        viewModelScope.launch {
            val start = month.atDay(1)
            val end = month.atEndOfMonth()
            val allRecords = dao.getRecordsBetween(start.toString(), end.toString())
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE
            _records.clear()
            allRecords.forEach {
                val localDate = LocalDate.parse(it.date, formatter)
                _records[localDate] = it.count
            }
        }
    }
}
