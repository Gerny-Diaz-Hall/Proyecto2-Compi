/*
 * Lexer.flex - Especificacion del analizador lexico (scanner) para JFlex.
 *
 * OBJETIVO      : reconocer los tokens del lenguaje, registrarlos en la tabla de simbolos
 *                 que les corresponde y entregarlos uno por uno al parser generado por CUP.
 * ENTRADA       : archivo fuente codificado en UTF-8.
 * SALIDA        : objetos Symbol (id del token, linea, columna, lexema) por cada llamada a
 *                 next_token(); ademas la lista de errores lexicos y la
 *                 tabla de simbolos llena (getTablaSimbolos()).
 * RESTRICCIONES : - Los literales numericos NO llevan signo: el '-' siempre es el token RESTA
 *                   y la gramatica decide si es resta o negativo unario.
 *                 - Un flotante necesita digitos a ambos lados del punto (2.20 si; 2. y .5 no).
 *                 - Un caracter es exactamente un simbolo entre comillas simples ('a').
 *                 - Las cadenas no pueden ocupar mas de una linea.
 *                 - Lineas y columnas se reportan desde 1 (JFlex las cuenta desde 0).
 */

package compilador;
import java_cup.runtime.Symbol;

%%

/* ------------------------------ Opciones ------------------------------ */
/* %public/%class : genera la clase publica Lexer                          */
/* %unicode       : acepta todo Unicode (necesario para ¿: ʃ: є: Ͱ » λ θ Σ) */
/* %line/%column  : JFlex lleva la linea (yyline) y columna (yycolumn)      */
/* %cup           : el scanner implementa la interfaz Scanner de CUP        */

%public
%class Lexer
%unicode
%line
%column
%cup

%eofval{
    return new Symbol(sym.EOF, yyline + 1, yycolumn + 1, "fin de archivo");
%eofval}

%{
    /* Tablas de simbolos y lista de todos los tokens encontrados */
    private final TablaSimbolos tablaSimbolos = new TablaSimbolos();

    /* Errores lexicos encontrados (el scanner no se detiene si se los encuentra) */
    private final java.util.List<String> errores = new java.util.ArrayList<>();

    /* Acceso a los resultados para que Main escriba los reportes */
    public TablaSimbolos getTablaSimbolos() { return tablaSimbolos; }
    public java.util.List<String> getErrores() { return errores; }
    
    /* Nombres cortos de las cuatro tablas de simbolos */
    private static final TablaSimbolos.Tipo RESERVADA = TablaSimbolos.Tipo.RESERVADAS;
    private static final TablaSimbolos.Tipo IDENT     = TablaSimbolos.Tipo.IDENTIFICADORES;
    private static final TablaSimbolos.Tipo LITERAL   = TablaSimbolos.Tipo.LITERALES;
    private static final TablaSimbolos.Tipo OPERADOR  = TablaSimbolos.Tipo.OPERADORES;

    /*
     * tok - Crea un token, lo registra en su tabla y lo entrega al parser.
     * ENTRADA : id (constante de sym), nombre del token y tabla de simbolos destino.
     * SALIDA  : Symbol con linea y columna en base 1 y el lexema como valor.
     */
    private Symbol tok(int id, String nombre, TablaSimbolos.Tipo tabla) {
        tablaSimbolos.registrar(tabla, nombre, yytext(), yyline + 1, yycolumn + 1);
        return new Symbol(id, yyline + 1, yycolumn + 1, yytext());
    }

    /*
     * error - Registra un error lexico y permite que el scanner continue (modo panico).
     * ENTRADA : descripcion del error; el lexema invalido se toma de yytext().
     * SALIDA  : agrega a la lista un mensaje con linea, columna, descripcion y lexema.
     */
    private void error(String mensaje) {
        errores.add("Error lexico en linea " + (yyline + 1) + ", columna " + (yycolumn + 1)
                    + ": " + mensaje + " -> '" + yytext().replace("\n", "\\n").replace("\r", "") + "'");
    }
%}

/* ------------------------------- Macros ------------------------------- */

/* Espacios en blanco, saltos de linea se ignoran */
ESPACIO      = [ \t\r\n\f\uFEFF]+

/* Identificador: inicia con letra o '_', sigue con letras, digitos o '_' */
LETRA        = [a-zA-Z_]
DIGITO       = [0-9]
ID           = {LETRA}({LETRA}|{DIGITO})*

/* Literales SIN signo: el "-" es un token aparte que maneja la gramatica  */
ENTERO       = {DIGITO}+
FLOTANTE     = {DIGITO}+"."{DIGITO}+
CARACTER     = "'"[^'\r\n]"'"
CADENA       = \"[^\"\r\n]*\"

/* Comentarios: linea = | ... fin de linea ;  bloque = ¡ ... ! */
COMENT_LINEA      = "|"[^\r\n]*
COMENT_BLOQUE     = \u00A1[^!]*"!"
COMENT_SIN_CERRAR = \u00A1[^!]*

/* Simbolos especiales del lenguaje (glifo -> codigo unicode, esto se hizo para evitar problemas de codificacion) */
BLOQUE_ABRE   = \u00BF":"       /* ¿:  */
BLOQUE_CIERRA = ":?"             /* :?  */
INDICE_ABRE   = \u0283":"        /* ʃ:  */
INDICE_CIERRA = ":"\u0285        /* :ʅ  */
PAREN_ABRE    = \u0454":"        /* є:  */
PAREN_CIERRA  = ":"\u044D        /* :э  */
ASIGNACION    = \u0370           /* Ͱ   */
FIN_SENT      = \u00BB           /* »   */
OP_AND        = \u03BB           /* λ   */
OP_OR         = \u03B8           /* θ   */
OP_NOT        = \u03A3           /* Σ   */

%%

/* ------------------------------- Reglas ------------------------------- */
/* JFlex siempre toma el lexema MAS LARGO; si dos reglas reconocen el mismo  */
/* largo, gana la que aparece primero. Por eso las reservadas van antes que  */
/* los identificadores, y "++", "//", "<=" antes que "+", "/", "<".          */

/* ---- Espacios y comentarios: no generan token ---- */
{ESPACIO}            { /* se ignora */ }
{COMENT_LINEA}       { /* se ignora */ }
{COMENT_BLOQUE}      { /* se ignora */ }
{COMENT_SIN_CERRAR}  { error("comentario multilinea sin cerrar (falta '!')"); }

/* ---- Palabras reservadas ---- */
"val"        { return tok(sym.VAL,       "VAL",       RESERVADA); }
"int"        { return tok(sym.INT,       "INT",       RESERVADA); }
"float"      { return tok(sym.FLOAT,     "FLOAT",     RESERVADA); }
"boolean"    { return tok(sym.BOOLEAN,   "BOOLEAN",   RESERVADA); }
"char"       { return tok(sym.CHAR,      "CHAR",      RESERVADA); }
"string"     { return tok(sym.STRING,    "STRING",    RESERVADA); }
"void"       { return tok(sym.VOID,      "VOID",      RESERVADA); }
"principal"  { return tok(sym.PRINCIPAL, "PRINCIPAL", RESERVADA); }
"true"       { return tok(sym.TRUE,      "TRUE",      RESERVADA); }
"false"      { return tok(sym.FALSE,     "FALSE",     RESERVADA); }
"if"         { return tok(sym.IF,        "IF",        RESERVADA); }
"elif"       { return tok(sym.ELIF,      "ELIF",      RESERVADA); }
"else"       { return tok(sym.ELSE,      "ELSE",      RESERVADA); }
"while"      { return tok(sym.WHILE,     "WHILE",     RESERVADA); }
"for"        { return tok(sym.FOR,       "FOR",       RESERVADA); }
"return"     { return tok(sym.RETURN,    "RETURN",    RESERVADA); }
"break"      { return tok(sym.BREAK,     "BREAK",     RESERVADA); }
"read"       { return tok(sym.READ,      "READ",      RESERVADA); }
"write"      { return tok(sym.WRITE,     "WRITE",     RESERVADA); }

/* ---- Identificadores y literales ---- */
/* FLOTANTE va antes que ENTERO para que 2.20 sea un solo token */
{ID}         { return tok(sym.IDENTIFICADOR,    "IDENTIFICADOR",    IDENT);   }
{FLOTANTE}   { return tok(sym.LITERAL_FLOTANTE, "LITERAL_FLOTANTE", LITERAL); }
{ENTERO}     { return tok(sym.LITERAL_ENTERO,   "LITERAL_ENTERO",   LITERAL); }
{CARACTER}   { return tok(sym.LITERAL_CARACTER, "LITERAL_CARACTER", LITERAL); }
{CADENA}     { return tok(sym.LITERAL_CADENA,   "LITERAL_CADENA",   LITERAL); }

/* ---- Operadores aritmeticos ---- */
/* "++", "--" y "//" van antes que "+", "-" y "/" para que se tome el mas largo */
"++"         { return tok(sym.INC,     "INC",     OPERADOR); }
"--"         { return tok(sym.DEC,     "DEC",     OPERADOR); }
"+"          { return tok(sym.SUMA,    "SUMA",    OPERADOR); }
"-"          { return tok(sym.RESTA,   "RESTA",   OPERADOR); }
"*"          { return tok(sym.MULT,    "MULT",    OPERADOR); }
"//"         { return tok(sym.DIV_ENT, "DIV_ENT", OPERADOR); }
"/"          { return tok(sym.DIV,     "DIV",     OPERADOR); }
"%"          { return tok(sym.MOD,     "MOD",     OPERADOR); }
"^"          { return tok(sym.POT,     "POT",     OPERADOR); }

/* ---- Operadores relacionales ---- */
"<="         { return tok(sym.MENOR_IG,  "MENOR_IG",  OPERADOR); }
">="         { return tok(sym.MAYOR_IG,  "MAYOR_IG",  OPERADOR); }
"=="         { return tok(sym.IGUAL,     "IGUAL",     OPERADOR); }
"!="         { return tok(sym.DIFERENTE, "DIFERENTE", OPERADOR); }
"<"          { return tok(sym.MENOR,     "MENOR",     OPERADOR); }
">"          { return tok(sym.MAYOR,     "MAYOR",     OPERADOR); }

/* ---- Operadores logicos: conjuncion λ, disyuncion θ, negacion Σ ---- */
{OP_AND}     { return tok(sym.AND, "AND", OPERADOR); }
{OP_OR}      { return tok(sym.OR,  "OR",  OPERADOR); }
{OP_NOT}     { return tok(sym.NOT, "NOT", OPERADOR); }

/* ---- Delimitadores y simbolos especiales  ---- */
{ASIGNACION}     { return tok(sym.ASIGNACION,    "ASIGNACION",    OPERADOR); }
{FIN_SENT}       { return tok(sym.FIN_SENT,      "FIN_SENT",      OPERADOR); }
","              { return tok(sym.COMA,          "COMA",          OPERADOR); }
{BLOQUE_ABRE}    { return tok(sym.BLOQUE_ABRE,   "BLOQUE_ABRE",   OPERADOR); }
{BLOQUE_CIERRA}  { return tok(sym.BLOQUE_CIERRA, "BLOQUE_CIERRA", OPERADOR); }
{INDICE_ABRE}    { return tok(sym.INDICE_ABRE,   "INDICE_ABRE",   OPERADOR); }
{INDICE_CIERRA}  { return tok(sym.INDICE_CIERRA, "INDICE_CIERRA", OPERADOR); }
{PAREN_ABRE}     { return tok(sym.PAREN_ABRE,    "PAREN_ABRE",    OPERADOR); }
{PAREN_CIERRA}   { return tok(sym.PAREN_CIERRA,  "PAREN_CIERRA",  OPERADOR); }

/* Errores lexicos: se reporta el error, se descarta y se sigue (modo panico) */
/* Van al final para que solo entren si ninguna regla valida coincide */
\"[^\"\r\n]*          { error("cadena sin cerrar (falta '\"' en la misma linea)"); }
"'"[^'\r\n]*"'"?      { error("literal de caracter mal formado (debe tener exactamente 1 caracter)"); }
[^]                   { error("simbolo no reconocido por el lenguaje"); }
