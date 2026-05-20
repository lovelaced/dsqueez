package app.dsqueez

import android.app.Application
import app.dsqueez.nativebridge.Resampler

class DsqueezApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Resampler.tryLoad()
    }
}
