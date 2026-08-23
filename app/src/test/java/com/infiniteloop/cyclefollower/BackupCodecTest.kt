package com.infiniteloop.cyclefollower

import com.infiniteloop.cyclefollower.backup.BackupCodec
import com.infiniteloop.cyclefollower.data.Contraception
import com.infiniteloop.cyclefollower.data.DayLog
import com.infiniteloop.cyclefollower.data.DayMood
import com.infiniteloop.cyclefollower.data.PmsSeverity
import com.infiniteloop.cyclefollower.data.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BackupCodecTest {

    private val profile = UserProfile(
        partnerName = "Ana",
        periodStarts = listOf(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 30)),
        statedCycleLength = 29,
        periodLength = 4,
        contraception = Contraception.HORMONAL_IUD,
        pmsSeverity = PmsSeverity.SEVERE,
        pmdd = true,
        symptoms = setOf("CRAMPS", "INSOMNIA"),
        setupComplete = true,
        discreetMode = true,
        appLock = true,
        dayLogs = listOf(
            DayLog(LocalDate.of(2026, 5, 20), DayMood.ROUGH, setOf("CRAMPS")),
            DayLog(LocalDate.of(2026, 5, 21), DayMood.GOOD),
        ),
    ).normalised()

    @Test
    fun `a plain backup round trips every field`() {
        val text = BackupCodec.encode(profile)
        assertFalse(BackupCodec.isEncrypted(text))
        val result = BackupCodec.decode(text)
        assertTrue(result is BackupCodec.Result.Ok)
        assertEquals(profile, (result as BackupCodec.Result.Ok).profile)
    }

    @Test
    fun `an encrypted backup round trips and does not leak its contents`() {
        val text = BackupCodec.encode(profile, password = "correct horse")
        assertTrue(BackupCodec.isEncrypted(text))
        assertFalse("her name must not survive in the ciphertext", text.contains("Ana"))
        assertFalse(text.contains("CRAMPS"))

        val result = BackupCodec.decode(text, password = "correct horse")
        assertTrue(result is BackupCodec.Result.Ok)
        assertEquals(profile, (result as BackupCodec.Result.Ok).profile)
    }

    @Test
    fun `the wrong password fails cleanly rather than returning nonsense`() {
        val text = BackupCodec.encode(profile, password = "correct horse")
        assertEquals(BackupCodec.Result.WrongPassword, BackupCodec.decode(text, password = "wrong horse"))
    }

    @Test
    fun `an encrypted backup opened with no password asks for one`() {
        val text = BackupCodec.encode(profile, password = "hunter2")
        assertEquals(BackupCodec.Result.NeedsPassword, BackupCodec.decode(text))
    }

    @Test
    fun `two encryptions of the same profile differ`() {
        // A fixed salt or iv would make identical backups byte-identical and leak that nothing changed.
        val a = BackupCodec.encode(profile, password = "hunter2")
        val b = BackupCodec.encode(profile, password = "hunter2")
        assertNotEquals(a, b)
    }

    @Test
    fun `arbitrary files are rejected instead of half-imported`() {
        assertEquals(BackupCodec.Result.NotABackup, BackupCodec.decode("not json at all"))
        assertEquals(BackupCodec.Result.NotABackup, BackupCodec.decode("""{"hello":"world"}"""))
        assertEquals(
            BackupCodec.Result.NotABackup,
            BackupCodec.decode("""{"magic":"something.else","version":1,"payload":"{}"}"""),
        )
    }

    @Test
    fun `a backup from a newer app version is refused rather than misread`() {
        val text = BackupCodec.encode(profile).replace("\"version\": 1", "\"version\": 99")
        val result = BackupCodec.decode(text)
        assertTrue(result is BackupCodec.Result.Unreadable)
    }

    @Test
    fun `an empty profile round trips`() {
        val blank = UserProfile()
        val result = BackupCodec.decode(BackupCodec.encode(blank))
        assertEquals(blank.normalised(), (result as BackupCodec.Result.Ok).profile)
    }
}
