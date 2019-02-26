/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.poshi.runner;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import com.liferay.poshi.runner.elements.PoshiElement;
import com.liferay.poshi.runner.elements.PoshiNodeFactory;
import com.liferay.poshi.runner.script.PoshiScriptParserException;
import com.liferay.poshi.runner.util.Dom4JUtil;
import com.liferay.poshi.runner.util.FileUtil;
import com.liferay.poshi.runner.util.OSDetector;

import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;

import org.apache.tools.ant.DirectoryScanner;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.util.NodeComparator;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static junit.framework.TestCase.fail;

/**
 * @author Kenji Heigel
 */
public class PoshiScriptEvaluator {

	public static final String poshiDirName =
		"/Users/kenji/Projects/github/" +
//			"liferay-portal/master/" +
//			"liferay-portal-ee/7.1.x/" +
//			"liferay-portal-ee/7.0.x/" +
//			"liferay-portal-ee/ee-6.2.x/" +
//			"liferay-portal-ee/ee-6.2.10/" +
//			"liferay-portal-ee/ee-6.1.x/" +
//			"com-liferay-commerce/";
 			"liferay-qa-portal-legacy-ee/";
//			"liferay-qa-websites-ee/sync/";
//				"portal-web/test/functional/com/liferay/portalweb/";

	@Test
	public void evaluateFunctionsXML() {
		List<Result> results = new ArrayList<>();

		for (String functionFilePath : getFunctionFilePaths()) {
			try {
				results.add(evaluateXMLFile(functionFilePath));
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}

		System.out.println("Function results:");

		evaluateResults(results);
	}

	@Test
	public void evaluateMacrosXML() throws Exception {
		List<Result> results = new ArrayList<>();

		for (String macroFilePath : getMacroFilePaths()) {
			try {
				results.add(evaluateXMLFile(macroFilePath));
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}

		System.out.println("Macro results:");

		evaluateResults(results);
	}

	@Test
	public void evaluateTestCasesXML() throws Exception {
		List<Result> results = new ArrayList<>();

		for (String testCaseFilePath : getTestCaseFilePaths()) {
			try {
				results.add(evaluateXMLFile(testCaseFilePath));
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}

		System.out.println("Testcase results:");

		evaluateResults(results);
	}

	@BeforeClass
	public static void setUp() throws Exception {
		String[] poshiFileNames = {"**/*.function"};

		PoshiRunnerContext.readFiles(poshiFileNames, poshiDirName);

		PoshiNodeFactory.setValidatePoshiScript(false);

//		PoshiRunnerContext.readFiles(poshiFileNames, "/Users/kenji/Projects/github/liferay-qa-websites-ee/shared");
	}

	protected static void init() {
		if (!FileUtil.exists(poshiDirName)) {
			System.out.print(poshiDirName + " does not exist");

			return;
		}

		DirectoryScanner directoryScanner = new DirectoryScanner();

		directoryScanner.setBasedir(poshiDirName);

		String[] includes = {"**\\*.function", "**\\*.macro", "**\\*.testcase"};

		directoryScanner.setIncludes(includes);

		directoryScanner.scan();

		for (String filePath : directoryScanner.getIncludedFiles()) {
			filePath = poshiDirName + filePath;

			if (OSDetector.isWindows()) {
				filePath = filePath.replace("/", "\\");
			}

			if (filePath.endsWith(".function")) {
				_functionFilePaths.add(filePath);

				continue;
			}

			if (filePath.endsWith(".macro")) {
				_macroFilePaths.add(filePath);

				continue;
			}

			if (filePath.endsWith(".testcase")) {
				_testCaseFilePaths.add(filePath);

				continue;
			}
		}
	}

	protected void evaluateResults(List<Result> results) {
		int successfulCommands = 0;
		int succesfulFiles = 0;
		int totalCommands = 0;
		int totalFiles = 0;

		for (Result result : results) {
			int successfulPerFile = result.getSuccessfulCommandElements();
			int totalPerFile = result.getTotalCommandElements();

			totalFiles++;

			if (successfulPerFile == totalPerFile) {
				succesfulFiles++;
			}

			successfulCommands = successfulCommands + successfulPerFile;
			totalCommands = totalCommands + totalPerFile;
		}

		StringBuilder sb = new StringBuilder();

		sb.append("\n");
		sb.append(successfulCommands);
		sb.append(" / ");
		sb.append(totalCommands);
		sb.append(" (");
		sb.append((successfulCommands * 100 / totalCommands));
		sb.append("%) commands were successfuly translated.\n");

		sb.append(succesfulFiles);
		sb.append(" / ");
		sb.append(totalFiles);
		sb.append(" (");
		sb.append((succesfulFiles * 100 / totalFiles));
		sb.append("%) files were successfuly translated.\n");

		if(succesfulFiles != totalFiles) {
			fail(sb.toString());
		}

		System.out.println(sb.toString());
	}

	protected Result evaluateXMLFile(String filePath)
		throws DocumentException, IOException, PoshiScriptTranslationException,
			RuntimeException {

		URL url = FileUtil.getURL(new File(filePath));

		PoshiElement poshiElement =
			(PoshiElement)PoshiNodeFactory.newPoshiNodeFromFile(url);

		String fileContent = FileUtil.read(filePath);

		int start = filePath.lastIndexOf("/");

		String fileName = filePath.substring(start + 1, filePath.length());

		if (!fileContent.trim().startsWith("<definition")) {
			throw new PoshiScriptTranslationException(
				filePath + " is not an XML file, could not evaluate XML");
		}

		Document document = Dom4JUtil.parse(fileContent);

		Element rootElement = document.getRootElement();

		Dom4JUtil.removeWhiteSpaceTextNodes(rootElement);

		Multimap<String, Element> rootChildElementMultiMap =
			ArrayListMultimap.create();

		for (Iterator i = rootElement.elementIterator(); i.hasNext();) {
			Element element = (Element)i.next();

			rootChildElementMultiMap.put(element.getName(), element);
		}

		int commandElementFailures = 0;
		int failingDualTranslationCommandElements = 0;
		int successfulCommandElements = 0;
		int totalCommandElements =
			rootChildElementMultiMap.get("command").size();

		if (areElementsEqual(rootElement, poshiElement)) {
			try {
				String poshiScript = poshiElement.toPoshiScript();

				PoshiElement newPoshiElement =
					(PoshiElement)PoshiNodeFactory.newPoshiNode(
						poshiScript, url);

				if (areElementsEqual(rootElement, newPoshiElement)) {
					return new Result(
						0,0, filePath, totalCommandElements,
						totalCommandElements);
				}
			}
			catch (Exception e) {
				System.out.println(filePath + " not translateable.");

				e.printStackTrace();

				return new Result(0, 0, filePath, 0, totalCommandElements);
			}
		}

		Multimap<String, Element> poshiChildElementMultiMap =
			ArrayListMultimap.create();

		for (Iterator i = poshiElement.elementIterator(); i.hasNext();) {
			Element element = (Element)i.next();

			poshiChildElementMultiMap.put(element.getName(), element);
		}

		for (String tagName : rootChildElementMultiMap.keySet()) {
			List<Element> rootChildElements =
				new ArrayList(rootChildElementMultiMap.get(tagName));
			List<PoshiElement> poshiChildElements =
				new ArrayList(poshiChildElementMultiMap.get(tagName));

			for (int i = 0; i < poshiChildElements.size(); i++) {
				Element rootChildElement = rootChildElements.get(i);
				PoshiElement poshiChildElement = poshiChildElements.get(i);

				if (!areElementsEqual(rootChildElement, poshiChildElement)) {
					if (tagName.equals("command")){
						System.out.println(
							fileName + "#" +
								rootChildElement.attributeValue("name"));

						commandElementFailures++;
					}

					String rootChildString = Dom4JUtil.format(rootChildElement);
					String poshiChildString = Dom4JUtil.format(
						poshiChildElement);

					Patch patch = DiffUtils.diff(
						stringToLines(rootChildString),
						stringToLines(poshiChildString));

					for (Delta delta : patch.getDeltas()) {
						StringCompare.print(delta);
					}

					continue;
				}

				try {
					String childPoshiScript = poshiChildElement.toPoshiScript();

					PoshiElement newChildPoshiElement =
						(PoshiElement) PoshiNodeFactory.newPoshiNode(
							poshiElement, childPoshiScript);

					if (areElementsEqual(
							rootChildElement, newChildPoshiElement)) {

						if (tagName.equals("command")){
							successfulCommandElements++;
						}
					}
					else {
						if (tagName.equals("command")){
							System.out.println(
								fileName + "#" +
								rootChildElement.attributeValue("name"));

							failingDualTranslationCommandElements++;
						}

						String baseString = Dom4JUtil.format(rootChildElement);
						String newString = Dom4JUtil.format(
							newChildPoshiElement);

						Patch patch = DiffUtils.diff(
							stringToLines(baseString),
							stringToLines(newString));

						for (Delta delta : patch.getDeltas()) {
							StringCompare.print(delta);
						}

					}
				}
				catch (PoshiScriptParserException pspe) {
					failingDualTranslationCommandElements++;

					System.out.println(fileName);

					pspe.printStackTrace();
				}
			}
		}

		return new Result(
			commandElementFailures, failingDualTranslationCommandElements,
			filePath, successfulCommandElements, totalCommandElements);
	}

	public static boolean areElementsEqual(Element element1, Element element2) {
		NodeComparator nodeComparator = new NodeComparator();

		int compare = 1;

		try {
			compare = nodeComparator.compare(element1, element2);
		}
		catch (Exception e) {
		}

		if (compare == 0) {
			return true;
		}

		return false;
	}

	private static List<String> stringToLines(String s) {
		BufferedReader br = null;
		String line = "";
		List<String> lines = new LinkedList<String>();

		try {
			br = new BufferedReader(new StringReader(s));
			while ((line = br.readLine()) != null) {
				lines.add(line);
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (br != null) {
				try {
					br.close();
				}
				catch (IOException e) {
				}
			}
		}
		return lines;
	}

	protected static Set<String> getFunctionFilePaths() {
		return _functionFilePaths;
	}

	protected static Set<String> getMacroFilePaths() {
		return _macroFilePaths;
	}

	protected static Set<String> getTestCaseFilePaths() {
		return _testCaseFilePaths;
	}

	private static Set<String> _functionFilePaths = new TreeSet<>();
	private static Set<String> _macroFilePaths = new TreeSet<>();
	private static Set<String> _testCaseFilePaths = new TreeSet<>();

	static {
		init();
	}

	private class PoshiScriptTranslationException extends Exception {

		PoshiScriptTranslationException(String message) {
			super(message);
		}
	}

	private class Result {
		Result(
			int failingCommandElements,
			int failingDualTranslationCommandElements, String filePath,
			int successfulCommandElements, int totalCommandElements) {

			_failingCommandElements = failingCommandElements;
			_failingDualTranslationCommandElements =
				failingDualTranslationCommandElements;
			_filePath = filePath;
			_successfulCommandElements = successfulCommandElements;
			_totalCommandElements = totalCommandElements;
		}

		public int getFailingCommandElements() {
			return _failingCommandElements;
		}

		public int getFailingDualTranslationCommandElements() {
			return _failingDualTranslationCommandElements;
		}

		public int getSuccessfulCommandElements() {
			return _successfulCommandElements;
		}

		public String getFilePath() {
			return _filePath;
		}

		public int getTotalCommandElements() {
			return _totalCommandElements;
		}

		private int _failingCommandElements;
		private int _failingDualTranslationCommandElements;
		private int _successfulCommandElements;
		private String _filePath;
		private int _totalCommandElements;
	}
}
