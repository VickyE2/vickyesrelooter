package io.github.vickye2.vickyesrelooter.manager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TableIO {
    public static final ExecutorService TABLE_EXECUTOR =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "Table-Creation-Worker");
                t.setDaemon(true);
                return t;
            });
}