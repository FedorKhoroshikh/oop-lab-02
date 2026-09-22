package lab2;

import java.util.function.Consumer;

/**
 * Demonstration: the runner is given a TextTools instance and calls every
 * annotated protected and private method the required number of times.
 */
public final class Main {

    /** Where the log goes. The coursework will plug a text area in here. */
    private static final Consumer<String> OUT = System.out::println;

    public static void main(String[] args) {
        OUT.accept("=== Lab 2: a custom annotation and reflection ===");

        new AnnotatedCallRunner(OUT).run(new TextTools());

        OUT.accept("""

                Skipped on purpose:
                  length(CharSequence)  - annotated, but public
                  firstChar(String)     - protected, but not annotated
                  checksum(byte[], long) - private, but not annotated""");
    }
}
