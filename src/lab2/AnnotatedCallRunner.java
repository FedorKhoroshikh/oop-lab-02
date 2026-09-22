package lab2;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * The "other class" required by the task: it takes an arbitrary object,
 * finds its protected and private methods annotated with {@link Repeat} and
 * calls each of them as many times as the annotation says.
 * <p>
 * Nothing here knows the signatures of those methods — parameters are built
 * by {@link ArgumentFactory} from the reflected types, so adding a method
 * with a brand new set of parameters to {@link TextTools} changes nothing
 * in this class.
 */
public final class AnnotatedCallRunner {

    private final ArgumentFactory arguments = new ArgumentFactory();
    private final Consumer<String> out;

    public AnnotatedCallRunner(Consumer<String> out) {
        this.out = out;
    }

    /** Runs every annotated protected or private method of the target. */
    public void run(Object target) {
        List<Method> methods = selectMethods(target.getClass());
        out.accept("Target: " + target.getClass().getSimpleName()
                + ", methods to call: " + methods.size());

        for (Method method : methods) {
            int times = method.getAnnotation(Repeat.class).value();
            out.accept("%n%s %s(%s) — @Repeat(%d)".formatted(
                    accessOf(method), method.getName(), parameterNames(method), times));
            invokeRepeatedly(target, method, times);
        }
    }

    /**
     * Declared methods that are annotated AND are protected or private.
     * Public ones are filtered out even when annotated — the task asks for
     * the two restricted access levels only.
     */
    private List<Method> selectMethods(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Repeat.class))
                .filter(m -> {
                    int modifiers = m.getModifiers();
                    return Modifier.isProtected(modifiers) || Modifier.isPrivate(modifiers);
                })
                .sorted(Comparator.comparing(Method::getName))   // stable output
                .toList();
    }

    private void invokeRepeatedly(Object target, Method method, int times) {
        method.setAccessible(true);            // private methods stay private for javac
        for (int call = 1; call <= times; call++) {
            Object[] args = buildArguments(method);
            try {
                Object result = method.invoke(target, args);
                out.accept("  call %d: %s -> %s".formatted(
                        call, Arrays.deepToString(args), format(result)));
            } catch (ReflectiveOperationException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                out.accept("  call %d failed: %s".formatted(call, cause));
            }
        }
    }

    /** Arguments come from the reflected types, never from a hard-coded list. */
    private Object[] buildArguments(Method method) {
        return Arrays.stream(method.getGenericParameterTypes())
                .map(arguments::valueFor)      // never null, as the task demands
                .toArray();
    }

    private String parameterNames(Method method) {
        return Arrays.stream(method.getParameterTypes())
                .map(Class::getSimpleName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    private String accessOf(Method method) {
        int modifiers = method.getModifiers();
        if (Modifier.isPrivate(modifiers)) {
            return "private";
        }
        if (Modifier.isProtected(modifiers)) {
            return "protected";
        }
        return Modifier.isPublic(modifiers) ? "public" : "package-private";
    }

    private String format(Object result) {
        if (result == null) {
            return "void";
        }
        if (result.getClass().isArray()) {
            return Arrays.deepToString(new Object[]{result});
        }
        return String.format(Locale.ROOT, "%s", result);
    }
}
