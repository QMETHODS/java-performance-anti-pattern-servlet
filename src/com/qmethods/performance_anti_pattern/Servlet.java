package com.qmethods.performance_anti_pattern;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.function.Function;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * The Performance Antipattern Servlet is an Apache Tomcat Servlet with the purpose to create different load scenarios
 * and to carry out micro benchmarks.
 * 
 * @author nikolai.moesus@qmethods.com
 * @version 1.0
 */
@WebServlet("/Servlet")
public class Servlet extends HttpServlet {
	private static final long serialVersionUID = 123321123L;
	private static final String[] availableCases = {
			"concatStringsPlus",
			"concatStringsBuilder",
			"concatManyStringsPlus",
			"concatManyStringsBuilder",
			"exception",
			"staticException",
			"noException",
			"exceptionRecursion10",
			"staticExceptionRecursion10",
			"noExceptionRecursion10",
			"exceptionRecursion100",
			"staticExceptionRecursion100",
			"noExceptionRecursion100",
			"microServiceDirect",
			"microServiceLocal",
			"microServiceLocalOtherTomcat",
			"microServiceRemote"
	};
	
	private static final Long WARMUP = 20000L;
	private static Long runs;
	private static boolean warmup;
	private static boolean garbage;
	private static boolean sleep;
	private static long timeExpired;

	public Servlet() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		/* prepare HTML writer */
		response.setContentType("text/html");
		PrintWriter printWriter = response.getWriter();

		/* check if servlet is used to respond to a micro call */
		if (request.getParameter("microcall") != null) {
			printWriter.print(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()));
			return;
		}
		
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
			benchmark(Servlet::concatStringsWithPlus);
			break;
		case "concatStringsBuilder":
			benchmark(Servlet::concatStringsWithBuilder);
			break;
		case "concatManyStringsPlus":
			benchmark(Servlet::concatManyStringsWithPlus);
			break;
		case "concatManyStringsBuilder":
			benchmark(Servlet::concatManyStringsWithBuilder);
			break;
		case "exception":
			benchmark(Servlet::exceptionAsControlFlow);
			break;
		case "staticException":
			benchmark(Servlet::staticExceptionAsControlFlow);
			break;
		case "noException":
			benchmark(Servlet::regularControlFlow);
			break;
		case "exceptionRecursion10":
			benchmark(Servlet::exceptionAsControlFlowRecursion10);
			break;
		case "staticExceptionRecursion10":
			benchmark(Servlet::staticExceptionAsControlFlowRecursion10);
			break;
		case "noExceptionRecursion10":
			benchmark(Servlet::regularControlFlowRecursion10);
			break;
		case "exceptionRecursion100":
			benchmark(Servlet::exceptionAsControlFlowRecursion100);
			break;
		case "staticExceptionRecursion100":
			benchmark(Servlet::staticExceptionAsControlFlowRecursion100);
			break;
		case "noExceptionRecursion100":
			benchmark(Servlet::regularControlFlowRecursion100);
			break;
		case "microServiceDirect":
			benchmark(Servlet::microserviceDirect);
			break;
		case "microServiceLocal":
			benchmark(Servlet::microserviceLocal);
			break;
		case "microServiceLocalOtherTomcat":
			benchmark(Servlet::microserviceLocalOtherTomcat);
			break;
		case "microServiceRemote":
			benchmark(Servlet::microserviceRemote);
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
			printWriter.print("Pausiere 1 ms bei jedem Durchlauf<br>\n");
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

	/**
	 * Higher order function to increase the function call stack.
	 * 
	 * @param depth How many recursive calls shall be made.
	 * @param func The load function that eventually shall be called.
	 * @param runs Required by the load function, simply passed on.
	 */
	private static void addRecursion(int depth, Function<Long,Integer> func, Long runs) {
		if (depth == 0) {
			func.apply(runs);
		} else {
			addRecursion(depth - 1, func, runs);
		}
	}

	/**
	 * Custom exception class without special capabilities.  
	 */
	static class FlowException extends Exception {
		private static final long serialVersionUID = 3564515923514860472L;

		public FlowException() {
		}

		public FlowException(String message) {
			super(message);
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
	
	private static Integer exceptionAsControlFlow(Long runs) {
		for (long run = 0; run < runs; ++run) {
			try {
//				int sum = 0;
//				for (int row = 0; row < 100; ++row) {
//					for (int col = 0; col < 100; ++col) {
//						sum += row + col;
//						if (sum >= 1000) {
							throw new FlowException();
//						}
//					}
//				}
			} catch (FlowException e) {
				// goto here
			}
			doSleep();
		}
		return 42;
	}

	private static Integer staticExceptionAsControlFlow(Long runs) {
		FlowException ex = new FlowException();
		for (long run = 0; run < runs; ++run) {
			try {
//				int sum = 0;
//				for (int row = 0; row < 100; ++row) {
//					for (int col = 0; col < 100; ++col) {
//						sum += row + col;
//						if (sum >= 1000) {
							throw ex;
//						}
//					}
//				}
			} catch (FlowException e) {
				// goto here
			}
			doSleep();
		}
		return 42;
	}

	private static Integer regularControlFlow(Long runs) {
		boolean foo = true;;
		for (long run = 0; run < runs; ++run) {

			if (foo) foo = false;
			else     foo = true;
			
//			int sum = 0;
//			for (int row = 0; row < 100 && sum < 1000; ++row) {
//				for (int col = 0; col < 100 && sum < 1000; ++col) {
//					sum += row + col;
//				}
//			}
			doSleep();
		}
		return 42;
	}

	private static Integer exceptionAsControlFlowRecursion10(Long runs) {
		addRecursion(10, Servlet::exceptionAsControlFlow, runs);
		return 42;
	}

	private static Integer staticExceptionAsControlFlowRecursion10(Long runs) {
		addRecursion(10, Servlet::staticExceptionAsControlFlow, runs);
		return 42;
	}

	private static Integer regularControlFlowRecursion10(Long runs) {
		addRecursion(10, Servlet::regularControlFlow, runs);
		return 42;
	}
	
	private static Integer exceptionAsControlFlowRecursion100(Long runs) {
		addRecursion(100, Servlet::exceptionAsControlFlow, runs);
		return 42;
	}

	private static Integer staticExceptionAsControlFlowRecursion100(Long runs) {
		addRecursion(100, Servlet::staticExceptionAsControlFlow, runs);
		return 42;
	}

	private static Integer regularControlFlowRecursion100(Long runs) {
		addRecursion(100, Servlet::regularControlFlow, runs);
		return 42;
	}
	
	private static Integer microserviceDirect(Long runs) {
		String res = null;
		for (long run = 0; run < runs; ++run) {
			res = fakeMicroservice();
			doSleep();
		}
		return 42 + (res.equals("response") ? 23 : 42);
	}
	
	private static String fakeMicroservice() {
		return (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()));
	}
	
	private static Integer microserviceLocal(Long runs) {
		final String url = "http://localhost:8080/Performance-Anti-Pattern-Servlet/Servlet?microcall=on";
		String res = null;
		for (long run = 0; run < runs; ++run) {
			res = callMicroservice(url);
			
			doSleep();
		}
		return 42 + (res.equals("response") ? 23 : 42);
	}
	
	private static Integer microserviceLocalOtherTomcat(Long runs) {
		final String url = "http://localhost:8081/Performance-Anti-Pattern-Servlet/Servlet?microcall=on";
		String res = null;
		for (long run = 0; run < runs; ++run) {
			res = callMicroservice(url);
			
			doSleep();
		}
		return 42 + (res.equals("response") ? 23 : 42);
	}
	
	// use IP as the dns lookup of computer name does not work here (but in browser it does)
	private static Integer microserviceRemote(Long runs) {
		final String url = "http://192.168.0.133:8080/Performance-Anti-Pattern-Servlet/Servlet?microcall=on";
		String res = null;
		for (long run = 0; run < runs; ++run) {
			res = callMicroservice(url);
			
			doSleep();
		}
		return 42 + (res.equals("response") ? 23 : 42);
	}

	/*
	 * The TCP connection stays alive although the HttpURLConnection object is not reusable.
	 * https://docs.oracle.com/javase/8/docs/technotes/guides/net/http-keepalive.html
	 */
    private static String callMicroservice (final String address) {
		try {
			URL url = new URL(address);

	        // make connection
	        HttpURLConnection connec = (HttpURLConnection) url.openConnection();

	        // use GET method
	        connec.setDoOutput(true);
	        connec.setDoInput(true);
	        connec.setInstanceFollowRedirects(false); 
	        connec.setRequestMethod("GET"); 
	        connec.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
	        connec.setRequestProperty("charset", "utf-8");
	        connec.setUseCaches (false);

	        BufferedReader in = new BufferedReader(new InputStreamReader(connec.getInputStream()));
	        String inputLine;
	        StringBuilder response = new StringBuilder();

	        while ((inputLine = in.readLine()) != null) {
	            response.append(inputLine);
	        }
	        in.close();

	        //print result
//	        System.out.println(response.toString());
	        return response.toString();
		} catch (MalformedURLException e) {
			e.printStackTrace();
	        return "fail";
		} catch (IOException e) {
			e.printStackTrace();
	        return "fail";
		}
    }
}
