package ntut.csie.filemaker.test;

/**
 * 供 JavaFileToStringTest 使用的範例
 * @author pig
 */
public class JavaFileToStringExample {
	private int memberInt;
	private String memberString;
	
	public void sayHello() {
	}

	protected void doSayHello() {
	}
	
	public int plusOne(int beforePlus) {
		return returnWithPlusOne(beforePlus);
	}

	private int returnWithPlusOne(int beforePlus) {
		return beforePlus + 1;
	}
}
