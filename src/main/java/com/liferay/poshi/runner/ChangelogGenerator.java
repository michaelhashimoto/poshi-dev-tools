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

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClientFactory;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;

import java.net.URI;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Kenji Heigel
 */
public class ChangelogGenerator {
	public static void main(String[] args) throws Exception {
		File gitWorkDir = new File(
			"/Users/kenji/Projects/github/com-liferay-poshi-runner");

		Git git = Git.open(gitWorkDir);

		ObjectId lastPoshiReleaseSHA = ObjectId.fromString(
			"37033d4d9c84a0fcbc66cc83704263756ca74c5b");

		ObjectId newPoshiReleaseSHA = ObjectId.fromString(
			"1745038d7a647711d0b81faaf218f09cff8a9590");

		Iterable<RevCommit> commits =
			git.log().addRange(lastPoshiReleaseSHA, newPoshiReleaseSHA).call();

		Set<String> tickets = new TreeSet<>();

		for (RevCommit commit : commits) {
			String commitMessage = commit.getFullMessage();

			commitMessage = commitMessage.trim();

			Pattern ticketPattern = Pattern.compile("LRQA[\\S]*");

			Matcher matcher = ticketPattern.matcher(commitMessage);

			if (matcher.find()) {
				String ticketID = matcher.group();

				tickets.add(ticketID);
			}
		}

		JiraRestClientFactory jiraRestClientFactory =
			new AsynchronousJiraRestClientFactory();

		URI uri = new URI("https://issues.liferay.com");

		JiraRestClient jiraRestClient =
			jiraRestClientFactory.createWithBasicHttpAuthentication(
				uri, Authentication.JIRA_USERNAME,
					Authentication.JIRA_PASSWORD);

		Map<String, List<String>> ticketGroups = new TreeMap<>();

		String ticketListString = tickets.toString();

		ticketListString = ticketListString.replace("[", "(");
		ticketListString = ticketListString.replace("]", ")");

		ticketListString = URLEncoder.encode(ticketListString, "UTF-8");


		System.out.println(
			"https://issues.liferay.com/issues/?jql=key%20in" +
			ticketListString);

		for (String ticketID : tickets) {
			Issue issue =
				jiraRestClient.getIssueClient().getIssue(ticketID).claim();

			boolean missingLabel = true;

			for (String label : issue.getLabels()) {
				if (label.startsWith("poshi_")) {
					if (!ticketGroups.containsKey(label)) {
						List<String> ticketList = new ArrayList<>();

						String ticketMessage = getTicketMarkdownURL(ticketID) +
							" - " + issue.getSummary();

						ticketList.add(ticketMessage);

						ticketGroups.put(label, ticketList);

						missingLabel = false;

						break;
					}

					String ticketMessage = getTicketMarkdownURL(ticketID) +
						" - " + issue.getSummary();

					ticketGroups.get(label).add(ticketMessage);

					missingLabel = false;

					break;
				}
			}

			if (missingLabel) {
				System.out.println(
					"Missing poshi label: " + getTicketURL(ticketID));
			}
		}

		System.out.println("\nGenerated changelog:\n");

		for (Map.Entry<String, List<String>> entry : ticketGroups.entrySet()) {
			String label = entry.getKey();

			label = upperCaseEachWord(label.replace("_", " "));

			System.out.println("_" + label.replace("Poshi ", "") + "_");

			for (String ticketMessage : entry.getValue()) {
				System.out.println("* " + ticketMessage);
			}

			System.out.println("");
		}

		jiraRestClient.close();
	}

	public static String getTicketMarkdownURL(String ticketID) {
		StringBuilder sb = new StringBuilder();

		sb.append("[");

		sb.append(ticketID);

		sb.append("]");
		sb.append("(");

		sb.append(getTicketURL(ticketID));

		sb.append(")");

		return sb.toString();
	}

	public static String getTicketURL(String ticketID) {
		return "https://issues.liferay.com/browse/" + ticketID;
	}

	public static String upperCaseEachWord(String s) {
		char[] chars = s.toCharArray();

		chars[0] = Character.toUpperCase(chars[0]);

		for(int x = 1; x < chars.length; x++) {
			if(chars[x-1] == ' '){
				chars[x] = Character.toUpperCase(chars[x]);
			}
		}

		return new String(chars);
	}

}
