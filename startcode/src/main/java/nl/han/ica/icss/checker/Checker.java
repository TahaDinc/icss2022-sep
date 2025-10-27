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

    private void checkAssignment(VariableAssignment child) {
        ExpressionType type = evaluateExpression(child.expression);
        variableTypes.peek().put(child.name.name, type);
    }

    private ExpressionType evaluateExpression(Expression expression) {
        if (expression instanceof ColorLiteral) {
            return ExpressionType.COLOR;
        } else if (expression instanceof PixelLiteral) {
            return ExpressionType.PIXEL;
        } else if (expression instanceof ScalarLiteral) {
            return ExpressionType.SCALAR;
        } else if (expression instanceof PercentageLiteral) {
            return ExpressionType.PERCENTAGE;
        } else if (expression instanceof VariableReference) {
            String varName = ((VariableReference) expression).name;
            for (HashMap<String, ExpressionType> scope : variableTypes) {
                if (scope.containsKey(varName)) {
                    return scope.get(varName);
                }
            }
            expression.setError("Variable " + varName + " is not defined");
            return null;
        } else if (expression instanceof Operation) {
            Operation op = (Operation) expression;
            // attempt to retrieve operands (lhs / rhs)
            Expression leftExpr = op.lhs;
            Expression rightExpr = op.rhs;
            ExpressionType left = evaluateExpression(leftExpr);
            ExpressionType right = evaluateExpression(rightExpr);

            if (left == null || right == null) return null;

            // CH03: Colors not allowed in operations
            if (left == ExpressionType.COLOR || right == ExpressionType.COLOR) {
                expression.setError("Colors cannot be used in operations");
                return null;
            }

            // determine operator and check rules using concrete operation classes
            if (op instanceof AddOperation || op instanceof SubtractOperation) {
                // CH02: operands of + and - must be same type
                if (left != right) {
                    expression.setError("Operands of + and - must be of the same type");
                    return null;
                }
                return left;
            } else if (op instanceof MultiplyOperation) {
                // CH02: at least one operand must be scalar
                if (left != ExpressionType.SCALAR && right != ExpressionType.SCALAR) {
                    expression.setError("At least one operand of * must be a scalar");
                    return null;
                }
                // result type: if one side is scalar, result is the other type
                if (left == ExpressionType.SCALAR) return right;
                return left;
            } else {
                // other operators if present
                return null;
            }
        }
        return null;
    }
}