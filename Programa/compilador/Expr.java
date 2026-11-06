package compilador;
 
import java.util.ArrayList;
import java.util.List;
 
/*
 * Expr - Resultado de analizar una expresion.
 *
 * OBJETIVO      : es el valor que "sube" por las producciones de expresiones del parser.
 *                 Cada produccion recibe los Expr de sus hijos y devuelve uno nuevo con el
 *                 tipo de la expresion completa (y, en la fase de codigo intermedio, donde
 *                 queda el resultado y las instrucciones para calcularlo).
 * ENTRADA       : el tipo calculado y la posicion de la expresion en el fuente.
 * SALIDA        : objeto que consultan las validaciones semanticas.
 * RESTRICCIONES : - tipo == ERROR significa que la expresion ya tuvo un error reportado:
 *                   quien la use NO debe reportar otro error por ella.
 *                 - Si una produccion todavia no calcula su Expr, llega null; las
 *                   validaciones tratan null como "tipo desconocido" y no reportan nada.
 *                 - linea == 0 significa posicion desconocida.
 */
public class Expr {
 
    /* Tipo de la expresion (INT, FLOAT, ..., o ERROR) */
    public Tipo tipo;
 
    /* Posicion de la expresion en el archivo fuente (para reportar errores) */
    public int linea, columna;
 
    /* Si la expresion es directamente una variable o un acceso a arreglo, su simbolo */
    public Simbolo simbolo;
 
    /*
     * true si la expresion es el NOMBRE de un arreglo sin indice (ej. "arr" en vez de
     * "arr ʃ:0:ʅ"). Las operaciones aritmeticas, logicas y relacionales deben rechazarlo.
     */
    public boolean arregloCompleto = false;
 
    /* ---- Para la fase de codigo intermedio (se usan mas adelante) ---- */
 
    /* Donde queda el resultado: un temporal (t1), un literal (5) o un nombre */
    public String dir;
 
    /* Instrucciones de tres direcciones que calculan esta expresion */
    public final List<String> codigo = new ArrayList<>();
 
    public Expr(Tipo tipo, int linea, int columna) {
        this.tipo = tipo;
        this.linea = linea;
        this.columna = columna;
    }
 
    /* error - Crea una expresion de tipo ERROR (ya se reporto el problema) */
    public static Expr error(int linea, int columna) {
        return new Expr(Tipo.ERROR, linea, columna);
    }
 
    /* tipoDe - Tipo de una expresion que puede ser null (null = desconocido) */
    public static Tipo tipoDe(Expr expresion) {
        return expresion == null ? null : expresion.tipo;
    }
}
 