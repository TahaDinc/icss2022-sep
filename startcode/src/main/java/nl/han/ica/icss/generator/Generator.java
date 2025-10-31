package nl.han.ica.icss.generator;


import nl.han.ica.icss.ast.*;

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
        String indent = INDENT.repeat(indentLevel);
        StringBuilder css = new StringBuilder(indent);

        boolean isFirstSelector = true;
        for (Object selector : node.selectors) {
            if (!isFirstSelector) css.append(", ");
            isFirstSelector = false;
            css.append(selector.toString());
        }

        css.append(" {\n");
        for (Object body : node.body) {
            if (body instanceof Declaration) {
                css.append(generateDeclaration((Declaration) body, indentLevel + 1));
            }
        }
        css.append(indent).append("}\n");
        return css.toString();
    }

    private String generateDeclaration(Declaration node, int indentLevel) {
        if (node == null || node.property == null) return "";
        String indent = INDENT.repeat(indentLevel);
        return indent + node.property.name + ": " + literalToString(node.expression) + ";\n";
    }


private String literalToString(Expression expr) {
        if (expr == null) return "";

        String raw = expr.toString();

        if (raw.contains("Pixel literal")) {
            return raw.replaceAll("Pixel literal|\\[|\\]|\\(|\\)|\\|", "").trim() + "px";
        }
        if (raw.contains("Percentage literal")) {
            return raw.replaceAll("Percentage literal|\\[|\\]|\\(|\\)|\\|", "").trim() + "%";
        }
        if (raw.contains("Color literal")) {
            return raw.replaceAll("Color literal|\\[|\\]|\\(|\\)|\\|", "").trim();
        }
        if (raw.contains("Scalar literal")) {
            return raw.replaceAll("Scalar literal|\\[|\\]|\\(|\\)|\\|", "").trim();
        }
        if (raw.contains("Bool literal")) {
            return raw.replaceAll("Bool literal|\\[|\\]|\\(|\\)|\\|", "").trim();
        }
        if (raw.contains("VariableReference")) {
            return raw.replaceAll("VariableReference|\\[|\\]|\\(|\\)|\\|", "").trim();
        }

        return raw.replaceAll("\\[|\\]|\\(|\\)|\\|", "").trim();
    }

}
