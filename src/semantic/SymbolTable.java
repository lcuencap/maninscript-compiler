package semantic;

import java.util.HashMap;
import java.util.Map;

/**
 * Tabla de simbolos enlazada con un ambito padre opcional.
 */
public class SymbolTable {
    private final Map<String, Symbol> symbols = new HashMap<>();
    private final SymbolTable parent;

    public SymbolTable(SymbolTable parent) {
        this.parent = parent;
    }

    // Comprueba si un nombre existe solo en este ambito.
    public boolean isDefinedLocally(String name) {
        return symbols.containsKey(name);
    }

    // Registra un simbolo en el ambito actual.
    public void define(Symbol symbol) {
        symbols.put(symbol.getName(), symbol);
    }

    // Busca un simbolo en este ambito y luego en los padres.
    public Symbol resolve(String name) {
        if (symbols.containsKey(name)) {
            return symbols.get(name);
        }
        if (parent != null) {
            return parent.resolve(name);
        }
        return null;
    }
}
