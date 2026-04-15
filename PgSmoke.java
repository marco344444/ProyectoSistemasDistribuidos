import java.sql.*;
public class PgSmoke {
  public static void main(String[] args) throws Exception {
    String url = "jdbc:postgresql://localhost:5432/imageproc";
    String user = "imageproc";
    String pass = "imageproc123";
    try (Connection c = DriverManager.getConnection(url, user, pass)) {
      System.out.println("OK JDBC " + c.getMetaData().getDatabaseProductVersion());
    }
  }
}
