package ntut.csie.filemaker.sampleCode4VisitorTest;

public class VariableDeclarationStatementSampleCode {
	private String fieldString;
	
	public VariableDeclarationStatementSampleCode() {
		fieldString = "I can can a can.";
	}
	
	public void testField() {
		fieldString.compareTo("Are you kidding me?");
		fieldString.toLowerCase();
	}
	
	public void declareInMethodUseInIfStatement() {
		String localString = "�d�s������";
		int nothing = 1;
		if(nothing == 1) {
			localString.getBytes();
			localString.intern();
		}
	}
	
	public void declareSameNameInstanceInDifferentMethodDeclaration_MD1() {
		String sameName = "�@���@���A����ӳB����";
		sameName.toCharArray();
	}
	
	public void declareSameNameInstanceInDifferentMethodDeclaration_MD2() {
		String sameName = "�@���@���A����ӳB����";
		sameName.toCharArray();
	}
}