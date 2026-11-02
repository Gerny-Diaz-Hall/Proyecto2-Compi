package compilador;

import java.io.PrintWriter;
import java.util.*;

/*
 * Objetivo: Gestionar, clasificar y almacenar todos los tokens que va encontrando al analizar
 * reparte los tokens que se va encontrando en 4 tablas 
 * 
 * Entrada: Recibe los datos de cada token por la funcion de registrar todas las veces que se reconocen patrones
 * 
 * Salidas: genera el texto y crea los reportes "tokens.txt" y "tablas_simbolos.txt"
 * 
 * Restricciones: no se guardan los tipos de datos ni identificadores en la parte lexica pero se completan en semantica
 */
public class TablaSimbolos {

    /*
     * tablas de reportes:
     * -reservadas: Guarda lexema, token, ocurrencias y líneas
     * -identificadores: Guarda lexema, primera línea, ocurrencias y líneas.
     * -literales: Guarda lexema, tipo de literal, ocurrencias y líneas.
     * -operadores: Guarda lexema, token, ocurrencias y líneas
     */
    public enum Tipo {
        RESERVADAS("TABLA DE PALABRAS RESERVADAS", "lexema, token, ocurrencias, lineas"),
        IDENTIFICADORES("TABLA DE IDENTIFICADORES", "lexema, primera linea, ocurrencias, lineas (tipo/ambito: fase semantica)"),
        LITERALES("TABLA DE LITERALES", "lexema (valor), tipo de literal, ocurrencias, lineas"),
        OPERADORES("TABLA DE OPERADORES Y SIMBOLOS", "lexema, token, ocurrencias, lineas");

        public final String titulo, informacion;
        Tipo(String titulo, String informacion) { this.titulo = titulo; this.informacion = informacion; }
    }

    /* Presenta una fila individual dentro de una tabla de símbolos */
    public static class Entrada {
        public final String lexema, token;
        public final int primeraLinea;
        public int ocurrencias = 0;
        public final TreeSet<Integer> lineas = new TreeSet<>();
        Entrada(String lexema, String token, int linea) { this.lexema = lexema; this.token = token; this.primeraLinea = linea; }
    }

    /* Presenta el registro de secuencia de cada token a como fueron saliendo*/
    public static class Registro {
        public final int linea, columna;
        public final String token, lexema;
        public final Tipo tabla;
        Registro(int l, int c, String t, String lx, Tipo tb) { linea = l; columna = c; token = t; lexema = lx; tabla = tb; }
    }

    private final Map<Tipo, LinkedHashMap<String, Entrada>> tablas = new EnumMap<>(Tipo.class);
    private final List<Registro> tokens = new ArrayList<>();

    public TablaSimbolos() { for (Tipo t : Tipo.values()) tablas.put(t, new LinkedHashMap<>()); }

    /*
     * Agrega un nuevo token al historial general y actualiza la tabla de símbolos correspondiente, pero si el lexema ya estaba en la tabla
     * se le suma en la ocurrencia y se anota la nueva linea
     */
    public void registrar(Tipo tabla, String token, String lexema, int linea, int columna) {
        tokens.add(new Registro(linea, columna, token, lexema, tabla));
        Entrada e = tablas.get(tabla).computeIfAbsent(lexema, k -> new Entrada(lexema, token, linea));
        e.ocurrencias++;
        e.lineas.add(linea);
    }

    public List<Registro> getTokens() { return tokens; }
    public Map<Tipo, LinkedHashMap<String, Entrada>> getTablas() { return tablas; }

    /*
     * Construir reporte: hace print de la lista de los tokens de columna, token, lexema y tabla 
     */
    public void escribirTokens(PrintWriter out) {
        out.println("LINEA:COL\tTOKEN\tLEXEMA\tTABLA DE SIMBOLOS");
        for (Registro r : tokens)
            out.println(r.linea + ":" + r.columna + "\t" + r.token + "\t" + r.lexema + "\t" + r.tabla.name());
        out.println();
        out.println("Total de tokens: " + tokens.size());
    }

    /*
     * Construir reporte: hace print de todas las tablas, se muestran los lexemas y las lineas en las que sale y cuanto se usa
     */
    public void escribirTablas(PrintWriter out) {
        for (Tipo t : Tipo.values()) {
            out.println("==== " + t.titulo + " ====");
            out.println("Informacion almacenada: " + t.informacion);
            for (Entrada e : tablas.get(t).values()) {
                String col = (t == Tipo.IDENTIFICADORES) ? "primera linea " + e.primeraLinea : "token " + e.token;
                out.println("  " + e.lexema + "\t| " + col + " | ocurrencias " + e.ocurrencias + " | lineas " + e.lineas);
            }
            out.println();
        }
    }
}