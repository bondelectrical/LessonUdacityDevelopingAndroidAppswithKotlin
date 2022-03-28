
package net.ucoz.abondarenko.devbytes.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ucoz.abondarenko.devbytes.database.VideosDatabase
import net.ucoz.abondarenko.devbytes.database.asDomainModel
import net.ucoz.abondarenko.devbytes.domain.Video
import net.ucoz.abondarenko.devbytes.network.Network
import net.ucoz.abondarenko.devbytes.network.asDatabaseModel

class VideosRepository(private val database: VideosDatabase) {


    val videos: LiveData<List<Video>> =
        Transformations.map(database.videoDao.getVideos()) {
            it.asDomainModel()
        }

    suspend fun refreshVideos() {
        withContext(Dispatchers.IO) {
            val playlist = Network.devbytes.getPlaylist().await()
            database.videoDao.insertAll(*playlist.asDatabaseModel())
        }
    }
}