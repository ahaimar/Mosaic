package com.packs.mosaic.support;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Minimal stand-in for a running libGDX application, so tests can exercise
 * the world/input classes without a window or GL context.
 *
 * Only two globals actually matter for the code under test:
 *   Gdx.gl       — Viewport.apply() forwards to HdpiUtils.glViewport(...)
 *   Gdx.graphics — Camera.unproject() reads getHeight() to flip the Y axis
 *
 * Both are dynamic proxies returning zero/false/null, with a handful of
 * meaningful overrides, rather than hand-written stubs of ~300 methods.
 */
public final class HeadlessGdx {

    public static final int SCREEN_WIDTH = 1280;
    public static final int SCREEN_HEIGHT = 720;

    private HeadlessGdx() {
    }

    /** Installs the stubs at the default 1280x720 screen size. Safe to call repeatedly. */
    public static void install() {
        install(SCREEN_WIDTH, SCREEN_HEIGHT);
    }

    public static void install(final int width, final int height) {
        Gdx.gl20 = proxy(GL20.class, (method, args) -> null);
        Gdx.gl = Gdx.gl20;
        Gdx.graphics = proxy(Graphics.class, (method, args) -> {
            switch (method.getName()) {
                case "getWidth":
                case "getBackBufferWidth":
                    return width;
                case "getHeight":
                case "getBackBufferHeight":
                    return height;
                case "getDeltaTime":
                case "getRawDeltaTime":
                    return 1f / 60f;
                case "getBackBufferScale":
                    return 1f;
                case "isGL30Available":
                case "isGL31Available":
                case "isGL32Available":
                    return false;
                default:
                    return null;
            }
        });
        Gdx.app = proxy(Application.class, (method, args) -> null);
        Gdx.input = proxy(Input.class, (method, args) -> null);
    }

    /** Y axis differs between the two directions, so this is not a plain identity round-trip. */
    public static int toInputY(float projectedY) {
        return Math.round(Gdx.graphics.getHeight() - projectedY);
    }

    private interface Handler {
        /** Return null to fall back to the method's zero value. */
        Object invoke(Method method, Object[] args) throws Throwable;
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(final Class<T> type, final Handler handler) {
        InvocationHandler invocationHandler = new InvocationHandler() {
            @Override
            public Object invoke(Object self, Method method, Object[] args) throws Throwable {
                switch (method.getName()) {
                    case "hashCode":
                        return System.identityHashCode(self);
                    case "equals":
                        return self == args[0];
                    case "toString":
                        return "HeadlessGdx<" + type.getSimpleName() + ">";
                    default:
                        break;
                }
                Object result = handler.invoke(method, args);
                return result != null ? result : zeroValue(method.getReturnType());
            }
        };
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, invocationHandler);
    }

    private static Object zeroValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return Boolean.FALSE;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == char.class) return (char) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0f;
        if (type == double.class) return 0d;
        return null; // void
    }
}
