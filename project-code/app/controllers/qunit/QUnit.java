package controllers.qunit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import models.qunit.TestResult;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import play.Logger;
import play.Logger.ALogger;
import play.Play;
import play.api.templates.Xml;
import play.modules.qunit.QUnitPlugin;
import play.mvc.Controller;
import play.mvc.Http.RequestBody;
import play.mvc.Result;
import play.mvc.BodyParser;

public class QUnit extends Controller {
	private static final ALogger logger = Logger.of("qunit");
	private static final ObjectMapper mapper = new ObjectMapper();
	private static final Charset UTF8 = Charset.forName("UTF-8");
	private static final String TEST_FOLDER = "public" + File.separatorChar + "test" + File.separatorChar + "javascript";
	
	/**
	 * List of the available qunit tests.
	 */
	public static Result list() {
		List<String> testFiles = findQUnitTests();
		return ok(views.html.qunit.QUnit.list.render(testFiles));
	}

	/**
	 * Run a qunit test.
	 * 
	 * @param test
	 *            The path of the test to run.
	 */
	public static Result run(String test) {
		logger.debug("Running test: "+test);
		return redirect(test);
	}

	/**
	 * Writes the result to a junit test result file
	 * 
	 * @param result
	 */
	public static Result result() {
		QUnitPlugin qUnitPlugin = Play.application().plugin(QUnitPlugin.class);
		if (!qUnitPlugin.enabled()) {
			return notFound();
		}
		try {
			JsonNode json = request().body().asJson();
			TestResult testResult = mapper.readValue(json, TestResult.class);	 
			Xml xml = views.xml.qunit.QUnit.xunit.render(testResult);
			 String testResultPath = getTestResultsPath();
			 File testResultsDir = new File(testResultPath);
			 if(!testResultsDir.exists()){
				 testResultsDir.mkdirs();
			 }
			 File xmlFile = new File(testResultPath + testResult.getFQName() + ".xml");
			 if(xmlFile.exists()) {
				 xmlFile.delete();
			 }
			 FileOutputStream fos = new FileOutputStream(xmlFile);
			 FileChannel fc = fos.getChannel();
			 fc.write(ByteBuffer.wrap(xml.body().getBytes(UTF8)));
			 fos.flush();
			 fos.close();
			return created();
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			return internalServerError(e.getMessage());
		}
	}

	private static String getTestResultsPath() {
		StringBuffer sb = new StringBuffer();
		sb.append(Play.application().path().getPath()).append(File.separatorChar);
		sb.append("target").append(File.separatorChar);
		sb.append("test-reports").append(File.separatorChar);	
		return sb.toString();
	};

	/**
	 * Finds qunit tests in all modules and the application.
	 * 
	 * @return List of html files that contain the QUnit tests.
	 */
	public static List<String> findQUnitTests() {
		List<String> result;
		File testFolder = Play.application().getFile(TEST_FOLDER);
		logger.debug("Looking for tests in: "+testFolder);
		if (testFolder.isDirectory()) {
			File[] files = testFolder.listFiles(new QunitFileFilter());		
			result = relativiseFilenames(files);
		} else {
			result = Collections.emptyList();
		}
		return result;
	}
	
	private static List<String> relativiseFilenames(File[] files) {
		List<String> result = new ArrayList<String>();
		String publicPath = Play.application().getFile("public").getPath();
		for(File file : files) {
			String test = "/assets" + file.getPath().substring(publicPath.length(),file.getPath().length());
			result.add(test);
			logger.debug("Added test: " +test);
		}
		return result;
	}
	
	private static class QunitFileFilter implements FilenameFilter {
		//private static final Pattern namePattern = Pattern.compile("html");
		@Override
		public boolean accept(File dir, String name) {
			return name.endsWith(".html")? true : false;
		}
	}

}