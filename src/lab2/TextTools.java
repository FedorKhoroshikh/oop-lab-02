package lab2;

import java.util.List;
import java.util.Map;

import static java.lang.Math.clamp;

/**
 * The class under inspection: two or three methods of every access level,
 * all of them taking parameters, some of them annotated with {@link Repeat}.
 * <p>
 * Note the deliberate mix: {@code length} is annotated but public, so the
 * invoker must skip it; {@code firstChar} and {@code checksum} are of the
 * right access level but carry no annotation, so they must be skipped too.
 */
public class TextTools {

    // ---------- public ----------

    public String join(String left, String right) {
        return left + " " + right;
    }

    /** Annotated on purpose, yet public — the invoker has to ignore it. */
    @Repeat(5)
    public int length(CharSequence text) {
        return text.length();
    }

    public String shout(String text, int exclamations) {
        return text.toUpperCase() + "!".repeat(Math.max(0, exclamations));
    }

    // ---------- protected ----------

    @Repeat(2)
    protected String normalize(String text) {
        return text.strip().toLowerCase();
    }

    @Repeat(3)
    protected String merge(List<String> parts, String separator) {
        return String.join(separator, parts);
    }

    /** Right access level, but no annotation — must be skipped. */
    protected char firstChar(String text) {
        return text.isEmpty() ? '?' : text.charAt(0);
    }

    // ---------- private ----------

    @Repeat(2)
    private String mask(String text, int visible) {
        int keep = clamp(visible, 0, text.length());
        return text.substring(0, keep) + "*".repeat(text.length() - keep);
    }

    /** Right access level, but no annotation — must be skipped. */
    private long checksum(byte[] data, long salt) {
        long sum = salt;
        for (byte b : data) {
            sum = sum * 31 + b;
        }
        return sum;
    }

    @Repeat(1)
    private String render(Tag tag, Map<String, Integer> counters) {
        return tag + " counters=" + counters;
    }
}
