package es.xan.servantv3.productivity;

import es.xan.servantv3.messages.HNData;
import es.xan.servantv3.weather.WeatherUtils;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ProductivityUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductivityUtils.class);

    public static List<HNData> resolveData() {
        LOGGER.info("Resolving data from hn");
        List<HNData> output = new ArrayList<>();
        try {
            org.jsoup.nodes.Document doc = Jsoup.connect("https://histre.com/hn/").get();

            Elements cards = doc.select(".card-body");

            for (org.jsoup.nodes.Element priceRow : cards) {
                Elements links = priceRow.select("a.fs-3");
                if (links.size() < 1) continue;
                org.jsoup.nodes.Element link = links.get(0);
                String url = link.attr("href");
                String name = link.text();

                Elements contents = priceRow.select("div.text-muted");
                org.jsoup.nodes.Element content = contents.get(0);
                Elements hnLinks = content.select("a");
                org.jsoup.nodes.Element hnlink = hnLinks.get(0);
                String comments_text = hnlink.text();
                comments_text = RegExUtils.removeAll(comments_text, " comment(s)?");
                Integer comments = Integer.parseInt(comments_text);
                String commentsUrl = hnlink.attr("href");

                List<String> tags = new ArrayList<>();
                Elements tags_elements = priceRow.select("div.btn-group");
                for (org.jsoup.nodes.Element tag_element : tags_elements) {
                    String tag = tag_element.text();
                    if (StringUtils.isNotEmpty(tag))
                        tags.add(tag_element.text());
                }

                String date = computeDate(0);
                output.add(new HNData(url, name, comments, commentsUrl, StringUtils.join(tags), date, 0));
            }

        } catch (Exception e) {
            LOGGER.warn(e.getLocalizedMessage(), e);
        }

        return output;
    }

    static  String computeDate(int minusDays) {
        // Get the current date
        LocalDate currentDate = LocalDate.now();
        LocalDate actualDate = currentDate.minusDays(minusDays);

        // Define the formatter for yyyyMMdd
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        // Format the current date as a string
        String formattedDate = actualDate.format(formatter);

        // Print the formatted date
        return formattedDate;
    }


    /**
     * Inserta o actualiza un item en la tabla productivity_hn.
     * Previene SQL injection usando PreparedStatement y valida entradas básicas.
     */
    public static void upsertItem(Connection conn, HNData item) {
        boolean previousAutoCommit = true;

        try {
            previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false); // hacemos la operación atómica

            // 1) Comprobar existencia usando PreparedStatement (previene SQL injection)
            String selectSql = "SELECT 1 FROM productivity_hn WHERE url = ? AND date = ?";
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setString(1, sanitizeAndLimit(item.getUrl(), 200)); // validación básica
                selectStmt.setString(2, item.getDate());      // setDateParam maneja distintos tipos

                boolean found = false;
                try (ResultSet rs = selectStmt.executeQuery()) {
                    found = rs.next();
                }

                if (found) {
                    // 2) Si existe -> UPDATE (solo los campos indicados)
                    String updateSql = "UPDATE productivity_hn SET comments_url = ?, coments_counter = ?, tags = ?, times = times + 1 WHERE url = ? AND date = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        // Asumimos comments -> String o int según tu modelo
                        setObjectOrNull(updateStmt, 1, item.getCommentsUrl());
                        setObjectOrNull(updateStmt, 2, item.getCommentsCounter());
                        setObjectOrNull(updateStmt, 3, sanitizeAndLimit(item.getTags(), 1000));
                        updateStmt.setString(4, sanitizeAndLimit(item.getUrl(), 500));
                        updateStmt.setString( 5, item.getDate());

                        int updated = updateStmt.executeUpdate();
                        if (updated == 0) {
                            // Esto no debería ocurrir porque ya comprobamos existencia, pero podemos registrarlo
                            System.err.println("Advertencia: UPDATE no afectó filas aunque el SELECT indicó existencia.");
                        }
                    }
                } else {
                    // 3) Si no existe -> INSERT
                    String insertSql = "INSERT INTO productivity_hn (url, date, comments_url, comments_counter, tags, times) VALUES (?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                        insertStmt.setString(1, sanitizeAndLimit(item.getUrl(), 500));
                        insertStmt.setString( 2, item.getDate());

                        setObjectOrNull(insertStmt, 3, item.getCommentsUrl());
                        setObjectOrNull(insertStmt, 4, item.getCommentsCounter());
                        setObjectOrNull(insertStmt, 5, sanitizeAndLimit(item.getTags(), 1000));
                        setObjectOrNull(insertStmt, 6, 1);

                        insertStmt.executeUpdate();
                    }
                }
            }

            conn.commit();
        } catch (SQLException sqle) {
            try {
                conn.rollback();
            } catch (SQLException exRollback) {
                LOGGER.warn(exRollback.getMessage(), exRollback);
            }
            LOGGER.warn(sqle.getMessage(), sqle);
        } finally {
            try {
                conn.setAutoCommit(previousAutoCommit);
            } catch (SQLException e) {
                LOGGER.warn(e.getMessage(), e);
            }
        }
    }

    /**
     * Si el valor es null llama a setNull, sino setObject (para int/String/etc).
     */
    private static void setObjectOrNull(PreparedStatement ps, int index, Object value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.NULL);
        } else if (value instanceof Integer) {
            ps.setInt(index, (Integer) value);
        } else if (value instanceof Long) {
            ps.setLong(index, (Long) value);
        } else {
            ps.setObject(index, value);
        }
    }

    /**
     * Validación básica y limitación de longitud para evitar inputs demasiado grandes.
     * Ajusta límites según tu esquema.
     */
    private static String sanitizeAndLimit(String input, int maxLength) {
        if (input == null) return null;
        String trimmed = input.trim();
        if (trimmed.length() > maxLength) {
            return trimmed.substring(0, maxLength); // truncamos para evitar inserts enormes
        }
        return trimmed;
    }



    public static void main(String agrs[]) {
        System.out.println(resolveData());
    }

    public static List<HNData> resolveDateData(Connection conn, String date) {
        List<HNData> output = new ArrayList<>();
        try {
            String selectSql = "SELECT url, name, comments_url, comments_counter, tags, date, times FROM productivity_hn WHERE date = ? ORDER BY times DESC";
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setString(1, date);

                try (ResultSet rs = selectStmt.executeQuery()) {
                    while (rs.next()) {
                        String url = rs.getString(1);
                        String name = rs.getString(2);
                        String comments_url = rs.getString(3);
                        Integer comments_counter = rs.getInt(4);
                        String tags = rs.getString(5);
                        String _date = rs.getString(6);
                        Integer times = rs.getInt(7);

                        output.add(new HNData(url, name, comments_counter, comments_url, tags, _date, times));
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage(), e);
        }

        return output;
    }
}
