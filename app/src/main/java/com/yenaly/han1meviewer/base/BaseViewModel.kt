package com.yenaly.han1meviewer.base

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.yenaly.han1meviewer.util.unsafeLazy

/**
 * ViewModel 基类，提供简单的父子 ViewModel 关联能力。
 */
open class BaseViewModel(
    @JvmField protected val application: Application
) : AndroidViewModel(application) {

    /** 父 ViewModel。 */
    var parent: BaseViewModel? = null
        private set

    /**
     * 将父 ViewModel 转换为指定类型，失败时返回 null。
     */
    @Suppress("UNCHECKED_CAST")
    fun <VM : BaseViewModel> parent(): VM? = parent as? VM

    /**
     * 获取指定类型的父 ViewModel，不存在时抛出异常。
     */
    fun <VM : BaseViewModel> requireParent(): VM = parent() ?: error("Parent not found")

    /**
     * 创建一个指定类型的子 ViewModel 延迟实例。
     */
    inline fun <reified VM : BaseViewModel> sub() = sub(VM::class.java)

    /**
     * 创建一个指定类型的子 ViewModel 延迟实例，并建立父子关系。
     */
    fun <VM : BaseViewModel> sub(clazz: Class<VM>): Lazy<VM> = unsafeLazy {
        clazz.getConstructor(Application::class.java).newInstance(application).also {
            it.parent = this
        }
    }
}
