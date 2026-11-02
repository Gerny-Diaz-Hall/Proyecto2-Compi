package compilador;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

/*
 * objetivo: 
 * para leer el codigo que se esta esribiendo y despues pasarlo por el jflex,
 * luego el cup para ver si lo que se esta mandando en codigo no tiene errores
 * 
 * entradas: la ruta del archivo de texto que se va a analizar
 * 
 * salidas: crea los reportes despues de la revision para poder verificar tokens, tablas de símbolos, 
 *  lexico y sintaxis para ver los resultados
 * 
 * restricciones: ser un archivo que exista en la direccion que se esta poniendo y se tiene que conectar al CUP
 */
public class Main {
    public static void main(String[] args) {
        // Revisa que le hayamos pasado el archivo al correr el programa
        if (args.length < 1) {
            System.err.println("Uso: java compilador.Main <archivo_fuente> [carpeta_salida]");
            return;
        }

        String archivoFuente = args[0];
        String salida = args.length > 1 ? args[1] : ".";
        new File(salida).mkdirs();

        System.out.println("==================================================");
        System.out.println("            Compilador Proyecto 01                ");
        System.out.println("==================================================");
        System.out.println("Analizando: " + archivoFuente);

        try {
            // ==========================================================
            // analiador lexico:
            // ==========================================================
            Lexer lexerTokens;
            try (Reader in = new InputStreamReader(new FileInputStream(archivoFuente), StandardCharsets.UTF_8)) {
                lexerTokens = new Lexer(in);
                // lee el codigo sacando los tokens para ir revisando detenidamente hasta terminar de revisar el documento
                while (lexerTokens.next_token().sym != sym.EOF) { 
                }
            }

            // se guardan los tokens y las tablas de simbolos en los archivos que se van a generar de los reportes al final
            escribir(salida + "/tokens.txt", lexerTokens.getTablaSimbolos()::escribirTokens);
            escribir(salida + "/tablas_simbolos.txt", lexerTokens.getTablaSimbolos()::escribirTablas);
            
            List<String> erroresLexicos = lexerTokens.getErrores();
            escribir(salida + "/errores_lexicos.txt", pw -> {
                if (erroresLexicos.isEmpty()) pw.println("Sin errores lexicos.");
                for (String e : erroresLexicos) pw.println(e);
            });

            System.out.println("\nReportes léxicos generados:");
            System.out.println("  - tokens.txt");
            System.out.println("  - tablas_simbolos.txt");
            System.out.println("  - errores_lexicos.txt");

            // ==========================================================
            // analizador sintactico
            // ==========================================================
            Reader lectorParser = new InputStreamReader(new FileInputStream(archivoFuente), StandardCharsets.UTF_8);
            Lexer scannerParser = new Lexer(lectorParser);
            parser sintactico = new parser(scannerParser);

            // se revisa si hay errores activando el panico por si se encuentran errores, que se anote lo que esta mal,
            // se pasa por alto la parte mala y luego se sigue revisando el resto del codigo
            try {
                sintactico.parse();
            } catch (Exception e) {
                // si el error ya es demasiado fuerte y rompe la estructura, se va al exception y se guarda de igual forma
            }

            // se anotan aqui los errores que se pudieron encontrar para escribirlos en el txt del reporte
            List<String> erroresSintacticos = sintactico.erroresSintacticos;
            escribir(salida + "/errores_sintacticos.txt", pw -> {
                if (erroresSintacticos.isEmpty()) pw.println("Sin errores sintacticos.");
                for (String e : erroresSintacticos) pw.println(e);
            });
            System.out.println("  - errores_sintacticos.txt");

            // ========================================================================
            // dar el resultado de la evaluacion del codigo que se le dio al analizador
            // ========================================================================
            boolean valido = erroresLexicos.isEmpty() && erroresSintacticos.isEmpty();
            System.out.println("\n==================================================");
            System.out.println(valido
                ? " Resultado: se puede generar por la gramatica"
                : " Resultado: no puede ser generado por los errores("
                  + erroresLexicos.size() + " lexicos, " + erroresSintacticos.size() + " sintacticos)");
            System.out.println("==================================================");

        } catch (Exception e) {
            System.err.println("Error al procesar el archivo: " + e.getMessage());
        }
    }

    // esto se usa como ayuda para generar los reportes, con esto se abren los rchivos y luego solo los cierra 
    private static void escribir(String ruta, java.util.function.Consumer<PrintWriter> c) throws IOException {
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(ruta), StandardCharsets.UTF_8))) { 
            c.accept(pw); 
        }
    }
}