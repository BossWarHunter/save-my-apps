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

import com.coolapps.savemyapps.AppsSynchronizer.SyncType;
import com.google.api.client.googleapis.extensions.android2.auth.GoogleAccountManager;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;


public class SaveMyApps extends ListActivity {
    
	private static final int ACCOUNTS_DIALOG = 0;
	private static final int CON_ERROR_DIALOG = 1;
	private static final String PREFS_NAME = "SaveMyAppsPrefs";
	private static final int REQUEST_AUTH = 0;
	//TODO: change the default list for a specific one
	public String DEFAULT_LIST_ID = "";
	public static String DEFAULT_LIST_NAME = "SaveMyAppsDefaultList";
	private GoogleAccountManager accountManager;
	public GTasksManager gTasksManager;
	private AppsListLoader appsListLoader;
		
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Check if the device is connected to the internet
        checkNetworkAvailability();
	}
	
	/**
	 * Checks if the device is currently connected to the internet, if so continue with
	 * the operations, if not show an error message and close the application. 
	 * */
	private void checkNetworkAvailability() {
	    ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwInfo = cm.getActiveNetworkInfo();
        if (activeNetwInfo != null) {
            if (!activeNetwInfo.isConnected()) {
            	showDialog(CON_ERROR_DIALOG);
            }
            startOperations();
        }
        else {
        	showDialog(CON_ERROR_DIALOG);;
        }
	}
	
	/**
	 * This method is called when the app is connected to the internet to set
	 * up the global variables and start the app operations.
	 * */
	private void startOperations() {
        // Set the view assigned to this activity
        this.setContentView(R.layout.apps_list);
        // Create a new async task to load the apps list
        appsListLoader = new AppsListLoader(this);
        // Create an account manager to handle the user google accounts
        accountManager = new GoogleAccountManager(this);
        // Create a tasks manager to communicate with the Google Tasks Service
        gTasksManager = new GTasksManager(this);
        chooseAccount(false);
	}

	/**
	 * If the user didn't choose a google account to synchronize his/her data
	 * with then {@link showDialog} is called, if there is already an account\
	 * chosen {@link accountChosen} is called.
	 * */
	public void chooseAccount(boolean tokenExpired) {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		// Get the previously chosen account (if any)
		String accountName = settings.getString("accountName", null);
	    Account account = accountManager.getAccountByName(accountName);
	    // If the account was chosen previously
	    if (account != null) {
	    	// If the access token expired invalidate it
	    	if (tokenExpired) {
	            accountManager.invalidateAuthToken(gTasksManager.getAccessToken());
	            gTasksManager.setAccessToken(null);
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
	    accountManager.manager.getAuthToken(account, gTasksManager.getAuthTokenType(), true, 
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
		            		gTasksManager.setAccessToken(bundle.getString(AccountManager.KEY_AUTHTOKEN));
		            		appsListLoader.execute(); // Load the apps list
		            	}						
					} catch (OperationCanceledException e) {
						e.printStackTrace();
					} catch (AuthenticatorException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
	          }
	    }, null);
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		switch (id) {
			//Create the accounts dialog so the user chooses which account to sync with
			case ACCOUNTS_DIALOG:
				builder.setTitle(R.string.account_dialog_title);
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
	    		break;
	    	// Create an error dialog for when the device is not connected to the internet	
			case CON_ERROR_DIALOG:
				builder.setTitle(R.string.con_error_dialog_title);
				builder.setMessage(R.string.con_error_message)
			       .setCancelable(false)
			       .setPositiveButton("OK", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			                SaveMyApps.this.finish();
			           }
			       });
				break;
			//TODO delete test dialog
			case 3:
				builder.setTitle(R.string.con_error_dialog_title);
				builder.setMessage(R.string.con_error_message)
			       .setCancelable(false)
			       .setPositiveButton("OK", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			                dialog.cancel();
			           }
			       });
				break;
		}
		return builder.create();
	}
		
	/**
	 * Saves the selected apps on the server, if they are not already there.
	 * 
	 * @param view
	 * */
	@SuppressWarnings("unchecked")
	public void saveApps(View view) {
		AppsListAdapter listAdapter = (AppsListAdapter) getListAdapter();
		// Get the apps that are selected (checkboxs are checked)
		ArrayList<AppInfo> appsToSave = listAdapter.getCheckedApps();
		// Create a new thread that will save the apps in the server
		AppsSynchronizer syncThread = new AppsSynchronizer(this, SyncType.SAVE);
		syncThread.execute(appsToSave);
	}

	/**
	 * Deletes the selected apps from the server, if they are there.
	 * 
	 * @param view
	 * */
	@SuppressWarnings("unchecked")
	public void unsaveApps(View view) {
		AppsListAdapter listAdapter = (AppsListAdapter) getListAdapter();
		// Get the apps that are selected (checkboxs are checked)
		ArrayList<AppInfo> appsToUnsave = listAdapter.getCheckedApps();
		// Create a new thread that will delete the apps from the server
		AppsSynchronizer syncThread = new AppsSynchronizer(this, SyncType.UNSAVE);
		syncThread.execute(appsToUnsave);
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
