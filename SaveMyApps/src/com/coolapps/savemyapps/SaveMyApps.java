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
import java.util.ArrayList;
import java.util.List;

import com.google.api.client.extensions.android2.AndroidHttp;
import com.google.api.client.googleapis.auth.oauth2.draft10.GoogleAccessProtectedResource;
import com.google.api.client.googleapis.extensions.android2.auth.GoogleAccountManager;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.json.JsonHttpRequest;
import com.google.api.client.http.json.JsonHttpRequestInitializer;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.tasks.model.Task;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.TasksRequest;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;


public class SaveMyApps extends ListActivity {
    
	private static final int ACCOUNTS_DIALOG = 0;
	private static final int REQUEST_AUTH = 0;
	private static final String PREFS_NAME = "SaveMyAppsPrefs";
	private static final String AUTH_TOKEN_TYPE = "Manage your tasks";
	private static final String API_KEY = "AIzaSyBtwFxJXY0Hxcjr45ls1KHSTvtlHeHaadg";
	//TODO: change the default list for a specific one
	private static final String DEFAULT_LIST = "@default";//"@savemyapps-default";
	private GoogleAccountManager accountManager;
	private final HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
	private final GoogleAccessProtectedResource accessProtectedResource = new GoogleAccessProtectedResource(null);
	private Tasks tasksService;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the view assigned to this activity
        this.setContentView(R.layout.apps_list);
        // Create an account manager to handle the user google accounts
        accountManager = new GoogleAccountManager(this);
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
        chooseAccount(false);
	}
	
	/**
	 * If the user didn't choose a google account to synchronize his/her data
	 * with then {@link showDialog} is called, if there is already an account\
	 * chosen {@link accountChosen} is called.
	 * */
	private void chooseAccount(boolean tokenExpired) {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		// Get the previously chosen account (if any)
		String accountName = settings.getString("accountName", null);
	    Account account = accountManager.getAccountByName(accountName);
	    // If the account was chosen previously
	    if (account != null) {
	    	// If the access token expired invalidate it
	    	if (tokenExpired) {
	            accountManager.invalidateAuthToken(accessProtectedResource.getAccessToken());
	            accessProtectedResource.setAccessToken(null);
	    	}
	    	accountChosen(account);
	    	return;
	    }
	    // If the account was not chosen yet show the accounts dialog
	    showDialog(ACCOUNTS_DIALOG);
	}

	/**
	 * Once the user choosed an account to synchronize his/her data with
	 * this method is called to get authorization to manage his/her
	 * tasks.
	 * */
	private void accountChosen(final Account account) {
		// Get the shared preferences
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
	    // Get an editor to modify the preferences
		SharedPreferences.Editor editor = settings.edit();
	    // Save the chosen account as a shared preference
		editor.putString("accountName", account.name);
	    editor.commit();
	    // Get the authentication token for the requests to the tasks service
	    accountManager.manager.getAuthToken(account, AUTH_TOKEN_TYPE, true, 
	    		new AccountManagerCallback<Bundle>() {

	    	public void run(AccountManagerFuture<Bundle> future) {
	            try {
	            	Bundle bundle = future.getResult();
	            	if (bundle.containsKey(AccountManager.KEY_INTENT)) {
	            		Intent intent = bundle.getParcelable(AccountManager.KEY_INTENT);
	            		intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
	            		startActivityForResult(intent, REQUEST_AUTH);
	            	} else if (bundle.containsKey(AccountManager.KEY_AUTHTOKEN)) {
	                    // Set a new access token
	            		accessProtectedResource.setAccessToken(bundle.getString(AccountManager.KEY_AUTHTOKEN));
	            		showAppsList(); // Load the apps list
	            	}
	            } catch (Exception e) {
	            	handleException(e);
	            }
	          }
	    }, null);
	}
	
	/**
	 * Set the list adapter for this activity and load the apps list to the UI.
	 * */
	private void showAppsList() {
        AppsListAdapter listAdapter = new AppsListAdapter(this, getAllApps()); 
        // Order the apps list in alphabetically
        listAdapter.sort(new AppNameComparator());
        // Set the adapter to fill the list
		setListAdapter(listAdapter);
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
			//Create the accounts dialog so the user chooses which account to sync with
			case ACCOUNTS_DIALOG:
	    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    		builder.setTitle("Select a Google account");
	    		final Account[] accounts = accountManager.getAccounts();
	    		final int accountsNun = accounts.length;
	    		String[] accountNames = new String[accountsNun];
	    		for (int i = 0; i < accountsNun; i++) {
	    			accountNames[i] = accounts[i].name;
	    		}
	    		builder.setItems(accountNames, new DialogInterface.OnClickListener() {
	    			public void onClick(DialogInterface dialog, int which) {
	    				accountChosen(accounts[which]);
	    			}
	    		});
	    		return builder.create();
	    	// TODO: add a progress dialog to be showed while loading the apps list	
		}
		return null;
	}
	
	/**
	 * Return a list of all the installed and saved apps.
	 * */
	private ArrayList<AppInfo> getAllApps() {
		// Get the list of apps installed on the device
		ArrayList<AppInfo> allApps = getInstalledApps();
		// Get the list of apps saved on the server
        ArrayList<AppInfo> savedApps = getSavedApps();
        // Merge the 2 list of apps
        for (int i=0; i<savedApps.size(); i++) {
        	AppInfo savedAppInfo = savedApps.get(i);
        	int appIndex = allApps.indexOf(savedAppInfo);
        	// If the app is already installed
        	if (appIndex != -1) { 
        		AppInfo appInfo = allApps.get(appIndex);
        		appInfo.setSaved(true);
        	}
        	else { // If the app is not installed
        		allApps.add(savedAppInfo);
        	}
        }
        return allApps;
	}
	
	/**
	 * Returns all the apps actually installed on the device, 
	 * with the option to choose the system packages or not.
	 */
	private ArrayList<AppInfo> getInstalledApps() {
		ArrayList<AppInfo> installedApps = new ArrayList<AppInfo>();        
	    List<ApplicationInfo> appsList = getPackageManager().getInstalledApplications(0);
	    for (int i=0; i<appsList.size(); i++) {
	    	ApplicationInfo app = appsList.get(i);
	        String appName = app.loadLabel(getPackageManager()).toString();
	        // If the appName is a real app and not a package name
	        if (!appName.contains(".")) {
	        	// TODO: add package name?
		        //newInfo.pname = p.packageName;
	        	AppInfo appInfo = new AppInfo(appName);
		        appInfo.setInstalled(true);
		        installedApps.add(appInfo);
	        }
	    }
	    return installedApps; 
	}
	
	/**
	 * Returns a list of all the apps saved on the server.
	 * */
	private ArrayList<AppInfo> getSavedApps() {
		ArrayList<AppInfo> savedApps = new ArrayList<AppInfo>();
		try {
			// Create the default list if it doesn't exist (TODO: put this somewhere more appropriate)
			/*TaskLists allTaskLists = tasksService.tasklists().list().execute();
			if (!allTaskLists.containsKey(DEFAULT_LIST)) {
				TaskList newTaskList = new TaskList();
				newTaskList.setId(DEFAULT_LIST);
				newTaskList.setTitle("SaveMyAppsDefaultList");
				tasksService.tasklists().insert(newTaskList).execute();				
			}*/
			// Get all the app names saved on the specified list
			List<Task> tasks = tasksService.tasks().list(DEFAULT_LIST).execute().getItems();
			if (tasks != null) {
				for (Task task : tasks) {
					AppInfo appInfo = new AppInfo(task.getTitle());
					// Set the task id for when it needs to be deleted
					appInfo.setId(task.getId());
					appInfo.setSaved(true);
					savedApps.add(appInfo);
			    }
			} 
		} catch (IOException e) {
			handleException(e);
		}
		return savedApps;
	}
	
	/**
	 * Saves the selected apps on the server, if they are not already there.
	 * 
	 * @param view
	 * */
	public void saveApps(View view) {
		AppsListAdapter listAdapter = (AppsListAdapter) getListAdapter();
		// Get the apps that are selected (checkboxs are checked)
		ArrayList<AppInfo> appsToSave = listAdapter.getCheckedApps();
		for (int i=0; i < appsToSave.size(); i++) {
			AppInfo appInfo = appsToSave.get(i);
			// If the app name is not saved on the server
			if (!appInfo.isSaved()) { 
				try {
					Task task = new Task();
					task.setTitle(appInfo.getName());
					Task savedTask = tasksService.tasks().insert(DEFAULT_LIST, task).execute();
					listAdapter.updateSavedState(appInfo, true, savedTask.getId());
				} catch (IOException e) {
					handleException(e);
				}
			}
		}
		// Notify the adapter that the state of the saved images changed
		listAdapter.notifyDataSetChanged();
	}

	/**
	 * Deletes the selected apps from the server, if they are there.
	 * 
	 * @param view
	 * */
	public void unsaveApps(View view) {
		AppsListAdapter listAdapter = (AppsListAdapter) getListAdapter();
		// Get the apps that are selected (checkboxs are checked)
		ArrayList<AppInfo> appsToUnsave = listAdapter.getCheckedApps();
		for (int i=0; i < appsToUnsave.size(); i++) {
			AppInfo appInfo = appsToUnsave.get(i);
			// If the app name is saved on the server
			if (appInfo.isSaved()) { 
				try {
					tasksService.tasks().delete(DEFAULT_LIST, appInfo.getId()).execute();
					listAdapter.updateSavedState(appInfo, false, null);
				} catch (IOException e) {
					handleException(e);
				}
			}
		}
		// Notify the adapter that the state of the saved images changed
		listAdapter.notifyDataSetChanged();
	}

	/**
	 * Verifies if the "Select All" checkbox is checked or not and update the checkbox 
	 * state of all the apps on the list according to it.
	 * 
	 * @param selectAllCheckBox
	 * */
	public void updateCkeck(View selectAllCheckBox) {
		AppsListAdapter listAdapter = (AppsListAdapter) getListAdapter();
		if (((CheckBox)selectAllCheckBox).isChecked()) { 
			// Check all the apps on the list
			listAdapter.updateCheckState(true);
		}
		else {
			// Un-check all the apps on the list
			listAdapter.updateCheckState(false);
		}
	}

	// TODO: add an error dialog that says 
	// "the app could not connect to the server, please check your Internet connection"
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
	    		chooseAccount(true);
	    		return;
	    	}
	    }
	    Log.e("SaveMyApps", e.getMessage(), e);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		showAppsList();
	}
	
}
	
	// TODO: check if I can use the code down here which has a better performance
/*	
	private ArrayList<CharSequence> getCheckedApps() {
		ListView listView = this.getListView();
		// Get all the apps whose checkbox is checked
		SparseBooleanArray checkedAppsPos = listView.getCheckedItemPositions();
		ArrayList<CharSequence> checkedApps = new ArrayList<CharSequence>();
		// Get all the names of the apps that were checked
		for (int i=0; i<checkedAppsPos.size(); i++) {
			View appView = listView.getChildAt(checkedAppsPos.keyAt(i));
			CharSequence appName = ((TextView) appView.findViewById(R.id.label)).getText();
			checkedApps.add(appName);
		}
		return checkedApps;
	}

// TODO: This function will display a context menu with the options to save, 
	// unsave or install app
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		//super.onListItemClick(l, v, position, id);
		// Get the item that was clicked
		//Object o = this.getListAdapter().getItem(position);
		//String keyword = o.toString();
		//Toast.makeText(this, "You selected: " + keyword, Toast.LENGTH_LONG)
		//		.show();
	}

*/
