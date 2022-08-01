package es.xan.servantv3.brain;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class UserContext {

    private final String mUser;

    public UserContext(String user) {
        this.mUser = user;
    }

    public String getUser() {
        return this.mUser;
    }

    protected Cache<String, String> memory = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    private static final String ATTENTION = "ATTENTION";


    public String getAttention() {
        try {
            return this.memory.get(ATTENTION, () -> "");
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public void setAttention(String attention) {
        this.memory.put(ATTENTION, attention);
    }
}
