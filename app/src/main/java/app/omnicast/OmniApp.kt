package app.omnicast

import android.app.Application
import app.omnicast.data.ServiceLocator
import coil.ImageLoader
import coil.ImageLoaderFactory

class OmniApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }

    // Coil usa el mismo OkHttpClient: así las miniaturas/portadas reciben el Bearer del servidor.
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient { ServiceLocator.http }
            .crossfade(true)
            .build()
}
