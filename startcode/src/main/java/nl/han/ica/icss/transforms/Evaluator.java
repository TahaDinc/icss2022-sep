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
    // stack van scopes met variabele namen en hun waarden
    private LinkedList<HashMap<String, Literal>> variableValues;

    public Evaluator() {
        // initialiseer variableValues
        variableValues = new LinkedList<>();
    }

    @Override
    public void apply(AST ast) {
        // reset variableValues voor elke nieuwe apply-aanroep
        variableValues.clear();
        // push een nieuwe scope voor globale variabelen
        variableValues.add(new HashMap<>());
        // start met het toepassen van de stylesheet
        applyStylesheet(ast.root);
    }

    private void applyStylesheet(Stylesheet sheet) {
        // loop door alle kinderen van stylesheet
        for (int i = 0; i < sheet.getChildren().size(); i++) {
            // haal het kind op
            ASTNode child = sheet.getChildren().get(i);
            // als het kind een stylerule is, pas de stylerule toe
            if (child instanceof Stylerule) {
                applyStylerule((Stylerule) child);
            // als het kind een variabele toewijzing is, pas de toewijzing toe
            } else if (child instanceof VariableAssignment) {
                applyVariableAssignment((VariableAssignment) child);
            }
        }
    }

    private void applyStylerule(Stylerule rule) {
        // push nieuwe scope
        HashMap<String, Literal> newScope = new HashMap<>();
        // kopieer variabelen uit de bovenliggende scope
        if (!variableValues.isEmpty()) {
            newScope.putAll(variableValues.getLast());
        }
        // voeg nieuwe scope toe aan stack
        variableValues.add(newScope);

        // loop door alle kinderen van stylerule
        List<ASTNode> children = rule.getChildren();
        for (ASTNode child : children) {
            // als het kind een declaratie is, pas de declaratie toe
            if (child instanceof Declaration) {
                applyDeclaration((Declaration) child);
            // als het kind een variabele toewijzing is, pas de toewijzing toe
            } else if (child instanceof VariableAssignment) {
                applyVariableAssignment((VariableAssignment) child);
            // als het kind een if-clause is, evalueer de if-clause
            } else if (child instanceof IfClause) {
                evaluateIfClause((IfClause) child, rule);
            }
        }

        // pop scope
        variableValues.removeLast();
    }

    private void evaluateIfClause(IfClause clause, Stylerule parentRule) {
        // evalueer de conditie van de if-clause
        Literal conditionResult = evaluateExpression(clause.conditionalExpression);
        // als de conditie geen BoolLiteral is, return
        if (!(conditionResult instanceof BoolLiteral)) return;

        // afhankelijk van de conditie, voer de body of else-body uit
        boolean condition = ((BoolLiteral) conditionResult).value;
        // selecteer de juiste body
        List<ASTNode> targetBody = condition
                // als waar, gebruik de body van if-clause
                ? clause.body
                // als onwaar, gebruik de body van else-clause indien aanwezig
                : (clause.elseClause != null ? clause.elseClause.body : null);
        // als er geen body is om uit te voeren, return
        if (targetBody == null) return;

        // loop door alle nodes in de geselecteerde body
        for (ASTNode node : targetBody) {
            // als de node een declaratie is, pas de declaratie toe en voeg toe aan parent rule
            // waarom toevoegen aan parent rule? Omdat na evaluatie van
            // if-clause de declaraties in de parent rule moeten komen te staan
            if (node instanceof Declaration) {
                applyDeclaration((Declaration) node);
                parentRule.body.add(node);
            // als de node een variabele toewijzing is, pas de toewijzing toe
            } else if (node instanceof VariableAssignment) {
                applyVariableAssignment((VariableAssignment) node);
            // als de node een if-clause is, evalueer de nested if-clause d.m.v. recursie
            } else if (node instanceof IfClause) {
                // gaat in recursie om geneste if-clauses te evalueren
                evaluateIfClause((IfClause) node, parentRule);
            }
        }
        // maak de if-clause leeg na evaluatie
        clause.body.clear();
        // zet else-clause op null na evaluatie
        clause.elseClause = null;
    }

    private void applyDeclaration(Declaration declaration) {
        // evalueer de expressie van de declaratie
        Literal evaluated = evaluateExpression(declaration.expression);
        // stel de expressie van de declaratie in op de geëvalueerde waarde
        declaration.expression = evaluated;
    }

    private void applyVariableAssignment(VariableAssignment assignment) {
        // evalueer de expressie van de toewijzing
        Literal evaluated = evaluateExpression(assignment.expression);
        // als variableValues niet leeg is, voeg de variabele toe aan de meest recente scope
        if (!variableValues.isEmpty()) {
            variableValues.getLast().put(assignment.name.name, evaluated);
        // anders, maak een nieuwe scope en voeg de variabele toe
        } else {
            HashMap<String, Literal> global = new HashMap<>();
            global.put(assignment.name.name, evaluated);
            variableValues.add(global);
        }
    }

    private Literal evaluateExpression(Expression expression) {
        // evalueer de expressie op basis van het type
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
            return null;
        }
    }

    private Literal lookupVariable(String name) {
        // doorloop alle scopes van achter naar voren
        // de reden waarom ik bij deze for-lus bij het einde van de lijst begin
        // komt omdat de meest recent toegevoegde scope (de meest lokale) als laatste in de lijst staat
        for (int i = variableValues.size() - 1; i >= 0; i--) {
            HashMap<String, Literal> scope = variableValues.get(i);
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        // als variabele niet gevonden is, return null
        return null;
    }

    private PixelLiteral evaluateAddOperation(AddOperation expression) {
        // evalueer linker- en rechterzijde van de som
        Literal left = evaluateExpression((Expression) expression.lhs);
        Literal right = evaluateExpression((Expression) expression.rhs);

        // als beide zijden pixel literals zijn, tel ze op
        if (left instanceof PixelLiteral && right instanceof PixelLiteral) {
            int resultValue = ((PixelLiteral) left).value + ((PixelLiteral) right).value;
            return new PixelLiteral(resultValue);
        }
        // als één zijde een pixel literal is en de andere een scalar literal, tel ze op
        if (left instanceof PixelLiteral && right instanceof ScalarLiteral) {
            int resultValue = ((PixelLiteral) left).value + ((ScalarLiteral) right).value;
            return new PixelLiteral(resultValue);
        }
        // als één zijde een scalar literal is en de andere een pixel literal, tel ze op
        if (left instanceof ScalarLiteral && right instanceof PixelLiteral) {
            int resultValue = ((ScalarLiteral) left).value + ((PixelLiteral) right).value;
            return new PixelLiteral(resultValue);
        }
        // als geen van bovenstaande, return de pixel literal als die er is
        return left instanceof PixelLiteral ? (PixelLiteral) left : (right instanceof PixelLiteral ? (PixelLiteral) right : null);
    }

    private PixelLiteral evaluateSubtractOperation(SubtractOperation expression) {
        // evalueer linker- en rechterzijde van de aftrekking
        Literal left = evaluateExpression((Expression) expression.lhs);
        Literal right = evaluateExpression((Expression) expression.rhs);

        // als beide zijden pixel literals zijn, trek ze af
        if (left instanceof PixelLiteral && right instanceof PixelLiteral) {
            int resultValue = ((PixelLiteral) left).value - ((PixelLiteral) right).value;
            return new PixelLiteral(resultValue);
        }
        // als één zijde een pixel literal is en de andere een scalar literal, trek ze af
        if (left instanceof PixelLiteral && right instanceof ScalarLiteral) {
            int resultValue = ((PixelLiteral) left).value - ((ScalarLiteral) right).value;
            return new PixelLiteral(resultValue);
        }
        // als één zijde een scalar literal is en de andere een pixel literal, trek ze af
        if (left instanceof ScalarLiteral && right instanceof PixelLiteral) {
            int resultValue = ((ScalarLiteral) left).value - ((PixelLiteral) right).value;
            return new PixelLiteral(resultValue);
        }
        // als geen van bovenstaande, return de pixel literal als die er is
        return left instanceof PixelLiteral ? (PixelLiteral) left : null;
    }

    private Literal evaluateMultiplyOperation(MultiplyOperation expression) {
        // evalueer linker- en rechterzijde van de vermenigvuldiging
        Literal left = evaluateExpression((Expression) expression.lhs);
        Literal right = evaluateExpression((Expression) expression.rhs);

        // als één zijde een scalar literal is en de andere een pixel literal, vermenigvuldig ze
        if (left instanceof ScalarLiteral && right instanceof PixelLiteral) {
            int resultValue = ((ScalarLiteral) left).value * ((PixelLiteral) right).value;
            return new PixelLiteral(resultValue);
        }
        // als één zijde een pixel literal is en de andere een scalar literal, vermenigvuldig ze
        if (left instanceof PixelLiteral && right instanceof ScalarLiteral) {
            int resultValue = ((PixelLiteral) left).value * ((ScalarLiteral) right).value;
            return new PixelLiteral(resultValue);
        }
        // als beide zijden scalar literals zijn, vermenigvuldig ze
        if (left instanceof ScalarLiteral && right instanceof ScalarLiteral) {
            return new ScalarLiteral(((ScalarLiteral) left).value * ((ScalarLiteral) right).value);
        }
        // als geen van bovenstaande, return de pixel literal als die er is
        return left != null ? left : right;
    }
}