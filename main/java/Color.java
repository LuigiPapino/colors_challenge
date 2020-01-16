
public class Color {
    final int index;
    final Type type;

    public Color(String index, String type) {
        this.index = Integer.valueOf(index);
        if (type.contentEquals("M")) {
            this.type = Type.M;
        } else if (type.contentEquals("G")) {
            this.type = Type.G;
        } else {
            throw new IllegalArgumentException(type + " is not a valid input");
        }

    }

    public Color(int index, Type type) {
        this.index = index;
        this.type = type;
    }

    public static Color buildGloss(int index) {
        return new Color(index, Type.G);
    }

    public static Color buildMatte(int index) {
        return new Color(index, Type.M);
    }

    public boolean hasIndex(int index) {
        return this.index == index;
    }

    public boolean isMatte() {
        return type == Type.M;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Color color = (Color) o;

        if (index != color.index) return false;
        return type == color.type;
    }

    @Override
    public String toString() {
        return "{" + index +
                type +
                '}';
    }


    @Override
    public int hashCode() {
        int result = index;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    public boolean isGloss() {
        return type == Type.G;
    }


    public enum Type {G, M}
}
