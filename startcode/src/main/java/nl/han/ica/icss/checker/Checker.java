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


    private IHANLinkedList<HashMap<String, ExpressionType>> variableTypes;

    public void check(AST ast) {
        // variableTypes = new HANLinkedList<>();

    }

    
}
