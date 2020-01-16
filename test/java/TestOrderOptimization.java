import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.sun.istack.internal.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.paukov.combinatorics3.Generator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static java.util.stream.Collectors.toList;


@RunWith(JUnit4.class)
public class TestOrderOptimization {

    String test1 = "5\n1 M 3 G 5 G\n2 G 3 M 4 G\n5 M";

    String test2 = "5\n2 M\n5 G\n1 G\n5 G 1 G 4 M\n3 G\n5 G\n3 G 5 G 1 G\n3 G\n2 M\n5 G 1 G\n2 M\n5 G\n4 M\n5 G 4 M";

    String test3 = "1\n1 M\n1 G";

    String test4 = "2\n1 G 2 M\n1 M";


    @Test
    public void testFromStrings() {
        OrderOptimization orderOptimization = new OrderOptimization();
        for (String input : new String[]{test1, test2, test3, test4}) {
            testFromString(orderOptimization, input);
        }
    }

    private void testFromString(OrderOptimization orderOptimization, String input) {
        System.out.println("== Input ==");
        System.out.println(input);
        System.out.println("== Solutions ==");
        String solutions = orderOptimization.execute(input.split("\n"));
        System.out.println(solutions);
        System.out.println();
    }

    @Test
    public void testCustom() {
        OrderOptimization orderOptimization = new OrderOptimization();
        String input = "4\n" +
                "1 M\n" +
                "1 G 2 M\n" +
                "1 M 2 G\n" +
                "2 G 3 M\n" +
                "2 M 3 G\n" +
                "1 M 4 G";
        //input = test2;
        testFromString(orderOptimization, input);
    }

    @Test
    public void testAgainstOracle() throws Exception {
        int numColors = 5;
        int numClients = 6;
        //generate all possible orders from a single client
        List<List<String>> combinations = Generator.permutation(new String[]{"X", "M", "G"}).withRepetitions(numColors).stream()
                .filter(client -> client.stream().filter(c -> c.equals("M")).count() == 1).collect(toList());
        System.out.println("== Combination ==");
        List<String> combs = combinations.stream().map(this::formatRow).filter(string -> !string.isEmpty()).collect(toList());
        //System.out.println(combs);
        OrderOptimization optimization = new OrderOptimization();
        OrderOptimization oracle = new OracleOrderOptimization();
        //generate all subsets
        long count = Generator.combination(combs).simple(numClients).stream().count();
        System.out.println("count " + count);
        Generator.combination(combs).simple(numClients).stream().parallel().forEach(strings -> {
            //System.out.println(String.format("Solving %d/%d", curren++, count));
            String input = numColors + "\n" + formatListOfRows(strings);
            String output = optimization.execute(input.split("\n"));
            String outputOracle = oracle.execute(input.split("\n"));
            long outputMatteCount = output.chars().filter(c -> c == 'M').count();
            long oracleMatteCount = outputOracle.chars().filter(c -> c == 'M').count();
            if (outputMatteCount != oracleMatteCount) {
                System.out.println("======");
                System.out.println(input);
                System.out.println("===Solution: " + output);
                System.out.println("===Oracle  : " + outputOracle);
                assert false;
            }
        });
    }

    @Test
    public void bigTest() throws Exception {
        int numColors = 1000;
        int numClients = 100000;
        Random random = new Random(40);
        List<String> order = new ArrayList<>(numClients);
        for (int i = 0; i < numClients; i++) {
            StringBuilder builder = new StringBuilder();
            boolean matteAdded = false;
            for (int j = 1; j <= numColors; j++) {
                if (random.nextBoolean()) {
                    if (random.nextBoolean()) {
                        builder.append(j).append(" G ");
                    } else if (!matteAdded) {
                        builder.append(j).append(" M ");
                        matteAdded = true;
                    }
                }

            }
            builder.deleteCharAt(builder.length() - 1);
            order.add(builder.toString());

        }
        String input = numColors + "\n" + formatListOfRows(order);
        String output = new OrderOptimization().execute(input.split("\n"));
        System.out.println("===Solution: " + output);
    }


    @Test
    public void testPrintConsole() throws Exception {
        int numColors = 3;
        //generate all possible orders from a single client
        List<List<String>> combinations = Generator.permutation(new String[]{"X", "M", "G"}).withRepetitions(numColors).stream().collect(toList());
        System.out.println("== Combination ==");
        List<String> combs = combinations.stream().map(this::formatRow).filter(string -> !string.isEmpty()).collect(toList());
        System.out.println(combs);
        OrderOptimization optimization = new OrderOptimization();
        //generate all subsets
        Generator.combination(combs).simple(1).stream().forEach(strings -> {
            String input = numColors + "\n" + formatListOfRows(strings);
            System.out.println("======");
            System.out.println(input);
            String output = optimization.execute(input.split("\n"));
            System.out.println("===Solution: " + output);
            System.out.println();
        });
    }


    private String formatRow(List<String> strings) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < strings.size(); i++) {
            String s = strings.get(i);
            if (s.contentEquals("X")) continue;
            builder.append(i + 1);
            builder.append(" ");
            builder.append(s);
            builder.append(" ");
        }
        return builder.toString().trim();
    }

    private String formatListOfRows(List<String> strings) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < strings.size(); i++) {
            String s = strings.get(i);
            builder.append(s);
            builder.append("\n");
        }
        return builder.deleteCharAt(builder.length() - 1).toString();
    }


    private class OracleOrderOptimization extends OrderOptimization {

        @Override
        protected List<Color> solve(@NotNull List<List<Color>> order) {
            //build all the possible solutions
            List<List<Color>> solutions = Generator.permutation(Color.Type.values()).withRepetitions(colorCount).stream()
                    .map(types -> Stream.of(types).mapIndexed((index, type) -> new Color(index + 1, type))
                            .collect(Collectors.toList())).collect(toList());
            //return only the valid solutions that satisfy the order
            return solveMin(solutions.stream().filter(solution -> isASolution(order, solution)).collect(toList()));
        }


        //Choose the optimal solution with less Matte color possible
        List<Color> solveMin(@NotNull List<List<Color>> solution) {
            return solution.stream().min((o1, o2) -> {
                Long l1 = o1.stream().filter(Color::isMatte).count();
                Long l2 = o2.stream().filter(Color::isMatte).count();
                return l1.compareTo(l2);
            }).orElse(Collections.emptyList());
        }
    }

}
