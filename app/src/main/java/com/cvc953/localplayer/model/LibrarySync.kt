package com.cvc953.localplayer.model

import org.json.JSONArray
import org.json.JSONObject

const val MIN_SONG_DURATION_MS = 30_000L

interface LibraryPreferences {
    fun isFirstScanDone(): Boolean

    fun setFirstScanDone()

    fun isAutoScanEnabled(): Boolean

    fun getMusicFolderUris(): List<String>

    fun getLibrarySignature(): String?

    fun setLibrarySignature(raw: String?)
}

interface SongsCacheStore {
    fun read(): String?

    fun write(json: String)

    fun clear()
}

/**
 * Lista de rutas que deben ser excluidas del escaneo. Esto incluye directorios de apps de
 * mensajería, notificaciones del sistema, tonos, alarmas, efectos de sonido y otras apps que
 * almacenan audio pero que no son canciones de música.
 */
private val EXCLUDED_PATH_FRAGMENTS =
    listOf(
        // Apps de mensajería y redes sociales
        "/WhatsApp/",
        "/Snapchat/",
        "/TikTok/",
        "/Instagram/",
        "/facebook/",
        "/Discord/",
        "/Viber/",
        "/Signal/",
        "/Skype/",
        "/Messenger/",
        "/.telegram/",
        "/Android/data/com.whatsapp/",
        "/Android/data/com.telegram/",
        "/Android/data/com.snapchat/",
        "/Android/data/com.tiktok/",
        "/Android/data/com.instagram/",
        "/Android/data/com.facebook/",
        "/Android/data/com.discord/",
        "/Android/data/com.viber/",
        "/Android/data/org.signal/",
        "/Android/data/com.skype/",
        "/Android/data/com.facebook.orca/",
        "/Android/media/com.whatsapp/",
        "/Android/media/com.telegram/",
        "/Android/media/com.snapchat/",
        "/Android/media/com.tiktok/",
        "/Android/media/com.instagram/",
        "/Android/media/com.facebook/",
        "/Android/media/com.discord/",
        "/Android/media/com.viber/",
        "/Android/media/org.signal/",
        "/Android/media/com.skype/",
        "/Android/media/com.facebook.orca/",
        // Directorios del sistema (notificaciones, tonos, alarmas)
        "/Notifications/",
        "/Ringtones/",
        "/Alarms/",
        "/UI/",
        "/system/media/audio/notifications/",
        "/system/media/audio/ringtones/",
        "/system/media/audio/alarms/",
        "/system/media/audio/ui/",
        // Grabaciones de voz y llamadas
        "/Recordings/",
        "/Voice Recorder/",
        "/Call Recording/",
        "/Call Recordings/",
        "/Voice/",
        "/Sounds/",
        "/AudioRecorder/",
        // Directorios de apps de juegos (suelen tener .ogg como efectos)
        "/Android/obb/",
        "/Android/data/com.game",
        "/Android/data/com.unity",
        "/game_data/",
        "/assets/sounds/",
        "/assets/audio/",
    )

fun isExcludedPath(filePath: String?): Boolean {
    if (filePath == null) return false
    return EXCLUDED_PATH_FRAGMENTS.any { filePath.contains(it, ignoreCase = true) }
}

fun isPlayableMusic(
    filePath: String?,
    durationMs: Long,
): Boolean = !isExcludedPath(filePath) && durationMs >= MIN_SONG_DURATION_MS

data class LibrarySignature(
    val songCount: Int,
    val maxId: Long,
    val maxDateAdded: Long,
    val maxDateModified: Long,
) {
    fun serialize(): String = "$songCount|$maxId|$maxDateAdded|$maxDateModified"

    companion object {
        val EMPTY = LibrarySignature(0, 0L, 0L, 0L)

        fun deserialize(raw: String?): LibrarySignature? {
            val parts = raw?.split('|') ?: return null
            if (parts.size != 4) return null
            return try {
                LibrarySignature(
                    songCount = parts[0].toInt(),
                    maxId = parts[1].toLong(),
                    maxDateAdded = parts[2].toLong(),
                    maxDateModified = parts[3].toLong(),
                )
            } catch (_: NumberFormatException) {
                null
            }
        }
    }
}

data class SongKey(
    val id: Long,
    val dateAdded: Long = 0L,
    val dateModified: Long = 0L,
)

data class LibraryDiff(
    val added: List<Long> = emptyList(),
    val removed: List<Long> = emptyList(),
    val modified: List<Long> = emptyList(),
) {
    val hasChanges: Boolean
        get() = added.isNotEmpty() || removed.isNotEmpty() || modified.isNotEmpty()

    companion object {
        val NONE = LibraryDiff()
    }
}

fun computeSignature(keys: List<SongKey>): LibrarySignature =
    LibrarySignature(
        songCount = keys.size,
        maxId = keys.maxOfOrNull { it.id } ?: 0L,
        maxDateAdded = keys.maxOfOrNull { it.dateAdded } ?: 0L,
        maxDateModified = keys.maxOfOrNull { it.dateModified } ?: 0L,
    )

fun diffLibrary(
    previous: List<SongKey>,
    current: List<SongKey>,
): LibraryDiff {
    val previousById = previous.associateBy { it.id }
    val currentById = current.associateBy { it.id }

    val added = current.filter { it.id !in previousById }.map { it.id }
    val removed = previous.filter { it.id !in currentById }.map { it.id }
    val modified =
        current.filter { candidate ->
            val old = previousById[candidate.id]
            old != null && old.dateModified != candidate.dateModified
        }.map { it.id }

    return LibraryDiff(added = added, removed = removed, modified = modified)
}

data class CachedSong(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val year: Int?,
    val uri: String,
    val duration: Long,
    val filePath: String?,
    val trackNumber: Int,
    val discNumber: Int,
    val sampleRate: Int?,
    val mimeType: String?,
    val dateAdded: Long,
    val dateModified: Long,
    val genre: String,
)

object SongsCacheCodec {
    const val SCHEMA = 2

    fun encode(songs: List<CachedSong>): String {
        val root = JSONObject()
        root.put("schema", SCHEMA)
        val array = JSONArray()
        songs.forEach { song ->
            array.put(
                JSONObject().apply {
                    put("id", song.id)
                    put("title", song.title)
                    put("artist", song.artist)
                    put("album", song.album)
                    put("year", song.year ?: JSONObject.NULL)
                    put("uri", song.uri)
                    put("duration", song.duration)
                    put("filePath", song.filePath ?: "")
                    put("trackNumber", song.trackNumber)
                    put("discNumber", song.discNumber)
                    put("sampleRate", song.sampleRate ?: JSONObject.NULL)
                    put("mimeType", song.mimeType ?: JSONObject.NULL)
                    put("dateAdded", song.dateAdded)
                    put("dateModified", song.dateModified)
                    put("genre", song.genre)
                },
            )
        }
        root.put("songs", array)
        return root.toString()
    }

    fun decode(raw: String?): List<CachedSong>? {
        if (raw.isNullOrBlank()) return null
        return try {
            val root = JSONObject(raw)
            if (root.optInt("schema", -1) != SCHEMA) return null
            val array = root.optJSONArray("songs") ?: return null

            val list = mutableListOf<CachedSong>()
            for (i in 0 until array.length()) {
                val o = array.optJSONObject(i) ?: continue
                val uri = o.optString("uri", "")
                if (uri.isEmpty()) continue
                list.add(
                    CachedSong(
                        id = o.optLong("id", 0L),
                        title = o.optString("title", ""),
                        artist = o.optString("artist", ""),
                        album = o.optString("album", ""),
                        year = if (o.isNull("year")) null else o.optInt("year"),
                        uri = uri,
                        duration = o.optLong("duration", 0L),
                        filePath = o.optString("filePath", "").takeIf { it.isNotEmpty() },
                        trackNumber = o.optInt("trackNumber", 0),
                        discNumber = o.optInt("discNumber", 0),
                        sampleRate = if (o.isNull("sampleRate")) null else o.optInt("sampleRate").takeIf { it > 0 },
                        mimeType = if (o.isNull("mimeType")) null else o.optString("mimeType", "").takeIf { it.isNotEmpty() },
                        dateAdded = o.optLong("dateAdded", 0L),
                        dateModified = o.optLong("dateModified", 0L),
                        genre = o.optString("genre", ""),
                    ),
                )
            }
            list
        } catch (_: Exception) {
            null
        }
    }
}
