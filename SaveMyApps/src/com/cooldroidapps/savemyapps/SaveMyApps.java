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
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import com.cooldroidapps.savemyapps.AppsSynchronizer.SyncType;
import com.cooldroidapps.savemyapps.AppInfo;
import com.google.api.client.googleapis.extensions.android2.auth.GoogleAccountManager;


public class SaveMyApps extends ListActivity {
    
	private static final int ACCOUNTS_DIALOG = 0;
	private static final int CON_ERROR_DIALOG = 1;
	private static final int HELP_DIALOG = 2;
	private static final int SORT_DIALOG = 3;
	private static final int PRO_UPDATE_DIALOG = 4;
	
	private static final String PREFS_NAME = "SaveMyAppsPrefs";
	private static final String showProUpdateDlgString = "showProUpdateDlg";
	private static final int REQUEST_AUTH = 0;
	//TODO: change the default list for a specific one
	public String DEFAULT_LIST_ID = "";
	public static String DEFAULT_LIST_NAME = "SaveMyAppsDefaultList";
	private GoogleAccountManager accountManager;
	private GTasksManager gTasksManager;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the view assigned to this activity
        this.setContentView(R.layout.apps_list);
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
        // Get the preference that checks if the pro update dialog must be showed
	    final boolean showProUpdateDlg = settings.getBoolean(showProUpdateDlgString, true);
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
		                    // Check if the device is connected to the internet
		                    loadAppsList();
		            		if (showProUpdateDlg) {
		                        showDialog(PRO_UPDATE_DIALOG);		            	    	
		            	    }
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
	
	/**
	 * Checks if the device is currently connected to the internet, if not 
	 * shows an error message. 
	 * 
	 *  @return true is the device is connected to the internet, false if not.
	 * */
	private boolean networkActive() {
	    ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwInfo = cm.getActiveNetworkInfo();
        if (activeNetwInfo != null && activeNetwInfo.isConnected()) {
            return true;
        }
        showDialog(CON_ERROR_DIALOG);
        return false;
	}
	
	/**
	 * Loads the apps list to the UI.
	 * */
	private void loadAppsList() {
		if (networkActive() && gTasksManager.tasksServiceUp()) {
			// Create a new async task to load the apps list
			new AppsListLoader(SaveMyApps.this).execute();
		}
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
			        	   dialog.dismiss();
			           }
			       });
				break;
			case SORT_DIALOG:
				builder.setTitle(R.string.sort_dialog_title);
	    		builder.setSingleChoiceItems(R.array.sort_dialog_list, 0, 
	    			new DialogInterface.OnClickListener() {
	    				public void onClick(DialogInterface dialog, int sortId) {
	    					SortAppsList(sortId);
	    					dialog.dismiss();
	    				}
	    			});
	    		break;
			case HELP_DIALOG:
				LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
				View helpDialogView = inflater.inflate(R.layout.help_dialog,
				                               (ViewGroup) findViewById(R.id.help_dialog));
				builder.setTitle(R.string.help_dialog_title);
				builder.setView(helpDialogView).setPositiveButton("OK", 
					new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			        	   dialog.dismiss();
			           }
			       });
				break;
			case PRO_UPDATE_DIALOG:
				builder.setTitle(R.string.pro_update_dlg_title);
				builder.setMessage(R.string.pro_update_dlg_msg)
			       .setCancelable(false)
			       .setPositiveButton(R.string.pro_update_dlg_pos_button, 
			    		   new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			       			Intent marketIntent = new Intent(Intent.ACTION_VIEW);
			    			marketIntent.setData(Uri.parse(
			    					getString(R.string.pro_version_url)));			
			    			startActivity(marketIntent);
			           }
			       }).setNegativeButton(R.string.pro_update_dlg_neg_button, 
			    		   new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			        	   dialog.dismiss();
			           }
			       }).setNeutralButton(R.string.pro_update_dlg_neutral_button, 
			    		   new DialogInterface.OnClickListener() {
							@Override
						public void onClick(DialogInterface dialog, int which) {
							// Get the shared preferences
							SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
							// Get an editor to modify the preferences
							SharedPreferences.Editor editor = settings.edit();
						    // Change to false the preference that checks if the
							// pro update dialog must be showed when the app starts
							editor.putBoolean(showProUpdateDlgString, false);
							editor.commit();
							dialog.dismiss();
						}
					});
				break;
		}
		return builder.create();
	}
		
	/**
	 * Sorts the apps list according to the sort chosen by the user.
	 * 
	 * @param sortType
	 * 			Sort type used to list the apps.
	 * */
	private void SortAppsList(int sortType) {
		AppsListAdapter appsListAdapter = (AppsListAdapter)getListAdapter();
		if (appsListAdapter != null) {
			AppInfoComparator appInfoComparator = AppInfoComparator.getInstance();
			appInfoComparator.setComparisonType(sortType);
			appsListAdapter.sort(appInfoComparator);
		}
	}
	
	/**
	 * Saves the selected apps on the server, if they are not already there.
	 * 
	 * @param view
	 * */
	@SuppressWarnings("unchecked")
	public void saveApps(View view) {
		if (networkActive()) {
			// Get the apps that are selected (checkboxs are checked)
			ArrayList<AppInfo> appsToSave = getCheckedApps();
			// Create a new thread that will save the apps in the server
			AppsSynchronizer syncThread = new AppsSynchronizer(this, SyncType.SAVE);
			syncThread.execute(appsToSave);
		}
	}

	/**
	 * Deletes the selected apps from the server, if they are there.
	 * 
	 * @param view
	 * */
	@SuppressWarnings("unchecked")
	public void unsaveApps(View view) {
		if (networkActive()) {
			// Get the apps that are selected (checkboxs are checked)
			ArrayList<AppInfo> appsToUnsave = getCheckedApps();
			// Create a new thread that will delete the apps from the server
			AppsSynchronizer syncThread = new AppsSynchronizer(this, SyncType.UNSAVE);
			syncThread.execute(appsToUnsave);
		}
	}
	
	/**
	 * Verifies if the "Select All" checkbox is checked or not and update the checkbox 
	 * state of all the apps on the list according to it.
	 * 
	 * @param selectAllCheckBox
	 * */
	public void updateCkeck(View selectAllCheckBox) {
		AppsListAdapter listAdapter = (AppsListAdapter) getListAdapter();
		if (listAdapter != null) {
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.options_menu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case R.id.refresh:
				loadAppsList();
				return true;
			case R.id.share:
				shareAppsInfo(getCheckedApps());
				return true;
			case R.id.sort:
				showDialog(SORT_DIALOG);
				return true;
			case R.id.settings:
				// Call the settings activity
				Intent intent = new Intent(this, Preferences.class);
				startActivity(intent);
				return true;
			case R.id.help:
				showDialog(HELP_DIALOG);
				return true;
			case R.id.exit:
				this.finish();
				return true;
			default:
		        return super.onOptionsItemSelected(item);
		}
	}
	
	/**
	 * Shares information about the apps chosen by the user, using 
	 * any of the sharing apps installed on the device that allow text 
	 * sharing.
	 * 
	 * @param appsToShare
	 * 			List of apps which information will be shared.
	 * */
	private void shareAppsInfo(ArrayList<AppInfo> appsToShare) {
		// Create an intent to connect to the sharing app 
		Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
		// Set the type of content that will be shared (this way Android filters
		// the apps that can share this type of content and only shows those
		// as options for the user)
		sharingIntent.setType("text/plain");
		// Set the subject (only available for some apps like GMail)
		sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, 
				getString(R.string.share_subject));
		// Set the text that will be shared
		sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, 
				getAppsInfoToShare(appsToShare));
		// Create and show the share apps chooser
		startActivity(Intent.createChooser(sharingIntent, 
				getString(R.string.share_dialog_title)));
	}
	
	/**
	 * Get the information to share from the apps info list and put
	 * it in a string with a "user friendly" format.
	 * 
	 * @param appsToShare
	 * 			List of apps information.
	 * 
	 * @return Apps information in a String with user friendly format.
	 * */
	private String getAppsInfoToShare(ArrayList<AppInfo> appsToShare) {
		// Get the user settings to find out what he/she wants to share
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		boolean useLabels = settings.getBoolean(
				getString(R.string.share_pref_labels_key), true);
		boolean shareName = settings.getBoolean(
				getString(R.string.share_pref_app_name_key), true);
		boolean sharePckName = settings.getBoolean(
				getString(R.string.share_pref_app_pckname_key), true);
		// StringBuilder is used instead of String because it has better
		// performance when appending a lot of strings
		StringBuilder stringBuilder = new StringBuilder();
		for (AppInfo appInfo : appsToShare) {
			appendShareInfo(shareName, useLabels, stringBuilder, 
					R.string.share_body_app_name, appInfo.getName());
			appendShareInfo(sharePckName, useLabels, stringBuilder, 
					R.string.share_body_app_pckname, appInfo.getPackageName());
			stringBuilder.append("\n");
		}
		return stringBuilder.toString();
	}
	
	private void appendShareInfo(boolean shareBool, boolean useLabels, 
			StringBuilder shareString, int labelId, String shareInfo) {
		if (shareBool) {
			shareString.append("\n");
			if (useLabels) {
				shareString.append(getString(labelId) + " ");
			}
			shareString.append(shareInfo);								
		}
	}
	
	/**
	 * Returns the list of apps chosen by the user, or an empty list 
	 * if non is chosen (or if there is no list adapter assigned to 
	 * the list activity).
	 * */
	private ArrayList<AppInfo> getCheckedApps() {
		AppsListAdapter appsListAdapter = (AppsListAdapter) getListAdapter();
		// If there is a list adapter assigned to the list activity
		if (appsListAdapter != null) {
			return appsListAdapter.getCheckedApps();
		} else {
			return new ArrayList<AppInfo>();
		}
	}
	
	public GTasksManager getGTasksManager() {
		return this.gTasksManager;
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
