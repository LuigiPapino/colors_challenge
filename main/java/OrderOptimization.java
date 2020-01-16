import com.sun.istack.internal.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;


public class OrderOptimization {

    int colorCount;

    private static long initTime;

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Provide a file name as first parameter");
            return;
        }
        if (!new File(args[0]).exists()) {
            System.out.println("File not found");
            return;
        }
        initTime = System.currentTimeMillis();
        Stream<String> stream = Files.lines(Paths.get(args[0])).map(String::trim).filter(s -> !s.isEmpty());
        String output = new OrderOptimization().execute(stream.toArray(String[]::new));
        System.out.println(  output);
        //System.out.println("Execution time:" + (System.currentTimeMillis() - initTime));
    }

    public String execute(@NotNull String[] input) {

        colorCount = Integer.valueOf(input[0]);
        List<List<Color>> order = convertInput(input);

        List<Color> solution = solve(order);
        if (!solution.isEmpty()) {
            return solution.stream()
                    .map(color -> color.type.toString())
                    .reduce((s, s2) -> String.format("%s %s", s, s2))
                    .orElse("Something very bad happened");
        } else {
            return "No solution exists";
        }

    }


    /**
     * The solving strategy is to simplify step by step the order with three different actions, after each action shrink the order removing the clients satisfied
     * 1 - {#addOrphans} add the colors not used by any client or a gloss specified by one client only
     * 2 - {#addClientEssential} add the colors specified only by one client
     * 3 - {#addMostFrequentGloss} add the most frequent gloss
     *
     * @param orderInput
     * @return the solution or an empty list if no solution exist
     */
    List<Color> solve(@NotNull List<List<Color>> orderInput) {
        List<List<Color>> order = new ArrayList<>();

        //clone order
        for (List<Color> row : orderInput) {
            List<Color> newRow = new ArrayList<>(colorCount);
            newRow.addAll(row);
            order.add(newRow);
        }

        List<Color> solution = new ArrayList<>(colorCount);
        while (order.size() > 0) {

            if (addOrphans(solution, order)) {
                continue;
            }
            if (addClientEssential(solution, order)) {
                continue;
            }

            boolean found = addMostFrequentGloss(solution, order);

            //no useful gloss colors anymore
            if (!found) {
                break;
            }
        }

        List<Color> result = IntStream.range(1, colorCount + 1).mapToObj(Color::buildGloss).collect(Collectors.toList());
        solution.forEach(color -> result.set(color.index - 1, color));
        if (isASolution(orderInput, result))
            return result;
        else
            return Collections.emptyList();

    }

    /**
     * Add the most frequent gloss color
     * @param solution
     * @param order
     * @return true if any color has been added to the solution
     */
    private boolean addMostFrequentGloss(List<Color> solution, List<List<Color>> order) {
        //frequency for each gloss color
        int[] frequency = new int[colorCount];
        order.stream().flatMap(List::stream).filter(Color::isGloss).forEach(c -> frequency[c.index - 1]++);

        int max = Arrays.stream(frequency).max().orElse(0);
        boolean found = false;
        // choose the gloss color with higher frequency
        if (max > 0) {
            for (int i = 0; i < frequency.length; i++) {
                if (frequency[i] == max) {
                    Color color = new Color(i + 1, Color.Type.G);
                    if (columnSatisfiedBy(order, color)) {
                        solution.add(color);
                        found = true;
                        break;
                    }
                }
            }
        }
        return found;
    }

    /**
     * If a color has not be specified by any client, assign gloss to it.
     * If a gloss color has a frequency == 1 is safe to add to the solution
     *
     * @param solution
     * @param order
     * @return true if any color has been added to the solution
     */
    private boolean addOrphans(List<Color> solution, List<List<Color>> order) {
        int[] frequency = new int[colorCount];
        order.stream().flatMap(List::stream).forEach(c -> frequency[c.index - 1]++);
        List<Color> solutionNew = new ArrayList<>(colorCount);
        for (int i = 0; i < frequency.length; i++) {
            if (frequency[i] == 0) {
                Color color = new Color(i + 1, Color.Type.G);
                if (solution.stream().noneMatch(c -> c.hasIndex(color.index))) {
                    solutionNew.add(color);
                }
            }
            if (frequency[i] == 1) {
                int index = i + 1;
                Color color = order.stream().flatMap(List::stream).filter(c -> c.hasIndex(index)).findFirst().orElse(null);
                if (color != null && color.isGloss() && solution.stream().noneMatch(c -> c.hasIndex(color.index))) {
                    solutionNew.add(color);
                }
            }

        }

        solution.addAll(solutionNew);
        for (Color color : solutionNew) {
            columnSatisfiedBy(order, color);
        }

        return !solutionNew.isEmpty();
    }

    /**
     * If a client has only one color, it's necessary for the solution
     *
     * @param solution
     * @param order
     * @return true if any color has been added to the solution
     */
    private boolean addClientEssential(List<Color> solution, List<List<Color>> order) {
        List<Color> solutionNew = new ArrayList<>(colorCount);

        for (List<Color> client : order) {
            if (client.size() == 1) {
                if (solution.stream().noneMatch(c -> c.hasIndex(client.get(0).index))) {
                    solutionNew.add(client.get(0));
                }
            }
        }
        solution.addAll(solutionNew);

        for (Color color : solutionNew) {
            columnSatisfiedBy(order, color);
        }


        return !solutionNew.isEmpty();

    }


    /**
     * Check if the color satisfy one or more clients, in that case the clients and the column/color-index will be removed from the order
     *
     * @param orderList
     * @param color
     * @return if the color satisfy at least a clietn
     */
    boolean columnSatisfiedBy(List<List<Color>> orderList, Color color) {
        int column = color.index - 1;
        Color[][] orderArray = expand(orderList, colorCount);
        List<List<Color>> rowsToRemove = new ArrayList<>();
        boolean satisfy = true;

        for (int i = 0; i < orderArray.length; i++) {
            Color it = orderArray[i][column];
            if (it != null) {
                if (it.type != color.type) {
                    if (orderList.get(i).size() == 1) { //check if there are other element in the row
                        satisfy = false;
                        break;
                    } else {
                        //there are others element in the row
                    }
                } else {
                    rowsToRemove.add(orderList.get(i));
                }
            }
        }

        if (satisfy) {
            //remove rows;
            orderList.removeAll(rowsToRemove);
            //remove column
            for (List<Color> colors : orderList) {
                for (int j = 0; j < colors.size(); j++) {
                    Color color1 = colors.get(j);
                    if (color1.hasIndex(color.index)) {
                        colors.remove(j);
                        break;
                    }
                }
            }
            return true;
        } else {
            return false;
        }

    }

    Color[][] expand(List<List<Color>> order, int colorCount) {
        Color[][] result = new Color[order.size()][];
        for (int i = 0; i < order.size(); i++) {
            List<Color> colors = order.get(i);
            Color[] row = new Color[colorCount];
            for (Color color : colors) {
                row[color.index - 1] = color;
            }
            result[i] = row;
        }
        return result;
    }


    /**
     * Jump the first line and convert to a list of color
     */
    private List<List<Color>> convertInput(String[] input) {
        List<List<Color>> list = new ArrayList<>(input.length - 1);
        Arrays.stream(input).skip(1).forEach(row -> {
            List<Color> client = new ArrayList<>();
            list.add(client);
            String[] tokens = row.split(" ");
            for (int j = 0; j < tokens.length; j += 2) {
                client.add(new Color(tokens[j], tokens[j + 1]));
            }
        });
        return list;
    }


    /**
     * Utility method to check if a solution is valid
     *
     * @param order
     * @param solution
     * @return
     */
    protected boolean isASolution(@NotNull List<List<Color>> order, List<Color> solution) {
        return order.stream().allMatch(client -> client.stream().anyMatch(solution::contains));
    }


}
