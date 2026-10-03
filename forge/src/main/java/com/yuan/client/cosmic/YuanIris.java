package com.yuan.client.cosmic;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class YuanIris {
    private static final Logger LOGGER = LogManager.getLogger("YuanIris");
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private static MethodHandle getInstance;
    private static MethodHandle isShaderPackInUse;
    private static boolean apiAbsent;
    private static boolean lookupBroken;

    private YuanIris() {
    }

    /**
     * The shader pack can be toggled from the video settings at any time, so the state itself is
     * queried on every call; only the reflection lookup is cached. A missing Iris means no shader
     * pack, while a broken lookup keeps failing closed (assume a pack is active) because the styles
     * that fall back to vanilla are the cheaper mistake.
     */
    public static boolean isShaderPackActive() {
        if (lookupBroken) {
            return true;
        }
        if (apiAbsent) {
            return false;
        }
        try {
            ensureApi();
            return (boolean) isShaderPackInUse.invoke(getInstance.invoke());
        } catch (ClassNotFoundException absent) {
            apiAbsent = true;
            return false;
        } catch (Throwable broken) {
            lookupBroken = true;
            LOGGER.warn("Iris API query failed, assuming a shader pack is active", broken);
            return true;
        }
    }

    private static void ensureApi() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException {
        if (getInstance != null && isShaderPackInUse != null) {
            return;
        }
        Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
        getInstance = LOOKUP.findStatic(api, "getInstance", MethodType.methodType(api));
        isShaderPackInUse = LOOKUP.findVirtual(api, "isShaderPackInUse", MethodType.methodType(boolean.class));
    }
}
