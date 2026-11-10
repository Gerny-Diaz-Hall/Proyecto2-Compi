package compilador;

import java.util.ArrayList;
import java.util.List;

/*
 * Simbolo - Una entrada de la tabla de simbolos semantica.
 *
 * OBJETIVO      : guardar todo lo que el analisis semantico necesita saber de un
 *                 identificador declarado: su tipo, que es (variable, arreglo, parametro
 *                 o funcion), donde se declaro y en que ambito vive.
 * ENTRADA       : los datos de la declaracion (nombre, tipo, categoria, linea, columna).
 * SALIDA        : un objeto consultable desde las acciones semanticas del parser.
 * RESTRICCIONES : - tamano solo aplica a ARREGLO (en los demas queda en -1).
 *                 - parametros solo aplica a FUNCION (tipos en orden de declaracion).
 *                 - Para FUNCION, tipo es el tipo de retorno.
 */
public class Simbolo {

    /* Que clase de identificador es */
    public enum Categoria { VARIABLE, ARREGLO, PARAMETRO, FUNCION }

    public final String nombre;
    public final Tipo tipo;
    public final Categoria categoria;
    public final int linea, columna;

    /* Tamano del arreglo (solo ARREGLO; -1 en los demas) */
    public int tamano = -1;

    /* Tipos de los parametros en orden (solo FUNCION) */
    public final List<Tipo> parametros = new ArrayList<>();

    /* Nombre del ambito donde se declaro (lo llena TablaSemantica al declarar) */
    public String ambito = "";

    public Simbolo(String nombre, Tipo tipo, Categoria categoria, int linea, int columna) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.categoria = categoria;
        this.linea = linea;
        this.columna = columna;
    }

    /* true si es un arreglo (para validar que se use con indice) */
    public boolean esArreglo() { return categoria == Categoria.ARREGLO; }

    /* true si es una funcion (para validar llamadas) */
    public boolean esFuncion() { return categoria == Categoria.FUNCION; }

    /* true si es variable o parametro escalar (se puede asignar sin indice) */
    public boolean esEscalar() { return categoria == Categoria.VARIABLE || categoria == Categoria.PARAMETRO; }

    /*
     * firma - Texto con la firma de una funcion, por ejemplo "int suma(int, int)".
     * SALIDA : la firma (solo tiene sentido para FUNCION; para otros devuelve el tipo).
     */
    public String firma() {
        if (!esFuncion()) return tipo.nombre;
        StringBuilder sb = new StringBuilder(tipo.nombre).append(' ').append(nombre).append('(');
        for (int i = 0; i < parametros.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(parametros.get(i).nombre);
        }
        return sb.append(')').toString();
    }

    /* Fila para el reporte de la tabla de simbolos */
    @Override
    public String toString() {
        String extra = esArreglo() ? " | tamano " + tamano
                     : esFuncion() ? " | firma " + firma() : "";
        return String.format("  %-15s | %-9s | %-7s | linea %d, columna %d%s",
                nombre, categoria, tipo.nombre, linea, columna, extra);
    }
}
