package utils;

import org.junit.jupiter.api.Test;
import standalone.TurboHttpService;

import static org.junit.jupiter.api.Assertions.*;

class TurboHttpServiceTest {

    @Test
    void testValidHttpUrl() {
        TurboHttpService service = new TurboHttpService("http://example.com");
        assertEquals("example.com", service.host());
        assertEquals(80, service.port());
        assertFalse(service.secure());
    }

    @Test
    void testValidHttpsUrl() {
        TurboHttpService service = new TurboHttpService("https://example.com");
        assertEquals("example.com", service.host());
        assertEquals(443, service.port());
        assertTrue(service.secure());
    }

    @Test
    void testHttpUrlWithCustomPort() {
        TurboHttpService service = new TurboHttpService("http://example.com:8080");
        assertEquals("example.com", service.host());
        assertEquals(8080, service.port());
        assertFalse(service.secure());
    }

    @Test
    void testHttpsUrlWithCustomPort() {
        TurboHttpService service = new TurboHttpService("https://example.com:8443");
        assertEquals("example.com", service.host());
        assertEquals(8443, service.port());
        assertTrue(service.secure());
    }

    @Test
    void testUrlWithSubdomain() {
        TurboHttpService service = new TurboHttpService("https://sub.example.com");
        assertEquals("sub.example.com", service.host());
        assertEquals(443, service.port());
        assertTrue(service.secure());
    }

    @Test
    void testUrlWithIpAddress() {
        TurboHttpService service = new TurboHttpService("http://192.168.1.1");
        assertEquals("192.168.1.1", service.host());
        assertEquals(80, service.port());
        assertFalse(service.secure());
    }

    @Test
    void testUrlWithEmptyPort() {
        TurboHttpService service = new TurboHttpService("http://example.com:");
        assertEquals("example.com", service.host());
        assertEquals(80, service.port());
        assertFalse(service.secure());
    }

    @Test
    void testUrlWithPath() {
        TurboHttpService service = new TurboHttpService("https://example.com/path/to/resource");
        assertEquals("example.com", service.host());
        assertEquals(443, service.port());
        assertTrue(service.secure());
    }

    @Test
    void testInvalidUrlThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new TurboHttpService("invalid-url"));
    }

    @Test
    void testNullUrlThrowsException() {
        assertThrows(NullPointerException.class, () -> new TurboHttpService(null));
    }

    @Test
    void testUrlWithoutSchemeThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new TurboHttpService("example.com"));
    }
}
