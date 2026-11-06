package compilador;
 
import java.util.List;
 
/*
 * InfoDeclaracion - Lo que viene despues del nombre en una declaracion "val".
 *
 * OBJETIVO      : transportar, desde resto_escalar / resto_numerico hasta la produccion
 *                 declaracion, si se declara un escalar o un arreglo, su tamano y su valor
 *                 inicial, para que declaracion registre el simbolo y valide todo junto.
 * ENTRADA       : - escalar:  el Expr del valor inicial (o null si no tiene).
 *                 - arreglo:  el lexema del tamano y la lista de valores iniciales (o null).
 * SALIDA        : un objeto que lee TablaSemantica.declararVariable.
 * RESTRICCIONES : - tamano queda en -1 si el lexema no cabe en un int.
 *                 - Las posiciones son las del token ASIGNACION (valor inicial) y del
 *                   literal del tamano, para reportar errores en el lugar correcto.
 */
public class InfoDeclaracion {
 
    public final boolean esArreglo;
 
    /* ---- Escalar ---- */
    public final Expr inicial;               // valor inicial (null si no tiene)
    public final int lineaInicial, columnaInicial;
 
    /* ---- Arreglo ---- */
    public final int tamano;                 // -1 si no es arreglo o no cabe en int
    public final int lineaTamano, columnaTamano;
    public final List<Expr> valores;         // valores iniciales (null si no tiene)
 
    private InfoDeclaracion(boolean esArreglo, Expr inicial, int lineaInicial, int columnaInicial,
                            int tamano, int lineaTamano, int columnaTamano, List<Expr> valores) {
        this.esArreglo = esArreglo;
        this.inicial = inicial;
        this.lineaInicial = lineaInicial;
        this.columnaInicial = columnaInicial;
        this.tamano = tamano;
        this.lineaTamano = lineaTamano;
        this.columnaTamano = columnaTamano;
        this.valores = valores;
    }
 
    /*
     * escalar - Declaracion sin indice: "val int x »" o "val int x Ͱ expr »".
     * ENTRADA : el valor inicial (null si no hay) y la posicion del Ͱ.
     */
    public static InfoDeclaracion escalar(Expr inicial, int linea, int columna) {
        return new InfoDeclaracion(false, inicial, linea, columna, -1, 0, 0, null);
    }
 
    /*
     * arreglo - Declaracion con indice: "val int a ʃ:3:ʅ »" o con "Ͱ ¿: v1, v2 :? »".
     * ENTRADA : lexema del tamano, su posicion y la lista de valores (null si no hay).
     */
    public static InfoDeclaracion arreglo(String lexemaTamano, int linea, int columna, List<Expr> valores) {
        int tamano;
        try { tamano = Integer.parseInt(lexemaTamano); }
        catch (NumberFormatException noCabeEnInt) { tamano = -1; }
        return new InfoDeclaracion(true, null, 0, 0, tamano, linea, columna, valores);
    }
}
 