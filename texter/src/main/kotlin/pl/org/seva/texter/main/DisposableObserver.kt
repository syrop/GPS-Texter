package pl.org.seva.texter.main

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.OnLifecycleEvent
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

fun LifecycleOwner.liveDisposable() = CompositeDisposable().also {
    lifecycle.addObserver(DisposableObserver(it))
}

infix fun Disposable.addTo(compositeDisposable: CompositeDisposable) = compositeDisposable.add(this)

class DisposableObserver(private val disposable: Disposable) : LifecycleObserver {
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() = disposable.dispose()
}
