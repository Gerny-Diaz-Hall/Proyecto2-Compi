package compilador;
 
/*
 * Tipo - Tipos de datos del lenguaje.
 *
 * OBJETIVO      : representar el tipo de cada variable, parametro, funcion y expresion
 *                 para poder hacer la comprobacion de tipos (tipado explicito y fuerte).
 * ENTRADA       : el lexema de la palabra reservada del tipo ("int", "float", ...).
 * SALIDA        : la constante del enum correspondiente.
 * RESTRICCIONES : - ERROR no existe en el lenguaje; se usa cuando una expresion ya tiene
 *                   un error semantico, para no reportar el mismo error varias veces
 *                   (errores en cascada).
 *                 - VOID solo lo usa principal.
 */
public enum Tipo {
    INT("int"),
    FLOAT("float"),
    BOOLEAN("boolean"),
    CHAR("char"),
    STRING("string"),
    VOID("void"),
    ERROR("error");
 
    /* Nombre del tipo como se escribe en el lenguaje (para reportes y codigo intermedio) */
    public final String nombre;
 
    Tipo(String nombre) { this.nombre = nombre; }
 
    /*
     * desdeLexema - Convierte la palabra reservada del tipo en su constante.
     * ENTRADA : lexema ("int", "float", "boolean", "char", "string", "void").
     * SALIDA  : el Tipo correspondiente, o ERROR si el lexema no es un tipo.
     */
    public static Tipo desdeLexema(String lexema) {
        for (Tipo tipo : values())
            if (tipo.nombre.equals(lexema)) return tipo;
        return ERROR;
    }
 
    /* true si el tipo es int o float (los unicos que aceptan operadores aritmeticos) */
    public boolean esNumerico() { return this == INT || this == FLOAT; }
 
    /* true si el tipo es ERROR (la expresion ya fue reportada, no volver a reportar) */
    public boolean esError() { return this == ERROR; }
 
    /*
     * Decision de diseno (confirmar con el profesor):
     * false -> tipado fuerte estricto: un int NO se puede guardar en un float.
     * true  -> se permite guardar un int en un float (val float x Ͱ 5 »).
     */
    public static final boolean PERMITIR_INT_A_FLOAT = false;
 
    /*
     * asignable - Indica si un valor de tipo 'origen' se puede guardar donde se espera 'destino'
     *             (asignaciones, valores iniciales, argumentos y return).
     * ENTRADA : tipo esperado y tipo del valor; cualquiera puede ser null (desconocido).
     * SALIDA  : true si es valido, o si alguno es desconocido o ERROR (ya se reporto antes,
     *           asi no se generan errores en cascada).
     */
    public static boolean asignable(Tipo destino, Tipo origen) {
        if (destino == null || origen == null) return true;
        if (destino == ERROR || origen == ERROR) return true;
        if (destino == origen) return true;
        return PERMITIR_INT_A_FLOAT && destino == FLOAT && origen == INT;
    }
 
    @Override
    public String toString() { return nombre; }
}
 