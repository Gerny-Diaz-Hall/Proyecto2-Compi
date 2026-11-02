package compilador;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * LexerMain - Programa de prueba SOLO del scanner (el Main final integrara tambien el parser).
 *
 * ENTRADA : args[0] = archivo fuente (UTF-8); args[1] (opcional) = carpeta de salida (por defecto ".").
 * SALIDA  : tokens.txt, tablas_simbolos.txt, errores_lexicos.txt, y un resumen en consola.
 */
public class LexerMain {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) { System.err.println("Uso: java LexerMain <archivo_fuente> [carpeta_salida]"); return; }
        String salida = args.length > 1 ? args[1] : ".";
        new File(salida).mkdirs();

        Lexer lexer;
        try (Reader in = new InputStreamReader(new FileInputStream(args[0]), StandardCharsets.UTF_8)) {
            lexer = new Lexer(in);
            while (lexer.next_token().sym != sym.EOF) { /* el scanner registra todo */ }
        }

        escribir(salida + "/tokens.txt", lexer.getTablaSimbolos()::escribirTokens);
        escribir(salida + "/tablas_simbolos.txt", lexer.getTablaSimbolos()::escribirTablas);
        List<String> errores = lexer.getErrores();
        escribir(salida + "/errores_lexicos.txt", pw -> {
            if (errores.isEmpty()) pw.println("Sin errores lexicos.");
            for (String e : errores) pw.println(e);
        });

        System.out.println("Tokens encontrados : " + lexer.getTablaSimbolos().getTokens().size());
        System.out.println("Errores lexicos    : " + errores.size());
        for (String e : errores) System.out.println("  " + e);
        System.out.println("Archivos generados en: " + new File(salida).getAbsolutePath());
    }

    private static void escribir(String ruta, java.util.function.Consumer<PrintWriter> c) throws IOException {
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(ruta), StandardCharsets.UTF_8))) { c.accept(pw); }
    }
}
