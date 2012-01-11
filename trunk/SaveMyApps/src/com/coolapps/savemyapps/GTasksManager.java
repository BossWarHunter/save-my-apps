package com.coolapps.savemyapps;

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
import com.google.api.services.tasks.model.TaskLists;

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
	
	public String getAuthTokenType() {
		return AUTH_TOKEN_TYPE;
	}
	
	public void setAccessToken(String accessToken) {
        accessProtectedResource.setAccessToken(accessToken);
	}
	
	public String getAccessToken() {
		return accessProtectedResource.getAccessToken();
	}
	
	public Task insertTask(String listId, Task task) {
		try {
			return tasksService.tasks().insert(listId, task).execute();
		} catch (IOException e) {
			handleException(e);
		}
		return null;
	}
	
	public void deleteTask(String listId, String taskId) {
		try {
			tasksService.tasks().delete(listId, taskId).execute();
		} catch (IOException e) {
			handleException(e);		
		}
	}
	
	public List<Task> getTasks(String listId) {
		try {
			return tasksService.tasks().list(listId).execute().getItems();
		} catch (IOException e) {
			handleException(e);
		}
		return null;
	}
	
	public void insertTaskList(TaskList newTaskList) {
		try {
			tasksService.tasklists().insert(newTaskList).execute();
		} catch (IOException e) {
			handleException(e);
		}
	}
	
	public TaskLists getAllTaskLists() {
		try {
			return tasksService.tasklists().list().execute();
		} catch (IOException e) {
			handleException(e);
		}
		return null;
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
	    		this.mainActivity.chooseAccount(true);
	    		return;
	    	}
	    }
	    Log.e("SaveMyApps", e.getMessage(), e);
	}

}
