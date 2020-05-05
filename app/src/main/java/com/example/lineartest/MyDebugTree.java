package com.example.lineartest;

import android.util.Log;

import org.jetbrains.annotations.NotNull;

import timber.log.Timber;

public class MyDebugTree extends Timber.Tree {
    private static final int CALL_STACK_INDEX = 5;

    protected boolean isPraLogar(int priority) {
        if ( priority == Log.VERBOSE ) {
            return false;
        }
        return(true);
    }


    @Override
    protected void log(int priority, String tag, @NotNull String message, Throwable t) {
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        if (stackTrace.length <= CALL_STACK_INDEX) {
            throw new IllegalStateException(
                    "Synthetic stacktrace didn't have enough elements: are you using proguard?");
        }

        Integer linha = stackTrace[CALL_STACK_INDEX].getLineNumber();
        String metodo = stackTrace[CALL_STACK_INDEX].getMethodName();

        if ( isPraLogar(priority) ) {
            Log.println(priority, "AAA ${metodo}:($linha)", message);
        }

    }
}