package sv.edu.ues.alimentacion_hospitalaria.reportes.exception;

/** Interrumpe una carga limitada antes de recuperar filas que excedan el máximo indicado. */
public class LimiteResultadosException extends RuntimeException {
    private final long total;
    private final int limite;

    public LimiteResultadosException(long total, int limite) {
        super("La consulta supera el límite permitido de resultados.");
        this.total = total;
        this.limite = limite;
    }

    public long getTotal() { return total; }
    public int getLimite() { return limite; }
}
