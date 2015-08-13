package hextostring.tests;

import hextostring.Main;
import hextostring.Options;
import hextostring.convert.Converter;
import hextostring.convert.ConverterFactory;
import hextostring.debug.DebuggableLineList;
import hextostring.format.Formatter;
import hextostring.format.FormatterFactory;
import hextostring.utils.Charsets;
import hextostring.utils.IndentablePrintStream;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;

/**
 * Main test class.
 *
 * A test consists of an input and a corresponding expected,
 * manually written output.
 *
 * Every input file is converted and compared to the expected output.
 *
 * Test files are put into the "tests" directory at the root of the project.
 * Its architecture is as follows:
 *
 *   tests
 *   |-sjis (contains tests for games using Shift JIS)
 *   | |-[game1]
 *   | | |-input
 *   | | | |-0001.txt
 *   | | | |-0002.txt
 *   | | | |-...
 *   | | |-expected_output
 *   | |   |-0001.txt
 *   | |   |-0002.txt
 *   | |   |-...
 *   | |-[game2]
 *   | |-...
 *   |-utf16  (contains tests for games using UTF-16)
 *     |-[gameA]
 *     | |-input
 *     | | |-0001.txt
 *     | | |-0002.txt
 *     | | |-...
 *     | |-expected_output
 *     |   |-0001.txt
 *     |   |-0002.txt
 *     |   |-...
 *     |-[gameB]
 *     |-...
 *
 * It is assumed that all .txt files are encoded in UTF-8, without the BOM.
 *
 * @author Maxime PIA
 */
public class TestsLauncher {

	private static Formatter formatter =
		FormatterFactory.getFormatterInstance(true);
	private static Converter currentConverter;
	private static IndentablePrintStream out = new IndentablePrintStream();

	private static File[] listSortedFiles(File f) {
		File[] files = f.listFiles();
		Arrays.sort(files);
		return files;
	}

	private static void compareAll(File inputDirectory,
		File expectedOutputDirectory, int indentLevel) {

		File[] inputs = listSortedFiles(inputDirectory);
		File[] expectedOutputs = listSortedFiles(expectedOutputDirectory);

		for (int i = 0; i < inputs.length && i < expectedOutputs.length; ++i) {
			try {
				String input = new String(
					Files.readAllBytes(inputs[i].toPath()),
					Charsets.UTF8
				);
				String expectedOutput = new String(
					Files.readAllBytes(expectedOutputs[i].toPath()),
					Charsets.UTF8
				);

				DebuggableLineList dInput = currentConverter.convert(input);
				formatter.format(dInput);
				String actualOutput = dInput.toString(
					Options.DEFAULT_DEBUG_LEVEL,
					Options.DEFAULT_STRICTNESS
				);
				out.print(inputs[i].getName(), indentLevel);

				if (expectedOutput.equals(actualOutput)) {
					out.println(" OK ");
				} else {
					out.println(" FAILURE");
					out.println("expected:", indentLevel + 1);
					out.println(expectedOutput, indentLevel + 2);
					out.println("instead of:", indentLevel + 1);
					out.println(actualOutput, indentLevel + 2);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static void goThrough(File f, int indentLevel) {
		out.println(f.getName(), indentLevel);

		if (f.getName().equals("sjis")) {
			currentConverter =
				ConverterFactory.getConverterInstance(Charsets.SHIFT_JIS);
		} else if (f.getName().equals("utf16")) {
			currentConverter =
				ConverterFactory.getConverterInstance(Charsets.UTF16);
		}

		if (f.isDirectory()) {
			File[] files = listSortedFiles(f);
			boolean goFurther = true;

			if (files.length == 2) {
				if (files[0].getName().equals("expected_output")) {
					compareAll(files[1], files[0], indentLevel + 1);
					goFurther = false;
				}
			}

			// recursively go through every directory
			if (goFurther) {
				for (File children : files) {
					goThrough(children, indentLevel + 1);
				}
			}
		}
	}

	/**
	 * Starts the test campaign.
	 *
	 * @param args
	 * 			See {@link hextostring.Options options}
	 */
	public static void main(String[] args) {
		URL currentURL =
			Main.class.getProtectionDomain().getCodeSource().getLocation();
		File f = new File(currentURL.getPath() + "../tests");
		goThrough(f, 0);
	}

}