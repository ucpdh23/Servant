package es.xan.servantv3;

public interface ThrowingSupplier<T, E extends  Exception> {
    T get() throws E;
}
