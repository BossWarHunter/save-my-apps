package com.coolapps.savemyapps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.ProgressDialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;

import com.google.api.services.tasks.model.Task;
import com.google.api.services.tasks.model.TaskList;
import com.google.api.services.tasks.model.TaskLists;

public class AppsListLoader extends AsyncTask<Void, Void, ArrayList<AppInfo>>{

	private SaveMyApps listActivity;
	private ProgressDialog progressDialog;
	
	public AppsListLoader(SaveMyApps activity) {
		this.listActivity = activity;
		progressDialog = new ProgressDialog(this.listActivity);
		progressDialog.setMessage(this.listActivity.getString(R.string.loading_list_title));
		progressDialog.setIndeterminate(true);
		progressDialog.setCancelable(false);
	}
	
	@Override
	protected void onPreExecute() {
		progressDialog.show();
	}
	
	@Override
	protected ArrayList<AppInfo> doInBackground(Void... params) {
		// Create the default list where the app names will be saved (if it doesn't exists)
        this.createList(listActivity.DEFAULT_LIST_ID, "SaveMyAppsDefaultList");
        return getAllApps();
	}		
	
	@Override
	protected void onPostExecute(ArrayList<AppInfo> appsList) {
		// Remove the progress dialog from the UI
		progressDialog.dismiss();
        // Create the list adapter that loads the apps list to the UI
		AppsListAdapter listAdapter = new AppsListAdapter(this.listActivity, appsList);
        // Order the apps list alphabetically
        listAdapter.sort(new AppNameComparator());
        // Set the list adapter that loads the apps list to the UI
        this.listActivity.setListAdapter(listAdapter);
	}
	
	/**
	 * Creates a list in the server if it doesn't exist.
	 * 
	 * @param listId
	 * @param listName
	 * */
	private void createList(String listId, String listName) {
		try {
			TaskLists allTaskLists = listActivity.tasksService.tasklists().list().execute();
			if (!allTaskLists.containsKey(listId)) {
				TaskList newTaskList = new TaskList();
				newTaskList.setId("savemyappsdefault");
				newTaskList.setTitle(listName);
				listActivity.tasksService.tasklists().insert(newTaskList).execute();				
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
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
        int savedPassNum = savedApps.size();
        for (int i=0; i<savedPassNum; i++) {
        	AppInfo savedAppInfo = savedApps.get(i);
        	int appIndex = allApps.indexOf(savedAppInfo);
        	// If the app is already installed
        	if (appIndex != -1) { 
        		AppInfo appInfo = allApps.get(appIndex);
        		appInfo.setId(savedAppInfo.getId());
        		appInfo.setSaved(true);
        	} else { // If the app is not installed
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
		PackageManager packageManager = this.listActivity.getPackageManager();
	    List<ApplicationInfo> appsList = packageManager.getInstalledApplications(0);
	    for (int i=0; i<appsList.size(); i++) {
	    	ApplicationInfo app = appsList.get(i);
	        String appName = app.loadLabel(packageManager).toString();
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
			// Get all the app names saved on the specified list
			List<Task> tasks = this.listActivity.tasksService.tasks().list(
					listActivity.DEFAULT_LIST_ID).execute().getItems();
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
			listActivity.handleException(e);
		}
		return savedApps;
	}
}

