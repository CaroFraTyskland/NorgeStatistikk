import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Turns data from https://data.ssb.no/api/v0/dataset/26975.csv?lang=en
 * (population of Norwegian municipalities from 1986 until today) into tables or diagrams in Wikipedia syntax.
 *
 * The syntax works in the German Wikipedia. The table should work in every Wiki, the diagram probably not.
 */
public class MunicipalityOverTime {
    private static final String SSB_URL = "https://data.ssb.no/api/v0/dataset/26975.csv?lang=en";
    private static final String REF = "<ref>{{Internetquelle |url=" + SSB_URL +
            " |titel=Population. Municipalities, pr. 1.1., 1986 - latest year |abruf=" + getTodaysDate() +
            " |werk=ssb.no |sprache=en}}</ref>";

    private static final String data = getCSV();

    /*
        Can be changed depending on the language.
     */
    private static final String YEAR = "Jahr";
    private static final String POPULATION = "Einwohnerzahl";
    private static final String DEVELOPMENT = "Bevölkerungsentwicklung";

    //a few examples
    public static void main(String[] args) {
        //the municipality number has to correct and the year must either be 1986 or ending in '0' or '5'
        MyLambda critEveryFive = ((line, kommunenummer) ->
                line.contains("K-" + kommunenummer) &&
                        (lineToData(line).getYearAsInt() == 1986 || lineToData(line).getYearAsInt() % 5 == 0)
        );

        //the municipality number has to be correct and it has to be the first year of the decade
        MyLambda critEveryTen = ((line, kommunenummer) ->
                line.contains("K-" + kommunenummer) && lineToData(line).getYearAsInt() % 10 == 0
        );

        //only the municipality number has to be the correct one
        MyLambda critEveryYear = ((line, kommunenummer) ->
                line.contains("K-" + kommunenummer)
        );

        System.out.println(getPopulationTable("0301", critEveryFive)); //Oslo every five years
        System.out.println(getPopulationTable("3434", critEveryTen)); //Lom every ten years
        System.out.println(getPopulationTable("1120", critEveryYear)); //Utsira every year

        System.out.println(getPopulationDiagram("0301", critEveryFive)); //Oslo every five years
        System.out.println(getPopulationDiagram("3434", critEveryTen)); //Lom every ten years
        System.out.println(getPopulationDiagram("1120", critEveryYear)); //Utsira every year
    }

    /**
     * Retrieves CSV data from data.ssb.no
     *
     * @return the csv file as a string
     */
    private static String getCSV() {
        try {
            URL url = new URL(SSB_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));

            String line;
            StringBuilder text = new StringBuilder();

            while ((line = br.readLine()) != null) {
                text.append("\n").append(line);
            }

            return text.toString();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Returns the Wiki syntax for the population table containing the population for a given municipality.
     *
     * @param kommunenummer the municipality number (String because Oslo would be converted to 301)
     * @param requirement   the criteria used for data to be included into the list (e.g. 5 or 10 year jumps)
     * @return the wiki syntax of the table
     */
    public static String getPopulationTable(String kommunenummer, MyLambda requirement) {
        ArrayList<PopulationYear> list = getList(kommunenummer, requirement);
        if (list == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder("{| class=\"wikitable\"\n! " + YEAR + "");

        for (PopulationYear year : list) {
            builder.append(" !! ").append(year.getYear());
        }

        builder.append("\n|-\n| '''");
        builder.append(POPULATION);
        builder.append("'''");
        builder.append(REF);

        for (PopulationYear year : list) {
            builder.append(" || ").append(year.getPopulationString());
        }

        builder.append("\n|}");

        return builder.toString();
    }

    /**
     * Returns the Wiki syntax for a diagram with the population development for a given municipality.
     *
     * @param kommunenummer the municipality number (String because Oslo would be converted to 301)
     * @param requirement   the criteria used for data to be included into the list (e.g. 5 or 10 year jumps)
     * @return the wiki syntax of the diagram
     */
    private static String getPopulationDiagram(String kommunenummer, MyLambda requirement) {
        ArrayList<PopulationYear> list = getList(kommunenummer, requirement);

        if (list == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder("<div class=\"hintergrundfarbe1\" style=\"border:1px solid darkgray;" +
                " float:left; margin:0.5em 2em 1em 0; padding:5px; text-align:center;\">\n" +
                "'''" + DEVELOPMENT + "'''" + REF);

        builder.append("\n{{Graph:Chart|width=1000|height=150|type=rect");
        builder.append("\n|x=");
        builder.append(list.get(0).getYear());

        for (int i = 1; i < list.size(); i++) {
            builder.append(", ’");
            builder.append(list.get(i).getYearShort());
        }

        builder.append("\n|y=");

        builder.append(list.get(0).getPopulation());

        for (int i = 1; i < list.size(); i++) {
            builder.append(",").append(list.get(i).getPopulation());
        }
        builder.append("|showValues=}}\n</div>");

        return builder.toString();
    }

    /**
     * Goes through every line of the CSV data, checks if the line is supposed to be added to the list and returns
     * the list.
     *
     * @param kommunenummer the municipality number
     * @param requirement   the criteria applied to decide whether or not data is supposed to be added to the list
     * @return the list of data that fulfills the requirements
     */
    private static ArrayList<PopulationYear> getList(String kommunenummer, MyLambda requirement) {
        if (data == null) {
            return null;
        }

        ArrayList<PopulationYear> list = new ArrayList<>();

        String[] lines = data.split("\n");

        for (String line : lines) {
            if (requirement.acceptLine(line, kommunenummer)) {
                list.add(lineToData(line));
            }
        }

        return list;
    }

    /**
     * Turns line of the csv data into an object containing the year and the population of that year.
     * If the Norwegian version of the dataset is used, the substring for extracting the population has to be slightly
     * changed.
     *
     * @param line the line of the csv data
     * @return the data (year, population) for that line
     */
    private static PopulationYear lineToData(String line) {
        String year = line.substring(line.indexOf("\",\"") + 3, line.indexOf("\",\"Persons"));
        String population = line.substring(line.indexOf("\",\"Persons") + 12);

        return new PopulationYear(year, population);
    }

    /**
     * Get today's date for the reference.
     *
     * @return the data in the YYYY-MM-DD format.
     */
    private static String getTodaysDate() {
        Calendar calendar = Calendar.getInstance();

        int year = calendar.get(Calendar.YEAR);
        String month = (calendar.get(Calendar.MONTH) + 1) < 10 ? "0" + (calendar.get(Calendar.MONTH) + 1) : "" + (calendar.get(Calendar.MONTH) + 1);
        String day = calendar.get(Calendar.DATE) < 10 ? "0" + calendar.get(Calendar.DATE) : "" + (calendar.get(Calendar.DATE) + 1);

        return year + "-" + month + "-" + day;
    }

    /**
     * Stores a combination of year and population.
     */
    private static class PopulationYear {
        String year;
        String population;

        PopulationYear(String year, String population) {
            this.population = population;
            this.year = year;
        }

        public String getYear() {
            return year;
        }

        //1986 -> 86 etc.
        public String getYearShort() {
            return year.substring(2);
        }

        public int getYearAsInt() {
            return Integer.parseInt(year);
        }

        public String getPopulation() {
            return population;
        }

        //10000 -> 10.000 etc.
        public String getPopulationString() {
            int pop = Integer.parseInt(population);
            if (pop < 10000) {
                return population;
            } else {
                if (pop > 100000) {
                    return population.substring(0, 3) + "." + population.substring(3, 6);
                } else {
                    return population.substring(0, 2) + "." + population.substring(2, 5);
                }
            }
        }
    }

    private interface MyLambda {
        boolean acceptLine(String line, String kommunenummer);
    }
}