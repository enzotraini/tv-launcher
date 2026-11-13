package dev.enzotraini.tvlauncher.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InputsModelTest {
    @Test
    fun bindsFourHdmiPortsWithoutInventingConnection() {
        val reported = listOf(
            ReportedPort("hw2", "HDMI 2", ConnectionState.CONNECTED),
            ReportedPort("hdmi-3", "Vazio", null),
            ReportedPort("av", "AV", ConnectionState.DISCONNECTED),
        )
        val bound = bindSlots(reported, mapOf("hdmi-1" to "PlayStation 5"))
        assertEquals(listOf("hdmi-1", "hdmi-2", "hdmi-3", "hdmi-4", "antenna"), bound.map { it.slot.id })
        assertEquals("PlayStation 5", bound[0].name)
        assertEquals("HDMI 1", inputSubtitle(bound[0]))
        assertNull(bound[0].port)
        assertNull(connectionLabel(bound[0].port?.state))
        assertEquals("HDMI 2", bound[1].name)
        assertEquals(ConnectionState.CONNECTED, bound[1].port?.state)
        assertEquals("conectado", connectionLabel(bound[1].port?.state))
        assertEquals("sem sinal", connectionLabel(ConnectionState.DISCONNECTED))
        assertEquals("em espera", connectionLabel(ConnectionState.STANDBY))
        assertNull(connectionLabel(null))
        assertNull(connectionFromTvInputState(9))
        assertEquals(ConnectionState.CONNECTED, connectionFromTvInputState(0))
        assertNull(bound[2].port?.state)
        assertEquals("Vazio", bound[2].port?.label)
        assertNull(bound[3].port)
        assertNull(bound[4].port)
        assertEquals(2, hdmiNumber("Entrada HDMI2", "x"))
        assertEquals(1, hdmiNumber("", "HW1"))
        assertNull(hdmiNumber("Componente", "cvbs"))
    }

    @Test
    fun shortcutsAreSystemSettingsOnly() {
        val shortcuts = systemShortcuts()
        assertEquals(listOf("picture", "sound", "wifi", "bluetooth", "settings"), shortcuts.map { it.id })
        assertEquals("android.settings.DISPLAY_SETTINGS", shortcuts.first().actions.first())
        assertTrue(shortcuts.flatMap { it.actions + it.components.map { pair -> pair.first } }.none { it.startsWith("http") })
    }

    private fun assertTrue(value: Boolean) = org.junit.Assert.assertTrue(value)
}
