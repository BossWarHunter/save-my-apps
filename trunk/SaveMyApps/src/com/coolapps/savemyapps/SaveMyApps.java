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
import com.google.api.client.googleapis.extensions.android2.auth.GoogleAccountManager;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.tasks.model.Task;
import com.google.api.services.tasks.Tasks;

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
	private static final String AUTH_TOKEN_TYPE = "Manage your tasks";
	private static final String PREF = "SaveMyAppsPrefs";
	private static final String API_KEY = "AIzaSyBSdg34QaV73dM0BnsRGDapnMPPEzuT22M";
	private static final String DEFAULT_LIST = "@savemyapps-default";
	private GoogleAccountManager accountManager;
	private final HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
	private Tasks tasksService;// = new Tasks("CoolApps-SaveMyApps/1.0", httpTransport, new JacksonFactory());
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the view assigned to this activity
        this.setContentView(R.layout.apps_list);
        accountManager = new GoogleAccountManager(this);
        // Create a new service to send and get data from google tasks
        tasksService = new Tasks(httpTransport, new JacksonFactory());
        tasksService.setKey(API_KEY);
        tasksService.setApplicationName("CoolApps-SaveMyApps/1.0");
        gotAccount(false);
	}
	
	private void gotAccount(boolean tokenExpired) {
		SharedPreferences settings = getSharedPreferences(PREF, 0);
	    String accountName = settings.getString("accountName", null);
	    Account account = accountManager.getAccountByName(accountName);
	    // If the account was chosen previously
	    if (account != null) {
	    	gotAccount(account);
	    	return;
	    }
	    showDialog(ACCOUNTS_DIALOG);
	}

	private void gotAccount(final Account account) {
		SharedPreferences settings = getSharedPreferences(PREF, 0);
	    SharedPreferences.Editor editor = settings.edit();
	    editor.putString("accountName", account.name);
	    editor.commit();
	    accountManager.manager.getAuthToken(account, AUTH_TOKEN_TYPE, true, 
	    		new AccountManagerCallback<Bundle>() {

	          public void run(AccountManagerFuture<Bundle> future) {
	            try {
	              // TODO: the exception appears when the bundle is created...why?
	            	Bundle bundle = future.getResult();
	              if (bundle.containsKey(AccountManager.KEY_INTENT)) {
	            	  Intent intent = bundle.getParcelable(AccountManager.KEY_INTENT);
	            	  intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
	            	  startActivityForResult(intent, REQUEST_AUTH);
	              } else if (bundle.containsKey(AccountManager.KEY_AUTHTOKEN)) {
	                showAppsList(); // Load the apps list
	              // TODO: find out what exception is this catching and why?
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
        // Order the apps list according to their names
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
	    				gotAccount(accounts[which]);
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
        /*ArrayList<AppInfo> savedApps = getSavedApps();
        // Merge the 2 list of apps
        for (int i=0; i<savedApps.size(); i++) {
        	AppInfo savedAppInfo = savedApps.get(i);
        	int appIndex = allApps.indexOf(savedAppInfo);
        	if (appIndex != -1) { // If the app is already installed
        		AppInfo appInfo = allApps.get(appIndex);
        		appInfo.setSaved(true);
        	}
        	else { // If the app is not installed
        		allApps.add(savedAppInfo);
        	}
        }*/
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
			List<Task> tasks = tasksService.tasks().list(DEFAULT_LIST).execute().getItems();
			if (tasks != null) {
				for (Task task : tasks) {
					AppInfo appInfo = new AppInfo(task.getTitle());
					appInfo.setSaved(true);
					savedApps.add(appInfo);
			    }
			} 
		} catch (IOException e) {
			handleException(e);
		}
		return savedApps;
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
	    	// TODO: what is STATUS 401??
	    	if (statusCode == 401) {
	    		gotAccount(true);
	    		return;
	    	}
	    }
	    Log.e("SaveMyApps", e.getMessage(), e);
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
			if (!appInfo.isSaved()) { // If the app name is not saved on the server
				try {
					Task task = new Task();
					task.setTitle(appInfo.getName());
					task.setNotes(appInfo.getName());
					tasksService.tasks().insert(DEFAULT_LIST, task).execute();
				} catch (IOException e) {
					handleException(e);
				}
			}
		}
		listAdapter.updateSavedState(appsToSave, true);
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
			if (appInfo.isSaved()) { // If the app name is saved on the server
				try {
					tasksService.tasks().delete(DEFAULT_LIST, appInfo.getName()).execute();
				} catch (IOException e) {
					handleException(e);
				}
			}
		}
		listAdapter.updateSavedState(appsToUnsave, false);
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
