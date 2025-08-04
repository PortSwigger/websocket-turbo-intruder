package interpreter;

import logger.Logger;
import org.python.core.PyFunction;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

import java.io.Closeable;
import java.util.List;

public class Interpreter implements Closeable {
    private final PythonInterpreter interpreter;

    public Interpreter(Logger logger) {
        interpreter = new PythonInterpreter();

        interpreter.setOut(logger.outputStream());
        interpreter.setErr(logger.errorStream());
    }

    public void setVariable(String name, Object value) {
        interpreter.set(name, value);
    }

    public void execute(String code) {
        interpreter.exec(code);
    }

    public PyObject call(String function, List<String> args) {
        PyObject[] pyArgs = args.stream().map(interpreter::get).toArray(PyObject[]::new);
        PyFunction func = interpreter.get(function, PyFunction.class);
        return func.__call__(pyArgs);
    }

    @Override
    public void close() {
        interpreter.close();
    }
}
