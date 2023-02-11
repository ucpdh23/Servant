package es.xan.servantv3;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.*;
//import io.vertx.ext.web.Locale;

import java.util.*;

public class RoutingContextImpl /* implements RoutingContext */ {

    private Vertx mVertx;
    private Map<String,Object> mData;

    public RoutingContextImpl(Vertx vertx, Map<String,Object> data) {
        this.mVertx = vertx;
        this.mData = data;
    }
    //@Override
    public HttpServerRequest request() {
        return null;
    }

    //@Override
    public HttpServerResponse response() {
        return null;
    }

   // @Override
    public void next() {

    }

    //@Override
    public void fail(int i) {

    }

    //@Override
    public void fail(Throwable throwable) {

    }

    //@Override
    public RoutingContext put(String s, Object o) {
        return null;
    }

    //@Override
    public <T> T get(String s) {
        return null;
    }

    //@Override
    public <T> T remove(String s) {
        return null;
    }

    //@Override
    public Map<String, Object> data() {
        return this.mData;
    }

    //@Override
    public Vertx vertx() {
        return this.mVertx;
    }

    //@Override
    public String mountPoint() {
        return null;
    }

   // @Override
    public Route currentRoute() {
        return null;
    }

    //@Override
    public String normalisedPath() {
        return null;
    }
    /*

   // @Override
    public Cookie getCookie(String s) {
        return null;
    }

    //@Override
    public RoutingContext addCookie(Cookie cookie) {
        return null;
    }

    //@Override
    public Cookie removeCookie(String s) {
        return null;
    }

    //@Override
    public int cookieCount() {
        return 0;
    }

    //@Override
    public Set<Cookie> cookies() {
        return null;
    }
*/
    //@Override
    public String getBodyAsString() {
        return null;
    }

    //@Override
    public String getBodyAsString(String s) {
        return null;
    }

    //@Override
    public JsonObject getBodyAsJson() {
        return null;
    }

    //@Override
    public JsonArray getBodyAsJsonArray() {
        return null;
    }

    //@Override
    public Buffer getBody() {
        return null;
    }

   // @Override
    public Set<FileUpload> fileUploads() {
        return null;
    }

    //@Override
    public Session session() {
        return null;
    }

    //@Override
    public User user() {
        return null;
    }

    //@Override
    public Throwable failure() {
        return null;
    }

   // @Override
    public int statusCode() {
        return 0;
    }

  //  @Override
    public String getAcceptableContentType() {
        return null;
    }

   /* @Override
    public ParsedHeaderValues parsedHeaders() {
        return new ParsedHeaderValues() {
            @Override
            public List<MIMEHeader> accept() {
                return null;
            }

            @Override
            public List<ParsedHeaderValue> acceptCharset() {
                return null;
            }

            @Override
            public List<ParsedHeaderValue> acceptEncoding() {
                return null;
            }

            @Override
            public List<LanguageHeader> acceptLanguage() {
                return new ArrayList<>();
            }

            @Override
            public MIMEHeader contentType() {
                return null;
            }

            @Override
            public <T extends ParsedHeaderValue> T findBestUserAcceptedIn(List<T> list, Collection<T> collection) {
                return null;
            }
        };
    }

    @Override
    public int addHeadersEndHandler(Handler<Void> handler) {
        return 0;
    }

    @Override
    public boolean removeHeadersEndHandler(int i) {
        return false;
    }

    @Override
    public int addBodyEndHandler(Handler<Void> handler) {
        return 0;
    }

    @Override
    public boolean removeBodyEndHandler(int i) {
        return false;
    }

    @Override
    public boolean failed() {
        return false;
    }

    @Override
    public void setBody(Buffer buffer) {

    }

    @Override
    public void setSession(Session session) {

    }

    @Override
    public void setUser(User user) {

    }

    @Override
    public void clearUser() {

    }

    @Override
    public void setAcceptableContentType(String s) {

    }

    @Override
    public void reroute(HttpMethod httpMethod, String s) {

    }
/*
    @Override
    public List<Locale> acceptableLocales() {
        return null;
    }
*/
/*
    @Override
    public Map<String, String> pathParams() {
        return null;
    }

    @Override
    public String pathParam(String s) {
        return null;
    }*/
}
