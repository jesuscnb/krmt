package com.remotefamily.app

import org.junit.Assert.assertEquals
import org.junit.Test

class AppIdentityTest {
    @Test
    fun applicationNameIsRemoteFamily() {
        assertEquals("RemoteFamily", AppIdentity.name)
    }
}
