package ntut.csie.failFastUT.Thrown;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;

public class thrownFromFinallyExample {
	public static void callStaticMethod() throws IOException , SQLException {
		java.sql.Connection conn = null;
		FileWriter fw = null;
		try {
			fw = new FileWriter("test.txt");
			fw.write("test");
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			fw.close();
			fw.close();
			throwEx();
		}
	}

	private static void throwEx() throws SQLException {
		throw new SQLException();
	}
	
	public void callPublicMethod() throws IOException , SQLException {
		java.sql.Connection conn = null;
		FileWriter fw = null;
		try {
			fw = new FileWriter("test.txt");
			fw.write("test");
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			fw.close();
			fw.close();
			throwEx();
		}
	}
	
	private void callPrivateMethod() throws IOException , SQLException {
		java.sql.Connection conn = null;
		FileWriter fw = null;
		try {
			fw = new FileWriter("test.txt");
			fw.write("test");
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			fw.close();
			fw.close();
			throwEx();
		}
	}
}
