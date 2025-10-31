package nl.han.ica.icss.parser;

import java.util.Stack;


import nl.han.ica.datastructures.HANStack;
import nl.han.ica.datastructures.IHANStack;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;
import nl.han.ica.icss.ast.selectors.ClassSelector;
import nl.han.ica.icss.ast.selectors.IdSelector;
import nl.han.ica.icss.ast.selectors.TagSelector;

/**
 * This class extracts the ICSS Abstract Syntax Tree from the Antlr Parse tree.
 */
public class ASTListener extends ICSSBaseListener {

	// Dit is de gehele AST
	private AST ast;

	// Dit is een stack die bijhoudt in welke container we momenteel aan het toevoegen zijn
	private IHANStack<ASTNode> currentContainer;

	public ASTListener() {
        // Maak een nieuwe lege AST aan
		ast = new AST();
        // Maak een nieuwe lege stack aan voor de huidige container
		currentContainer = new HANStack<>();
	}
    // Geeft de gegenereerde AST terug
    public AST getAST() {
        return ast;
    }

    // Enter-methode voor stylesheet
	@Override public void enterStylesheet(ICSSParser.StylesheetContext ctx) {
        // Nieuwe Stylesheet node aanmaken
		Stylesheet stylesheet = new Stylesheet();
        // Push de stylesheet op de huidige container stack
		currentContainer.push(stylesheet);
	}

    // Exit-methode voor stylesheet
	@Override public void exitStylesheet(ICSSParser.StylesheetContext ctx) {
        // Pop de stylesheet van de huidige container stack
		Stylesheet stylesheet = (Stylesheet) currentContainer.pop();
        // Zet de stylesheet als root van de AST
		ast.root = stylesheet;
	}

    // Enter-methode voor een variable assignment
	@Override
	public void enterAssignment(ICSSParser.AssignmentContext ctx) {
        // Nieuwe VariableAssignment node aanmaken
		VariableAssignment assignment = new VariableAssignment();
        // Zet de naam van de variabele
		assignment.name = new VariableReference(ctx.CAPITAL_IDENT().getText());
        // Voeg de assignment toe aan de huidige container
		currentContainer.peek().addChild(assignment);
        // Push de assignment op de huidige container stack
		currentContainer.push(assignment);
	}

    // Exit-methode voor een variable assignment
	@Override
	public void exitAssignment(ICSSParser.AssignmentContext ctx) {
		currentContainer.pop();
	}

    // Enter-methode voor een variable reference
	@Override
	public void enterVariableReference(ICSSParser.VariableReferenceContext ctx) {
        // Nieuwe VariableReference node aanmaken
		VariableReference varRef = new VariableReference(ctx.CAPITAL_IDENT().getText());
        // Voeg de variable reference toe aan de huidige container
		currentContainer.peek().addChild(varRef);
	}

    // Enter-methode voor stylerule
	@Override public void enterStylerule(ICSSParser.StyleruleContext ctx) {
        // Nieuwe Stylerule node aanmaken
		Stylerule stylerule = new Stylerule();
        // Push de stylerule op de huidige container stack
		currentContainer.push(stylerule);
	}

    // Exit-methode voor stylerule
	@Override public void exitStylerule(ICSSParser.StyleruleContext ctx) {
        // Pop de stylerule van de huidige container stack
		Stylerule stylerule = (Stylerule) currentContainer.pop();
        // Voeg de stylerule toe aan de huidige container
		currentContainer.peek().addChild(stylerule);
	}

    // Enter-methode voor selector
	@Override public void enterSelector(ICSSParser.SelectorContext ctx) {
        // Als het een tag selector is
		if (ctx.LOWER_IDENT() != null) {
            // Nieuwe TagSelector node aanmaken
			TagSelector selector = new TagSelector(ctx.LOWER_IDENT().getText());
            // Voeg de tag selector toe aan de huidige container
			currentContainer.peek().addChild(selector);
        // Als het een class selector is
		} else if (ctx.CLASS_IDENT() != null) {
            // Nieuwe ClassSelector node aanmaken
			ClassSelector selector = new ClassSelector(ctx.CLASS_IDENT().getText());
            // Voeg de class selector toe aan de huidige container
			currentContainer.peek().addChild(selector);
        // Als het een id selector is
		} else if (ctx.ID_IDENT() != null) {
            // Nieuwe IdSelector node aanmaken
			IdSelector selector = new IdSelector(ctx.ID_IDENT().getText());
            // Voeg de id selector toe aan de huidige container
			currentContainer.peek().addChild(selector);
		}
	}

    // Enter-methode voor declaration
	@Override public void enterDeclaration(ICSSParser.DeclarationContext ctx) {
        // Nieuwe Declaration node aanmaken
		Declaration declaration = new Declaration(ctx.property().getText());
        // Push de declaration op de huidige container stack
		currentContainer.push(declaration);
	}

    // Exit-methode voor declaration
	@Override public void exitDeclaration(ICSSParser.DeclarationContext ctx) {
        // Pop de declaration van de huidige container stack
		Declaration declaration = (Declaration) currentContainer.pop();
        // Voeg de declaration toe aan de huidige container
		currentContainer.peek().addChild(declaration);
	}

    // Enter-methode voor if-clause
    @Override public void enterIfClause(ICSSParser.IfClauseContext ctx) {
        // Nieuwe IfClause node aanmaken
        IfClause ifClause = new IfClause();
        // Voeg de if-clause toe aan de huidige container
        currentContainer.peek().addChild(ifClause);
        // Push de if-clause op de huidige container stack
        currentContainer.push(ifClause);
    }

    // Exit-methode voor if-clause
    @Override public void exitIfClause(ICSSParser.IfClauseContext ctx) {
        // Pop de if-clause van de huidige container stack
        currentContainer.pop();
    }

    // Enter-methode voor else-clause
    @Override public void enterElseClause(ICSSParser.ElseClauseContext ctx) {
        // Nieuwe ElseClause node aanmaken
        ElseClause elseClause = new ElseClause();
        // Voeg de else-clause toe aan de huidige container
        currentContainer.peek().addChild(elseClause);
        // Push de else-clause op de huidige container stack
        currentContainer.push(elseClause);
    }

    // Exit-methode voor else-clause
    @Override public void exitElseClause(ICSSParser.ElseClauseContext ctx) {
        // Pop de else-clause van de huidige container stack
        currentContainer.pop();
    }

    // Enter-methode voor add-expression
	@Override public void enterAddExpression(ICSSParser.AddExpressionContext ctx) {
        // Nieuwe AddOperation node aanmaken
		AddOperation addOperation = new AddOperation();
        // Voeg de add-operation toe aan de huidige container
		currentContainer.peek().addChild(addOperation);
        // Push de add-operation op de huidige container stack
		currentContainer.push(addOperation);
	}

    // Exit-methode voor add-expression
	@Override public void exitAddExpression(ICSSParser.AddExpressionContext ctx) {
        // Pop de add-operation van de huidige container stack
		currentContainer.pop();
	}

    // Enter-methode voor subtract-expression
	@Override public void enterSubExpression(ICSSParser.SubExpressionContext ctx) {
        // Nieuwe SubtractOperation node aanmaken
		SubtractOperation subtractOperation = new SubtractOperation();
        // Voeg de subtract-operation toe aan de huidige container
		currentContainer.peek().addChild(subtractOperation);
        // Push de subtract-operation op de huidige container stack
		currentContainer.push(subtractOperation);
	}

    // Exit-methode voor subtract-expression
	@Override public void exitSubExpression(ICSSParser.SubExpressionContext ctx) {
        // Pop de subtract-operation van de huidige container stack
		currentContainer.pop();
	}

    // Enter-methode voor multiply-expression
	@Override public void enterMulExpression(ICSSParser.MulExpressionContext ctx) {
        // Nieuwe MultiplyOperation node aanmaken
		MultiplyOperation multiplyOperation = new MultiplyOperation();
        // Voeg de multiply-operation toe aan de huidige container
		currentContainer.peek().addChild(multiplyOperation);
        // Push de multiply-operation op de huidige container stack
		currentContainer.push(multiplyOperation);
	}

    // Exit-methode voor multiply-expression
	@Override public void exitMulExpression(ICSSParser.MulExpressionContext ctx) {
        // Pop de multiply-operation van de huidige container stack
		currentContainer.pop();
	}

    // Enter-methode voor scalar-literal
	@Override public void enterScalarLiteral(ICSSParser.ScalarLiteralContext ctx) {
        // Nieuwe ScalarLiteral node aanmaken
		ScalarLiteral scalarLiteral = new ScalarLiteral(ctx.SCALAR().getText());
        // Voeg de scalar-literal toe aan de huidige container
		currentContainer.peek().addChild(scalarLiteral);
	}

    // Enter-methode voor pixel-literal
	@Override public void enterPixelLiteral(ICSSParser.PixelLiteralContext ctx) {
        // Nieuwe PixelLiteral node aanmaken
		PixelLiteral pixelLiteral = new PixelLiteral((ctx.PIXELSIZE().getText()));
        // Voeg de pixel-literal toe aan de huidige container
		currentContainer.peek().addChild(pixelLiteral);
	}

    // Enter-methode voor color-literal
	@Override public void enterColorLiteral(ICSSParser.ColorLiteralContext ctx) {
        // Nieuwe ColorLiteral node aanmaken
		ColorLiteral colorLiteral = new ColorLiteral(ctx.COLOR().getText());
        // Voeg de color-literal toe aan de huidige container
		currentContainer.peek().addChild(colorLiteral);
	}

    // Enter-methode voor true bool-literal
    // Waarom heb ik de true en false literals niet hier gecombineerd?
    // Omdat ze verschillende waarden representeren (true en false)
    // en het duidelijker is om ze apart te behandelen.
	@Override public void enterTrueLiteral(ICSSParser.TrueLiteralContext ctx) {
        // Nieuwe BoolLiteral node aanmaken
		BoolLiteral boolLiteral = new BoolLiteral(true);
        // Voeg de bool-literal toe aan de huidige container
		currentContainer.peek().addChild(boolLiteral);
	}

    // Enter-methode voor false bool-literal
	@Override public void enterFalseLiteral(ICSSParser.FalseLiteralContext ctx) {
        // Nieuwe BoolLiteral node aanmaken
		BoolLiteral boolLiteral = new BoolLiteral(false);
        // Voeg de bool-literal toe aan de huidige container
		currentContainer.peek().addChild(boolLiteral);
	}
}