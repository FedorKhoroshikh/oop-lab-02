package lab2;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Comparator;

/**
 * Builds an argument for any parameter type, so that the invoker does not
 * have to know a single method signature in advance.
 * <p>
 * The factory never returns {@code null}: primitives get sample values,
 * known types get literals, collections are filled with elements of their
 * real generic type, records and ordinary classes are constructed through
 * reflection, and an arbitrary interface gets a dynamic {@link Proxy}.
 */
public final class ArgumentFactory {

    /** Guards against types that reference themselves through constructors. */
    private static final int MAX_DEPTH = 4;

    /**
     * Counter behind the sample values. Every requested argument gets its own
     * number, so repeated calls of the same method receive different data and
     * the effect of the method is actually visible in the log.
     */
    private int seq;

    /** Entry point: a ready-to-use, non-null value for the given type. */
    public Object valueFor(Type type) {
        return build(type, 0);
    }

    /** Next number in the sequence; every produced value takes its own. */
    private int next() {
        return ++seq;
    }

    /** Distinct sample string for every requested value. */
    private String sampleText() {
        return "text-" + next();
    }

    private Object build(Type type, int depth) {
        if (depth > MAX_DEPTH) {
            throw new IllegalStateException("type nesting is too deep: " + type);
        }
        return switch (type) {
            case Class<?> raw -> fromClass(raw, depth);
            case ParameterizedType parameterized -> fromParameterized(parameterized, depth);
            case GenericArrayType generic -> array(generic.getGenericComponentType(), depth);
            case WildcardType wildcard -> build(wildcard.getUpperBounds()[0], depth);
            case TypeVariable<?> variable -> build(variable.getBounds()[0], depth);
            default -> throw new IllegalArgumentException("unsupported type: " + type);
        };
    }

    private Object fromClass(Class<?> type, int depth) {
        Object primitive = primitiveOrWrapper(type);
        if (primitive != null) {
            return primitive;
        }
        if (type == String.class || type == CharSequence.class || type == Object.class) {
            return sampleText();
        }
        if (type == StringBuilder.class) {
            return new StringBuilder(sampleText());
        }
        if (type.isArray()) {
            return array(type.getComponentType(), depth);
        }
        if (type.isEnum()) {
            Object[] constants = type.getEnumConstants();
            if (constants.length == 0) {
                throw new IllegalArgumentException("enum without constants: " + type.getName());
            }
            return constants[0];
        }
        // raw collections: the element type is unknown, so a string will do
        if (type == List.class || type == Collection.class || type == Iterable.class) {
            return new ArrayList<>(List.of(sampleText(), sampleText()));
        }
        if (type == Set.class) {
            return new LinkedHashSet<>(List.of(sampleText()));
        }
        if (type == Map.class) {
            return new LinkedHashMap<>(Map.of(sampleText(), next()));
        }
        if (type == Optional.class) {
            return Optional.of(sampleText());
        }
        if (type.isInterface()) {
            return proxyFor(type);
        }
        return construct(type, depth);
    }

    private Object fromParameterized(ParameterizedType type, int depth) {
        Class<?> raw = (Class<?>) type.getRawType();
        Type[] args = type.getActualTypeArguments();

        if (Map.class.isAssignableFrom(raw)) {
            Map<Object, Object> map = new LinkedHashMap<>();
            map.put(build(args[0], depth + 1), build(args[1], depth + 1));
            return map;
        }
        if (Set.class.isAssignableFrom(raw)) {
            Set<Object> set = new LinkedHashSet<>();
            set.add(build(args[0], depth + 1));
            return set;
        }
        if (Collection.class.isAssignableFrom(raw) || raw == Iterable.class) {
            List<Object> list = new ArrayList<>();
            list.add(build(args[0], depth + 1));
            list.add(build(args[0], depth + 1));
            return list;
        }
        if (raw == Optional.class) {
            return Optional.of(build(args[0], depth + 1));
        }
        return fromClass(raw, depth);
    }

    private Object array(Type componentType, int depth) {
        Class<?> component = componentType instanceof Class<?> c
                ? c
                : (Class<?>) ((ParameterizedType) componentType).getRawType();
        Object array = Array.newInstance(component, 2);
        for (int i = 0; i < 2; i++) {
            Array.set(array, i, build(componentType, depth + 1));
        }
        return array;
    }

    /** Records and ordinary classes: take the shortest constructor and fill it. */
    private Object construct(Class<?> type, int depth) {
        Constructor<?>[] constructors = type.getDeclaredConstructors();
        if (constructors.length == 0) {
            throw new IllegalArgumentException("no constructor in " + type.getName());
        }
        Constructor<?> shortest = List.of(constructors).stream()
                .min(Comparator.comparingInt(Constructor::getParameterCount))
                .orElseThrow();

        Type[] parameters = shortest.getGenericParameterTypes();
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            args[i] = build(parameters[i], depth + 1);
        }
        try {
            shortest.setAccessible(true);
            return shortest.newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("cannot instantiate " + type.getName(), e);
        }
    }

    /**
     * An arbitrary interface still has to become a real object, so a dynamic
     * proxy is generated on the spot instead of passing null.
     */
    private Object proxyFor(Class<?> type) {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "toString" -> "proxy of " + type.getSimpleName();
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == (args == null ? null : args[0]);
            default -> defaultReturnValue(method);
        };
        return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private Object defaultReturnValue(Method method) {
        Class<?> returnType = method.getReturnType();
        if (returnType == void.class) {
            return null;
        }
        Object primitive = primitiveOrWrapper(returnType);
        return primitive != null ? primitive : (returnType == String.class ? sampleText() : null);
    }

    /** Sample values for primitives and their wrappers, {@code null} otherwise. */
    private Object primitiveOrWrapper(Class<?> type) {
        if (type == int.class || type == Integer.class) {
            return 1 + next() % 5;
        }
        if (type == long.class || type == Long.class) {
            return 40L + next();
        }
        if (type == double.class || type == Double.class) {
            return 0.5 * next() + 1;
        }
        if (type == float.class || type == Float.class) {
            return 0.5f * next() + 1;
        }
        if (type == short.class || type == Short.class) {
            return (short) (1 + next() % 7);
        }
        if (type == byte.class || type == Byte.class) {
            return (byte) (1 + next() % 3);
        }
        if (type == char.class || type == Character.class) {
            return (char) ('a' + next() % 26);
        }
        if (type == boolean.class || type == Boolean.class) {
            return next() % 2 == 0;
        }
        return null;
    }
}
