package lab2;

/**
 * A small value object used as a method parameter. It exists to prove that
 * the invoker can build a custom type as well: the argument factory has to
 * pick a constructor and fill it, instead of passing null.
 */
public record Tag(String name, int level) {

    @Override
    public String toString() {
        return "<" + name + " level=" + level + ">";
    }
}
