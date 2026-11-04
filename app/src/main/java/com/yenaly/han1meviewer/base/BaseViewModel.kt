package com.yenaly.han1meviewer.base

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.yenaly.han1meviewer.util.unsafeLazy

open class BaseViewModel(
    @JvmField protected val application: Application
) : AndroidViewModel(application) {

    var parent: BaseViewModel? = null
        private set

    @Suppress("UNCHECKED_CAST")
    fun <VM : BaseViewModel> parent(): VM? = parent as? VM

    fun <VM : BaseViewModel> requireParent(): VM = parent() ?: error("Parent not found")

    inline fun <reified VM : BaseViewModel> sub() = sub(VM::class.java)

    fun <VM : BaseViewModel> sub(clazz: Class<VM>): Lazy<VM> = unsafeLazy {
        clazz.getConstructor(Application::class.java).newInstance(application).also {
            it.parent = this
        }
    }
}
