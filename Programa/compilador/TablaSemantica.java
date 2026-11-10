package compilador;
 
import java.io.PrintWriter;
import java.util.*;
 
/*
 * TablaSemantica - Tabla de simbolos con ambitos para el analisis semantico.
 *
 * OBJETIVO      : registrar cada identificador declarado (variables, arreglos, parametros
 *                 y funciones) en el ambito donde se declaro, buscarlo cuando se usa y
 *                 reportar los errores semanticos de declaracion. Tambien lleva el
 *                 contexto que necesitan otras validaciones: la funcion actual (para
 *                 return), si estamos dentro de un ciclo (para break) y cuantos principal hay.
 * ENTRADA       : llamadas desde las acciones semanticas de terminales.cup.
 * SALIDA        : simbolos encontrados, lista de errores semanticos y el reporte
 *                 tabla_simbolos_semantica.txt con todos los ambitos.
 * RESTRICCIONES : - Los ambitos forman una pila: global -> funcion -> bloque -> bloque...
 *                 - Al buscar se recorre la pila desde el ambito mas interno hacia el global.
 *                 - No se permite declarar un nombre que ya existe en el mismo ambito.
 *                 - Si PERMITIR_OCULTAR es false, tampoco se permite declarar un nombre que
 *                   ya existe en un ambito exterior (ej. local con el mismo nombre que una global).
 */
public class TablaSemantica {
 
    /*
     * Decision de diseno (confirmar con el profesor):
     * false -> una variable local NO puede llamarse igual que una global o un parametro.
     * true  -> si puede; la local "oculta" a la exterior mientras dure su ambito.
     */
    public static final boolean PERMITIR_OCULTAR = false;
 
    /* Un ambito: su nombre (para el reporte) y sus simbolos en orden de declaracion */
    public static class Ambito {
        public final String nombre;
        public final int nivel;
        public final LinkedHashMap<String, Simbolo> simbolos = new LinkedHashMap<>();
        Ambito(String nombre, int nivel) { this.nombre = nombre; this.nivel = nivel; }
    }
 
    /* Pila de ambitos activos: el tope es el ambito actual */
    private final Deque<Ambito> pila = new ArrayDeque<>();
 
    /* Todos los ambitos que existieron, en orden de apertura (para el reporte final) */
    private final List<Ambito> historial = new ArrayList<>();
 
    /* Errores semanticos encontrados (el analisis no se detiene) */
    private final List<String> errores = new ArrayList<>();
 
    /* Contexto para validar return y break */
    private Simbolo funcionActual = null;

    /* Generador de codigo intermedio (temporales para el resultado de las llamadas) */
    private GeneradorCodigo generador = null;

    /* usarGenerador - Lo llama el parser al crearse, para compartir el mismo generador */
    public void usarGenerador(GeneradorCodigo generador) { this.generador = generador; }
    private int profundidadCiclos = 0;
 
    /* true si la funcion actual ya tuvo al menos un return (con o sin valor);
       si es false al cerrar una funcion con tipo, falta el return */
    private boolean encontroReturn = false;
 
    /* Cuantas veces se declaro principal (debe ser exactamente 1) */
    private int cantidadPrincipal = 0;
 
    /* Contador para dar nombres unicos a los bloques en el reporte */
    private int contadorBloques = 0;
 
    /* Al crear la tabla se abre el ambito global */
    public TablaSemantica() { abrirAmbito("global"); }
 
    // ======================================================================
    // Ambitos
    // ======================================================================
 
    /*
     * abrirAmbito - Crea un ambito nuevo encima de la pila.
     * ENTRADA : nombre para el reporte (ej. "funcion suma").
     */
    public void abrirAmbito(String nombre) {
        Ambito nuevo = new Ambito(nombre, pila.size());
        pila.push(nuevo);
        historial.add(nuevo);
    }
 
    /* abrirBloque - Abre un ambito para un bloque ¿: :? con nombre automatico */
    public void abrirBloque() { abrirAmbito("bloque " + (++contadorBloques)); }
 
    /* abrirFor - Abre el ambito del encabezado de un for (ahi vive la variable de control) */
    public void abrirFor() { abrirAmbito("for " + (++contadorBloques)); }
 
    /*
     * volverAGlobal - Cierra todos los ambitos hasta dejar solo el global.
     * Se usa al terminar cada funcion: si un error sintactico dejo algun bloque sin
     * cerrar, aqui se corrige para que la siguiente funcion empiece limpia.
     */
    public void volverAGlobal() {
        while (pila.size() > 1) pila.pop();
    }
 
    /* cerrarAmbito - Quita el ambito actual (nunca quita el global) */
    public void cerrarAmbito() {
        if (pila.size() > 1) pila.pop();
    }
 
    /* Nombre del ambito actual */
    public String ambitoActual() { return pila.peek().nombre; }
 
    // ======================================================================
    // Declarar y buscar
    // ======================================================================
 
    /*
     * declarar - Agrega un simbolo al ambito actual.
     * ENTRADA : el simbolo con nombre, tipo, categoria, linea y columna.
     * SALIDA  : true si se declaro; false si el nombre ya existia (y se reporta el error).
     */
    public boolean declarar(Simbolo nuevo) {
        Simbolo existente = pila.peek().simbolos.get(nuevo.nombre);
        if (existente == null && !PERMITIR_OCULTAR) existente = buscar(nuevo.nombre);
        if (existente != null) {
            error(nuevo.linea, nuevo.columna, "'" + nuevo.nombre + "' ya fue declarado en la linea "
                    + existente.linea + " (ambito " + existente.ambito + ")");
            return false;
        }
        nuevo.ambito = pila.peek().nombre;
        pila.peek().simbolos.put(nuevo.nombre, nuevo);
        return true;
    }
 
    /*
     * buscar - Busca un nombre desde el ambito actual hacia el global.
     * SALIDA : el simbolo, o null si no esta declarado (NO reporta error).
     */
    public Simbolo buscar(String nombre) {
        for (Ambito ambito : pila) {       // ArrayDeque itera desde el tope (mas interno)
            Simbolo encontrado = ambito.simbolos.get(nombre);
            if (encontrado != null) return encontrado;
        }
        return null;
    }
 
    /*
     * buscarDeclarado - Busca un identificador que se esta usando en una expresion o sentencia.
     * ENTRADA : nombre, linea y columna donde se usa.
     * SALIDA  : el simbolo, o null si no existe (y en ese caso reporta el error).
     */
    public Simbolo buscarDeclarado(String nombre, int linea, int columna) {
        Simbolo encontrado = buscar(nombre);
        if (encontrado == null) error(linea, columna, "'" + nombre + "' no ha sido declarado");
        return encontrado;
    }
 
    // ======================================================================
    // Contexto: funcion actual, ciclos y principal
    // ======================================================================
 
    /* Se llama al entrar al cuerpo de una funcion (null al salir) */
    public void setFuncionActual(Simbolo funcion) { funcionActual = funcion; }
    public Simbolo getFuncionActual() { return funcionActual; }
 
    /* Se llama al entrar/salir de un while o for (para validar break) */
    public void entrarCiclo() { profundidadCiclos++; }
    public void salirCiclo()  { if (profundidadCiclos > 0) profundidadCiclos--; }
    public boolean dentroDeCiclo() { return profundidadCiclos > 0; }
 
    /*
     * registrarPrincipal - Cuenta las declaraciones de principal.
     * Si ya habia uno, reporta el error en la linea del segundo.
     * SALIDA : true si es el primero (valido), false si es repetido.
     */
    public boolean registrarPrincipal(int linea, int columna) {
        cantidadPrincipal++;
        if (cantidadPrincipal > 1) {
            error(linea, columna, "el programa ya tiene un procedimiento principal (solo se permite uno)");
            return false;
        }
        return true;
    }
 
    /* verificarPrincipal - Se llama al final del programa: debe existir al menos uno */
    public void verificarPrincipal() {
        if (cantidadPrincipal == 0)
            errores.add("Error semantico: el programa no tiene procedimiento principal");
    }
 
    // ======================================================================
    // Acciones que llama el parser (terminales.cup)
    // Cada metodo corresponde a una produccion; el comentario dice cual.
    // ======================================================================
 
    /*
     * entrarFuncion - Al leer el encabezado de una funcion, antes de su cuerpo.
     *                 (produccion: funcion)
     * ENTRADA : tipo de retorno, nombre, posicion y parametros (ya como Simbolo).
     * HACE    : declara la funcion en el ambito global, abre el ambito de la funcion,
     *           declara los parametros ahi y la marca como funcion actual.
     * NOTA    : la funcion se declara ANTES del cuerpo, por eso se permite la recursion.
     */
    public void entrarFuncion(Tipo tipoRetorno, String nombre, int linea, int columna, List<Simbolo> parametros) {
        volverAGlobal();
        Simbolo funcion = new Simbolo(nombre, tipoRetorno, Simbolo.Categoria.FUNCION, linea, columna);
        for (Simbolo parametro : parametros) funcion.parametros.add(parametro.tipo);
        declarar(funcion);
        abrirAmbito("funcion " + nombre);
        for (Simbolo parametro : parametros) declarar(parametro);
        funcionActual = funcion;
        encontroReturn = false;
        profundidadCiclos = 0;
    }
 
    /*
     * entrarPrincipal - Al leer "void principal є: :э", antes de su cuerpo.
     *                   (produccion: principal)
     * HACE    : cuenta el principal (error si es el segundo), lo registra en la tabla
     *           si es el primero, abre su ambito y lo marca como funcion actual.
     */
    public void entrarPrincipal(int linea, int columna) {
        volverAGlobal();
        Simbolo principal = new Simbolo("principal", Tipo.VOID, Simbolo.Categoria.FUNCION, linea, columna);
        if (registrarPrincipal(linea, columna)) {
            principal.ambito = pila.peek().nombre;
            pila.peek().simbolos.put(principal.nombre, principal);
        }
        abrirAmbito("principal");
        funcionActual = principal;
        encontroReturn = false;
        profundidadCiclos = 0;
    }
 
    /*
     * salirFuncion - Al terminar el cuerpo de una funcion o de principal.
     *                (produccion: funcion y principal)
     * HACE    : si la funcion tiene tipo y nunca retorno un valor, lo reporta;
     *           despues vuelve al ambito global y limpia el contexto.
     */
    public void salirFuncion() {
        if (funcionActual != null && funcionActual.tipo != Tipo.VOID && !encontroReturn)
            error(funcionActual.linea, funcionActual.columna, "la funcion '" + funcionActual.nombre
                    + "' debe retornar un valor de tipo " + funcionActual.tipo);
        volverAGlobal();
        funcionActual = null;
        profundidadCiclos = 0;
    }
 
    /*
     * verificarReturn - Valida un return contra el tipo de la funcion actual.
     *                   (produccion: return_stmt)
     * ENTRADA : tieneValor (false si es "return »"), el valor retornado (puede ser null
     *           si su tipo todavia no se calcula) y la posicion del return.
     * REGLAS  : - principal (void) no puede retornar un valor.
     *           - una funcion con tipo debe retornar un valor de ese tipo.
     */
    public void verificarReturn(boolean tieneValor, Expr valor, int linea, int columna) {
        if (funcionActual == null) return;   // funcion con encabezado invalido: no se valida
        encontroReturn = true;                   // si falta el valor se reporta aqui, no otra vez al cerrar
        if (funcionActual.tipo == Tipo.VOID) {
            if (tieneValor)
                error(linea, columna, "principal no puede retornar un valor");
            return;
        }
        if (!tieneValor) {
            error(linea, columna, "la funcion '" + funcionActual.nombre + "' debe retornar un valor de tipo "
                    + funcionActual.tipo);
            return;
        }
        if (!Tipo.asignable(funcionActual.tipo, Expr.tipoDe(valor)))
            error(linea, columna, "tipo de retorno incorrecto en '" + funcionActual.nombre + "': se esperaba "
                    + funcionActual.tipo + " pero se encontro " + valor.tipo);
    }

    /*
     * codigoReturn - Codigo intermedio de un return.
     *                (produccion: return_stmt, despues de verificarReturn)
     * ENTRADA : el valor retornado (null si es "return »").
     * SALIDA  : - con valor: el codigo del valor y   return t1
     *           - sin valor:                         return
     */
    public List<String> codigoReturn(Expr valor) {
        List<String> codigo = new ArrayList<>();
        if (valor == null) {
            codigo.add("return");
        } else {
            codigo.addAll(valor.codigo);
            codigo.add("return " + (valor.dir == null ? "?" : valor.dir));
        }
        return codigo;
    }
 
    /*
     * declararVariable - Registra una declaracion "val tipo nombre ...".
     *                    (produccion: declaracion y for_init)
     * ENTRADA : tipo, nombre, posicion y lo que vino despues del nombre (InfoDeclaracion).
     * REGLAS  : - no puede estar repetida (lo valida declarar).
     *           - arreglo: tamano mayor que 0; no mas valores que el tamano;
     *             cada valor del tipo del arreglo.
     *           - escalar: el valor inicial debe ser del tipo de la variable.
     */
    public void declararVariable(Tipo tipo, String nombre, int linea, int columna, InfoDeclaracion info) {
        if (info.esArreglo) {
            Simbolo arreglo = new Simbolo(nombre, tipo, Simbolo.Categoria.ARREGLO, linea, columna);
            arreglo.tamano = info.tamano;
            declarar(arreglo);
            if (info.tamano <= 0)
                error(info.lineaTamano, info.columnaTamano, "el tamano del arreglo '" + nombre
                        + "' debe ser un entero mayor que 0");
            if (info.valores != null) {
                if (info.tamano > 0 && info.valores.size() > info.tamano)
                    error(linea, columna, "el arreglo '" + nombre + "' tiene tamano " + info.tamano
                            + " pero se inicializa con " + info.valores.size() + " valores");
                for (int posicion = 0; posicion < info.valores.size(); posicion++) {
                    Expr valor = info.valores.get(posicion);
                    if (!Tipo.asignable(tipo, Expr.tipoDe(valor)))
                        error(posLinea(valor, linea), posColumna(valor, columna), "el valor " + (posicion + 1)
                                + " del arreglo '" + nombre + "' debe ser " + tipo + " pero es " + valor.tipo);
                }
            }
        } else {
            declarar(new Simbolo(nombre, tipo, Simbolo.Categoria.VARIABLE, linea, columna));
            if (info.inicial != null && !Tipo.asignable(tipo, info.inicial.tipo))
                error(info.lineaInicial, info.columnaInicial, "no se puede asignar un valor " + info.inicial.tipo
                        + " a la variable '" + nombre + "' de tipo " + tipo);
        }
    }
 
    /*
     * usarVariable - Uso de un identificador sin indice como valor o destino.
     *                (produccion: variable)
     * SALIDA  : Expr con el tipo de la variable; ERROR si no existe o si es una funcion.
     * NOTA    : si es un arreglo sin indice, marca arregloCompleto = true para que las
     *           operaciones que no lo aceptan lo rechacen.
     */
    public Expr usarVariable(String nombre, int linea, int columna) {
        Simbolo variable = buscarDeclarado(nombre, linea, columna);
        if (variable == null) return Expr.error(linea, columna);
        if (variable.esFuncion()) {
            error(linea, columna, "'" + nombre + "' es una funcion; para llamarla use " + nombre + " є: ... :э");
            return Expr.error(linea, columna);
        }
        Expr resultado = new Expr(variable.tipo, linea, columna);
        resultado.simbolo = variable;
        resultado.arregloCompleto = variable.esArreglo();
        return resultado;
    }
 
    /*
     * usarArreglo - Acceso "nombre ʃ: indice :ʅ".
     *               (produccion: acceso_arreglo)
     * REGLAS  : el nombre debe ser un arreglo y el indice debe ser int.
     * SALIDA  : Expr con el tipo de los elementos del arreglo; ERROR si algo falla.
     */
    public Expr usarArreglo(String nombre, int linea, int columna, Expr indice) {
        Simbolo arreglo = buscarDeclarado(nombre, linea, columna);
        if (arreglo == null) return Expr.error(linea, columna);
        if (!arreglo.esArreglo()) {
            error(linea, columna, "'" + nombre + "' no es un arreglo, no se puede usar con indice");
            return Expr.error(linea, columna);
        }
        Tipo tipoIndice = Expr.tipoDe(indice);
        if (tipoIndice != null && !tipoIndice.esError() && tipoIndice != Tipo.INT)
            error(posLinea(indice, linea), posColumna(indice, columna), "el indice del arreglo '" + nombre
                    + "' debe ser int pero es " + tipoIndice);
        Expr resultado = new Expr(arreglo.tipo, linea, columna);
        resultado.simbolo = arreglo;
        return resultado;
    }
 
    /*
     * llamarFuncion - Llamada "nombre є: arg1, arg2 :э".
     *                 (produccion: llamada_funcion)
     * REGLAS  : la funcion debe existir (declarada antes o ser la actual: recursion),
     *           la cantidad de argumentos debe coincidir y cada uno debe ser del tipo
     *           de su parametro.
     * SALIDA  : Expr con el tipo de retorno de la funcion; ERROR si no existe.
     *           Ademas genera su codigo intermedio:
     *               <codigo de cada argumento>
     *               param t1
     *               param t2
     *               t3 = call nombre, 2     (si es void: solo  call nombre, 2)
     */
    public Expr llamarFuncion(String nombre, int linea, int columna, List<Expr> argumentos) {
        Simbolo funcion = buscar(nombre);
        if (funcion == null) {
            error(linea, columna, "la funcion '" + nombre + "' no ha sido declarada");
            return Expr.error(linea, columna);
        }
        if (!funcion.esFuncion()) {
            error(linea, columna, "'" + nombre + "' no es una funcion");
            return Expr.error(linea, columna);
        }
        if (argumentos.size() != funcion.parametros.size()) {
            error(linea, columna, "la funcion " + funcion.firma() + " espera " + funcion.parametros.size()
                    + " argumento(s) pero recibio " + argumentos.size());
        } else {
            for (int posicion = 0; posicion < argumentos.size(); posicion++) {
                Expr argumento = argumentos.get(posicion);
                Tipo tipoEsperado = funcion.parametros.get(posicion);
                if (!Tipo.asignable(tipoEsperado, Expr.tipoDe(argumento)))
                    error(posLinea(argumento, linea), posColumna(argumento, columna), "el argumento "
                            + (posicion + 1) + " de " + funcion.firma() + " debe ser " + tipoEsperado
                            + " pero es " + argumento.tipo);
            }
        }
        return codigoLlamada(funcion, linea, columna, argumentos);
    }

    /*
     * codigoLlamada - Arma el codigo intermedio de una llamada ya validada.
     * ENTRADA : la funcion llamada, la posicion y los argumentos (cada uno con su codigo).
     * SALIDA  : Expr del tipo de retorno; si no es void, el resultado queda en un temporal.
     * NOTA    : primero se calculan TODOS los argumentos y despues se hacen los param
     *           seguidos, asi una llamada dentro de un argumento no mezcla sus param.
     */
    private Expr codigoLlamada(Simbolo funcion, int linea, int columna, List<Expr> argumentos) {
        Expr resultado = new Expr(funcion.tipo, linea, columna);
        if (generador == null) return resultado;
        for (Expr argumento : argumentos)
            if (argumento != null) resultado.codigo.addAll(argumento.codigo);
        for (Expr argumento : argumentos)
            resultado.codigo.add("param " + (argumento == null || argumento.dir == null ? "?" : argumento.dir));
        String llamada = "call " + funcion.nombre + ", " + argumentos.size();
        if (funcion.tipo == Tipo.VOID) {
            resultado.codigo.add(llamada);
        } else {
            resultado.dir = generador.nuevoTemporal();
            resultado.codigo.add(resultado.dir + " = " + llamada);
        }
        return resultado;
    }
 
    /* Posicion de una expresion, o la de respaldo si la expresion no la tiene */
    private int posLinea(Expr expresion, int respaldo) {
        return (expresion != null && expresion.linea > 0) ? expresion.linea : respaldo;
    }
    private int posColumna(Expr expresion, int respaldo) {
        return (expresion != null && expresion.linea > 0) ? expresion.columna : respaldo;
    }
 
    // ======================================================================
    // Errores y reportes
    // ======================================================================
 
    /* error - Registra un error semantico con linea y columna; el analisis continua */
    public void error(int linea, int columna, String mensaje) {
        errores.add("Error semantico en linea " + linea + ", columna " + columna + ": " + mensaje);
    }
 
    public List<String> getErrores() { return errores; }
 
    /*
     * escribirTabla - Escribe todos los ambitos con sus simbolos.
     * SALIDA : texto con un bloque por ambito, indentado segun su nivel.
     */
    public void escribirTabla(PrintWriter out) {
        out.println("==== TABLA DE SIMBOLOS SEMANTICA ====");
        out.println("Informacion almacenada: nombre | categoria | tipo | posicion | tamano o firma");
        out.println();
        for (Ambito ambito : historial) {
            String sangria = "  ".repeat(ambito.nivel);
            out.println(sangria + "[" + ambito.nombre + "]");
            if (ambito.simbolos.isEmpty()) out.println(sangria + "  (sin declaraciones)");
            for (Simbolo simbolo : ambito.simbolos.values()) out.println(sangria + simbolo);
            out.println();
        }
    }
}
 