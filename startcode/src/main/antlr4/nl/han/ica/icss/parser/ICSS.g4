grammar ICSS;

//--- LEXER: ---

// IF support:
IF: 'if';
ELSE: 'else';
BOX_BRACKET_OPEN: '[';
BOX_BRACKET_CLOSE: ']';


//Literals
TRUE: 'TRUE';
FALSE: 'FALSE';
PIXELSIZE: [0-9]+ 'px';
PERCENTAGE: [0-9]+ '%';
SCALAR: [0-9]+;


//Color value takes precedence over id idents
COLOR: '#' [0-9a-f] [0-9a-f] [0-9a-f] [0-9a-f] [0-9a-f] [0-9a-f];

//Specific identifiers for id's and css classes
ID_IDENT: '#' [a-z0-9\-]+;
CLASS_IDENT: '.' [a-z0-9\-]+;

//General identifiers
LOWER_IDENT: [a-z] [a-z0-9\-]*;
CAPITAL_IDENT: [A-Z] [A-Za-z0-9_]*;

//All whitespace is skipped
WS: [ \t\r\n]+ -> skip;

//
OPEN_BRACE: '{';
CLOSE_BRACE: '}';
SEMICOLON: ';';
COLON: ':';
PLUS: '+';
MIN: '-';
MUL: '*';
ASSIGNMENT_OPERATOR: ':=';




//--- PARSER: ---
  // Stylesheet bevat meerdere assignments, stylrules of ifClauses
  stylesheet: (assignment | stylerule | ifClause)*;
  // Assignment verwacht in volgorde: CAPITAL_IDENT, ASSIGNMENT_OPERATOR, expression, SEMICOLON
  assignment: CAPITAL_IDENT ASSIGNMENT_OPERATOR expression SEMICOLON;
  // Stylerule verwacht in volgorde: selector, OPEN_BRACE, (declaration | assignment | ifClause)*, CLOSE_BRACE
  stylerule: selector OPEN_BRACE (declaration | assignment | ifClause)* CLOSE_BRACE;
  // Selector kan LOWER_IDENT, ID_IDENT of CLASS_IDENT zijn
  selector: LOWER_IDENT | ID_IDENT | CLASS_IDENT;
  // Declaration verwacht in volgorde: property(zie hieronder) LOWER_IDENT, COLON, expression, SEMICOLON
  declaration: property COLON expression SEMICOLON;
  // Property is een LOWER_IDENT
  property: LOWER_IDENT;
  // IfClause verwacht in volgorde: IF, BOX_BRACKET_OPEN, condition, BOX_BRACKET_CLOSE, OPEN_BRACE,
  // (assignment | stylerule | declaration | ifClause)*, CLOSE_BRACE, (elseClause)?
  ifClause
    : IF BOX_BRACKET_OPEN condition BOX_BRACKET_CLOSE OPEN_BRACE (assignment | stylerule | declaration | ifClause)* CLOSE_BRACE (elseClause)?
    ;
  // ElseClause verwacht in volgorde: ELSE, OPEN_BRACE,
  // (assignment | stylerule | declaration | ifClause)*, CLOSE_BRACE
  elseClause
    : ELSE OPEN_BRACE (assignment | stylerule | declaration | ifClause)* CLOSE_BRACE
    ;
  // Concdition bestaat uit een expression
  condition: expression;
  // Expression kan verschillende vormen hebben,
  // zoals rekenkundige bewerkingen, literals en variabele referenties
  expression
    // Hier wordt een expressie tussen haakjes gedefinieerd
    : '(' expression ')' #parenExpression
    // Definities voor rekenkundige bewerkingen
    | expression MUL expression #mulExpression
    | expression PLUS expression #addExpression
    | expression MIN expression #subExpression
    // Definities voor verschillende soorten literals en variabele referenties
    | COLOR #colorLiteral
    | PIXELSIZE #pixelLiteral
    | PERCENTAGE #percentageLiteral
    | SCALAR #scalarLiteral
    | CAPITAL_IDENT #variableReference
    | TRUE #trueLiteral
    | FALSE #falseLiteral;

