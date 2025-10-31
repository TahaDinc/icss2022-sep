package nl.han.ica.icss.checker;

import javafx.scene.paint.Color;
import nl.han.ica.datastructures.IHANLinkedList;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;
import nl.han.ica.icss.ast.types.ExpressionType;

import java.util.HashMap;
import java.util.LinkedList;

public class Checker {
    // stack van scopes met variabele namen en hun types
    private LinkedList<HashMap<String, ExpressionType>> variableTypes = new LinkedList<>();

    public void check(AST ast) {
        // initialiseer de globale scope
        variableTypes = new LinkedList<>();
        // push een nieuwe scope voor de globale variabelen
        variableTypes.push(new HashMap<>());
        // laat de root van de AST-boom controleren
        checkStylesheet(ast.root);
    }

    private void checkStylesheet(Stylesheet sheet) {
        // loop door alle kinderen van stylesheet
        for (ASTNode child : sheet.getChildren()) {
            // als de kind een stylerule is, check de stylerule
            if (child instanceof Stylerule) {
                checkStylerule((Stylerule) child);
            // als de kind een variabele toewijzing is, check de toewijzing
            } else if (child instanceof VariableAssignment) {
                checkAssignment((VariableAssignment) child);
            }
        }
    }

    private void checkStylerule(Stylerule rule) {
        // push een nieuwe scope voor de stylerule
        variableTypes.push(new HashMap<>());
        // loop door alle kinderen van stylerule
        for (ASTNode child : rule.getChildren()) {
            // als de kind een declaratie is, check de declaratie
            if (child instanceof Declaration) {
                checkDeclaration((Declaration) child);
            // als de kind een variabele toewijzing is, check de toewijzing
            } else if (child instanceof VariableAssignment) {
                checkAssignment((VariableAssignment) child);
            // als de kind een if-clause is, check de if-clause
            } else if (child instanceof IfClause) {
                checkIfClause((IfClause) child);
            }
        }
        // pop de scope van de stylerule
        variableTypes.pop();
    }

    private void checkIfClause(IfClause ifClause) {
        // controleer of de conditie van if-clause een boolean is
        ExpressionType conditionType = evaluateExpression(ifClause.conditionalExpression);
        if (conditionType != ExpressionType.BOOL) {
            ifClause.setError("If-clause condition must be a boolean literal");
        }
        // push een nieuwe scope voor de if-clause
        variableTypes.push(new HashMap<>());
        // loop door alle kinderen van if-clause
        for (ASTNode child : ifClause.body) {
            // als de kind een declaratie is, check de declaratie
            if (child instanceof Declaration) {
                checkDeclaration((Declaration) child);
            // als de kind een variabele toewijzing is, check de toewijzing
            } else if (child instanceof VariableAssignment) {
                checkAssignment((VariableAssignment) child);
            }
        }
        // pop de scope van de if-clause
        variableTypes.pop();

        // zelfde als de if-clause
        if (ifClause.elseClause != null) {
            variableTypes.push(new HashMap<>());
            for (ASTNode child : ifClause.elseClause.body) {
                if (child instanceof Declaration) {
                    checkDeclaration((Declaration) child);
                } else if (child instanceof VariableAssignment) {
                    checkAssignment((VariableAssignment) child);
                }
            }
            variableTypes.pop();
        }
    }

    private void checkDeclaration(Declaration declaration) {
        // controleer of de waarde van de declaratie overeenkomt met het type van de property
        ExpressionType type = evaluateExpression(declaration.expression);

        // controleer of de property overeenkomt met het type van de waarde
        if (declaration.property.name.equals("width")) {
            if (type != ExpressionType.PIXEL) {
                declaration.setError("Only pixel literals can be assigned to width property");
            }
        } else if (declaration.property.name.equals("color") || declaration.property.name.equals("background-color")) {
            if (type != ExpressionType.COLOR) {
                declaration.setError("Only color literals can be assigned to color property");
            }
        }
    }

    private void checkAssignment(VariableAssignment child) {
        // evalueer de expressie van de variabele toewijzing
        ExpressionType type = evaluateExpression(child.expression);
        // sla de variabele naam en type op in de huidige scope
        variableTypes.peek().put(child.name.name, type);
    }

    private ExpressionType evaluateExpression(Expression expression) {
        // bepaal het type van de expressie
        if (expression instanceof ColorLiteral) {
            return ExpressionType.COLOR;
        } else if (expression instanceof PixelLiteral) {
            return ExpressionType.PIXEL;
        } else if (expression instanceof ScalarLiteral) {
            return ExpressionType.SCALAR;
        } else if (expression instanceof PercentageLiteral) {
            return ExpressionType.PERCENTAGE;
        } else if (expression instanceof BoolLiteral) {
            return ExpressionType.BOOL;
        // als de expressie een variabele referentie is
        } else if (expression instanceof VariableReference) {
            // zoek de variabele op in de scopes
            String varName = ((VariableReference) expression).name;
            // kijk eerst in de huidige scope
            HashMap<String, ExpressionType> currentScope = variableTypes.peek();
            // kijk of de variabele in de huidige scope zit
            if (currentScope != null && currentScope.containsKey(varName)) {
                return currentScope.get(varName);
            }
            // als de variabele niet in de huidige scope zit, kijk in de andere scopes
            HashMap<String, ExpressionType> globalScope = variableTypes.getLast();
            // loop door alle scopes
            for (HashMap<String, ExpressionType> scope : variableTypes) {
                // sla de huidige scope over
                if (scope == currentScope) continue;
                // kijk of de variabele in de scope zit
                if (scope.containsKey(varName)) {
                    // als we in de globale scope zitten, is de variabele toegankelijk
                    if (scope == globalScope) {
                        return scope.get(varName);
                    // anders is de variabele niet toegankelijk
                    } else {
                        expression.setError("Variable " + varName + " is not accessible in this selector");
                        return scope.get(varName);
                    }
                }
            }
            // als de variabele niet gevonden is, geef een foutmelding
            expression.setError("Variable " + varName + " is not defined");
            return null;
        // als de expressie een operatie is
        } else if (expression instanceof Operation) {
            // evalueer de linker en rechter operand
            Operation op = (Operation) expression;
            // haal de linker en rechter expressies op
            Expression leftExpr = op.lhs;
            Expression rightExpr = op.rhs;
            // evalueer de linker en rechter expressies
            ExpressionType left = evaluateExpression(leftExpr);
            ExpressionType right = evaluateExpression(rightExpr);

            // als een van de types null is, return null
            if (left == null || right == null) return null;

            // kleuren kunnen niet gebruikt worden in operaties
            if (left == ExpressionType.COLOR || right == ExpressionType.COLOR) {
                expression.setError("Colors cannot be used in operations");
                return null;
            }

            // bepaal het resultaat type op basis van de operatie
            // bijvoorbeeld: bij + en - moeten de types gelijk zijn, zoals PIXEL + PIXEL
            if (op instanceof AddOperation || op instanceof SubtractOperation) {
                if (left != right) {
                    expression.setError("Operands of + and - must be of the same type");
                    return null;
                }
                return left;
            // bij * moet een van de types SCALAR zijn
            // bijvoorbeeld: SCALAR * PIXEL or PIXEL * SCALAR
            } else if (op instanceof MultiplyOperation) {
                if (left != ExpressionType.SCALAR && right != ExpressionType.SCALAR) {
                    expression.setError("At least one operand of * must be a scalar");
                    return null;
                }
                if (left == ExpressionType.SCALAR) return right;
                return left;
            // anders als het een onbekende operatie is, returnt hij null
            } else {
                return null;
            }
        }
        // als het een onbekende expressie is, returnt hij null
        return null;
    }
}
