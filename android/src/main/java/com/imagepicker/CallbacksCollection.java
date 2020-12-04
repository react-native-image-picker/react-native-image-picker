package com.imagepicker;


import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import com.facebook.react.bridge.Callback;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class CallbacksCollection {
    final static  Map<Integer, Pair<Callback, Integer>> callbackMap = new HashMap<>();
    final static Random random = new Random();

    public static Integer set(@NonNull final Callback cb) {
        int id = random.nextInt(65535);
        callbackMap.put(id, Pair.create(cb, 0));
        return id;
    }

    public static void invoke(final Integer cbId, Object... args) {
        if (!callbackMap.containsKey(cbId)) {
            return;
        }
        Pair<Callback, Integer> result = callbackMap.get(cbId);
        callbackMap.remove(cbId);
        result.first.invoke(args);
    }

    public static Pair<Callback, Integer> pop(final Integer cbId) {
        if (!callbackMap.containsKey(cbId)) {
            return null;
        }
        Pair result = callbackMap.get(cbId);
        callbackMap.remove(cbId);
        return result;
    }

    public static void setCode(final Integer cbId, final Integer code) {
        if (!callbackMap.containsKey(cbId)) {
            return;
        }
        Pair<Callback, Integer> result = callbackMap.get(cbId);
        callbackMap.remove(cbId);
        callbackMap.put(cbId, Pair.create(result.first, code));
    }
}
