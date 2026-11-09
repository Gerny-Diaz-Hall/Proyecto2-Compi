package compilador;

import java.util.ArrayList;
import java.util.List;

/*
 * AnalizadorExpresiones - revision de tipos y codigo intermedio de expresiones y sentencias.
 *
 * OBJETIVO      : validar los tipos de las expresiones aritmeticas, relacionales y logicas,
 *                 de las asignaciones, de read/write y de las condiciones, y al mismo tiempo
 *                 generar su codigo de tres direcciones. Tambien arma el codigo de las
 *                 estructuras de control (if/elif/else, while, for y break).
 * ENTRADA       : los Expr que suben por las producciones del parser (terminales.cup),
 *                 los lexemas de los operadores y la posicion (linea, columna) de cada uno.
 * SALIDA        : - un Expr nuevo con su tipo, su temporal (dir) y su codigo, o
 *                 - una lista de instrucciones (para las sentencias).
 *                 Los errores se reportan en la TablaSemantica (misma lista de errores).
 * RESTRICCIONES : - Tipado fuerte estricto: int y float NO se mezclan (ver Tipo.asignable).
 *                 - Si un operando ya tiene tipo ERROR, no se reporta otro error por el
 *                   (se evitan los errores en cascada) y el resultado tambien es ERROR.
 *                 - Cada identificador y cada literal se carga en un temporal antes de usarse.
 */
public class AnalizadorExpresiones {

    private final TablaSemantica tabla;
    private final GeneradorCodigo generador;

    public AnalizadorExpresiones(TablaSemantica tabla, GeneradorCodigo generador) {
        this.tabla = tabla;
        this.generador = generador;
    }

    // ======================================================================
    // Ayudas
    // ======================================================================

    /* true si alguna expresion falta (error sintactico) o ya tiene tipo ERROR */
    private static boolean hayError(Expr... expresiones) {
        for (Expr expresion : expresiones)
            if (expresion == null || expresion.tipo == null || expresion.tipo.esError()) return true;
        return false;
    }

    /* Donde queda el resultado de una expresion ("?" si no se pudo calcular) */
    private static String dir(Expr expresion) {
        return (expresion == null || expresion.dir == null) ? "?" : expresion.dir;
    }

    /* Codigo de una expresion (lista vacia si no tiene) */
    private static List<String> codigo(Expr expresion) {
        return expresion == null ? new ArrayList<>() : expresion.codigo;
    }

    /* Nombre de la variable de una expresion, para los mensajes */
    private static String nombre(Expr expresion) {
        return expresion.simbolo == null ? "?" : expresion.simbolo.nombre;
    }

    private void error(int linea, int columna, String mensaje) { tabla.error(linea, columna, mensaje); }

    // ======================================================================
    // Operandos: literales y variables
    // ======================================================================

    /*
     * literal - ejemplo (5, -2.5, true, 'a', "hola").
     *           (producciones: numero_con_signo, booleano, caracter, cadena)
     * SALIDA  : Expr del tipo dado, con el literal cargado en un temporal: t1 = 5
     */
    public Expr literal(Tipo tipo, String lexema, int linea, int columna) {
        Expr resultado = new Expr(tipo, linea, columna);
        resultado.valorLiteral = lexema;
        resultado.dir = generador.nuevoTemporal();
        resultado.codigo.add(resultado.dir + " = " + lexema);
        return resultado;
    }

    /*
     * cargar - Usa una variable, un elemento de arreglo o una llamada como VALOR.
     *          (producciones: base ::= valor, expr_booleana ::= valor, parametro_write)
     * REGLAS  : un arreglo sin indice no se puede usar como valor.
     * SALIDA  : Expr con el valor cargado en un temporal: t1 = x   o   t2 = arr[t1]
     * NOTA    : las llamadas a funcion (simbolo == null) pasan tal cual: su codigo
     *           (param / call) lo arma la Persona A en TablaSemantica.llamarFuncion.
     */
    public Expr cargar(Expr valor) {
        if (valor == null) return Expr.error(0, 0);
        if (valor.tipo.esError()) return valor;
        if (valor.arregloCompleto) {
            error(valor.linea, valor.columna, "el arreglo '" + nombre(valor)
                    + "' no se puede usar como valor sin indice ʃ: :ʅ");
            return Expr.error(valor.linea, valor.columna);
        }
        if (valor.simbolo == null) return valor;

        Expr resultado = new Expr(valor.tipo, valor.linea, valor.columna);
        resultado.simbolo = valor.simbolo;
        resultado.dir = generador.nuevoTemporal();
        if (valor.indice != null) {
            resultado.codigo.addAll(codigo(valor.indice));
            resultado.codigo.add(resultado.dir + " = " + nombre(valor) + "[" + dir(valor.indice) + "]");
        } else {
            resultado.codigo.add(resultado.dir + " = " + nombre(valor));
        }
        return resultado;
    }

    // ======================================================================
    // Expresiones aritmeticas
    // ======================================================================

    /*
     * aritmetica - Operacion binaria a op b con op en + - * / // % ^.
     *              (producciones: expr_arit, termino, residuo, factor)
     * REGLAS  : - los dos operandos deben ser int o float, y del MISMO tipo.
     *           - + - * / : el resultado es del tipo de los operandos (/ es entera con int
     *             y decimal con float).
     *           - // y %  : solo int.
     *           - ^       : solo int y el exponente no puede ser un literal negativo.
     * SALIDA  : Expr con t3 = t1 op t2, o ERROR si los tipos no son validos.
     */
    public Expr aritmetica(String operador, Expr izquierda, Expr derecha, int linea, int columna) {
        if (hayError(izquierda, derecha)) return Expr.error(linea, columna);
        Tipo a = izquierda.tipo, b = derecha.tipo;

        if (!a.esNumerico() || !b.esNumerico()) {
            error(linea, columna, "el operador " + operador + " solo acepta int o float, pero recibio "
                    + a + " " + operador + " " + b);
            return Expr.error(linea, columna);
        }
        boolean soloEnteros = operador.equals("//") || operador.equals("%") || operador.equals("^");
        if (soloEnteros && (a != Tipo.INT || b != Tipo.INT)) {
            error(linea, columna, "el operador " + operador + " solo acepta operandos int, pero recibio "
                    + a + " " + operador + " " + b);
            return Expr.error(linea, columna);
        }
        if (a != b) {
            error(linea, columna, "no se pueden mezclar int y float (tipado fuerte): " + a + " " + operador + " " + b);
            return Expr.error(linea, columna);
        }
        if (operador.equals("^") && derecha.valorLiteral != null && derecha.valorLiteral.startsWith("-")) {
            error(derecha.linea, derecha.columna, "la potencia solo acepta exponentes positivos, pero el exponente es "
                    + derecha.valorLiteral);
            return Expr.error(linea, columna);
        }
        return operacion(a, izquierda, operador, derecha, linea, columna);
    }

    /* operacion - Junta el codigo de los dos operandos y agrega t = a op b */
    private Expr operacion(Tipo tipo, Expr izquierda, String operador, Expr derecha, int linea, int columna) {
        Expr resultado = new Expr(tipo, linea, columna);
        resultado.codigo.addAll(codigo(izquierda));
        resultado.codigo.addAll(codigo(derecha));
        resultado.dir = generador.nuevoTemporal();
        resultado.codigo.add(resultado.dir + " = " + dir(izquierda) + " " + operador + " " + dir(derecha));
        return resultado;
    }

    /*
     * incremento - x++ o x-- (despues del operando).
     *              (producciones: base y for_update)
     * REGLAS  : x debe ser una variable o parametro int o float (no arreglo ni funcion).
     * SALIDA  : Expr cuyo valor es el de x ANTES de cambiar:
     *               t1 = x
     *               t2 = t1 + 1
     *               x = t2          (el resultado de la expresion queda en t1)
     */
    public Expr incremento(String nombre, String operador, int linea, int columna) {
        Expr variable = tabla.usarVariable(nombre, linea, columna);
        if (hayError(variable)) return Expr.error(linea, columna);
        if (variable.arregloCompleto) {
            error(linea, columna, "no se puede aplicar " + operador + " al arreglo '" + nombre + "'");
            return Expr.error(linea, columna);
        }
        if (!variable.tipo.esNumerico()) {
            error(linea, columna, "el operador " + operador + " solo se aplica a variables int o float, pero '"
                    + nombre + "' es " + variable.tipo);
            return Expr.error(linea, columna);
        }
        String uno = variable.tipo == Tipo.FLOAT ? "1.0" : "1";
        String simbolo = operador.equals("++") ? "+" : "-";
        Expr resultado = new Expr(variable.tipo, linea, columna);
        resultado.dir = generador.nuevoTemporal();
        String nuevo = generador.nuevoTemporal();
        resultado.codigo.add(resultado.dir + " = " + nombre);
        resultado.codigo.add(nuevo + " = " + resultado.dir + " " + simbolo + " " + uno);
        resultado.codigo.add(nombre + " = " + nuevo);
        return resultado;
    }

    // ======================================================================
    // Expresiones relacionales y logicas
    // ======================================================================

    /*
     * relacional - ʃ: a :ʅ op ʃ: b :ʅ con op en < <= > >= == !=.
     *              (produccion: expr_relacional)
     * REGLAS  : - < <= > >= : int o float, los dos del mismo tipo.
     *           - == !=      : ademas permiten boolean y char; los dos del mismo tipo.
     * SALIDA  : Expr boolean con t3 = t1 op t2.
     */
    public Expr relacional(String operador, Expr izquierda, Expr derecha, int linea, int columna) {
        if (hayError(izquierda, derecha)) return Expr.error(linea, columna);
        Tipo a = izquierda.tipo, b = derecha.tipo;
        boolean igualdad = operador.equals("==") || operador.equals("!=");

        boolean aValido = a.esNumerico() || (igualdad && (a == Tipo.BOOLEAN || a == Tipo.CHAR));
        boolean bValido = b.esNumerico() || (igualdad && (b == Tipo.BOOLEAN || b == Tipo.CHAR));
        if (!aValido || !bValido) {
            String permitidos = igualdad ? "int, float, boolean o char" : "int o float";
            error(linea, columna, "el operador " + operador + " solo compara " + permitidos + ", pero recibio "
                    + a + " " + operador + " " + b);
            return Expr.error(linea, columna);
        }
        if (a != b) {
            error(linea, columna, "no se pueden comparar valores de tipos distintos (tipado fuerte): "
                    + a + " " + operador + " " + b);
            return Expr.error(linea, columna);
        }
        return operacion(Tipo.BOOLEAN, izquierda, operador, derecha, linea, columna);
    }

    /*
     * logica - є: a :э λ є: b :э (and) o є: a :э θ є: b :э (or).
     *          (produccion: expr_logica)
     * ENTRADA : operadorCodigo es "&&" o "||" (como se escribe en el codigo intermedio).
     * REGLAS  : los dos operandos deben ser boolean.
     */
    public Expr logica(String operadorCodigo, String lexema, Expr izquierda, Expr derecha, int linea, int columna) {
        if (hayError(izquierda, derecha)) return Expr.error(linea, columna);
        if (izquierda.tipo != Tipo.BOOLEAN || derecha.tipo != Tipo.BOOLEAN) {
            error(linea, columna, "el operador logico " + lexema + " solo acepta boolean, pero recibio "
                    + izquierda.tipo + " " + lexema + " " + derecha.tipo);
            return Expr.error(linea, columna);
        }
        return operacion(Tipo.BOOLEAN, izquierda, operadorCodigo, derecha, linea, columna);
    }

    /*
     * negacion - Σ є: a :э
     *            (produccion: expr_logica)
     * REGLAS  : el operando debe ser boolean.
     * SALIDA  : Expr boolean con t2 = ! t1.
     */
    public Expr negacion(String lexema, Expr operando, int linea, int columna) {
        if (hayError(operando)) return Expr.error(linea, columna);
        if (operando.tipo != Tipo.BOOLEAN) {
            error(linea, columna, "la negacion " + lexema + " solo acepta boolean, pero recibio " + operando.tipo);
            return Expr.error(linea, columna);
        }
        Expr resultado = new Expr(Tipo.BOOLEAN, linea, columna);
        resultado.codigo.addAll(codigo(operando));
        resultado.dir = generador.nuevoTemporal();
        resultado.codigo.add(resultado.dir + " = ! " + dir(operando));
        return resultado;
    }

    // ======================================================================
    // Sentencias: asignacion, declaracion, read, write
    // ======================================================================

    /*
     * asignacion - destino Ͱ valor
     *              (producciones: asignacion_var, for_init, for_update)
     * REGLAS  : - el destino no puede ser un arreglo completo (sin indice).
     *           - el valor debe ser del mismo tipo que el destino.
     * SALIDA  : codigo del valor y luego  x = t1   o   arr[t2] = t1
     */
    public List<String> asignacion(Expr destino, Expr valor, int linea, int columna) {
        List<String> resultado = new ArrayList<>();
        if (hayError(destino)) return resultado;
        if (destino.arregloCompleto) {
            error(destino.linea, destino.columna, "no se puede asignar a todo el arreglo '" + nombre(destino)
                    + "'; use un indice ʃ: :ʅ");
            return resultado;
        }
        if (hayError(valor)) return resultado;
        if (!Tipo.asignable(destino.tipo, valor.tipo)) {
            error(linea, columna, "no se puede asignar un valor " + valor.tipo + " a '" + nombre(destino)
                    + "' de tipo " + destino.tipo);
            return resultado;
        }
        if (destino.indice != null) {
            resultado.addAll(codigo(destino.indice));
            resultado.addAll(codigo(valor));
            resultado.add(nombre(destino) + "[" + dir(destino.indice) + "] = " + dir(valor));
        } else {
            resultado.addAll(codigo(valor));
            resultado.add(nombre(destino) + " = " + dir(valor));
        }
        return resultado;
    }

    /*
     * declaracion - Codigo del valor inicial de un "val" (los tipos ya los valido
     *               TablaSemantica.declararVariable).
     *               (producciones: declaracion y for_init)
     * SALIDA  : - escalar con valor:  codigo del valor y  x = t1
     *           - arreglo con valores: cada valor y  arr[0] = t1, arr[1] = t2, ...
     *           - sin valor inicial:   lista vacia.
     */
    public List<String> declaracion(String nombre, InfoDeclaracion info) {
        List<String> resultado = new ArrayList<>();
        if (info == null) return resultado;
        if (!info.esArreglo) {
            if (info.inicial != null) {
                resultado.addAll(codigo(info.inicial));
                resultado.add(nombre + " = " + dir(info.inicial));
            }
        } else if (info.valores != null) {
            for (int posicion = 0; posicion < info.valores.size(); posicion++) {
                Expr valor = info.valores.get(posicion);
                resultado.addAll(codigo(valor));
                resultado.add(nombre + "[" + posicion + "] = " + dir(valor));
            }
        }
        return resultado;
    }

    /*
     * leer - read є: destino :э
     *        (produccion: funcion_io)
     * REGLAS  : solo se leen int, float o char, en una variable o un elemento de arreglo.
     * SALIDA  : read x   o   read arr[t1]
     */
    public List<String> leer(Expr destino, int linea, int columna) {
        List<String> resultado = new ArrayList<>();
        if (hayError(destino)) return resultado;
        if (destino.arregloCompleto) {
            error(destino.linea, destino.columna, "read no puede leer todo el arreglo '" + nombre(destino)
                    + "'; use un indice ʃ: :ʅ");
            return resultado;
        }
        Tipo tipo = destino.tipo;
        if (tipo != Tipo.INT && tipo != Tipo.FLOAT && tipo != Tipo.CHAR) {
            error(linea, columna, "read solo puede leer int, float o char, pero '" + nombre(destino) + "' es " + tipo);
            return resultado;
        }
        if (destino.indice != null) {
            resultado.addAll(codigo(destino.indice));
            resultado.add("read " + nombre(destino) + "[" + dir(destino.indice) + "]");
        } else {
            resultado.add("read " + nombre(destino));
        }
        return resultado;
    }

    /*
     * escribir - write є: valor :э
     *            (produccion: funcion_io)
     * REGLAS  : solo se escriben string, int, boolean o float (literales o variables).
     * SALIDA  : codigo del valor y  write t1
     */
    public List<String> escribir(Expr valor, int linea, int columna) {
        List<String> resultado = new ArrayList<>();
        if (hayError(valor)) return resultado;
        Tipo tipo = valor.tipo;
        if (tipo != Tipo.STRING && tipo != Tipo.INT && tipo != Tipo.BOOLEAN && tipo != Tipo.FLOAT) {
            error(linea, columna, "write solo puede escribir string, int, boolean o float, pero recibio " + tipo);
            return resultado;
        }
        resultado.addAll(codigo(valor));
        resultado.add("write " + dir(valor));
        return resultado;
    }

    // ======================================================================
    // Estructuras de control
    // ======================================================================

    /*
     * condicion - Valida que la condicion de un if/elif/while/for sea boolean.
     *             (producciones: condicion y ciclo_for)
     * SALIDA  : la misma expresion, o ERROR si no es boolean.
     */
    public Expr condicion(Expr expresion, int linea, int columna) {
        if (hayError(expresion)) return Expr.error(linea, columna);
        if (expresion.tipo != Tipo.BOOLEAN) {
            error(expresion.linea > 0 ? expresion.linea : linea, expresion.linea > 0 ? expresion.columna : columna,
                    "la condicion debe ser boolean, pero es " + expresion.tipo);
            return Expr.error(linea, columna);
        }
        return expresion;
    }

    /*
     * si - Codigo de if / elif / else.
     *      (produccion: si_condicional)
     * ENTRADA : condicion y cuerpo del if, lista de elif (puede ser vacia) y cuerpo del
     *           else (null si no hay else).
     * SALIDA  : por cada rama:
     *               <codigo de la condicion>
     *               if t1 goto L2
     *               goto L3
     *           L2:
     *               <cuerpo>
     *               goto L1        (salta al final; no hace falta en la ultima rama)
     *           L3:
     *           ... siguiente rama o el else ...
     *           L1:
     */
    public List<String> si(Expr condicion, List<String> cuerpo, List<Rama> elifs, List<String> cuerpoElse) {
        List<String> resultado = new ArrayList<>();
        String fin = generador.nuevaEtiqueta();
        boolean hayMas = !elifs.isEmpty() || cuerpoElse != null;
        rama(resultado, condicion, cuerpo, fin, hayMas);
        for (int i = 0; i < elifs.size(); i++) {
            boolean noEsUltima = i < elifs.size() - 1 || cuerpoElse != null;
            rama(resultado, elifs.get(i).condicion, elifs.get(i).cuerpo, fin, noEsUltima);
        }
        if (cuerpoElse != null) resultado.addAll(cuerpoElse);
        resultado.add(fin + ":");
        return resultado;
    }

    /* rama - Una rama del if: si la condicion es true ejecuta el cuerpo, si no salta a la siguiente */
    private void rama(List<String> resultado, Expr condicion, List<String> cuerpo, String fin, boolean saltarAlFin) {
        String verdadero = generador.nuevaEtiqueta();
        String siguiente = generador.nuevaEtiqueta();
        resultado.addAll(codigo(condicion));
        resultado.add("if " + dir(condicion) + " goto " + verdadero);
        resultado.add("goto " + siguiente);
        resultado.add(verdadero + ":");
        if (cuerpo != null) resultado.addAll(cuerpo);
        if (saltarAlFin) resultado.add("goto " + fin);
        resultado.add(siguiente + ":");
    }

    /*
     * mientras - Codigo de un while (las etiquetas se crearon al leer "while").
     *            (produccion: ciclo_while)
     * SALIDA  :
     *           L1:                 (ciclo.inicio)
     *               <condicion>
     *               if t1 goto L2
     *               goto L3
     *           L2:                 (ciclo.cuerpo)
     *               <cuerpo>
     *               goto L1
     *           L3:                 (ciclo.fin, aqui salta un break)
     */
    public List<String> mientras(Ciclo ciclo, Expr condicion, List<String> cuerpo) {
        List<String> resultado = new ArrayList<>();
        resultado.add(ciclo.inicio + ":");
        resultado.addAll(codigo(condicion));
        resultado.add("if " + dir(condicion) + " goto " + ciclo.cuerpo);
        resultado.add("goto " + ciclo.fin);
        resultado.add(ciclo.cuerpo + ":");
        if (cuerpo != null) resultado.addAll(cuerpo);
        resultado.add("goto " + ciclo.inicio);
        resultado.add(ciclo.fin + ":");
        generador.cerrarCiclo();
        return resultado;
    }

    /*
     * para - Codigo de un for: igual que el while, pero con la inicializacion antes de
     *        la condicion y la actualizacion al final del cuerpo.
     *        (produccion: ciclo_for)
     */
    public List<String> para(Ciclo ciclo, List<String> inicio, Expr condicion, List<String> actualizacion,
                             List<String> cuerpo) {
        List<String> resultado = new ArrayList<>();
        if (inicio != null) resultado.addAll(inicio);
        resultado.add(ciclo.inicio + ":");
        resultado.addAll(codigo(condicion));
        resultado.add("if " + dir(condicion) + " goto " + ciclo.cuerpo);
        resultado.add("goto " + ciclo.fin);
        resultado.add(ciclo.cuerpo + ":");
        if (cuerpo != null) resultado.addAll(cuerpo);
        if (actualizacion != null) resultado.addAll(actualizacion);
        resultado.add("goto " + ciclo.inicio);
        resultado.add(ciclo.fin + ":");
        generador.cerrarCiclo();
        return resultado;
    }

    /*
     * romper - Codigo de break: salta al final del ciclo mas interno.
     *          (produccion: break_stmt; que este dentro de un ciclo lo valida TablaSemantica)
     */
    public List<String> romper() {
        List<String> resultado = new ArrayList<>();
        Ciclo ciclo = generador.cicloActual();
        if (ciclo != null) resultado.add("goto " + ciclo.fin);
        return resultado;
    }
}
