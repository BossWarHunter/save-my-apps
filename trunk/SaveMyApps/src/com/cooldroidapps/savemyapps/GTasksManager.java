package com.cooldroidapps.savemyapps;

/*
 * Copyright 2011 Franco Sabadini - fsabadi@gmail.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at	
 * 
 * http://www.apache.org/licenses/LICENSE-2.0	
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
**/

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import com.google.api.client.extensions.android2.AndroidHttp;
import com.google.api.client.googleapis.auth.oauth2.draft10.GoogleAccessProtectedResource;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.json.JsonHttpRequest;
import com.google.api.client.http.json.JsonHttpRequestInitializer;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.TasksRequest;
import com.google.api.services.tasks.model.Task;
import com.google.api.services.tasks.model.TaskList;


public class GTasksManager {

	private SaveMyApps mainActivity;
	private Tasks tasksService;
	private static final String API_KEY = "AIzaSyBtwFxJXY0Hxcjr45ls1KHSTvtlHeHaadg";
	private static final String AUTH_TOKEN_TYPE = "Manage your tasks";
	private final HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
	private final GoogleAccessProtectedResource accessProtectedResource = new GoogleAccessProtectedResource(null);
	
	public GTasksManager(SaveMyApps mainActivity) {
		this.mainActivity = mainActivity;
		// Create a new service builder that creates instances of task services
        Tasks.Builder serviceBuilder = Tasks.builder(httpTransport, new JacksonFactory());
        serviceBuilder.setApplicationName("SaveMyApps");
        serviceBuilder.setHttpRequestInitializer(accessProtectedResource);
        serviceBuilder.setJsonHttpRequestInitializer(new JsonHttpRequestInitializer() {
            public void initialize(JsonHttpRequest request) throws IOException {
              TasksRequest tasksRequest = (TasksRequest) request;
              tasksRequest.setKey(API_KEY);
            }
          });
        // Build an instance of a tasks service
        tasksService = serviceBuilder.build();
	}
	
	/**
	 * Checks if the connection to the tasks service is working fine by doing a 
	 * simple request.
	 * 
	 * @return true if the service is working fine, false if not.
	 * */
	public boolean tasksServiceUp() {
		try {
			Tasks.Tasklists.List listsReq = tasksService.tasklists().list();
			// Only return the id and title of every list (to improve performance)
			listsReq.setFields("items(id)");
			listsReq.execute();
		} catch (IOException e) {
			handleException(e);
			return false;
		}
		return true;
	}
	
	public String getAuthTokenType() {
		return AUTH_TOKEN_TYPE;
	}
	
	public void setAccessToken(String accessToken) {
        accessProtectedResource.setAccessToken(accessToken);
	}
	
	public String getAccessToken() {
		return accessProtectedResource.getAccessToken();
	}
	
	public String createTask(String listId, String taskTitle) {
		try {
			Task task = new Task();
			task.setTitle(taskTitle);
			return tasksService.tasks().insert(listId, task).execute().getId();
		} catch (IOException e) {
			handleException(e);
		}
		return null;
	}
	
	public boolean deleteTask(String listId, String taskId) {
		try {
			tasksService.tasks().delete(listId, taskId).execute();
			return true;
		} catch (IOException e) {
			handleException(e);		
		}
		return false;
	}
	
	/**
	 * Returns a list of all the apps saved on the server.
	 * */
	public ArrayList<AppInfo> getSavedApps() {
		ArrayList<AppInfo> savedApps = new ArrayList<AppInfo>();
		// Get all the app names saved on the specified list
		List<Task> tasks = getTasks(mainActivity.DEFAULT_LIST_ID);
		if (tasks != null) {
			for (Task task : tasks) {
				AppInfo appInfo = new AppInfo(task.getTitle());
				// Set the task id
				appInfo.setId(task.getId());
				appInfo.setSaved(true);
				savedApps.add(appInfo);
		    }
		} 
		return savedApps;
	}

	
	private List<Task> getTasks(String listId) {
		try {
			List<Task> allResults = new ArrayList<Task>();
			Tasks.TasksOperations.List tasksReq = tasksService.tasks().list(listId);
			String nextPageToken = null;
			// Only return the id and title of every task (to improve performance),
			// and the token to get the next page of results (in case there are more
			// than 100 results)
			tasksReq.setFields("nextPageToken,items(id,title)");
			// Iterate to get all the results while there are pages (each page contains
			// a max of 100 results)
			do {
				com.google.api.services.tasks.model.Tasks tasksResp = tasksReq.execute();
				allResults.addAll(tasksResp.getItems());
				nextPageToken = tasksResp.getNextPageToken();
				tasksReq.setPageToken(nextPageToken);
			} while (nextPageToken != null);			
			return allResults;
		} catch (IOException e) {
			handleException(e);
		}
		return null;
	}
	
	/**
	 * Creates a list in the server if it doesn't exist.
	 * 
	 * @param listName
	 * */
	public void createTaskList(String listName) {
		try {
			TaskList newTaskList = new TaskList();
			newTaskList.setTitle(listName);
			TaskList taskList = tasksService.tasklists().insert(newTaskList).execute();
			//TODO make this better to support several lists
			mainActivity.DEFAULT_LIST_ID = taskList.getId();
		} catch (IOException e) {
			handleException(e);
		}
	}

	/**
	 * @return Id of the list given in the server.
	 * */
	public String getListId(String listTitle) {
		List<TaskList> taskLists = getAllTaskLists();
		if (taskLists != null) {
			// Look in the tasks list for the list with the given title
			for (TaskList tl : taskLists) {
				if (tl.getTitle().equals(listTitle)) {
					return tl.getId();
				}
			}
		}
		return null;
	}
	
	private List<TaskList> getAllTaskLists() {
		List<TaskList> tasksList = null;
		try {
			Tasks.Tasklists.List listsReq = tasksService.tasklists().list();
			// Only return the id and title of every list (to improve performance)
			listsReq.setFields("items(id,title)");
			tasksList = listsReq.execute().getItems();
		} catch (IOException e) {
			handleException(e);
		}
		return tasksList;
	}
	
	private void handleException(Exception e) {
		e.printStackTrace();
	    if (e instanceof HttpResponseException) {
	    	HttpResponse response = ((HttpResponseException) e).getResponse();
	    	int statusCode = response.getStatusCode();
	    	try {
	    		response.ignore();
	    	} catch (IOException e1) {
	    		e1.printStackTrace();
	    	}
	    	// 401 = Authentication error, the auth token expired.
	    	if (statusCode == 401) {
	    		mainActivity.chooseAccount(true);
	    		return;
	    	}
	    }
	    Log.e("SaveMyApps", e.getMessage(), e);
	}

}
