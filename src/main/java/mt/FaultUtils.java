package mt;

import java.util.function.Function;

public final class FaultUtils {

    private FaultUtils() {
        throw new UnsupportedOperationException("Utility class should not be instantiated");
    }

    public static FaultCommand splitCommand(String command) {
        var split = command.split(",");
        return new FaultCommand(split);
    }

    public static int binarySearch(int n, Function<Integer, Boolean> predicate) {
        var i = 0;
        var j = n;
        while (i < j) {
            var h = (i + j) >> 1;
            if (!predicate.apply(h)) {
                i = h + 1;
            } else {
                j = h;
            }
        }

        return i;
    }
}
