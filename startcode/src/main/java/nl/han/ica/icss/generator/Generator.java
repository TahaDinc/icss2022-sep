package nl.han.ica.icss.generator;


import nl.han.ica.icss.ast.AST;
import nl.han.ica.icss.ast.Declaration;
import nl.han.ica.icss.ast.Stylerule;
import nl.han.ica.icss.ast.Stylesheet;

public class Generator {

    private static final String INDENT = "  ";

	public String generate(AST ast) {
        return generateStylesheet(ast.root, 0);
	}

    private String generateStylesheet(Stylesheet node, int indentLevel) {
        StringBuilder sb = new StringBuilder();
        for (Object child : node.getChildren()) {
            if (child instanceof Stylerule) {
                sb.append(generateStylerule((Stylerule) child, indentLevel));
            }
        }
        return sb.toString();
    }

    private String generateStylerule(Stylerule node, int indentLevel) {
        StringBuilder sb = new StringBuilder();
        String indent = INDENT.repeat(indentLevel);

        StringBuilder selectors = new StringBuilder();
        for (int i = 0; i < node.selectors.size(); i++) {
            if (i > 0) selectors.append(", ");
            selectors.append(node.selectors.get(i).toString());
        }

        sb.append(indent).append(selectors).append(" {\n");
        for (Object b : node.body) {
            if (b instanceof Declaration) {
                sb.append(generateDeclaration((Declaration) b, indentLevel + 1));
            }
        }
        sb.append(indent).append("}\n");
        return sb.toString();
    }

private String generateDeclaration(Declaration node, int indentLevel) {
        String indent = INDENT.repeat(indentLevel);
        String raw = node.expression.toString();
        String expr = raw.replace("[", "")
                .replace("]", "")
                .replace("|", "")
                .replace("(", "")
                .replace(")", "")
                .trim();

        if (raw.contains("Pixel literal")) {
            expr = expr.replace("Pixel literal", "").trim();
            if (!expr.endsWith("px")) expr = expr + "px";
        } else if (raw.contains("Percentage literal")) {
            expr = expr.replace("Percentage literal", "").trim();
            if (!expr.endsWith("%")) expr = expr + "%";
        } else if (raw.contains("Color literal")) {
            expr = expr.replace("Color literal", "").trim();
        } else if (raw.contains("Scalar literal")) {
            expr = expr.replace("Scalar literal", "").trim();
        } else if (raw.contains("Bool literal")) {
            expr = expr.replace("Bool literal", "").trim();
        } else if (raw.contains("VariableReference")) {
            expr = expr.replace("VariableReference", "").trim();
        }

        return indent + node.property.name + ": " + expr + ";\n";
    }
}
