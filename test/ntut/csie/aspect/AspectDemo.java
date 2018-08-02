package ntut.csie.aspect;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;


public class AspectDemo {
	public static void callStaticMethod() {
		java.sql.Connection conn = null;
		FileWriter fw;
		try {
			fw = new FileWriter("test.txt");
			fw.write("test");
	        fw.flush();
	        fw.close();
	        conn = DriverManager.getConnection("test0","test1","test2");
		}catch (IOException e) {
			e.printStackTrace();
		}catch (SQLException e) {
		}
	}
	private static void throwEx() throws SQLException {
		throw new SQLException();
	}
	public void callPublicMethod() {
		java.sql.Connection conn = null;
		FileWriter fw;
		try {
			fw = new FileWriter("test.txt");
			fw.write("test");
	        fw.flush();
	        fw.close();
	        conn = DriverManager.getConnection("test0","test1","test2");
		}catch (IOException e) {
			e.printStackTrace();
		}catch (SQLException e) {
		}
	}
	private void callPrivateMethod() {
		java.sql.Connection conn = null;
		FileWriter fw;
		try {
			fw = new FileWriter("test.txt");
			fw.write("test");
	        fw.flush();
	        fw.close();
	        conn = DriverManager.getConnection("test0","test1","test2");
		}catch (IOException e) {
			e.printStackTrace();
		}catch (SQLException e) {
		}
	}
}
