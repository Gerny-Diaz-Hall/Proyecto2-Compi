package compilador;

import java.util.List;

/*
 * Rama - Un elif dentro de un if: su condicion y el codigo de su bloque.
 *
 * OBJETIVO      : juntar los elif en una lista para que cuando el parser termina de leer
 *                 todo el if, AnalizadorExpresiones arme el codigo de todas las ramas con
 *                 sus etiquetas.
 * ENTRADA       : el Expr de la condicion (con su codigo) y el codigo del bloque.
 * SALIDA        : un objeto que guarda las dos cosas.
 * RESTRICCIONES : condicion puede ser null si la condicion tuvo un error sintactico.
 */
public class Rama {
    public final Expr condicion;
    public final List<String> cuerpo;

    public Rama(Expr condicion, List<String> cuerpo) {
        this.condicion = condicion;
        this.cuerpo = cuerpo;
    }
}
