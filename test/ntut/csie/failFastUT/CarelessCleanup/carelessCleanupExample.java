package ntut.csie.failFastUT.CarelessCleanup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class carelessCleanupExample {
	static FileInputStream fileInputStream = null;
	File file = null;

	public void closeDirectly() throws Exception {
		fileInputStream.close(); // Safe
	}

	public static void callStaticMethod() throws IOException {

		try {
			fileInputStream.read();
			fileInputStream.close();
			fileInputStream.read();
			fileInputStream.close();
		} finally {

		}
	}

	public void callPublicMethod() throws IOException {

		try {
			fileInputStream.read();
			fileInputStream.close();
			fileInputStream.read();
			fileInputStream.close();
		} finally {

		}
	}
	
	private void callPrivateMethod() throws IOException {

		try {
			fileInputStream.read();
			fileInputStream.close();
			fileInputStream.read();
			fileInputStream.close();
		} finally {

		}
	}
}
