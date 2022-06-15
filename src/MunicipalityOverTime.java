import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MunicipalityOverTime {

    public static final String SSB_URL = "https://data.ssb.no/api/v0/dataset/26975.csv?lang=en";

    /**
     * Retrieves CSV data from data.ssb.no
     *
     * @return the csv file as a string
     */
    public static String getCSV() {
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
}