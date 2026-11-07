package compilador;

/*
 * Ciclo - Etiquetas de un while o de un for.
 *
 * OBJETIVO      : guardar las etiquetas que necesita un ciclo para su codigo intermedio.
 *                 Se crean al leer la palabra while/for (antes del cuerpo), asi un break
 *                 que aparece dentro del cuerpo ya sabe a que etiqueta saltar.
 * ENTRADA       : tres etiquetas nuevas creadas por GeneradorCodigo.
 * SALIDA        : las etiquetas que usa AnalizadorExpresiones al armar el ciclo.
 * RESTRICCIONES : - inicio : donde se evalua la condicion (el ciclo vuelve aqui).
 *                 - cuerpo : donde empieza el cuerpo (salto cuando la condicion es true).
 *                 - fin    : la salida del ciclo (condicion false o break).
 */
public class Ciclo {
    public final String inicio, cuerpo, fin;

    public Ciclo(String inicio, String cuerpo, String fin) {
        this.inicio = inicio;
        this.cuerpo = cuerpo;
        this.fin = fin;
    }
}
