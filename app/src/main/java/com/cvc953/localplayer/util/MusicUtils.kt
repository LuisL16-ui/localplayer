package com.cvc953.localplayer.util

import com.cvc953.localplayer.model.Album
import com.cvc953.localplayer.model.Song
import java.io.File

fun normalizeArtistName(artist: String): List<String> {
    val trimmed = artist.trim()
    if (trimmed.isEmpty()) return emptyList()

    val placeholder = "___ACDC___"
    var text = trimmed.replace("AC/DC", placeholder, ignoreCase = true)
    text = text.replace(Regex("""[\(\[](?:feat\.?|ft\.?|featuring|with)\s+([^\)\]]+)[\)\]]""", RegexOption.IGNORE_CASE)) { matchResult ->
        ", " + matchResult.groupValues[1]
    }
    text = text.replace(Regex("""(?i)\s+(?:feat\.?|ft\.?|featuring|with)\s+"""), ", ")
    text = text.replace(Regex("""\s*&\s*"""), ", ")
    text = text.replace(Regex("""\s*/\s*"""), ", ")
    text = text.replace(Regex("""\s*;\s*"""), ", ")

    return text
        .split(',')
        .map { it.replace(placeholder, "AC/DC").trim() }
        .filter { it.isNotEmpty() && !it.matches(Regex("""^[&,\-_./\\|]+$""")) }
        .distinct()
}

fun normalizeAlbumName(albumName: String): List<String> {
    val trimmed = albumName.trim()
    return if (trimmed.isNotEmpty()) listOf(trimmed) else emptyList()
}

fun groupSongsIntoAlbums(songs: List<Song>): List<Album> {
    if (songs.isEmpty()) return emptyList()

    val unknownAlbums = mutableListOf<Song>()
    val namedAlbumsMap = mutableMapOf<String, MutableList<Song>>()

    for (song in songs) {
        val rawAlbum = song.album.trim()
        val lower = rawAlbum.lowercase()
        if (rawAlbum.isEmpty() || lower == "desconocido" || lower == "<unknown>" || lower == "unknown") {
            unknownAlbums.add(song)
        } else {
            namedAlbumsMap.getOrPut(lower) { mutableListOf() }.add(song)
        }
    }

    val result = mutableListOf<Album>()

    unknownAlbums
        .groupBy { it.artist.ifBlank { "Desconocido" } }
        .forEach { (artist, artistSongs) ->
            result.add(Album("Desconocido", artist, artistSongs.size))
        }

    for ((_, albumSongs) in namedAlbumsMap) {
        val originalName = albumSongs
            .groupBy { it.album.trim() }
            .maxByOrNull { it.value.size }
            ?.key ?: albumSongs.first().album.trim()

        val clusters = mutableListOf<MutableList<Song>>()
        for (song in albumSongs) {
            val songArtists = normalizeArtistName(song.artist).map { it.lowercase() }
            val songFolder = song.filePath?.let { File(it).parent }

            val matchingClusters = clusters.filter { cluster ->
                cluster.any { other ->
                    val otherFolder = other.filePath?.let { File(it).parent }
                    val sameFolder = songFolder != null && otherFolder != null && songFolder == otherFolder
                    if (sameFolder) return@any true

                    val otherArtists = normalizeArtistName(other.artist).map { it.lowercase() }
                    songArtists.any { it in otherArtists }
                }
            }

            if (matchingClusters.isEmpty()) {
                clusters.add(mutableListOf(song))
            } else {
                val primaryCluster = matchingClusters.first()
                primaryCluster.add(song)
                if (matchingClusters.size > 1) {
                    for (i in 1 until matchingClusters.size) {
                        val toMerge = matchingClusters[i]
                        primaryCluster.addAll(toMerge)
                        clusters.remove(toMerge)
                    }
                }
            }
        }

        for (cluster in clusters) {
            val artistFrequencies = mutableMapOf<String, Int>()
            for (song in cluster) {
                for (artist in normalizeArtistName(song.artist)) {
                    val key = artist.trim()
                    if (key.isNotEmpty()) {
                        artistFrequencies[key] = (artistFrequencies[key] ?: 0) + 1
                    }
                }
            }

            val bestArtist = artistFrequencies.maxByOrNull { it.value }?.key
                ?: cluster.firstOrNull()?.artist?.ifBlank { "Desconocido" }
                ?: "Desconocido"

            result.add(Album(originalName, bestArtist, cluster.size))
        }
    }

    return result.sortedWith(compareBy({ it.name.lowercase() }, { it.artist.lowercase() }))
}

fun getSongsForAlbum(
    songs: List<Song>,
    albumName: String,
    artistName: String,
): List<Song> {
    val cleanAlbumName = albumName.trim()
    val isUnknownAlbum = cleanAlbumName.isEmpty() ||
        cleanAlbumName.equals("desconocido", ignoreCase = true) ||
        cleanAlbumName.equals("<unknown>", ignoreCase = true) ||
        cleanAlbumName.equals("unknown", ignoreCase = true)

    val albumSongs = if (isUnknownAlbum) {
        songs.filter { song ->
            val a = song.album.trim().lowercase()
            a.isEmpty() || a == "desconocido" || a == "<unknown>" || a == "unknown"
        }
    } else {
        songs.filter { song ->
            normalizeAlbumName(song.album).any { it.equals(cleanAlbumName, ignoreCase = true) }
        }
    }
    if (albumSongs.isEmpty()) return emptyList()

    val targetArtists = normalizeArtistName(artistName).map { it.lowercase() }
    if (targetArtists.isEmpty() || targetArtists.contains("desconocido") || targetArtists.contains("<unknown>")) {
        return albumSongs
    }

    val matchedSongs = albumSongs.filter { song ->
        val songArtists = normalizeArtistName(song.artist).map { it.lowercase() }
        songArtists.any { it in targetArtists }
    }

    return if (matchedSongs.isNotEmpty()) matchedSongs else albumSongs
}
