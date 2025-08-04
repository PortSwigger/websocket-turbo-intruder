package standalone;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.core.Marker;
import burp.api.montoya.http.message.Cookie;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.MimeType;
import burp.api.montoya.http.message.StatusCodeClass;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.http.message.responses.analysis.Attribute;
import burp.api.montoya.http.message.responses.analysis.AttributeType;
import burp.api.montoya.http.message.responses.analysis.KeywordCount;

import java.util.List;
import java.util.regex.Pattern;

public class TurboHttpResponse implements burp.api.montoya.http.message.responses.HttpResponse {
    private final String response;

    public TurboHttpResponse(String response) {
        this.response = response;
    }

    @Override
    public short statusCode() {
        return 0;
    }

    @Override
    public String reasonPhrase() {
        return "";
    }

    @Override
    public boolean isStatusCodeClass(StatusCodeClass statusCodeClass) {
        return false;
    }

    @Override
    public String httpVersion() {
        return "";
    }

    @Override
    public List<HttpHeader> headers() {
        return List.of();
    }

    @Override
    public boolean hasHeader(HttpHeader httpHeader) {
        return false;
    }

    @Override
    public boolean hasHeader(String s) {
        return false;
    }

    @Override
    public boolean hasHeader(String s, String s1) {
        return false;
    }

    @Override
    public HttpHeader header(String s) {
        return null;
    }

    @Override
    public String headerValue(String s) {
        return "";
    }

    @Override
    public ByteArray body() {
        return null;
    }

    @Override
    public String bodyToString() {
        return "";
    }

    @Override
    public int bodyOffset() {
        return 0;
    }

    @Override
    public List<Marker> markers() {
        return List.of();
    }

    @Override
    public List<Cookie> cookies() {
        return List.of();
    }

    @Override
    public Cookie cookie(String s) {
        return null;
    }

    @Override
    public String cookieValue(String s) {
        return "";
    }

    @Override
    public boolean hasCookie(String s) {
        return false;
    }

    @Override
    public boolean hasCookie(Cookie cookie) {
        return false;
    }

    @Override
    public MimeType mimeType() {
        return null;
    }

    @Override
    public MimeType statedMimeType() {
        return null;
    }

    @Override
    public MimeType inferredMimeType() {
        return null;
    }

    @Override
    public List<KeywordCount> keywordCounts(String... strings) {
        return List.of();
    }

    @Override
    public List<Attribute> attributes(AttributeType... attributeTypes) {
        return List.of();
    }

    @Override
    public boolean contains(String s, boolean b) {
        return false;
    }

    @Override
    public boolean contains(Pattern pattern) {
        return false;
    }

    @Override
    public ByteArray toByteArray() {
        return null;
    }

    @Override
    public burp.api.montoya.http.message.responses.HttpResponse copyToTempFile() {
        return null;
    }

    @Override
    public burp.api.montoya.http.message.responses.HttpResponse withStatusCode(short i) {
        return null;
    }

    @Override
    public burp.api.montoya.http.message.responses.HttpResponse withReasonPhrase(String s) {
        return null;
    }

    @Override
    public burp.api.montoya.http.message.responses.HttpResponse withHttpVersion(String s) {
        return null;
    }

    @Override
    public burp.api.montoya.http.message.responses.HttpResponse withBody(String s) {
        return null;
    }

    @Override
    public burp.api.montoya.http.message.responses.HttpResponse withBody(ByteArray byteArray) {
        return null;
    }

    @Override
    public burp.api.montoya.http.message.responses.HttpResponse withAddedHeader(HttpHeader httpHeader) {
        return null;
    }

    @Override
    public burp.api.montoya.http.message.responses.HttpResponse withAddedHeader(String s, String s1) {
        return null;
    }

    @Override
    public HttpResponse withAddedHeaders(List<? extends HttpHeader> list) {
        return null;
    }

    @Override
    public HttpResponse withAddedHeaders(HttpHeader... httpHeaders) {
        return null;
    }

    @Override
    public burp.api.montoya.http.message.responses.HttpResponse withUpdatedHeader(HttpHeader httpHeader) {
        return null;
    }

    @Override
    public burp.api.montoya.http.message.responses.HttpResponse withUpdatedHeader(String s, String s1) {
        return null;
    }

    @Override
    public HttpResponse withUpdatedHeaders(List<? extends HttpHeader> list) {
        return null;
    }

    @Override
    public HttpResponse withUpdatedHeaders(HttpHeader... httpHeaders) {
        return null;
    }

    @Override
    public burp.api.montoya.http.message.responses.HttpResponse withRemovedHeader(HttpHeader httpHeader) {
        return null;
    }

    @Override
    public burp.api.montoya.http.message.responses.HttpResponse withRemovedHeader(String s) {
        return null;
    }

    @Override
    public HttpResponse withRemovedHeaders(List<? extends HttpHeader> list) {
        return null;
    }

    @Override
    public HttpResponse withRemovedHeaders(HttpHeader... httpHeaders) {
        return null;
    }

    @Override
    public burp.api.montoya.http.message.responses.HttpResponse withMarkers(List<Marker> list) {
        return null;
    }

    @Override
    public burp.api.montoya.http.message.responses.HttpResponse withMarkers(Marker... markers) {
        return null;
    }

    @Override
    public String toString() {
        return response;
    }
}
