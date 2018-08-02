package ntut.csie.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodInvocation;

public class MethodInvocationCollectorVisitor extends ASTVisitor{
	private final List <MethodInvocation> methodInvocations = new ArrayList <MethodInvocation> ();
	private final List <MethodInvocation> firstInv = new ArrayList <MethodInvocation> ();
	
	int count = 0;
	  @Override
	  public boolean visit (final MethodInvocation methodInvocation) {
		  /**
		   * 在Careless Cleanup某一段程式碼裡如果有兩個以上獨立區塊的TryStatement，會藉由count來區分不同TryStatement，
		   * 來拿到每個TryStatement中的第一個會丟出例外的Method Invocation用來產生UT
		   * #1.firstInv[0]是第一個TryStmt內第一個會丟出例外的method
		   * #2.firstInv[1]是第二個TryStmt內第一個會丟出例外的method
		   * 下圖為firstInv List的儲存示意圖
		   * ----- -----
		   * | #1 | #2 |......
		   * ----- -----   
		   */
		  if(methodInvocation.resolveMethodBinding()
					.getExceptionTypes().length != 0 && count==0){
			  firstInv.add(methodInvocation);
			  count++;
		  }
		  methodInvocations.add (methodInvocation);
	    return super.visit (methodInvocation);
	  }

	  public List<MethodInvocation> getMethodInvocations () {
	    return Collections.unmodifiableList (methodInvocations);
	  }
	  
	  public List<MethodInvocation> getFirstInvocations () {
	    return Collections.unmodifiableList (firstInv);
	  }
	  
	  public void resetFirstInv(){
		  count = 0;
	  }
}
