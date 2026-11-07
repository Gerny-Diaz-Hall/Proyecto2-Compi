package compilador;

import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/*
 * GeneradorCodigo - Crea temporales y etiquetas y arma el archivo de codigo intermedio.
 *
 * OBJETIVO      : centralizar todo lo que necesita la generacion de codigo de tres
 *                 direcciones: nombres nuevos de temporales y de etiquetas,
 *                 la pila de ciclos abiertos y la lista final de instrucciones que se escribe en el archivo.
 * ENTRADA       : llamadas desde AnalizadorExpresiones y desde las acciones de terminales.cup.
 * SALIDA        : el archivo codigo_intermedio.txt (lo escribe Main con escribir()).
 * RESTRICCIONES : - El codigo de cada sentencia se arma como una lista de String y se va
 *                   juntando de abajo hacia arriba (el parser es LR, ascendente).
 *                 - Solo se escribe el archivo si el programa no tiene errores (Main lo decide).
 */
public class GeneradorCodigo {

    /* Contadores para que cada temporal y cada etiqueta tenga un nombre unico */
    private int contadorTemporales = 0;
    private int contadorEtiquetas = 0;

    /* Ciclos abiertos (while/for): el tope es el ciclo mas interno, al que sale un break */
    private final Deque<Ciclo> ciclos = new ArrayDeque<>();

    /* Codigo de las declaraciones globales */
    private final List<String> codigoGlobal = new ArrayList<>();

    /* Codigo de las funciones y de principal, en el orden en que aparecen */
    private final List<String> codigoFunciones = new ArrayList<>();

    // ======================================================================
    // Temporales y etiquetas
    // ======================================================================

    /* nuevoTemporal - Devuelve un nombre de temporal sin usar (t1, t2, ...) */
    public String nuevoTemporal() { return "t" + (++contadorTemporales); }

    /* nuevaEtiqueta - Devuelve un nombre de etiqueta sin usar (L1, L2, ...) */
    public String nuevaEtiqueta() { return "L" + (++contadorEtiquetas); }

    // ======================================================================
    // Ciclos (para break)
    // ======================================================================

    /*
     * abrirCiclo - Se llama al leer "while" o "for", ANTES de su condicion y su cuerpo.
     * SALIDA : el Ciclo con sus etiquetas ya creadas; queda en el tope de la pila para que
     *          un break dentro del cuerpo sepa a que etiqueta saltar.
     */
    public Ciclo abrirCiclo() {
        Ciclo ciclo = new Ciclo(nuevaEtiqueta(), nuevaEtiqueta(), nuevaEtiqueta());
        ciclos.push(ciclo);
        return ciclo;
    }

    /* cerrarCiclo - Se llama al terminar el while/for: saca el ciclo de la pila */
    public void cerrarCiclo() { if (!ciclos.isEmpty()) ciclos.pop(); }

    /* cicloActual - El ciclo mas interno, o null si no hay ninguno abierto */
    public Ciclo cicloActual() { return ciclos.peek(); }

    // ======================================================================
    // Armar el archivo
    // ======================================================================

    /* agregarGlobal - Codigo de una declaracion global con valor inicial */
    public void agregarGlobal(List<String> codigo) {
        if (codigo != null) codigoGlobal.addAll(codigo);
    }

    /*
     * agregarFuncion - Agrega el cuerpo de una funcion entre sus etiquetas
     *                  de inicio y fin, con el formato tipo_nombre: ... tipo_nombre_end.
     * ENTRADA : tipo de retorno, nombre y el codigo del cuerpo (lista de instrucciones).
     * NOTA    : punto de enganche para la Persona A, que completa aqui los parametros,
     *           el return y las llamadas (param / call).
     */
    public void agregarFuncion(Tipo tipo, String nombre, List<String> cuerpo) {
        String etiqueta = (tipo == null ? "void" : tipo.nombre) + "_" + nombre;
        codigoFunciones.add(etiqueta + ":");
        if (cuerpo != null)
            for (String instruccion : cuerpo) codigoFunciones.add(instruccion);
        codigoFunciones.add(etiqueta + "_end:");
        codigoFunciones.add("");
    }

    /*
     * escribir - Escribe el codigo intermedio completo.
     * FORMATO : las etiquetas (terminan en ':') van pegadas al margen y las instrucciones
     *           con sangria, para que se lea facil.
     */
    public void escribir(PrintWriter out) {
        out.println("==== CODIGO INTERMEDIO (tres direcciones) ====");
        out.println();
        if (!codigoGlobal.isEmpty()) {
            out.println("global:");
            for (String instruccion : codigoGlobal) out.println(formato(instruccion));
            out.println("global_end:");
            out.println();
        }
        for (String instruccion : codigoFunciones) out.println(formato(instruccion));
    }

    /* Las etiquetas sin sangria, todo lo demas con 4 espacios */
    private String formato(String instruccion) {
        return (instruccion.isEmpty() || instruccion.endsWith(":")) ? instruccion : "    " + instruccion;
    }
}
