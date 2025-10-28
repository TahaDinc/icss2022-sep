package nl.han.ica.icss.transforms;

import nl.han.ica.datastructures.IHANLinkedList;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.BoolLiteral;
import nl.han.ica.icss.ast.literals.PercentageLiteral;
import nl.han.ica.icss.ast.literals.PixelLiteral;
import nl.han.ica.icss.ast.literals.ScalarLiteral;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class Evaluator implements Transform {

    private LinkedList<HashMap<String, Literal>> variableValues;

    public Evaluator() {
        variableValues = new LinkedList<>();
    }

    @Override
    public void apply(AST ast) {
        // initialize global scope
        variableValues.clear();
        variableValues.add(new HashMap<>());
        applyStylesheet(ast.root);
    }

    private void applyStylesheet(Stylesheet sheet) {
        // process top-level children (could be variable assignments and stylerules)
        for (int i = 0; i < sheet.getChildren().size(); i++) {
            ASTNode child = sheet.getChildren().get(i);
            if (child instanceof Stylerule) {
                applyStylerule((Stylerule) child);
            } else if (child instanceof VariableAssignment) {
                applyVariableAssignment((VariableAssignment) child);
            }
        }
    }

    private void applyStylerule(Stylerule rule) {
        // create a new scope for this stylerule that inherits current (global) values
        HashMap<String, Literal> newScope = new HashMap<>();
        if (!variableValues.isEmpty()) {
            newScope.putAll(variableValues.getLast());
        }
        variableValues.add(newScope);

        List<ASTNode> children = rule.getChildren();
        int i = 0;
        while (i < children.size()) {
            ASTNode child = children.get(i);
            if (child instanceof Declaration) {
                applyDeclaration((Declaration) child);
                i++;
            } else if (child instanceof VariableAssignment) {
                applyVariableAssignment((VariableAssignment) child);
                i++;
            } else if (child instanceof IfClause) {
                IfClause ifc = (IfClause) child;
                Literal condLit = evaluateExpression(ifc.getConditionalExpression());
                boolean takeIf = false;
                if (condLit instanceof BoolLiteral) {
                    takeIf = ((BoolLiteral) condLit).value;
                }
                // decide replacement nodes
                List<ASTNode> replacement;
                if (takeIf) {
                    replacement = ifc.body;
                } else {
                    if (ifc.elseClause != null) {
                        replacement = ifc.elseClause.body;
                    } else {
                        replacement = new java.util.ArrayList<>();
                    }
                }
                // remove the IfClause node and insert the replacement nodes at position i
                children.remove(i);
                if (!replacement.isEmpty()) {
                    children.addAll(i, replacement);
                }
                // do not increment i here so newly inserted nodes are processed
            } else {
                // generic traversal for any other ASTNode types: try to process their children if present
                if (!child.getChildren().isEmpty()) {
                    // naive recursive handling: if it's a stylerule-like node we could call appropriate methods,
                    // but here just iterate its children by temporarily applying simple transformations
                    for (ASTNode grandchild : child.getChildren()) {
                        if (grandchild instanceof Declaration) {
                            applyDeclaration((Declaration) grandchild);
                        } else if (grandchild instanceof VariableAssignment) {
                            applyVariableAssignment((VariableAssignment) grandchild);
                        }
                    }
                }
                i++;
            }
        }

        // pop scope
        variableValues.removeLast();
    }

    private void applyDeclaration(Declaration declaration) {
        Literal evaluated = evaluateExpression(declaration.expression);
        declaration.expression = evaluated;
    }

    private void applyVariableAssignment(VariableAssignment assignment) {
        Literal evaluated = evaluateExpression(assignment.expression);
        if (!variableValues.isEmpty()) {
            variableValues.getLast().put(String.valueOf(assignment.name), evaluated);
        } else {
            HashMap<String, Literal> global = new HashMap<>();
            global.put(String.valueOf(assignment.name), evaluated);
            variableValues.add(global);
        }
    }

    private Literal evaluateExpression(Expression expression) {
        if (expression instanceof Literal) {
            return (Literal) expression;
        } else if (expression instanceof AddOperation) {
            return evaluateAddOperation((AddOperation) expression);
        } else if (expression instanceof SubtractOperation) {
            return evaluateSubtractOperation((SubtractOperation) expression);
        } else if (expression instanceof MultiplyOperation) {
            return evaluateMultiplyOperation((MultiplyOperation) expression);
        } else if (expression instanceof VariableReference) {
            return lookupVariable(((VariableReference) expression).name);
        } else {
            // fallback: return null to indicate unable to evaluate
            return null;
        }
    }

    private Literal lookupVariable(String name) {
        if (variableValues == null) return null;
        for (int i = variableValues.size() - 1; i >= 0; i--) {
            HashMap<String, Literal> scope = variableValues.get(i);
            if (scope.containsKey(name)) return scope.get(name);
        }
        return null;
    }

    private PixelLiteral evaluateAddOperation(AddOperation expression) {
        Literal left = evaluateExpression((Expression) expression.lhs);
        Literal right = evaluateExpression((Expression) expression.rhs);

        if (left instanceof PixelLiteral && right instanceof PixelLiteral) {
            int resultValue = ((PixelLiteral) left).value + ((PixelLiteral) right).value;
            return new PixelLiteral(resultValue);
        }
        // fallback behavior: if one is pixel and other is scalar/percentage try to coerce scalar to pixels by simple addition
        if (left instanceof PixelLiteral && right instanceof ScalarLiteral) {
            int resultValue = ((PixelLiteral) left).value + ((ScalarLiteral) right).value;
            return new PixelLiteral(resultValue);
        }
        if (left instanceof ScalarLiteral && right instanceof PixelLiteral) {
            int resultValue = ((ScalarLiteral) left).value + ((PixelLiteral) right).value;
            return new PixelLiteral(resultValue);
        }
        // unknown types: return left if exists
        return left instanceof PixelLiteral ? (PixelLiteral) left : (right instanceof PixelLiteral ? (PixelLiteral) right : null);
    }

    private PixelLiteral evaluateSubtractOperation(SubtractOperation expression) {
        Literal left = evaluateExpression((Expression) expression.lhs);
        Literal right = evaluateExpression((Expression) expression.rhs);

        if (left instanceof PixelLiteral && right instanceof PixelLiteral) {
            int resultValue = ((PixelLiteral) left).value - ((PixelLiteral) right).value;
            return new PixelLiteral(resultValue);
        }
        if (left instanceof PixelLiteral && right instanceof ScalarLiteral) {
            int resultValue = ((PixelLiteral) left).value - ((ScalarLiteral) right).value;
            return new PixelLiteral(resultValue);
        }
        if (left instanceof ScalarLiteral && right instanceof PixelLiteral) {
            int resultValue = ((ScalarLiteral) left).value - ((PixelLiteral) right).value;
            return new PixelLiteral(resultValue);
        }
        return left instanceof PixelLiteral ? (PixelLiteral) left : null;
    }

    private Literal evaluateMultiplyOperation(MultiplyOperation expression) {
        Literal left = evaluateExpression((Expression) expression.lhs);
        Literal right = evaluateExpression((Expression) expression.rhs);

        // scalar * pixel or pixel * scalar
        if (left instanceof ScalarLiteral && right instanceof PixelLiteral) {
            int resultValue = ((ScalarLiteral) left).value * ((PixelLiteral) right).value;
            return new PixelLiteral(resultValue);
        }
        if (left instanceof PixelLiteral && right instanceof ScalarLiteral) {
            int resultValue = ((PixelLiteral) left).value * ((ScalarLiteral) right).value;
            return new PixelLiteral(resultValue);
        }
        if (left instanceof ScalarLiteral && right instanceof ScalarLiteral) {
            return new ScalarLiteral(((ScalarLiteral) left).value * ((ScalarLiteral) right).value);
        }
        // fallback
        return left != null ? left : right;
    }
}