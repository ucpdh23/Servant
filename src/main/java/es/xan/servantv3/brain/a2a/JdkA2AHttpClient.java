package es.xan.servantv3.brain.a2a;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.function.Consumer;

import io.a2a.client.http.A2AHttpClient;
import io.a2a.client.http.A2AHttpResponse;
import org.jspecify.annotations.Nullable;

public class JdkA2AHttpClient implements A2AHttpClient {
    private final HttpClient httpClient;

    public JdkA2AHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public A2AHttpClient.GetBuilder createGet() {
        return new JdkA2AHttpClient.JdkGetBuilder();
    }

    public A2AHttpClient.PostBuilder createPost() {
        return new JdkA2AHttpClient.JdkPostBuilder();
    }

    public A2AHttpClient.DeleteBuilder createDelete() {
        return new JdkA2AHttpClient.JdkDeleteBuilder();
    }

    private static boolean isSuccessStatus(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    private class JdkGetBuilder extends JdkBuilder<GetBuilder> implements A2AHttpClient.GetBuilder {
        private JdkGetBuilder() {
            super();
        }

        private HttpRequest.Builder createRequestBuilder(boolean SSE) throws IOException {
            HttpRequest.Builder builder = super.createRequestBuilder().GET();
            if (SSE) {
                builder.header("Accept", "text/event-stream");
            }

            return builder;
        }

        public A2AHttpResponse get() throws IOException, InterruptedException {
            HttpRequest request = this.createRequestBuilder(false).build();
            HttpResponse<String> response = JdkA2AHttpClient.this.httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new JdkA2AHttpClient.JdkHttpResponse(response);
        }

        public CompletableFuture<Void> getAsyncSSE(Consumer<String> messageConsumer, Consumer<Throwable> errorConsumer, Runnable completeRunnable) throws IOException, InterruptedException {
            HttpRequest request = this.createRequestBuilder(true).build();
            return super.asyncRequest(request, messageConsumer, errorConsumer, completeRunnable);
        }
    }

    private class JdkPostBuilder extends JdkA2AHttpClient.JdkBuilder<PostBuilder> implements A2AHttpClient.PostBuilder {
        String body = "";

        private JdkPostBuilder() {
            super();
        }

        public A2AHttpClient.PostBuilder body(String body) {
            this.body = body;
            return (A2AHttpClient.PostBuilder)this.self();
        }

        private HttpRequest.Builder createRequestBuilder(boolean SSE) throws IOException {
            HttpRequest.Builder builder = super.createRequestBuilder().POST(BodyPublishers.ofString(this.body, StandardCharsets.UTF_8));
            if (SSE) {
                builder.header("Accept", "text/event-stream");
            }

            return builder;
        }

        public A2AHttpResponse post() throws IOException, InterruptedException {
            HttpRequest request = this.createRequestBuilder(false).POST(BodyPublishers.ofString(this.body, StandardCharsets.UTF_8)).build();
            HttpResponse<String> response = JdkA2AHttpClient.this.httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 401) {
                throw new IOException("Authentication failed: Client credentials are missing or invalid");
            } else if (response.statusCode() == 403) {
                throw new IOException("Authorization failed: Client does not have permission for the operation");
            } else {
                return new JdkHttpResponse(response);
            }
        }

        public CompletableFuture<Void> postAsyncSSE(Consumer<String> messageConsumer, Consumer<Throwable> errorConsumer, Runnable completeRunnable) throws IOException, InterruptedException {
            HttpRequest request = this.createRequestBuilder(true).build();
            return super.asyncRequest(request, messageConsumer, errorConsumer, completeRunnable);
        }
    }

    private class JdkDeleteBuilder extends JdkA2AHttpClient.JdkBuilder<DeleteBuilder> implements A2AHttpClient.DeleteBuilder {
        private JdkDeleteBuilder() {
            super();
        }

        public A2AHttpResponse delete() throws IOException, InterruptedException {
            HttpRequest request = super.createRequestBuilder().DELETE().build();
            HttpResponse<String> response = JdkA2AHttpClient.this.httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new JdkHttpResponse(response);
        }
    }

    private static record JdkHttpResponse(HttpResponse<String> response) implements A2AHttpResponse {
        private JdkHttpResponse(HttpResponse<String> response) {
            this.response = response;
        }

        public int status() {
            return this.response.statusCode();
        }

        public boolean success() {
            return success(this.response);
        }

        static boolean success(HttpResponse<?> response) {
            return response.statusCode() >= 200 && response.statusCode() < 300;
        }

        public String body() {
            return (String)this.response.body();
        }

        public HttpResponse<String> response() {
            return this.response;
        }
    }

    private abstract class JdkBuilder<T extends A2AHttpClient.Builder<T>> implements A2AHttpClient.Builder<T> {
        private String url = "";
        private Map<String, String> headers = new HashMap();

        private JdkBuilder() {
        }

        public T url(String url) {
            this.url = url;
            return this.self();
        }

        public T addHeader(String name, String value) {
            this.headers.put(name, value);
            return this.self();
        }

        public T addHeaders(Map<String, String> headers) {
            if (headers != null && !headers.isEmpty()) {
                Iterator var2 = headers.entrySet().iterator();

                while(var2.hasNext()) {
                    Map.Entry<String, String> entry = (Map.Entry)var2.next();
                    this.addHeader((String)entry.getKey(), (String)entry.getValue());
                }
            }

            return this.self();
        }

        T self() {
            return (T) this;
        }

        protected HttpRequest.Builder createRequestBuilder() throws IOException {
            HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(this.url));
            Iterator var2 = this.headers.entrySet().iterator();

            while(var2.hasNext()) {
                Map.Entry<String, String> headerEntry = (Map.Entry)var2.next();
                builder.header((String)headerEntry.getKey(), (String)headerEntry.getValue());
            }

            return builder;
        }

        protected CompletableFuture<Void> asyncRequest(HttpRequest request, final Consumer<String> messageConsumer, final Consumer<Throwable> errorConsumer, final Runnable completeRunnable) {
            Flow.Subscriber<String> subscriber = new Flow.Subscriber<String>() {
                private Flow.@Nullable Subscription subscription;
                private volatile boolean errorRaised = false;

                public void onSubscribe(Flow.Subscription subscription) {
                    this.subscription = subscription;
                    this.subscription.request(1L);
                }

                public void onNext(String item) {
                    if (item != null && item.startsWith("data:")) {
                        item = item.substring(5).trim();
                        if (!item.isEmpty()) {
                            messageConsumer.accept(item);
                        }
                    }

                    if (this.subscription != null) {
                        this.subscription.request(1L);
                    }

                }

                public void onError(Throwable throwable) {
                    if (!this.errorRaised) {
                        this.errorRaised = true;
                        errorConsumer.accept(throwable);
                    }

                    if (this.subscription != null) {
                        this.subscription.cancel();
                    }

                }

                public void onComplete() {
                    if (!this.errorRaised) {
                        completeRunnable.run();
                    }

                    if (this.subscription != null) {
                        this.subscription.cancel();
                    }

                }
            };
            HttpResponse.BodyHandler<Void> bodyHandler = (responseInfo) -> {
                if (responseInfo.statusCode() != 401 && responseInfo.statusCode() != 403) {
                    return BodyHandlers.fromLineSubscriber(subscriber).apply(responseInfo);
                } else {
                    final String errorMessage;
                    if (responseInfo.statusCode() == 401) {
                        errorMessage = "Authentication failed: Client credentials are missing or invalid";
                    } else {
                        errorMessage = "Authorization failed: Client does not have permission for the operation";
                    }

                    return BodySubscribers.fromSubscriber(new Flow.Subscriber<List<ByteBuffer>>() {
                        public void onSubscribe(Flow.Subscription subscription) {
                            subscriber.onError(new IOException(errorMessage));
                        }

                        public void onNext(List<ByteBuffer> item) {
                        }

                        public void onError(Throwable throwable) {
                        }

                        public void onComplete() {
                        }
                    });
                }
            };
            return JdkA2AHttpClient.this.httpClient.sendAsync(request, bodyHandler).thenAccept((response) -> {
                if (!JdkA2AHttpClient.isSuccessStatus(response.statusCode()) && response.statusCode() != 401 && response.statusCode() != 403) {
                    int var10003 = response.statusCode();
                    subscriber.onError(new IOException("Request failed with status " + var10003 + ":" + String.valueOf(response.body())));
                }

            });
        }
    }
}
