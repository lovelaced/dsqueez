package app.dsqueez

import android.app.Application
import app.dsqueez.nativebridge.Vips

class DsqueezApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Vips.tryLoad()
    }
}
