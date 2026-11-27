package com.hyperlauncher.ai
import com.hyperlauncher.ai.data.local.HiddenPin
import org.junit.Assert.*
import org.junit.Test
class HiddenPinTest {
    @Test fun saltedHashesAndConstantTimeVerification() {
        val a=HiddenPin.create("123456");val b=HiddenPin.create("123456")
        assertNotEquals(a,b);assertTrue(HiddenPin.verify("123456",a));assertFalse(HiddenPin.verify("654321",a));assertFalse(HiddenPin.verify("123456","malformed"))
    }
}
