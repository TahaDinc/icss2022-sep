package nl.han.ica.icss.checker;

import javafx.scene.paint.Color;
import nl.han.ica.datastructures.IHANLinkedList;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.ColorLiteral;
import nl.han.ica.icss.ast.literals.PixelLiteral;
import nl.han.ica.icss.ast.literals.ScalarLiteral;
import nl.han.ica.icss.ast.literals.PercentageLiteral;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;
import nl.han.ica.icss.ast.types.ExpressionType;

import java.util.HashMap;
import java.util.LinkedList;

public class Checker {
    private LinkedList<HashMap<String, ExpressionType>> variableTypes = new LinkedList<>();

    public void check(AST ast) {
        variableTypes = new LinkedList<>();
        // global scope
        variableTypes.push(new HashMap<>());
        checkStylesheet(ast.root);
    }

    private void checkStylesheet(Stylesheet sheet) {
        for (ASTNode child : sheet.getChildren()) {
            if (child instanceof Stylerule) {
                checkStylerule((Stylerule) child);
            } else if (child instanceof VariableAssignment) {
                checkAssignment((VariableAssignment) child);
            }
        }
    }

    private void checkStylerule(Stylerule rule) {
        // create a new scope for this stylerule
        variableTypes.push(new HashMap<>());
        for (ASTNode child : rule.getChildren()) {
            if (child instanceof Declaration) {
                checkDeclaration((Declaration) child);
            } else if (child instanceof VariableAssignment) {
                checkAssignment((VariableAssignment) child);
            } else if (child instanceof IfClause) {
                checkIfClause((IfClause) child);
            }
        }
        // leave scope
        variableTypes.pop();
    }

    private void checkIfClause(IfClause ifClause) {
        // create a new scope for this if clause
        variableTypes.push(new HashMap<>());
        for (ASTNode child : ifClause.body) {
            if (child instanceof Declaration) {
                checkDeclaration((Declaration) child);
            } else if (child instanceof VariableAssignment) {
                checkAssignment((VariableAssignment) child);
            }
        }
        // leave scope
        variableTypes.pop();

        // check else clause if present
        if (ifClause.elseClause != null) {
            // create a new scope for else clause
            variableTypes.push(new HashMap<>());
            for (ASTNode child : ifClause.elseClause.body) {
                if (child instanceof Declaration) {
                    checkDeclaration((Declaration) child);
                } else if (child instanceof VariableAssignment) {
                    checkAssignment((VariableAssignment) child);
                }
            }
            // leave scope
            variableTypes.pop();
        }
    }

    private void checkDeclaration(Declaration declaration) {
        // Haal expression type
        ExpressionType type = evaluateExpression(declaration.expression);

        switch (declaration.property.name) {
            case "width":
                if (type != ExpressionType.PIXEL) {
                    declaration.setError("Only pixel literals can be assigned to width property");
                }
                break;
            case "color":
            case "background-color":
                if (type != ExpressionType.COLOR) {
                    declaration.setError("Only color literals can be assigned to color property");
                }
                break;
            default:
                // hier kan je andere properties toevoegen later
                break;
        }
    }

