package com.uniwise.payment_service.modules.payment.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

class VnPayUtilsTest {

    @Test
    void hmacSHA512_throwsOnNullOrEmptyInput() {
        assertThrows(IllegalArgumentException.class, () -> VnPayUtils.hmacSHA512(null, "data"));
        assertThrows(IllegalArgumentException.class, () -> VnPayUtils.hmacSHA512("", "data"));
        assertThrows(IllegalArgumentException.class, () -> VnPayUtils.hmacSHA512("secret", null));
    }

    @Test
    void verifySignature_validatesCorrectlyAndRejectsBlankOrTampered() {
        String secret = "test-secret-key";
        String data = "vnp_Amount=1000000&vnp_TmnCode=OQXNQEMM&vnp_TxnRef=TXN_123";
        String validHash = VnPayUtils.hmacSHA512(secret, data);

        assertTrue(VnPayUtils.verifySignature(secret, data, validHash));
        assertTrue(VnPayUtils.verifySignature(secret, data, validHash.toUpperCase()));
        assertFalse(VnPayUtils.verifySignature(secret, data, null));
        assertFalse(VnPayUtils.verifySignature(secret, data, "   "));
        assertFalse(VnPayUtils.verifySignature(secret, data, validHash.substring(0, validHash.length() - 1) + "0"));
        assertFalse(VnPayUtils.verifySignature("wrong-secret", data, validHash));
    }

    @Test
    void buildQueryString_sortsKeysAndPreservesPlusForSpaces() {
        String query = VnPayUtils.buildQueryString(Map.of(
                "vnp_TxnRef", "TXN_1",
                "vnp_OrderInfo", "Thanh toan khoa hoc"));

        assertEquals("vnp_OrderInfo=Thanh+toan+khoa+hoc&vnp_TxnRef=TXN_1", query);
    }
}
