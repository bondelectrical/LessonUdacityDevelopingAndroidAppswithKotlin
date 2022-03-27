
package net.ucoz.abondarenko.devbytes.domain

import net.ucoz.abondarenko.devbytes.util.smartTruncate


data class Video(val title: String,
                 val description: String,
                 val url: String,
                 val updated: String,
                 val thumbnail: String) {

    /**
     * Short description is used for displaying truncated descriptions in the UI
     */
    val shortDescription: String
        get() = description.smartTruncate(200)
}
