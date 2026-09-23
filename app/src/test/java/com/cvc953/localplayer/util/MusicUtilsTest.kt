package com.cvc953.localplayer.util

import android.net.Uri
import com.cvc953.localplayer.model.Song
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicUtilsTest {

    private fun dummySong(id: Long, title: String, artist: String, album: String): Song {
        return Song(
            id = id,
            title = title,
            artist = artist,
            album = album,
            year = 2023,
            uri = mockk<Uri>(relaxed = true),
            duration = 180000L,
            trackNumber = id.toInt(),
            discNumber = 1,
            filePath = "/storage/emulated/0/Music/$album/$title.mp3",
        )
    }

    @Test
    fun testNormalizeArtistName_handlesCollaborationsAndCleanFormatting() {
        assertEquals(listOf("Emilia"), normalizeArtistName("Emilia"))
        assertEquals(listOf("Emilia", "NATHY PELUSO"), normalizeArtistName("Emilia & NATHY PELUSO"))
        assertEquals(listOf("Carín León", "Luis Mexia"), normalizeArtistName("Carín León & Luis Mexia"))
        assertEquals(listOf("Emilia", "LUDMILLA", "ZECCA"), normalizeArtistName("Emilia, LUDMILLA, & ZECCA"))
        assertEquals(listOf("Santi Celli", "Ale Sergi"), normalizeArtistName("Santi Celli (feat. Ale Sergi)"))
        assertEquals(listOf("Taylor Swift", "Post Malone"), normalizeArtistName("Taylor Swift (with Post Malone)"))
        assertEquals(listOf("Carlos Vives", "Emilia", "Wisin", "Xavi"), normalizeArtistName("Carlos Vives, ,, Emilia, Wisin, &, Xavi"))
        assertEquals(listOf("AC/DC"), normalizeArtistName("AC/DC"))
    }

    @Test
    fun testGroupSongsIntoAlbums_mergesCollaborationsUnderDominantArtist() {
        val songs = listOf(
            dummySong(1, "Facts", "Emilia", ".mp3"),
            dummySong(2, "Jagger", "Emilia", ".mp3"),
            dummySong(3, "JET_Set", "Emilia & NATHY PELUSO", ".mp3"),
            dummySong(4, "La_Original", "Emilia & TINI", ".mp3"),
            dummySong(5, "Primera Cita", "Carín León", "Colmillo De Leche"),
            dummySong(6, "Pedazo De Tonto", "Carín León & Luis Mexia", "Colmillo De Leche"),
        )

        val albums = groupSongsIntoAlbums(songs)

        assertEquals(2, albums.size)

        val mp3Album = albums.find { it.name == ".mp3" }
        assertEquals(".mp3", mp3Album?.name)
        assertEquals("Emilia", mp3Album?.artist)
        assertEquals(4, mp3Album?.songCount)

        val colmilloAlbum = albums.find { it.name == "Colmillo De Leche" }
        assertEquals("Colmillo De Leche", colmilloAlbum?.name)
        assertEquals("Carín León", colmilloAlbum?.artist)
        assertEquals(2, colmilloAlbum?.songCount)
    }

    @Test
    fun testGetSongsForAlbum_retrievesAllAlbumTracksIncludingCollaborations() {
        val songs = listOf(
            dummySong(1, "Facts", "Emilia", ".mp3"),
            dummySong(2, "JET_Set", "Emilia & NATHY PELUSO", ".mp3"),
            dummySong(3, "La_Original", "Emilia & TINI", ".mp3"),
            dummySong(4, "Primera Cita", "Carín León", "Colmillo De Leche"),
        )

        val emiliaAlbumSongs = getSongsForAlbum(songs, ".mp3", "Emilia")
        assertEquals(3, emiliaAlbumSongs.size)
        assertTrue(emiliaAlbumSongs.any { it.title == "JET_Set" })
        assertTrue(emiliaAlbumSongs.any { it.title == "La_Original" })

        val carinAlbumSongs = getSongsForAlbum(songs, "Colmillo De Leche", "Carín León")
        assertEquals(1, carinAlbumSongs.size)
        assertEquals("Primera Cita", carinAlbumSongs.first().title)
    }
}
