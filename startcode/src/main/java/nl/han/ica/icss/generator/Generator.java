package nl.han.ica.icss.generator;


import nl.han.ica.icss.ast.*;

public class Generator {
    // indentie voor geneste regels
    private static final String INDENT = "  ";

	public String generate(AST ast) {
        // start met het genereren van de stylesheet
        return generateStylesheet(ast.root, 0);
	}

    // genereer de stylesheet door alle stylerules te verwerken
    private String generateStylesheet(Stylesheet node, int indentLevel) {
        // bouw de CSS-string op
        StringBuilder sb = new StringBuilder();
        // loop door alle kinderen van de stylesheet
        for (Object child : node.getChildren()) {
            // als het kind een stylerule is, genereer de stylerule
            if (child instanceof Stylerule) {
                // voeg de gegenereerde stylerule toe aan de CSS-string
                sb.append(generateStylerule((Stylerule) child, indentLevel));
            }
        }
        // return de volledige CSS-string
        return sb.toString();
    }

    // genereer een stylerule met zijn selectors en body
    private String generateStylerule(Stylerule node, int indentLevel) {
        // bepaal de indentatie voor de huidige regel
        String indent = INDENT.repeat(indentLevel);
        // bouw de CSS-string voor de stylerule op
        StringBuilder css = new StringBuilder(indent);

        // voeg alle selectors toe, gescheiden door komma's
        // wat doet de isFirstSelector variabele hier?
        // Het zorgt ervoor dat er geen komma voor de eerste selector wordt toegevoegd.
        boolean isFirstSelector = true;
        // loop door alle selectors van de stylerule
        for (Object selector : node.selectors) {
            // als het niet de eerste selector is, voeg een komma en spatie toe
            if (!isFirstSelector) css.append(", ");
            // anders, zet de vlag op false
            isFirstSelector = false;
            // voeg de selector toe aan de CSS-string
            css.append(selector.toString());
        }

        // voeg de openingshaakje toe voor de body van de stylerule
        css.append(" {\n");
        // loop door alle body-elementen van de stylerule
        for (Object body : node.body) {
            // als het body-element een declaratie is, genereer de declaratie
            if (body instanceof Declaration) {
                // voeg de gegenereerde declaratie toe aan de CSS-string
                css.append(generateDeclaration((Declaration) body, indentLevel + 1));
            }
        }
        // voeg de sluitingshaakje toe voor de stylerule
        css.append(indent).append("}\n");
        // return de volledige CSS-string voor de stylerule
        return css.toString();
    }

    private String generateDeclaration(Declaration node, int indentLevel) {
        // controleer of de declaratie en zijn eigenschappen niet null zijn
        if (node == null || node.property == null) return "";
        // anders bepaal de indentatie voor de huidige regel
        String indent = INDENT.repeat(indentLevel);
        // return de CSS-string voor de declaratie in het juiste formaat
        return indent + node.property.name + ": " + literalToString(node.expression) + ";\n";
    }

    // converteer een expressie naar een stringrepresentatie
    private String literalToString(Expression expr) {
        // controleer of de expressie null is
        if (expr == null) return "";
        // haal de raw string van de expressie op
        String raw = expr.toString();
        // bepaal het type van de expressie en formatteer deze overeenkomstig
        // bijvoorbeeld: Pixel literal [ 100 ] wordt 100px
        // de replaceAll verwijdert de de volgende strings en tekens:
        // Pixel literal, Percentage literal, Color literal, Scalar literal, Bool literal, VariableReference
        // en de vierkante haken [], ronde haken () en verticale strepen |
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
        // als het geen van de bovenstaande types is, return de raw string zonder speciale tekens
        return raw.replaceAll("\\[|\\]|\\(|\\)|\\|", "").trim();
    }
}
