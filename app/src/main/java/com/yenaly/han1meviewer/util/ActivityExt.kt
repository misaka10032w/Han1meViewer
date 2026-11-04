package com.yenaly.han1meviewer.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

inline fun <reified T : Activity> Context.findActivity(): T {
    return findActivityOrNull() ?: error("No activity of type ${T::class.java.simpleName} found")
}

inline fun <reified T : Activity> Context.findActivityOrNull(): T? {
    var context = this
    while (context is ContextWrapper) {
        if (context is T) {
            return context
        }
        context = context.baseContext
    }
    return null
}

fun Context.requireComponentActivity(): ComponentActivity {
    return (this as? ComponentActivity ?: findActivityOrNull<ComponentActivity>())
        ?: ActivityManager.currentActivity.get() as? ComponentActivity
        ?: error("No ComponentActivity found")
}

private val atomicInteger = AtomicInteger()

suspend fun <I, O> Context.awaitActivityResult(
    contract: ActivityResultContract<I, O>,
    input: I,
): O {
    val key = "activity_rq#${atomicInteger.getAndIncrement()}"

    val activity = this.requireComponentActivity()
    val lifecycle = activity.lifecycle
    var launcher: ActivityResultLauncher<I>? = null
    val observer: LifecycleEventObserver = object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (Lifecycle.Event.ON_DESTROY === event) {
                launcher?.unregister()
                lifecycle.removeObserver(this)
            }
        }
    }

    return withContext(Dispatchers.Main) {
        lifecycle.addObserver(observer)
        suspendCoroutine(object : Function1<Continuation<O>, Unit> {
            private var resumed = false
            override fun invoke(cont: Continuation<O>) {
                launcher = activity.activityResultRegistry.register(key, contract) {
                    if (!resumed) {
                        resumed = true
                        launcher?.unregister()
                        lifecycle.removeObserver(observer)
                        cont.resume(it)
                    }
                }.apply { launch(input) }
            }
        })
    }
}

suspend fun Context.requestPermission(key: String): Boolean {
    if (ContextCompat.checkSelfPermission(
            this, key
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    ) return true
    return awaitActivityResult(ActivityResultContracts.RequestPermission(), key)
}

inline fun <reified T : Activity> Activity.startActivity() {
    startActivity(Intent(this, T::class.java))
}

infix fun Context.browse(uri: String) {
    val intent = Intent(Intent.ACTION_VIEW, uri.toUri())
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    startActivity(intent)
}
