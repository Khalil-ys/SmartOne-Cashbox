package az.smartonecashbox.smartCassa;

import java.sql.*;

public class ConnectionDB {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/database_name";
    private static final String USER = "username";
    private static final String PASSWORD = "password";

    public Connection getConnection() {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);
            System.out.println("Bazaya bağlandı.");
        } catch (SQLException e) {
            System.out.println("Bazaya bağlantı hatası: " + e.getMessage());
            e.printStackTrace();
        }
        return connection;
    }

    public void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("DbBaza bağlantısı kapatıldı.");
            } catch (SQLException e) {
                System.out.println("DbBaza kapatılırken xeta bas verdi: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void closePreparedStatement(PreparedStatement pstmt) {
        if (pstmt != null) {
            try {
                pstmt.close();
            } catch (SQLException e) {
                System.out.println("PreparedStatement qapatilarken xeta oldu: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                System.out.println("ResultSet qapatilarken xeta oldu: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
