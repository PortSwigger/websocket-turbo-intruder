package standalone;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.core.Marker;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.ContentType;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.params.HttpParameter;
import burp.api.montoya.http.message.params.HttpParameterType;
import burp.api.montoya.http.message.params.ParsedHttpParameter;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.requests.HttpTransformation;

import java.util.List;
import java.util.regex.Pattern;

public class TurboHttpRequest implements HttpRequest {
    private String url;
    private String request;
    private TurboHttpService service;

    public TurboHttpRequest(String request, String url) {
        this.request = request;
        this.url = url;
        this.service = new TurboHttpService(url);
    }

    @Override
    public boolean isInScope() {
        return false;
    }

    @Override
    public HttpService httpService() {
        return service;
    }

    @Override
    public String url() {
        return this.url;
    }

    @Override
    public String method() {
        return "";
    }

    @Override
    public String path() {
        return "";
    }

    @Override
    public String query() {
        return "";
    }

    @Override
    public String pathWithoutQuery() {
        return "";
    }

    @Override
    public String fileExtension() {
        return "";
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
    public ContentType contentType() {
        return null;
    }

    @Override
    public List<ParsedHttpParameter> parameters() {
        return List.of();
    }

    @Override
    public List<ParsedHttpParameter> parameters(HttpParameterType httpParameterType) {
        return List.of();
    }

    @Override
    public boolean hasParameters() {
        return false;
    }

    @Override
    public boolean hasParameters(HttpParameterType httpParameterType) {
        return false;
    }

    @Override
    public ParsedHttpParameter parameter(String s, HttpParameterType httpParameterType) {
        return null;
    }

    @Override
    public String parameterValue(String s, HttpParameterType httpParameterType) {
        return "";
    }

    @Override
    public ParsedHttpParameter parameter(String s) {
        return null;
    }

    @Override
    public String parameterValue(String s) {
        return "";
    }

    @Override
    public boolean hasParameter(String s, HttpParameterType httpParameterType) {
        return false;
    }

    @Override
    public boolean hasParameter(HttpParameter httpParameter) {
        return false;
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
    public HttpRequest copyToTempFile() {
        return null;
    }

    @Override
    public HttpRequest withService(HttpService httpService) {
        return null;
    }

    @Override
    public HttpRequest withPath(String s) {
        return null;
    }

    @Override
    public HttpRequest withMethod(String s) {
        return null;
    }

    @Override
    public HttpRequest withHeader(HttpHeader httpHeader) {
        return null;
    }

    @Override
    public HttpRequest withHeader(String s, String s1) {
        return null;
    }

    @Override
    public HttpRequest withParameter(HttpParameter httpParameter) {
        return null;
    }

    @Override
    public HttpRequest withAddedParameters(List<? extends HttpParameter> list) {
        return null;
    }

    @Override
    public HttpRequest withAddedParameters(HttpParameter... httpParameters) {
        return null;
    }

    @Override
    public HttpRequest withRemovedParameters(List<? extends HttpParameter> list) {
        return null;
    }

    @Override
    public HttpRequest withRemovedParameters(HttpParameter... httpParameters) {
        return null;
    }

    @Override
    public HttpRequest withUpdatedParameters(List<? extends HttpParameter> list) {
        return null;
    }

    @Override
    public HttpRequest withUpdatedParameters(HttpParameter... httpParameters) {
        return null;
    }

    @Override
    public HttpRequest withTransformationApplied(HttpTransformation httpTransformation) {
        return null;
    }

    @Override
    public HttpRequest withBody(String s) {
        return null;
    }

    @Override
    public HttpRequest withBody(ByteArray byteArray) {
        return null;
    }

    @Override
    public HttpRequest withAddedHeader(String s, String s1) {
        return null;
    }

    @Override
    public HttpRequest withAddedHeader(HttpHeader httpHeader) {
        return null;
    }

    @Override
    public HttpRequest withAddedHeaders(List<? extends HttpHeader> list) {
        return null;
    }

    @Override
    public HttpRequest withAddedHeaders(HttpHeader... httpHeaders) {
        return null;
    }

    @Override
    public HttpRequest withUpdatedHeader(String s, String s1) {
        return null;
    }

    @Override
    public HttpRequest withUpdatedHeader(HttpHeader httpHeader) {
        return null;
    }

    @Override
    public HttpRequest withUpdatedHeaders(List<? extends HttpHeader> list) {
        return null;
    }

    @Override
    public HttpRequest withUpdatedHeaders(HttpHeader... httpHeaders) {
        return null;
    }

    @Override
    public HttpRequest withRemovedHeader(String s) {
        return null;
    }

    @Override
    public HttpRequest withRemovedHeader(HttpHeader httpHeader) {
        return null;
    }

    @Override
    public HttpRequest withRemovedHeaders(List<? extends HttpHeader> list) {
        return null;
    }

    @Override
    public HttpRequest withRemovedHeaders(HttpHeader... httpHeaders) {
        return null;
    }

    @Override
    public HttpRequest withMarkers(List<Marker> list) {
        return null;
    }

    @Override
    public HttpRequest withMarkers(Marker... markers) {
        return null;
    }

    @Override
    public HttpRequest withDefaultHeaders() {
        return null;
    }

    @Override
    public String toString() {
        return this.request;
    }
}
