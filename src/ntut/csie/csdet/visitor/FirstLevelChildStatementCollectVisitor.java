package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Statement;

public class FirstLevelChildStatementCollectVisitor extends ASTVisitor {

	private List<Statement> childrens;
	private boolean isVisitedParentNode;

	public List<Statement> getChildrens() {
		return childrens;
	}

	public FirstLevelChildStatementCollectVisitor(ASTNode parentNode) {
		childrens = new ArrayList<Statement>();
		isVisitedParentNode = false;
	}

	@Override
	public boolean preVisit2(ASTNode node) {
		if(!isVisitedParentNode) {
			isVisitedParentNode = true;
			return true;
		} else {
			if (node instanceof Statement) {
				childrens.add((Statement) node);
			}
			return false;
		}
	}

}
