package com.yenaly.han1meviewer.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.dao.CheckInRecordDatabase
import com.yenaly.han1meviewer.logic.entity.CheckInRecordEntity
import com.yenaly.han1meviewer.logic.entity.CheckInType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class CheckInWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_CHECK_IN = "com.yenaly.han1meviewer.ACTION_WIDGET_CHECK_IN"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) scope.launch { refresh(context, mgr, id) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_CHECK_IN) {
            val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
            if (id != -1) scope.launch {
                withContext(Dispatchers.IO) {
                    val dao = CheckInRecordDatabase.getDatabase(context).checkInDao()
                    val t = LocalDate.now().toString()
                    val curCount = dao.getCountByDate(t)
                    if (curCount < 20) {
                        dao.insert(
                            CheckInRecordEntity(
                                date = t,
                                time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
                                type = CheckInType.MASTURBATION.storeName,
                                sideDishes = "",
                                feeling = ""
                            )
                        )
                    }
                }
                refresh(context, AppWidgetManager.getInstance(context), id)
            }
        }
    }

    private suspend fun refresh(c: Context, mgr: AppWidgetManager, id: Int) {
        val (today, days, total, best) = withContext(Dispatchers.IO) {
            val dao = CheckInRecordDatabase.getDatabase(c).checkInDao()
            val m = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
            val t = dao.getCountByDate(LocalDate.now().toString())
            val dDates = dao.getMonthlyCheckedDates(m)
            val d = dDates.size
            val tot = dao.getMonthlyCheckInTotal(m)
            var s = 0; var cur = 0
            val recs = dao.getRecordsBetween(
                YearMonth.now().atDay(1).toString(),
                YearMonth.now().atEndOfMonth().toString()
            )
            val map = mutableMapOf<String, Int>()
            recs.forEach { map[it.date] = (map[it.date] ?: 0) + 1 }
            for (day in 1..YearMonth.now().lengthOfMonth()) {
                val dt = YearMonth.now().atDay(day)
                if ((map[dt.toString()] ?: 0) > 0) { cur++; s = maxOf(s, cur) } else cur = 0
            }
            listOf(t, d, tot, s)
        }

        val views = RemoteViews(c.packageName, R.layout.checkin_widget)

        views.setTextViewText(R.id.tv_today, c.getString(R.string.counts, today))
        views.setTextViewText(R.id.tv_today_label, c.getString(R.string.widget_today_label))
        views.setTextViewText(R.id.tv_month_stats, "${c.getString(R.string.days, days)} / ${c.getString(R.string.counts, total)}")
        views.setTextViewText(R.id.tv_streak, "${c.getString(R.string.best_streak)} ${c.getString(R.string.days, best)}")
        views.setTextViewText(R.id.btn_checkin, c.getString(R.string.checkin))

        val ci = Intent(c, CheckInWidgetProvider::class.java).apply {
            action = ACTION_CHECK_IN; putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        }
        views.setOnClickPendingIntent(R.id.btn_checkin, PendingIntent.getBroadcast(
            c, id, ci, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        ))

        val oi = c.packageManager.getLaunchIntentForPackage(c.packageName)
        if (oi != null) {
            oi.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            views.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(
                c, id + 1000, oi, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ))
        }

        mgr.updateAppWidget(id, views)
    }
}
