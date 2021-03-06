package fr.loganbraga.hogwash;

import fr.loganbraga.hogwash.Language.Compiler;
import fr.loganbraga.hogwash.Front.Parameters;
import fr.loganbraga.hogwash.Front.Explainer;
import fr.loganbraga.hogwash.Error.*;
import java.io.File;
import java.util.ResourceBundle;
import java.util.Observer;
import java.util.Observable;
import org.fusesource.jansi.AnsiConsole;

public class Hogwash implements Observer {

	protected static final String VERSION = "0.1.0";
	protected static ResourceBundle ERROR_KEYS;
	protected Parameters parameters;

	public Hogwash(Parameters parameters) {
		this.parameters = parameters;
	}

	public void run() {
		if (!this.parameters.files.isEmpty()) {
			File inputFile = new File(this.parameters.files.get(0));

			int maxErrors = parameters.quickFail ? 1 : 50;
			ErrorReporter er = new ErrorReporter(maxErrors, ERROR_KEYS);
			if (parameters.noWarnings) er.setNoWarnings();
			if (parameters.strict) er.setWarningsToErrorsConversion();

			Compiler compiler = new Compiler(inputFile, er);
			compiler.addObserver(this);
			compiler.compile();
		}
	}

	protected void handleErrors(ErrorReporter er) {
		if (er.hasErrors()) {
			System.err.print(er.reportErrors(this.parameters.errorSorting));
			System.exit(1);
		}
		if (er.hasWarnings()) System.err.print(er.reportWarnings(this.parameters.errorSorting));
	}

	public void update(Observable o, Object message) {
		if (o instanceof Compiler)
			this.handleErrors(((Compiler) o).getErrorReporter());
	}

	public static void main(String[] args) {
		Hogwash.ERROR_KEYS = ResourceBundle.getBundle("errors.ErrorMessages");

		AnsiConsole.systemInstall();

		Hogwash hogwash = null;
		ErrorReporter preArgsER = new ErrorReporter(1, ERROR_KEYS);
		Parameters parameters = new Parameters("Hogwash", VERSION, preArgsER);

		try {
			parameters.parse(args);
			if (parameters.help || args.length == 0) {
				System.out.println(parameters.printHelp());
				System.exit(0);
			}

			if (parameters.explain != null) {
				Explainer explainer = new Explainer(preArgsER);
				System.out.println(explainer.explain(parameters.explain));
				System.exit(0);
			}

			hogwash = new Hogwash(parameters);
			hogwash.run();
		} catch (TooManyErrorsException e) { 
			System.err.print(e.getErrorReporter().reportErrors(parameters.errorSorting));
			System.exit(1);
		} catch (Exception e) {
			ErrorReporter exceptionalErrorRep = new ErrorReporter(50, ERROR_KEYS);
			exceptionalErrorRep.addError(new BaseError(
						new ErrorMessage(ErrorKind.EXCEPTIONAL_ERROR, e.getMessage())));
			System.err.print(exceptionalErrorRep.reportErrors(null));
			System.exit(2);
		}

		AnsiConsole.systemUninstall();
	}

}
