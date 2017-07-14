package com.qmethods.java_performance_antipattern_servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.function.Function;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@WebServlet("/Performer")
public class Performer extends HttpServlet {
	private static final long serialVersionUID = 123321123L;
	private static final String[] availableCases = {
			"concatStringsPlus",
			"concatStringsBuilder",
			"concatManyStringsPlus",
			"concatManyStringsBuilder"
	};
	
	private static final Long WARMUP = 20000L;
	private static Long runs;
	private static boolean warmup;
	private static boolean garbage;
	private static boolean sleep;
	private static long timeExpired;

	public Performer() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException
	{
		/* prepare HTML writer */
		response.setContentType("text/html");
		PrintWriter printWriter = response.getWriter();

		/* get global parameters and check for validity */
		String testCase = request.getParameter("case");
		int magnitude;
		try {
			magnitude = Integer.parseInt(request.getParameter("magnitude"));
		} catch (NumberFormatException e) {
			magnitude = -1;
		}
		warmup = (request.getParameter("warmup") != null && request.getParameter("warmup").equals("on"));
		garbage = (request.getParameter("garbage") != null && request.getParameter("garbage").equals("on"));
		sleep = (request.getParameter("sleep") != null && request.getParameter("sleep").equals("on"));

		if (testCase == null || !Arrays.asList(availableCases).contains(testCase) || magnitude < 3 || magnitude > 9) {
			printWriter.print(
					"<!doctype html>\n" +
					"<html>\n" +
					"<body>\n" +
					"\n" +
					"<h2>Fehler!</h2>\n" +
					"<br>\n" +
					"<p>Die Parameter 'case' und/oder 'magnitude' wurden nicht korrekt übergeben!</p>\n" +
					"\n" +
					"</body>\n" +
					"</html>\n");
			return;
		}

		runs = (long) Math.pow(10, magnitude);

		/* select and perform the respective benchmark */
		switch (testCase) {
		case "concatStringsPlus":
			benchmark(Performer::concatStringsWithPlus);
			break;
		case "concatStringsBuilder":
			benchmark(Performer::concatStringsWithBuilder);
			break;
		case "concatManyStringsPlus":
			benchmark(Performer::concatManyStringsWithPlus);
			break;
		case "concatManyStringsBuilder":
			benchmark(Performer::concatManyStringsWithBuilder);
			break;
		}

		/* prepare HTML result site */
		printWriter.print(
				"<!doctype html>\n" +
				"<html>\n" +
				"<body>\n" +
				"\n" +
				"<h2>Benchmark abgeschlossen!</h2>\n" +
				"<br>\n" +
				"<p><b>Ergebnis:</b> " + timeExpired/1000000.0 + " ms</p>" +
				"<p><b>Verwendete Optionen:</b><br>\n" +
				"Benchmark: " + testCase + "<br>\n" +
				"Durchläufe: 10^" + magnitude + " (" + runs + ")</p>\n" +
				"\n" +
				"<p>\n");
		if (warmup) {
			printWriter.print("Warm-Up durchführen<br>\n");
		}
		if (garbage) {
			printWriter.print("Garbage Collection vor dem Benchmark<br>\n");
		}
		if (sleep) {
			printWriter.print("Schlafe 1 ms bei jedem Durchlauf<br>\n");
		}
		printWriter.print(
				"</p>\n" +
				"\n" +
				"<a href=\"index.html\">Zurück</a><br>\n" +
				"<br>\n" +
				"<br>\n" +
				"<a href=\"https://www.qmethods.com\"><img src=\"https://www.qmethods.de/img/navigate/qmethods-logo-vertical.svg\"></a>" +
				"</body>\n" +
				"</html>\n");
	}

	/**
	 * Higher order function to run a benchmark with arbitrary load function. 
	 * 
	 * @param benchmarkFunction The load generating function that will be called.
	 */
	private static void benchmark(Function<Long,Integer> benchmarkFunction) {
		if (warmup) {
			benchmarkFunction.apply(WARMUP);
		}
		
		if (garbage) {
			System.gc();
		}
		
		long start = System.nanoTime();
		benchmarkFunction.apply(runs);
		long end = System.nanoTime();
		timeExpired = end - start;
	}
	
	/**
	 * Helper function to sleep 1 ms if configured. Does nothing otherwise. 
	 */
	private static void doSleep() {
		if (sleep) {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {  // as we have only one thread, we do not expect interrupts at all and will just exit
				e.printStackTrace();
				System.exit(0);
			}
		}
	}
	
	
	/* * * LOAD GENERATING METHODS * * */
	/* Must fulfill the signature: static Integer someName (Long) */ 
	
	private static Integer concatStringsWithPlus (Long runs) {
		final String[] parts = { "these", "are", "separate", "parts", "of", "a", "string", "that", "get", "connected" };
		
		for (long i = 0; i < runs; ++i) {
			String str = "Text:";
			for (int j = 0; j < 10; ++j) {
				str = str + " " + parts[j];
			}
			doSleep();
		}
		return 42;
	}
	
	private static Integer concatStringsWithBuilder (Long runs) {
		final String[] parts = { "these", "are", "separate", "parts", "of", "a", "string", "that", "get", "connected" };
		
		for (long i = 0; i < runs; ++i) {
			StringBuilder builder = new StringBuilder();
			builder.append("Text:");
			for (int j = 0; j < 10; ++j) {
				builder.append(" ");
				builder.append(parts[j]);
			}
			@SuppressWarnings("unused")
			String str = builder.toString();
			doSleep();
		}
		return 42;
	}

	private static Integer concatManyStringsWithPlus (Long runs) {
		for (long i = 0; i < runs; ++i) {
			String str = "Text:";
			for (int j = 0; j < 100; ++j) {
				str = str + " something";
			}
			doSleep();
		}
		return 42;
	}
	
	private static Integer concatManyStringsWithBuilder (Long runs) {
		for (long i = 0; i < runs; ++i) {
			StringBuilder builder = new StringBuilder();
			builder.append("Text:");
			for (int j = 0; j < 100; ++j) {
				builder.append(" something");
			}
			@SuppressWarnings("unused")
			String str = builder.toString();
			doSleep();
		}
		return 42;
	}
}
