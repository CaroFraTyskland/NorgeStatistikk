import java.util.ArrayList;

/**
 * Metadata for https://de.wikipedia.org/wiki/Vorlage:Metadaten_Einwohnerzahl_NO
 * The population for the "fylke" and the whole country is left out because it's not that much work anyway.
 */
public class MunicipalityToMetadata {

    private static final String data = MunicipalityOverTime.getCSV();

    private static final String CURRENT_YEAR = "2022";

    public static void main(String[] args) {
        System.out.println(getMetadata());
    }

    /**
     * Runs through the list of metadata entries and creates the Wiki syntax for it.
     *
     * @return the Wiki syntax for the metadata
     */
    public static String getMetadata() {
        ArrayList<MetadataEntry> list = getList();

        if (list == null) {
            return "";
        }

        list.sort(MetadataEntry::compareTo);

        StringBuilder builder = new StringBuilder();

        for (MetadataEntry entry : list) {
            builder.append("| ");
            builder.append(entry.kommunenummer).append(" = ");
            builder.append(entry.population);
            builder.append(" <!-- ").append(entry.municipalityName).append(" -->\n");
        }

        return builder.toString();
    }

    /**
     * Goes through every line of the CSV data, checks if the line is supposed to be added to the list
     * (thus being data from the current year) and returns the list.
     *
     * @return the list of data from the current year
     */
    private static ArrayList<MetadataEntry> getList() {
        if (data == null) {
            return null;
        }

        ArrayList<MetadataEntry> list = new ArrayList<>();

        String[] lines = data.split("\n");

        for (String line : lines) {
            if (line.contains("\"Persons\"")) {
                MetadataEntry entry = lineToData(line);
                if (entry.year.equals(CURRENT_YEAR) && entry.kommunenummer.length() == 4) {
                    list.add(entry);
                }
            }
        }

        return list;
    }

    /**
     * Retrieves the year, population, municipality number and name from a line of the CSV data.
     *
     * @param line the line
     * @return the MetadataEntry object containing the data
     */
    private static MetadataEntry lineToData(String line) {
        String year = line.substring(line.indexOf("\",\"") + 3, line.indexOf("\",\"Persons"));
        String population = line.substring(line.indexOf("\",\"Persons") + 12);
        String kommunenummer = line.substring(3, line.indexOf(" "));
        String municipalityName = line.substring(line.indexOf(" ") + 1, line.indexOf("\",\""));

        return new MetadataEntry(year, municipalityName, population, kommunenummer);
    }

    private static class MetadataEntry implements Comparable<MetadataEntry> {
        String year;
        String municipalityName;
        String population;
        String kommunenummer;

        public MetadataEntry(String year, String municipalityName, String population, String kommunenummer) {
            this.year = year;
            this.municipalityName = municipalityName;
            this.population = population;
            this.kommunenummer = kommunenummer;

            setMunicipalityName();
        }

        private void setMunicipalityName() {
            municipalityName = municipalityName.replace("�", "?");
        }

        @Override
        public String toString() {
            return "MetadataEntry{" +
                    "year='" + year + '\'' +
                    ", municipalityName='" + municipalityName + '\'' +
                    ", population='" + population + '\'' +
                    ", kommunenummer='" + kommunenummer + '\'' +
                    '}';
        }

        @Override
        public int compareTo(MetadataEntry o) {
            return kommunenummer.compareTo(o.kommunenummer);
        }
    }
}