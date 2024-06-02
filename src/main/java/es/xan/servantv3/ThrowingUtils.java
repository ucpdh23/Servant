package es.xan.servantv3;

import java.util.function.Predicate;
import java.util.function.Supplier;

public class ThrowingUtils {
    public static <T> T retry3times(Supplier<T> function, Predicate<T> verifier) {
        for (int i =0; i < 3; i++) {
            try {
                T value = function.get();
                if (verifier.test(value)) {
                    return value;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public static <T> T throwingSuplierWrapper(ThrowingSupplier<T, Exception> throwingConsumer) {
        try {
            return throwingConsumer.get();
        } catch (Exception ex) {
            throw new ServantException(ex);
        }
    }
}
